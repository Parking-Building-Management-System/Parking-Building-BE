package com.smartpark.swp391.modules.manager.dto.staff;

import com.smartpark.swp391.modules.identity.enumType.UserStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ManagerStaffUpdateRequest(
    @NotBlank @Size(max = 255) String fullName, @Size(max = 20) String phone, UserStatus status) {}
