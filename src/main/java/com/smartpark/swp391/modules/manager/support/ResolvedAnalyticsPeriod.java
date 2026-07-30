package com.smartpark.swp391.modules.manager.support;

import com.smartpark.swp391.modules.manager.dto.analytics.ManagerAnalyticsOverviewResponse.Period;
import java.time.LocalDate;
import java.time.ZoneId;

public record ResolvedAnalyticsPeriod(
    LocalDate currentFrom,
    LocalDate currentToExclusive,
    LocalDate comparisonFrom,
    LocalDate comparisonToExclusive,
    Period currentPeriod,
    Period comparisonPeriod,
    AnalyticsTrendGranularity trendGranularity,
    ZoneId zoneId) {}
