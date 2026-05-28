package com.smartpark.swp391.modules.admin.dto.permission;

import java.util.UUID;
import lombok.Builder;

@Builder
public record PermissionResponse(
    UUID id,
    String name,
    String scope,
    String module,
    String resource,
    String label,
    String action,
    String description,
    String status) {}
