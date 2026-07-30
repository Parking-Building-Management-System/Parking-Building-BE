package com.smartpark.swp391.modules.manager.service;

import com.smartpark.swp391.modules.manager.dto.analytics.ManagerAnalyticsOverviewResponse;
import com.smartpark.swp391.modules.manager.dto.analytics.ManagerAnalyticsQuery;

public interface ManagerAnalyticsService {
  ManagerAnalyticsOverviewResponse getOverview(ManagerAnalyticsQuery query);
}
