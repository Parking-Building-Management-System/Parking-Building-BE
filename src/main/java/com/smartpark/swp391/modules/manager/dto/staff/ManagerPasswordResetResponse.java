package com.smartpark.swp391.modules.manager.dto.staff;

import com.smartpark.swp391.modules.identity.enumType.StaffPasswordResetStatus;
import com.smartpark.swp391.modules.identity.enumType.UserStatus;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Builder;

@Builder
public record ManagerPasswordResetResponse(
    UUID id,
    UUID staffId,
    String staffFullName,
    String staffUsername,
    UserStatus staffStatus,
    String requestedEmail,
    LocalDateTime requestedAt,
    StaffPasswordResetStatus status,
    LocalDateTime reviewedAt,
    UUID reviewedById,
    String reviewedByName,
    LocalDateTime completedAt,
    String rejectionReason) {}
