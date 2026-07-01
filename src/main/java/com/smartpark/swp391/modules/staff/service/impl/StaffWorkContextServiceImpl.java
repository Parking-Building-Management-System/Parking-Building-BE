package com.smartpark.swp391.modules.staff.service.impl;

import com.smartpark.swp391.common.exception.ApiException;
import com.smartpark.swp391.common.exception.ErrorCode;
import com.smartpark.swp391.modules.identity.entity.Device;
import com.smartpark.swp391.modules.identity.entity.Session;
import com.smartpark.swp391.modules.identity.enumType.DeviceStatus;
import com.smartpark.swp391.modules.identity.repository.SessionRepository;
import com.smartpark.swp391.modules.operation.entity.Kiosk;
import com.smartpark.swp391.modules.operation.enumType.KioskStatus;
import com.smartpark.swp391.modules.operation.repository.KioskStaffRepository;
import com.smartpark.swp391.modules.parking.entity.Parking;
import com.smartpark.swp391.modules.staff.dto.StaffResolvedContext;
import com.smartpark.swp391.modules.staff.dto.StaffWorkContextResponse;
import com.smartpark.swp391.modules.staff.service.StaffWorkContextService;
import java.time.LocalDateTime;
import java.util.Optional;
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
public class StaffWorkContextServiceImpl implements StaffWorkContextService {

  SessionRepository sessionRepository;
  KioskStaffRepository kioskStaffRepository;

  @Override
  @Transactional(readOnly = true)
  public Optional<StaffWorkContextResponse> resolveCurrentContext() {
    Jwt jwt = currentJwt();
    if (jwt == null) {
      return Optional.empty();
    }
    return resolveResolvedContext(
            claimUuid(jwt, "session_id"), claimUuid(jwt, "user_id"), claimUuid(jwt, "tenant_id"))
        .map(this::toPublicResponse);
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<StaffResolvedContext> resolveCurrentResolvedContext() {
    Jwt jwt = currentJwt();
    if (jwt == null) {
      return Optional.empty();
    }
    return resolveResolvedContext(
        claimUuid(jwt, "session_id"), claimUuid(jwt, "user_id"), claimUuid(jwt, "tenant_id"));
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<StaffWorkContextResponse> resolveContext(
      UUID sessionId, UUID userId, UUID tenantId) {
    return resolveResolvedContext(sessionId, userId, tenantId).map(this::toPublicResponse);
  }

  private Optional<StaffResolvedContext> resolveResolvedContext(
      UUID sessionId, UUID userId, UUID tenantId) {
    if (sessionId == null || userId == null || tenantId == null) {
      return Optional.empty();
    }

    Session session =
        sessionRepository
            .findActiveByIdAndUserIdWithDevice(sessionId, userId, LocalDateTime.now())
            .orElse(null);
    if (session == null) {
      return Optional.empty();
    }

    Device device = session.getDevice();
    if (device.getStatus() != DeviceStatus.APPROVED || device.getKiosk() == null) {
      return Optional.empty();
    }
    if (device.getExpiresAt() != null && device.getExpiresAt().isBefore(LocalDateTime.now())) {
      return Optional.empty();
    }

    Kiosk kiosk = device.getKiosk();
    if (kiosk.getStatus() != KioskStatus.ACTIVE || !tenantId.equals(kiosk.getTenant().getId())) {
      return Optional.empty();
    }
    if (!kioskStaffRepository.existsActiveAssignment(tenantId, kiosk.getId(), userId)) {
      return Optional.empty();
    }

    Parking parking = kiosk.getParking();
    return Optional.of(
        StaffResolvedContext.builder()
            .tenantId(tenantId)
            .staffId(userId)
            .kioskId(kiosk.getId())
            .kioskName(kiosk.getName())
            .kioskType(kiosk.getType())
            .parkingId(parking.getId())
            .parkingName(parking.getName())
            .build());
  }

  @Override
  @Transactional(readOnly = true)
  public StaffWorkContextResponse requireCurrentContext() {
    return resolveCurrentContext()
        .orElseThrow(() -> new ApiException(ErrorCode.FORBIDDEN_ACTION, "KIOSK_CONTEXT_REQUIRED"));
  }

  @Override
  @Transactional(readOnly = true)
  public StaffResolvedContext requireCurrentResolvedContext() {
    return resolveCurrentResolvedContext()
        .orElseThrow(() -> new ApiException(ErrorCode.FORBIDDEN_ACTION, "KIOSK_CONTEXT_REQUIRED"));
  }

  private StaffWorkContextResponse toPublicResponse(StaffResolvedContext context) {
    return StaffWorkContextResponse.builder()
        .kioskId(context.kioskId())
        .kioskName(context.kioskName())
        .kioskType(context.kioskType())
        .parkingId(context.parkingId())
        .parkingName(context.parkingName())
        .build();
  }

  private Jwt currentJwt() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth instanceof JwtAuthenticationToken jwtAuth) {
      return jwtAuth.getToken();
    }
    return null;
  }

  private UUID claimUuid(Jwt jwt, String claimName) {
    String value = jwt.getClaimAsString(claimName);
    if (value == null) {
      return null;
    }
    try {
      return UUID.fromString(value);
    } catch (IllegalArgumentException e) {
      return null;
    }
  }
}
