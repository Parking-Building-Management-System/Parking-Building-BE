package com.smartpark.swp391.modules.admin.dto.health;

import java.time.Instant;
import lombok.Builder;

@Builder
public record ServiceHealthResponse(
    String name, String status, Long latencyMs, Instant lastCheckedAt, String message) {}
