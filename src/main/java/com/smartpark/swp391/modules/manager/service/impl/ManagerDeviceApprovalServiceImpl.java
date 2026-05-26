package com.smartpark.swp391.modules.manager.service.impl;

import com.smartpark.swp391.common.exception.ApiException;
import com.smartpark.swp391.common.exception.ErrorCode;
import com.smartpark.swp391.infrastructure.tenant.TenantContext;
import com.smartpark.swp391.modules.identity.entity.Device;
import com.smartpark.swp391.modules.identity.entity.User;
import com.smartpark.swp391.modules.identity.enumType.DeviceStatus;
import com.smartpark.swp391.modules.identity.repository.DeviceRepository;
import com.smartpark.swp391.modules.manager.dto.device.DeviceApprovalRequest;
import com.smartpark.swp391.modules.manager.dto.device.ManagerDeviceResponse;
import com.smartpark.swp391.modules.manager.service.ManagerDeviceApprovalService;
import com.smartpark.swp391.modules.operation.entity.Kiosk;
import com.smartpark.swp391.modules.operation.repository.KioskRepository;
import com.smartpark.swp391.modules.operation.repository.KioskStaffRepository;
import com.smartpark.swp391.modules.parking.entity.Parking;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class ManagerDeviceApprovalServiceImpl implements ManagerDeviceApprovalService {

  DeviceRepository deviceRepository;
  KioskRepository kioskRepository;
  KioskStaffRepository kioskStaffRepository;

  @Override
  @Transactional(readOnly = true)
  public List<ManagerDeviceResponse> getPendingApprovals() {
    return deviceRepository.findByTenantIdAndStatus(currentTenantId(), DeviceStatus.PENDING).stream()
        .map(this::toResponse)
        .toList();
  }

  @Override
  @Transactional
  public ManagerDeviceResponse approve(UUID id, DeviceApprovalRequest request, UUID managerUserId) {
    UUID tenantId = currentTenantId();
    LocalDateTime now = LocalDateTime.now();
    validateExpiresAt(request.expiresAt(), now);
    Device device = getTenantDevice(id, tenantId);
    Kiosk kiosk =
        kioskRepository
            .findTenantKioskById(request.kioskId(), tenantId)
            .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Kiosk not found"));

    if (!kioskStaffRepository.existsActiveAssignment(tenantId, kiosk.getId(), device.getUser().getId())) {
      throw new ApiException(
          ErrorCode.FORBIDDEN_ACTION, "STAFF_NOT_ASSIGNED_TO_KIOSK");
    }

    device.setStatus(DeviceStatus.APPROVED);
    device.setKiosk(kiosk);
    device.setApprovedBy(managerUserId);
    device.setApprovedAt(now);
    device.setExpiresAt(request.expiresAt());
    return toResponse(deviceRepository.save(device));
  }

  @Override
  @Transactional
  public ManagerDeviceResponse reject(UUID id) {
    Device device = getTenantDevice(id, currentTenantId());
    device.setStatus(DeviceStatus.REJECTED);
    device.setKiosk(null);
    return toResponse(deviceRepository.save(device));
  }

  @Override
  @Transactional
  public ManagerDeviceResponse revoke(UUID id) {
    Device device = getTenantDevice(id, currentTenantId());
    device.setStatus(DeviceStatus.SUSPENDED);
    device.setKiosk(null);
    return toResponse(deviceRepository.save(device));
  }

  private Device getTenantDevice(UUID id, UUID tenantId) {
    return deviceRepository
        .findTenantDeviceById(id, tenantId)
        .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Device not found"));
  }

  private void validateExpiresAt(LocalDateTime expiresAt, LocalDateTime now) {
    if (expiresAt != null && !expiresAt.isAfter(now)) {
      throw new ApiException(
          ErrorCode.INVALID_INPUT, "DEVICE_APPROVAL_EXPIRES_AT_MUST_BE_FUTURE");
    }
  }

  private ManagerDeviceResponse toResponse(Device device) {
    User staff = device.getUser();
    Kiosk kiosk = device.getKiosk();
    Parking parking = kiosk == null ? null : kiosk.getParking();
    return ManagerDeviceResponse.builder()
        .id(device.getId())
        .staffId(staff.getId())
        .staffUsername(staff.getUsername())
        .staffFullName(staff.getFullName())
        .fingerprint(device.getFingerprint())
        .label(device.getLabel())
        .status(device.getStatus())
        .kioskId(kiosk == null ? null : kiosk.getId())
        .kioskName(kiosk == null ? null : kiosk.getName())
        .parkingId(parking == null ? null : parking.getId())
        .parkingName(parking == null ? null : parking.getName())
        .approvedAt(device.getApprovedAt())
        .approvedBy(device.getApprovedBy())
        .expiresAt(device.getExpiresAt())
        .createdAt(device.getCreatedAt())
        .build();
  }

  private UUID currentTenantId() {
    return TenantContext.getTenantId()
        .orElseThrow(() -> new ApiException(ErrorCode.UNAUTHENTICATED));
  }
}
