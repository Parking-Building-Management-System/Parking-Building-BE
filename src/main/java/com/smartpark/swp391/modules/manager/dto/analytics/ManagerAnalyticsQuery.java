package com.smartpark.swp391.modules.manager.dto.analytics;

import com.smartpark.swp391.modules.manager.enumType.ManagerAnalyticsComparison;
import com.smartpark.swp391.modules.manager.enumType.ManagerAnalyticsPeriodType;
import java.time.LocalDate;
import java.util.UUID;

public record ManagerAnalyticsQuery(
    UUID parkingId,
    ManagerAnalyticsPeriodType periodType,
    LocalDate date,
    Integer year,
    Integer month,
    Integer quarter,
    LocalDate from,
    LocalDate to,
    ManagerAnalyticsComparison comparison,
    UUID vehicleTypeId) {}
