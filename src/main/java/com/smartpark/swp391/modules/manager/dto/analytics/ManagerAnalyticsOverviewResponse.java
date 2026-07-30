package com.smartpark.swp391.modules.manager.dto.analytics;

import com.smartpark.swp391.modules.manager.enumType.AnalyticsChangeDirection;
import com.smartpark.swp391.modules.manager.enumType.ManagerAnalyticsComparison;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record ManagerAnalyticsOverviewResponse(
    Period period,
    Period comparisonPeriod,
    ManagerAnalyticsComparison comparison,
    UUID selectedVehicleTypeId,
    CountComparison entries,
    CountComparison exits,
    Revenue revenue,
    CurrentOccupancy currentOccupancy,
    AverageOccupancy averageOccupancy,
    List<TrafficPoint> trafficTrend,
    List<TrafficPoint> comparisonTrafficTrend,
    List<VehicleTypeAnalytics> byVehicleType,
    List<PeakHour> peakHours) {

  public record Period(OffsetDateTime from, OffsetDateTime to, String label, String timeZone) {}

  public record CountComparison(
      long current,
      long comparison,
      long change,
      BigDecimal changePercent,
      AnalyticsChangeDirection direction) {}

  public record DecimalComparison(
      BigDecimal current,
      BigDecimal comparison,
      BigDecimal change,
      BigDecimal changePercent,
      AnalyticsChangeDirection direction) {}

  public record Revenue(
      DecimalComparison metric,
      RevenueBreakdown currentBreakdown,
      RevenueBreakdown comparisonBreakdown) {}

  public record RevenueBreakdown(
      BigDecimal payos,
      BigDecimal parkingCash,
      BigDecimal surchargeCash,
      BigDecimal penaltyCash,
      BigDecimal lostCardFine,
      BigDecimal total) {}

  public record CurrentOccupancy(
      long totalUsableSlots,
      long occupiedSlots,
      long availableSlots,
      long reservedSlots,
      BigDecimal occupancyRate,
      boolean comparisonAvailable,
      String note) {}

  public record AverageOccupancy(
      DecimalComparison averageOccupancyRate,
      BigDecimal currentAverageActiveSessions,
      BigDecimal comparisonAverageActiveSessions,
      long currentUsableCapacity,
      boolean approximation,
      String denominator,
      String note) {}

  public record TrafficPoint(OffsetDateTime bucketStart, String label, long entries, long exits) {}

  public record VehicleTypeAnalytics(
      UUID vehicleTypeId,
      String code,
      String name,
      CountComparison entries,
      CountComparison exits,
      DecimalComparison revenue,
      CurrentOccupancy currentOccupancy,
      AverageOccupancy averageOccupancy) {}

  public record PeakHour(
      UUID vehicleTypeId,
      String vehicleTypeCode,
      String vehicleTypeName,
      int hour,
      String label,
      long entryCount,
      long exitCount) {}
}
