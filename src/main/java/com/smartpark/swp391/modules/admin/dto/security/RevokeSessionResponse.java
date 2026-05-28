package com.smartpark.swp391.modules.admin.dto.security;

import java.util.UUID;
import lombok.Builder;

@Builder
public record RevokeSessionResponse(UUID sessionId, boolean revoked) {}
