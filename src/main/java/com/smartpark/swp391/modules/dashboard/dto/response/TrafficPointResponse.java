package com.smartpark.swp391.modules.dashboard.dto.response;

import java.time.LocalDateTime;
import lombok.Builder;

@Builder
public record TrafficPointResponse(
    LocalDateTime time, long requestCount, long errorCount, double averageDurationMs) {}
