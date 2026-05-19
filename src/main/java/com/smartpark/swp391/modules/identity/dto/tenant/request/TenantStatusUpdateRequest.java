package com.smartpark.swp391.modules.identity.dto.tenant.request;

import com.smartpark.swp391.modules.identity.enumType.TenantStatus;
import jakarta.validation.constraints.NotNull;

public record TenantStatusUpdateRequest(
    @NotNull(message = "Trạng thái tenant không được để trống") TenantStatus status) {}
