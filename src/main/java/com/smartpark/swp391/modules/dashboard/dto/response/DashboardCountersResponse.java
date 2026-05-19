package com.smartpark.swp391.modules.dashboard.dto.response;

import lombok.Builder;

@Builder
public record DashboardCountersResponse(long activeTenantCount, long activeParkingCount) {}
