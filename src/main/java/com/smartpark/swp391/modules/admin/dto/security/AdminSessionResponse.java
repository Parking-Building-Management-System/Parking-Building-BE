package com.smartpark.swp391.modules.admin.dto.security;

import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Builder;

@Builder
public record AdminSessionResponse(
    UUID id,
    UUID userId,
    String username,
    String role,
    UUID tenantId,
    String tenantName,
    UUID deviceId,
    String deviceLabel,
    String status,
    LocalDateTime createdAt,
    LocalDateTime expiresAt,
    LocalDateTime revokedAt) {}
