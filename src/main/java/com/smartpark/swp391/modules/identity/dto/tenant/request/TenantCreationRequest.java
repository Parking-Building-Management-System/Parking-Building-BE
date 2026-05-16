package com.smartpark.swp391.modules.identity.dto.tenant.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record TenantCreationRequest(
    @NotBlank(message = "Tên công ty không được để trống") String name,
    @NotBlank(message = "Slug không được để trống")
        @Pattern(
            regexp = "^[a-z0-9-]+$",
            message = "Slug chỉ được chứa chữ thường, số và dấu gạch ngang")
        String slug,
    @NotBlank(message = "Email không được để trống") @Email(message = "Email không đúng định dạng")
        String emailContact) {}
