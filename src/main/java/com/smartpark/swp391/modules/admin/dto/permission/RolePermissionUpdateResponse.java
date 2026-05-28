package com.smartpark.swp391.modules.admin.dto.permission;

import java.util.UUID;
import lombok.Builder;

@Builder
public record RolePermissionUpdateResponse(UUID roleId, int permissionCount) {}
