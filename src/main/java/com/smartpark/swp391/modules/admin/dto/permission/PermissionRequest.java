package com.smartpark.swp391.modules.admin.dto.permission;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PermissionRequest(
    @NotBlank @Size(max = 100) String scope,
    @NotBlank @Size(max = 100) String module,
    @NotBlank @Size(max = 100) String resource,
    @NotBlank @Size(max = 255) String label,
    @NotBlank @Size(max = 50) String action,
    @Size(max = 2000) String description,
    @Size(max = 20) String status) {}
