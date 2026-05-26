package com.smartpark.swp391.modules.manager.dto.kiosk;

import com.smartpark.swp391.modules.operation.enumType.KioskStatus;
import com.smartpark.swp391.modules.operation.enumType.KioskType;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Builder;

@Builder
public record ManagerKioskResponse(
    UUID id,
    UUID parkingId,
    String parkingName,
    String code,
    String name,
    KioskType type,
    KioskStatus status,
    long assignedStaffCount,
    LocalDateTime lastHeartbeatAt,
    LocalDateTime createdAt,
    LocalDateTime updatedAt) {}
