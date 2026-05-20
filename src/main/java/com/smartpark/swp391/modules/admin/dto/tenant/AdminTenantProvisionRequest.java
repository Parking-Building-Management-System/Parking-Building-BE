package com.smartpark.swp391.modules.admin.dto.tenant;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminTenantProvisionRequest(
    @NotBlank @Size(max = 255) String companyName,
    @NotBlank @Email @Size(max = 255) String managerEmail,
    @NotBlank @Size(min = 8, max = 72) String initialPassword) {}
