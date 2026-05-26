package com.smartpark.swp391.modules.manager.dto.staff;

import com.smartpark.swp391.modules.identity.enumType.UserStatus;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Builder;

@Builder
public record ManagerStaffResponse(
    UUID id,
    String username,
    String fullName,
    String phone,
    UserStatus status,
    LocalDateTime createdAt,
    LocalDateTime updatedAt) {}
