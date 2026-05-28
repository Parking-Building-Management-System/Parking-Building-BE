package com.smartpark.swp391.modules.admin.dto.security;

import java.util.UUID;
import lombok.Builder;

@Builder
public record ForceLogoutResponse(UUID userId, int revokedSessionCount) {}
