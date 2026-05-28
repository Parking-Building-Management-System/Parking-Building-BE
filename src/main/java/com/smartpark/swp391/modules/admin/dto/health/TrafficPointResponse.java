package com.smartpark.swp391.modules.admin.dto.health;

import java.time.Instant;
import lombok.Builder;

@Builder
public record TrafficPointResponse(
    Instant timestamp, long requestCount, long errorCount, long avgLatencyMs) {}
