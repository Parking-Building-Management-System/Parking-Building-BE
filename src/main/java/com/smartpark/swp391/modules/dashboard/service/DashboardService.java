package com.smartpark.swp391.modules.dashboard.service;

import com.smartpark.swp391.modules.dashboard.dto.response.DashboardCountersResponse;
import com.smartpark.swp391.modules.dashboard.dto.response.TrafficChartResponse;
import com.smartpark.swp391.modules.dashboard.enumType.TrafficBucket;
import java.time.LocalDateTime;

public interface DashboardService {

  DashboardCountersResponse getCounters();

  TrafficChartResponse getTraffic(LocalDateTime from, LocalDateTime to, TrafficBucket bucket);
}
