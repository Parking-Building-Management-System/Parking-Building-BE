package com.smartpark.swp391.modules.dashboard.dto.response;

import com.smartpark.swp391.modules.dashboard.enumType.TrafficBucket;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;

@Builder
public record TrafficChartResponse(
    TrafficBucket bucket,
    LocalDateTime from,
    LocalDateTime to,
    List<TrafficPointResponse> points) {}
