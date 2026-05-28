package com.smartpark.swp391.modules.admin.dto.health;

import java.time.Instant;
import lombok.Builder;

@Builder
public record SystemHealthSummaryResponse(
    String status,
    long uptimeSeconds,
    long totalRequests,
    double errorRate,
    long avgLatencyMs,
    long activeTenants,
    long activeSessions,
    Instant timestamp) {}
