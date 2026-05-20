package com.smartpark.swp391.modules.admin.dto.dashboard;

import java.time.LocalDateTime;
import lombok.Builder;

@Builder
public record AdminTrafficPointResponse(
    LocalDateTime bucketStart, long requestCount, long errorCount, double averageDurationMs) {}
