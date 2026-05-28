package com.smartpark.swp391.modules.admin.dto.permission;

import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

public record RolePermissionUpdateRequest(@NotNull List<UUID> permissionIds) {}
