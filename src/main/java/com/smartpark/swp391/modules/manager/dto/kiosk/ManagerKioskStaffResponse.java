package com.smartpark.swp391.modules.manager.dto.kiosk;

import com.smartpark.swp391.modules.identity.enumType.UserStatus;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Builder;

@Builder
public record ManagerKioskStaffResponse(
    UUID assignmentId,
    UUID staffId,
    String username,
    String fullName,
    String phone,
    UserStatus status,
    LocalDateTime assignedAt,
    boolean active) {}
