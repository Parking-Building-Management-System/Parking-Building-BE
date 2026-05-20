package com.smartpark.swp391.modules.admin.dto.dashboard;

import java.util.List;
import lombok.Builder;

@Builder
public record AdminDashboardStatsResponse(
    long activeTenantCount, long parkingCount, List<AdminTrafficPointResponse> traffic) {}
