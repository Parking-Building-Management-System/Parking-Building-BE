package com.smartpark.swp391.modules.admin.dto.permission;

import java.util.List;
import lombok.Builder;

@Builder
public record PermissionLabelNode(String label, List<PermissionActionNode> actions) {}
