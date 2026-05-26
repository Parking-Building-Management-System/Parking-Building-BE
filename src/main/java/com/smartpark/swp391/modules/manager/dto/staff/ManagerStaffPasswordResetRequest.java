package com.smartpark.swp391.modules.manager.dto.staff;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ManagerStaffPasswordResetRequest(@NotBlank @Size(max = 255) String newPassword) {}
