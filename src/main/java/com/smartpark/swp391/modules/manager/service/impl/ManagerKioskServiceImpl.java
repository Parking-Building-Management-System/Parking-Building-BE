package com.smartpark.swp391.modules.manager.service.impl;

import com.smartpark.swp391.common.exception.ApiException;
import com.smartpark.swp391.common.exception.ErrorCode;
import com.smartpark.swp391.infrastructure.tenant.TenantContext;
import com.smartpark.swp391.modules.identity.entity.Tenant;
import com.smartpark.swp391.modules.identity.entity.User;
import com.smartpark.swp391.modules.identity.repository.TenantRepository;
import com.smartpark.swp391.modules.identity.repository.UserRepository;
import com.smartpark.swp391.modules.manager.dto.kiosk.ManagerKioskRequest;
import com.smartpark.swp391.modules.manager.dto.kiosk.ManagerKioskResponse;
import com.smartpark.swp391.modules.manager.dto.kiosk.ManagerKioskStaffResponse;
import com.smartpark.swp391.modules.manager.dto.kiosk.ManagerKioskStatusRequest;
import com.smartpark.swp391.modules.manager.dto.kiosk.ManagerKioskUpdateRequest;
import com.smartpark.swp391.modules.manager.service.ManagerKioskService;
import com.smartpark.swp391.modules.operation.entity.Kiosk;
import com.smartpark.swp391.modules.operation.entity.KioskStaff;
import com.smartpark.swp391.modules.operation.enumType.KioskStatus;
import com.smartpark.swp391.modules.operation.enumType.KioskType;
import com.smartpark.swp391.modules.operation.repository.KioskRepository;
import com.smartpark.swp391.modules.operation.repository.KioskStaffRepository;
import com.smartpark.swp391.modules.parking.entity.Parking;
import com.smartpark.swp391.modules.parking.repository.ParkingRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class ManagerKioskServiceImpl implements ManagerKioskService {

  private static final String STAFF_ROLE = "STAFF";

  KioskRepository kioskRepository;
  KioskStaffRepository kioskStaffRepository;
  ParkingRepository parkingRepository;
  UserRepository userRepository;
  TenantRepository tenantRepository;

  @Override
  @Transactional(readOnly = true)
  public List<ManagerKioskResponse> getKiosks(
      UUID parkingId, KioskStatus status, KioskType type) {
    UUID tenantId = currentTenantId();
    if (parkingId != null) {
      assertParking(tenantId, parkingId);
    }
    List<Kiosk> kiosks = kioskRepository.findTenantKiosks(tenantId, parkingId, status, type);
    Map<UUID, Long> assignedStaffCounts = activeAssignmentCounts(tenantId, kiosks);
    return kiosks.stream()
        .map(kiosk -> toResponse(kiosk, assignedStaffCounts.getOrDefault(kiosk.getId(), 0L)))
        .toList();
  }

  @Override
  @Transactional
  public ManagerKioskResponse createKiosk(ManagerKioskRequest request) {
    UUID tenantId = currentTenantId();
    Tenant tenant = tenantRepository.getReferenceById(tenantId);
    Parking parking = assertParking(tenantId, request.parkingId());
    String name = request.name().trim();
    String code = uniqueCode(tenantId, parking.getId(), parking.getCode(), name);

    Kiosk kiosk =
        kioskRepository.save(
            Kiosk.builder()
                .tenant(tenant)
                .parking(parking)
                .code(code)
                .name(name)
                .type(request.type())
                .status(request.status())
                .isDeleted(false)
                .build());
    return toResponse(kiosk, 0);
  }

  @Override
  @Transactional(readOnly = true)
  public ManagerKioskResponse getKiosk(UUID id) {
    Kiosk kiosk = getTenantKiosk(id);
    return toResponse(kiosk, activeAssignmentCount(kiosk.getId()));
  }

  @Override
  @Transactional
  public ManagerKioskResponse updateKiosk(UUID id, ManagerKioskUpdateRequest request) {
    Kiosk kiosk = getTenantKiosk(id);
    kiosk.setName(request.name().trim());
    kiosk.setType(request.type());
    kiosk.setStatus(request.status());
    Kiosk saved = kioskRepository.save(kiosk);
    return toResponse(saved, activeAssignmentCount(saved.getId()));
  }

  @Override
  @Transactional
  public ManagerKioskResponse updateStatus(UUID id, ManagerKioskStatusRequest request) {
    Kiosk kiosk = getTenantKiosk(id);
    kiosk.setStatus(request.status());
    Kiosk saved = kioskRepository.save(kiosk);
    return toResponse(saved, activeAssignmentCount(saved.getId()));
  }

  @Override
  @Transactional
  public void deleteKiosk(UUID id) {
    Kiosk kiosk = getTenantKiosk(id);
    kiosk.setDeleted(true);
    kiosk.setStatus(KioskStatus.INACTIVE);
    kioskRepository.save(kiosk);
  }

  @Override
  @Transactional(readOnly = true)
  public List<ManagerKioskStaffResponse> getStaff(UUID kioskId) {
    Kiosk kiosk = getTenantKiosk(kioskId);
    return kioskStaffRepository.findActiveByTenantAndKiosk(currentTenantId(), kiosk.getId()).stream()
        .map(this::toStaffResponse)
        .toList();
  }

  @Override
  @Transactional
  public ManagerKioskStaffResponse assignStaff(UUID kioskId, UUID staffId) {
    Kiosk kiosk = getTenantKiosk(kioskId);
    User staff =
        userRepository
            .findTenantUserByIdAndRole(staffId, currentTenantId(), STAFF_ROLE)
            .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Staff not found"));

    KioskStaff assignment =
        kioskStaffRepository
            .findNoShiftAssignment(currentTenantId(), kiosk.getId(), staff.getId())
            .orElseGet(
                () ->
                    KioskStaff.builder()
                        .tenant(tenantRepository.getReferenceById(currentTenantId()))
                        .kiosk(kiosk)
                        .staffUser(staff)
                        .shift(null)
                        .assignedAt(LocalDateTime.now())
                        .build());
    assignment.setActive(true);
    return toStaffResponse(kioskStaffRepository.save(assignment));
  }

  @Override
  @Transactional
  public void unassignStaff(UUID kioskId, UUID staffId) {
    getTenantKiosk(kioskId);
    kioskStaffRepository.deactivateAssignments(currentTenantId(), kioskId, staffId);
  }

  private Parking assertParking(UUID tenantId, UUID parkingId) {
    return parkingRepository
        .findByIdAndTenantIdAndIsDeletedFalse(parkingId, tenantId)
        .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Parking not found"));
  }

  private Kiosk getTenantKiosk(UUID id) {
    return kioskRepository
        .findTenantKioskById(id, currentTenantId())
        .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Kiosk not found"));
  }

  private String uniqueCode(UUID tenantId, UUID parkingId, String parkingCode, String name) {
    String base =
        (parkingCode + "-" + name)
            .toUpperCase(Locale.ROOT)
            .replaceAll("[^A-Z0-9]+", "-")
            .replaceAll("(^-|-$)", "");
    if (base.length() > 42) {
      base = base.substring(0, 42).replaceAll("-$", "");
    }
    String candidate = base.isBlank() ? "KIOSK" : base;
    int suffix = 1;
    while (kioskRepository.existsByTenantIdAndParkingIdAndCodeIgnoreCase(
        tenantId, parkingId, candidate)) {
      String tail = "-" + suffix++;
      candidate = candidate.substring(0, Math.min(candidate.length(), 50 - tail.length())) + tail;
    }
    return candidate;
  }

  private Map<UUID, Long> activeAssignmentCounts(UUID tenantId, List<Kiosk> kiosks) {
    if (kiosks.isEmpty()) {
      return Map.of();
    }
    List<UUID> kioskIds = kiosks.stream().map(Kiosk::getId).toList();
    return kioskStaffRepository.countActiveAssignmentsByKioskIds(tenantId, kioskIds).stream()
        .collect(
            Collectors.toMap(
                KioskStaffRepository.KioskStaffCountView::getKioskId,
                KioskStaffRepository.KioskStaffCountView::getAssignedStaffCount,
                Long::sum));
  }

  private long activeAssignmentCount(UUID kioskId) {
    return kioskStaffRepository.countActiveAssignments(currentTenantId(), kioskId);
  }

  private ManagerKioskResponse toResponse(Kiosk kiosk, long assignedStaffCount) {
    Parking parking = kiosk.getParking();
    return ManagerKioskResponse.builder()
        .id(kiosk.getId())
        .parkingId(parking.getId())
        .parkingName(parking.getName())
        .code(kiosk.getCode())
        .name(kiosk.getName())
        .type(kiosk.getType())
        .status(kiosk.getStatus())
        .assignedStaffCount(assignedStaffCount)
        .lastHeartbeatAt(kiosk.getLastHeartbeatAt())
        .createdAt(kiosk.getCreatedAt())
        .updatedAt(kiosk.getUpdatedAt())
        .build();
  }

  private ManagerKioskStaffResponse toStaffResponse(KioskStaff assignment) {
    User staff = assignment.getStaffUser();
    return ManagerKioskStaffResponse.builder()
        .assignmentId(assignment.getId())
        .staffId(staff.getId())
        .username(staff.getUsername())
        .fullName(staff.getFullName())
        .phone(staff.getPhone())
        .status(staff.getStatus())
        .assignedAt(assignment.getAssignedAt())
        .active(assignment.isActive())
        .build();
  }

  private UUID currentTenantId() {
    return TenantContext.getTenantId()
        .orElseThrow(() -> new ApiException(ErrorCode.UNAUTHENTICATED));
  }
}
