package com.smartpark.swp391.modules.admin.dto.tenant;

import com.smartpark.swp391.modules.identity.enumType.TenantStatus;
import java.util.UUID;
import lombok.Builder;

@Builder
public record AdminTenantStatusResponse(UUID id, TenantStatus status) {}
