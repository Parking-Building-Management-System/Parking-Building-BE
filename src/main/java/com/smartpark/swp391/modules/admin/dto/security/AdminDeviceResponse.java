package com.smartpark.swp391.modules.admin.dto.security;

import com.smartpark.swp391.modules.identity.enumType.DeviceStatus;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Builder;

@Builder
public record AdminDeviceResponse(
    UUID id,
    UUID userId,
    String username,
    UUID tenantId,
    String tenantName,
    String fingerprint,
    String label,
    DeviceStatus status,
    UUID kioskId,
    String kioskName,
    UUID approvedBy,
    LocalDateTime approvedAt,
    LocalDateTime expiresAt,
    LocalDateTime createdAt,
    LocalDateTime updatedAt) {}
