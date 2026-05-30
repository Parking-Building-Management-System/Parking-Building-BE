package com.smartpark.swp391.modules.tenant.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record TenantCreateRequest(
    @NotBlank(message = "Tên không được để trống") 
    String name,

    @NotBlank(message = "Email không được để trống") 
    @Email(message = "Email không đúng định dạng") 
    String email,

    @NotBlank(message = "Thông tin liên hệ không được để trống") 
    String contact
) {}
