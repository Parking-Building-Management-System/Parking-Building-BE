package com.smartpark.swp391.modules.manager.dto.device;

import com.smartpark.swp391.modules.identity.enumType.DeviceStatus;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Builder;

@Builder
public record ManagerDeviceResponse(
    UUID id,
    UUID staffId,
    String staffUsername,
    String staffFullName,
    String fingerprint,
    String label,
    DeviceStatus status,
    UUID kioskId,
    String kioskName,
    UUID parkingId,
    String parkingName,
    LocalDateTime approvedAt,
    UUID approvedBy,
    LocalDateTime expiresAt,
    LocalDateTime createdAt) {}
