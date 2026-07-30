package com.smartpark.swp391.modules.manager.dto.staff;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ManagerPasswordResetRejectRequest(
    @NotBlank @Size(min = 3, max = 1000) String reason) {}
