package com.smartpark.swp391.modules.admin.dto.masterdata;

import java.util.UUID;
import lombok.Builder;

@Builder
public record AdminRoleResponse(UUID id, String name, String desc) {}
