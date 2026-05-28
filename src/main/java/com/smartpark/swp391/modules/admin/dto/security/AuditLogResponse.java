package com.smartpark.swp391.modules.admin.dto.security;

import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Builder;

@Builder
public record AuditLogResponse(
    UUID id,
    UUID tenantId,
    String tenantName,
    UUID actorId,
    String actorUsername,
    String actorRole,
    String action,
    String resourceType,
    UUID resourceId,
    String severity,
    String reason,
    String ipAddress,
    String deviceFingerprint,
    LocalDateTime createdAt) {}
