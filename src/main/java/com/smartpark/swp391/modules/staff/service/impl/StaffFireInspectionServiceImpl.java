package com.smartpark.swp391.modules.staff.service.impl;

import com.smartpark.swp391.common.exception.ApiException;
import com.smartpark.swp391.common.exception.ErrorCode;
import com.smartpark.swp391.infrastructure.storage.dto.PresignedDownload;
import com.smartpark.swp391.infrastructure.storage.dto.PresignedUpload;
import com.smartpark.swp391.infrastructure.storage.service.StorageService;
import com.smartpark.swp391.infrastructure.tenant.TenantContext;
import com.smartpark.swp391.modules.firesafety.entity.FireExtinguisher;
import com.smartpark.swp391.modules.firesafety.entity.FireExtinguisherInspection;
import com.smartpark.swp391.modules.firesafety.enumType.FireExtinguisherStatus;
import com.smartpark.swp391.modules.firesafety.enumType.FireInspectionResult;
import com.smartpark.swp391.modules.firesafety.repository.FireExtinguisherInspectionRepository;
import com.smartpark.swp391.modules.firesafety.repository.FireExtinguisherRepository;
import com.smartpark.swp391.modules.identity.entity.User;
import com.smartpark.swp391.modules.identity.repository.TenantRepository;
import com.smartpark.swp391.modules.identity.repository.UserRepository;
import com.smartpark.swp391.modules.parking.entity.Floor;
import com.smartpark.swp391.modules.parking.entity.Parking;
import com.smartpark.swp391.modules.parking.entity.Zone;
import com.smartpark.swp391.modules.staff.dto.StaffWorkContextResponse;
import com.smartpark.swp391.modules.staff.dto.firesafety.StaffFireInspectionDueResponse;
import com.smartpark.swp391.modules.staff.dto.firesafety.StaffFireInspectionRequest;
import com.smartpark.swp391.modules.staff.dto.firesafety.StaffFireInspectionResponse;
import com.smartpark.swp391.modules.staff.dto.firesafety.StaffInspectionPhotoPresignRequest;
import com.smartpark.swp391.modules.staff.dto.firesafety.StaffInspectionPhotoPresignResponse;
import com.smartpark.swp391.modules.staff.service.StaffFireInspectionService;
import com.smartpark.swp391.modules.staff.service.StaffWorkContextService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class StaffFireInspectionServiceImpl implements StaffFireInspectionService {

  private static final Set<String> ALLOWED_PHOTO_CONTENT_TYPES =
      Set.of("image/jpeg", "image/png", "image/webp");

  FireExtinguisherRepository fireExtinguisherRepository;
  FireExtinguisherInspectionRepository inspectionRepository;
  TenantRepository tenantRepository;
  UserRepository userRepository;
  StaffWorkContextService staffWorkContextService;
  StorageService storageService;

  @Override
  @Transactional(readOnly = true)
  public List<StaffFireInspectionDueResponse> getDueInspections(
      UUID floorId, FireExtinguisherStatus status) {
    StaffWorkContextResponse workContext = staffWorkContextService.requireCurrentContext();
    return fireExtinguisherRepository
        .findDueForStaffParking(
            currentTenantId(),
            workContext.parkingId(),
            floorId,
            status,
            LocalDateTime.now(),
            LocalDate.now().plusDays(30))
        .stream()
        .map(this::toDueResponse)
        .toList();
  }

  @Override
  @Transactional(readOnly = true)
  public StaffInspectionPhotoPresignResponse createPhotoUpload(
      StaffInspectionPhotoPresignRequest request) {
    staffWorkContextService.requireCurrentContext();
    requireInspectionPhotoFile(request.fileName(), request.contentType());
    UUID tenantId = currentTenantId();
    UUID staffId = currentUserId();
    if (staffId == null) {
      throw new ApiException(ErrorCode.UNAUTHENTICATED);
    }

    PresignedUpload upload =
        storageService.createPresignedUpload(
            tenantId, "fire-inspections/" + staffId, request.fileName(), request.contentType());
    return StaffInspectionPhotoPresignResponse.builder()
        .uploadUrl(upload.uploadUrl())
        .objectKey(upload.objectKey())
        .method(upload.method())
        .headers(upload.headers())
        .expiresInSeconds(upload.expiresInSeconds())
        .build();
  }

  @Override
  @Transactional
  public StaffFireInspectionResponse submitInspection(StaffFireInspectionRequest request) {
    StaffWorkContextResponse workContext = staffWorkContextService.requireCurrentContext();
    FireExtinguisher extinguisher =
        fireExtinguisherRepository
            .findByIdAndTenantIdAndDeletedFalse(request.fireExtinguisherId(), currentTenantId())
            .orElseThrow(
                () ->
                    new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Fire extinguisher not found"));
    if (!extinguisher.getParking().getId().equals(workContext.parkingId())) {
      throw new ApiException(ErrorCode.FORBIDDEN_ACTION, "EXTINGUISHER_NOT_IN_KIOSK_PARKING");
    }
    String photoObjectKey = normalizePhotoObjectKey(request.photoObjectKey());

    LocalDateTime now = LocalDateTime.now();
    FireExtinguisherInspection inspection =
        FireExtinguisherInspection.builder()
            .tenant(tenantRepository.getReferenceById(currentTenantId()))
            .fireExtinguisher(extinguisher)
            .inspectedBy(currentUserReference())
            .result(request.result())
            .pressureOk(request.pressureOk())
            .sealOk(request.sealOk())
            .locationOk(request.locationOk())
            .expiryOk(request.expiryOk())
            .photoUrl(trimToNull(request.photoUrl()))
            .photoObjectKey(photoObjectKey)
            .note(trimToNull(request.note()))
            .inspectedAt(now)
            .nextInspectionAt(request.nextInspectionAt())
            .build();
    FireExtinguisherInspection saved = inspectionRepository.save(inspection);

    extinguisher.setLastInspectedAt(now);
    extinguisher.setNextInspectionAt(request.nextInspectionAt());
    extinguisher.setStatus(statusAfterInspection(extinguisher, request.result()));
    fireExtinguisherRepository.save(extinguisher);

    PhotoDisplay photoDisplay = resolvePhotoDisplay(saved.getPhotoUrl(), saved.getPhotoObjectKey());
    return StaffFireInspectionResponse.builder()
        .inspectionId(saved.getId())
        .fireExtinguisherId(extinguisher.getId())
        .code(extinguisher.getCode())
        .result(saved.getResult())
        .status(extinguisher.getStatus())
        .photoObjectKey(saved.getPhotoObjectKey())
        .photoDisplayUrl(photoDisplay.url())
        .photoUrlExpiresInSeconds(photoDisplay.expiresInSeconds())
        .inspectedAt(saved.getInspectedAt())
        .nextInspectionAt(saved.getNextInspectionAt())
        .build();
  }

  private FireExtinguisherStatus statusAfterInspection(
      FireExtinguisher extinguisher, FireInspectionResult result) {
    return switch (result) {
      case OK ->
          extinguisher.getExpiryDate() != null
                  && extinguisher.getExpiryDate().isBefore(LocalDate.now())
              ? FireExtinguisherStatus.EXPIRED
              : FireExtinguisherStatus.ACTIVE;
      case EXPIRED -> FireExtinguisherStatus.EXPIRED;
      case MISSING -> FireExtinguisherStatus.MISSING;
      case DAMAGED -> FireExtinguisherStatus.DAMAGED;
      case NEEDS_REPLACEMENT -> FireExtinguisherStatus.MAINTENANCE;
    };
  }

  private StaffFireInspectionDueResponse toDueResponse(FireExtinguisher extinguisher) {
    Parking parking = extinguisher.getParking();
    Floor floor = extinguisher.getFloor();
    Zone zone = extinguisher.getZone();
    return StaffFireInspectionDueResponse.builder()
        .fireExtinguisherId(extinguisher.getId())
        .code(extinguisher.getCode())
        .type(extinguisher.getType())
        .status(extinguisher.getStatus())
        .parkingName(parking.getName())
        .floorName(floor.getName())
        .zoneName(zone == null ? null : zone.getName())
        .locationDescription(extinguisher.getLocationDescription())
        .expiryDate(extinguisher.getExpiryDate())
        .lastInspectedAt(extinguisher.getLastInspectedAt())
        .nextInspectionAt(extinguisher.getNextInspectionAt())
        .build();
  }

  private User currentUserReference() {
    UUID userId = currentUserId();
    return userId == null ? null : userRepository.getReferenceById(userId);
  }

  private UUID currentUserId() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth instanceof JwtAuthenticationToken jwtAuth) {
      Jwt jwt = jwtAuth.getToken();
      String value = jwt.getClaimAsString("user_id");
      if (value != null) {
        try {
          return UUID.fromString(value);
        } catch (IllegalArgumentException ignored) {
          return null;
        }
      }
    }
    return null;
  }

  private String trimToNull(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }

  private void requireInspectionPhotoFile(String fileName, String contentType) {
    String normalizedContentType =
        contentType == null ? null : contentType.trim().toLowerCase(Locale.ROOT);
    if (normalizedContentType == null
        || !ALLOWED_PHOTO_CONTENT_TYPES.contains(normalizedContentType)) {
      throw new ApiException(
          ErrorCode.INVALID_INPUT, "contentType must be image/jpeg, image/png, or image/webp");
    }
    String lowerFileName = trimToNull(fileName);
    if (lowerFileName == null) {
      throw new ApiException(ErrorCode.INVALID_INPUT, "File name must not be blank");
    }
    lowerFileName = lowerFileName.toLowerCase(Locale.ROOT);
    boolean extensionMatches =
        ("image/jpeg".equals(normalizedContentType)
                && (lowerFileName.endsWith(".jpg") || lowerFileName.endsWith(".jpeg")))
            || ("image/png".equals(normalizedContentType) && lowerFileName.endsWith(".png"))
            || ("image/webp".equals(normalizedContentType) && lowerFileName.endsWith(".webp"));
    if (!extensionMatches) {
      throw new ApiException(ErrorCode.INVALID_INPUT, "File extension does not match contentType");
    }
  }

  private String normalizePhotoObjectKey(String value) {
    String objectKey = trimToNull(value);
    if (objectKey == null) {
      return null;
    }
    requireInspectionPhotoObjectKey(currentTenantId(), objectKey);
    return objectKey;
  }

  private void requireInspectionPhotoObjectKey(UUID tenantId, String objectKey) {
    String prefix = "tenants/" + tenantId + "/fire-inspections/";
    if (!objectKey.startsWith(prefix) || objectKey.contains("..")) {
      throw new ApiException(
          ErrorCode.FORBIDDEN_ACTION,
          "Photo object key is outside current tenant fire inspections");
    }
  }

  private PhotoDisplay resolvePhotoDisplay(String photoUrl, String photoObjectKey) {
    if (photoObjectKey != null && !photoObjectKey.isBlank()) {
      try {
        PresignedDownload download =
            storageService.createPresignedDownload(currentTenantId(), photoObjectKey);
        return new PhotoDisplay(download.downloadUrl(), download.expiresInSeconds());
      } catch (ApiException e) {
        return new PhotoDisplay(null, null);
      }
    }
    String legacyUrl = trimToNull(photoUrl);
    if (legacyUrl != null && isHttpUrl(legacyUrl)) {
      return new PhotoDisplay(legacyUrl, null);
    }
    return new PhotoDisplay(null, null);
  }

  private boolean isHttpUrl(String value) {
    return value.startsWith("http://") || value.startsWith("https://");
  }

  private UUID currentTenantId() {
    return TenantContext.getTenantId()
        .orElseThrow(() -> new ApiException(ErrorCode.UNAUTHENTICATED));
  }

  private record PhotoDisplay(String url, Long expiresInSeconds) {}
}
