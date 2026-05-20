package com.smartpark.swp391.modules.admin.dto.tenant;

import com.smartpark.swp391.modules.identity.enumType.TenantStatus;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Builder;

@Builder
public record AdminTenantSummaryResponse(
    UUID id,
    String name,
    String slug,
    String emailContact,
    TenantStatus status,
    LocalDateTime createdAt) {}
