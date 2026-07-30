package com.smartpark.swp391.modules.manager.service.impl;

import com.smartpark.swp391.common.exception.ApiException;
import com.smartpark.swp391.common.exception.ErrorCode;
import com.smartpark.swp391.infrastructure.tenant.TenantContext;
import com.smartpark.swp391.modules.manager.dto.analytics.ManagerAnalyticsOverviewResponse;
import com.smartpark.swp391.modules.manager.dto.analytics.ManagerAnalyticsOverviewResponse.AverageOccupancy;
import com.smartpark.swp391.modules.manager.dto.analytics.ManagerAnalyticsOverviewResponse.CountComparison;
import com.smartpark.swp391.modules.manager.dto.analytics.ManagerAnalyticsOverviewResponse.CurrentOccupancy;
import com.smartpark.swp391.modules.manager.dto.analytics.ManagerAnalyticsOverviewResponse.DecimalComparison;
import com.smartpark.swp391.modules.manager.dto.analytics.ManagerAnalyticsOverviewResponse.PeakHour;
import com.smartpark.swp391.modules.manager.dto.analytics.ManagerAnalyticsOverviewResponse.Revenue;
import com.smartpark.swp391.modules.manager.dto.analytics.ManagerAnalyticsOverviewResponse.RevenueBreakdown;
import com.smartpark.swp391.modules.manager.dto.analytics.ManagerAnalyticsOverviewResponse.TrafficPoint;
import com.smartpark.swp391.modules.manager.dto.analytics.ManagerAnalyticsOverviewResponse.VehicleTypeAnalytics;
import com.smartpark.swp391.modules.manager.dto.analytics.ManagerAnalyticsQuery;
import com.smartpark.swp391.modules.manager.enumType.AnalyticsChangeDirection;
import com.smartpark.swp391.modules.manager.enumType.ManagerAnalyticsComparison;
import com.smartpark.swp391.modules.manager.repository.ManagerAnalyticsRepository;
import com.smartpark.swp391.modules.manager.repository.ManagerAnalyticsRepository.AverageOccupancyAggregate;
import com.smartpark.swp391.modules.manager.repository.ManagerAnalyticsRepository.CurrentOccupancyAggregate;
import com.smartpark.swp391.modules.manager.repository.ManagerAnalyticsRepository.PeakHourAggregate;
import com.smartpark.swp391.modules.manager.repository.ManagerAnalyticsRepository.RevenueAggregate;
import com.smartpark.swp391.modules.manager.repository.ManagerAnalyticsRepository.TrafficAggregate;
import com.smartpark.swp391.modules.manager.repository.ManagerAnalyticsRepository.TrafficBucket;
import com.smartpark.swp391.modules.manager.service.ManagerAnalyticsService;
import com.smartpark.swp391.modules.manager.support.AnalyticsTrendGranularity;
import com.smartpark.swp391.modules.manager.support.ManagerAnalyticsPeriodResolver;
import com.smartpark.swp391.modules.manager.support.ResolvedAnalyticsPeriod;
import com.smartpark.swp391.modules.parking.repository.ParkingRepository;
import com.smartpark.swp391.modules.vehicle.entity.VehicleType;
import com.smartpark.swp391.modules.vehicle.repository.VehicleTypeRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class ManagerAnalyticsServiceImpl implements ManagerAnalyticsService {

  private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);
  private static final BigDecimal MONEY_ZERO = BigDecimal.ZERO.setScale(2);
  private static final String CURRENT_OCCUPANCY_NOTE =
      "Current occupancy is an exact snapshot of active-zone slots. Historical comparison is"
          + " unavailable because slot snapshots are not stored.";
  private static final String AVERAGE_OCCUPANCY_NOTE =
      "Approximation from hourly parking-session overlap. Historical rates use the current usable"
          + " slot capacity as the denominator.";
  private static final DateTimeFormatter HOUR_TREND_LABEL =
      DateTimeFormatter.ofPattern("dd MMM HH:mm", Locale.ENGLISH);
  private static final DateTimeFormatter DAY_TREND_LABEL =
      DateTimeFormatter.ofPattern("dd MMM", Locale.ENGLISH);
  private static final DateTimeFormatter MONTH_TREND_LABEL =
      DateTimeFormatter.ofPattern("MMM yyyy", Locale.ENGLISH);

  ManagerAnalyticsRepository analyticsRepository;
  ManagerAnalyticsPeriodResolver periodResolver;
  ParkingRepository parkingRepository;
  VehicleTypeRepository vehicleTypeRepository;

  @Override
  @Transactional(readOnly = true)
  public ManagerAnalyticsOverviewResponse getOverview(ManagerAnalyticsQuery query) {
    UUID tenantId = currentTenantId();
    requireParking(query == null ? null : query.parkingId(), tenantId);
    UUID selectedVehicleTypeId =
        requireActiveVehicleType(query == null ? null : query.vehicleTypeId());
    ResolvedAnalyticsPeriod periods = periodResolver.resolve(query);

    OffsetDateTime currentFrom = periods.currentPeriod().from();
    OffsetDateTime currentTo = periods.currentPeriod().to();
    OffsetDateTime comparisonFrom = periods.comparisonPeriod().from();
    OffsetDateTime comparisonTo = periods.comparisonPeriod().to();

    List<TrafficAggregate> currentTraffic =
        analyticsRepository.traffic(
            tenantId, query.parkingId(), selectedVehicleTypeId, currentFrom, currentTo);
    List<TrafficAggregate> comparisonTraffic =
        analyticsRepository.traffic(
            tenantId, query.parkingId(), selectedVehicleTypeId, comparisonFrom, comparisonTo);
    List<RevenueAggregate> currentRevenue =
        analyticsRepository.revenue(
            tenantId, query.parkingId(), selectedVehicleTypeId, currentFrom, currentTo);
    List<RevenueAggregate> comparisonRevenue =
        analyticsRepository.revenue(
            tenantId, query.parkingId(), selectedVehicleTypeId, comparisonFrom, comparisonTo);
    List<CurrentOccupancyAggregate> occupancy =
        analyticsRepository.currentOccupancy(tenantId, query.parkingId(), selectedVehicleTypeId);
    List<AverageOccupancyAggregate> currentAverage =
        analyticsRepository.averageOccupancy(
            tenantId, query.parkingId(), selectedVehicleTypeId, currentFrom, currentTo);
    List<AverageOccupancyAggregate> comparisonAverage =
        analyticsRepository.averageOccupancy(
            tenantId, query.parkingId(), selectedVehicleTypeId, comparisonFrom, comparisonTo);

    TrafficAggregate currentTrafficTotal = totalTraffic(currentTraffic);
    TrafficAggregate comparisonTrafficTotal = totalTraffic(comparisonTraffic);
    RevenueBreakdown currentBreakdown = revenueBreakdown(currentRevenue, null);
    RevenueBreakdown comparisonBreakdown = revenueBreakdown(comparisonRevenue, null);
    CurrentOccupancyAggregate occupancyTotal = totalOccupancy(occupancy);
    BigDecimal currentAverageSessions = totalAverage(currentAverage);
    BigDecimal comparisonAverageSessions = totalAverage(comparisonAverage);

    List<VehicleType> vehicleTypes = responseVehicleTypes(selectedVehicleTypeId);
    Map<UUID, VehicleType> vehicleTypesById =
        vehicleTypes.stream()
            .collect(
                Collectors.toMap(
                    VehicleType::getId,
                    Function.identity(),
                    (left, right) -> left,
                    LinkedHashMap::new));

    List<TrafficPoint> trafficTrend =
        filledTrend(
            periods,
            periods.currentPeriod().from(),
            periods.currentPeriod().to(),
            analyticsRepository.trafficTrend(
                tenantId,
                query.parkingId(),
                selectedVehicleTypeId,
                currentFrom,
                currentTo,
                periods.trendGranularity()));
    List<TrafficPoint> comparisonTrend =
        filledTrend(
            periods,
            periods.comparisonPeriod().from(),
            periods.comparisonPeriod().to(),
            analyticsRepository.trafficTrend(
                tenantId,
                query.parkingId(),
                selectedVehicleTypeId,
                comparisonFrom,
                comparisonTo,
                periods.trendGranularity()));

    List<PeakHour> peakHours =
        analyticsRepository
            .peakHours(tenantId, query.parkingId(), selectedVehicleTypeId, currentFrom, currentTo)
            .stream()
            .filter(row -> vehicleTypesById.containsKey(row.vehicleTypeId()))
            .map(row -> toPeakHour(row, vehicleTypesById.get(row.vehicleTypeId())))
            .toList();

    return new ManagerAnalyticsOverviewResponse(
        periods.currentPeriod(),
        periods.comparisonPeriod(),
        query.comparison() == null
            ? ManagerAnalyticsComparison.SAME_PERIOD_LAST_YEAR
            : query.comparison(),
        selectedVehicleTypeId,
        countComparison(currentTrafficTotal.entries(), comparisonTrafficTotal.entries()),
        countComparison(currentTrafficTotal.exits(), comparisonTrafficTotal.exits()),
        new Revenue(
            decimalComparison(currentBreakdown.total(), comparisonBreakdown.total()),
            currentBreakdown,
            comparisonBreakdown),
        currentOccupancy(occupancyTotal),
        averageOccupancy(
            currentAverageSessions, comparisonAverageSessions, occupancyTotal.usable()),
        trafficTrend,
        comparisonTrend,
        vehicleTypes.stream()
            .map(
                vehicleType ->
                    vehicleTypeAnalytics(
                        vehicleType,
                        currentTraffic,
                        comparisonTraffic,
                        currentRevenue,
                        comparisonRevenue,
                        occupancy,
                        currentAverage,
                        comparisonAverage))
            .toList(),
        peakHours);
  }

  private VehicleTypeAnalytics vehicleTypeAnalytics(
      VehicleType vehicleType,
      List<TrafficAggregate> currentTraffic,
      List<TrafficAggregate> comparisonTraffic,
      List<RevenueAggregate> currentRevenue,
      List<RevenueAggregate> comparisonRevenue,
      List<CurrentOccupancyAggregate> occupancy,
      List<AverageOccupancyAggregate> currentAverage,
      List<AverageOccupancyAggregate> comparisonAverage) {
    UUID vehicleTypeId = vehicleType.getId();
    TrafficAggregate current = trafficFor(currentTraffic, vehicleTypeId);
    TrafficAggregate previous = trafficFor(comparisonTraffic, vehicleTypeId);
    RevenueBreakdown currentVehicleRevenue = revenueBreakdown(currentRevenue, vehicleTypeId);
    RevenueBreakdown previousVehicleRevenue = revenueBreakdown(comparisonRevenue, vehicleTypeId);
    CurrentOccupancyAggregate currentVehicleOccupancy = occupancyFor(occupancy, vehicleTypeId);
    return new VehicleTypeAnalytics(
        vehicleTypeId,
        vehicleType.getCode(),
        vehicleType.getName(),
        countComparison(current.entries(), previous.entries()),
        countComparison(current.exits(), previous.exits()),
        decimalComparison(currentVehicleRevenue.total(), previousVehicleRevenue.total()),
        currentOccupancy(currentVehicleOccupancy),
        averageOccupancy(
            averageFor(currentAverage, vehicleTypeId),
            averageFor(comparisonAverage, vehicleTypeId),
            currentVehicleOccupancy.usable()));
  }

  private List<TrafficPoint> filledTrend(
      ResolvedAnalyticsPeriod periods,
      OffsetDateTime from,
      OffsetDateTime to,
      List<TrafficBucket> rawBuckets) {
    Map<Instant, TrafficBucket> bucketsByInstant =
        rawBuckets.stream()
            .collect(
                Collectors.toMap(
                    bucket -> bucket.bucketStart().toInstant(),
                    Function.identity(),
                    (left, right) -> left));
    List<TrafficPoint> points = new ArrayList<>();
    ZonedDateTime cursor =
        alignBucket(from.atZoneSameInstant(periods.zoneId()), periods.trendGranularity());
    ZonedDateTime end = to.atZoneSameInstant(periods.zoneId());
    while (cursor.isBefore(end)) {
      TrafficBucket bucket = bucketsByInstant.get(cursor.toInstant());
      points.add(
          new TrafficPoint(
              cursor.toOffsetDateTime(),
              trendLabel(cursor, periods.trendGranularity()),
              bucket == null ? 0 : bucket.entries(),
              bucket == null ? 0 : bucket.exits()));
      cursor = nextBucket(cursor, periods.trendGranularity());
    }
    return points;
  }

  private ZonedDateTime alignBucket(ZonedDateTime value, AnalyticsTrendGranularity granularity) {
    return switch (granularity) {
      case HOUR -> value.withMinute(0).withSecond(0).withNano(0);
      case DAY -> value.toLocalDate().atStartOfDay(value.getZone());
      case WEEK ->
          value
              .toLocalDate()
              .with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY))
              .atStartOfDay(value.getZone());
      case MONTH -> value.toLocalDate().withDayOfMonth(1).atStartOfDay(value.getZone());
    };
  }

  private ZonedDateTime nextBucket(ZonedDateTime value, AnalyticsTrendGranularity granularity) {
    return switch (granularity) {
      case HOUR -> value.plusHours(1);
      case DAY -> value.plusDays(1);
      case WEEK -> value.plusWeeks(1);
      case MONTH -> value.plusMonths(1);
    };
  }

  private String trendLabel(ZonedDateTime bucketStart, AnalyticsTrendGranularity granularity) {
    return switch (granularity) {
      case HOUR -> HOUR_TREND_LABEL.format(bucketStart);
      case DAY -> DAY_TREND_LABEL.format(bucketStart);
      case WEEK -> "Week of " + DAY_TREND_LABEL.format(bucketStart);
      case MONTH -> MONTH_TREND_LABEL.format(bucketStart);
    };
  }

  private PeakHour toPeakHour(PeakHourAggregate row, VehicleType vehicleType) {
    int nextHour = (row.hour() + 1) % 24;
    String label = "%02d:00–%02d:00".formatted(row.hour(), nextHour);
    return new PeakHour(
        row.vehicleTypeId(),
        vehicleType.getCode(),
        vehicleType.getName(),
        row.hour(),
        label,
        row.entryCount(),
        row.exitCount());
  }

  private CountComparison countComparison(long current, long comparison) {
    long change = current - comparison;
    if (comparison == 0 && current != 0) {
      return new CountComparison(
          current, comparison, change, null, AnalyticsChangeDirection.NOT_AVAILABLE);
    }
    BigDecimal percentage =
        comparison == 0
            ? BigDecimal.ZERO.setScale(2)
            : BigDecimal.valueOf(change)
                .multiply(ONE_HUNDRED)
                .divide(BigDecimal.valueOf(comparison), 2, RoundingMode.HALF_UP);
    return new CountComparison(
        current, comparison, change, percentage, numericDirection(current, comparison));
  }

  private DecimalComparison decimalComparison(BigDecimal current, BigDecimal comparison) {
    if (current == null || comparison == null) {
      return new DecimalComparison(
          current, comparison, null, null, AnalyticsChangeDirection.NOT_AVAILABLE);
    }
    BigDecimal normalizedCurrent = current.setScale(2, RoundingMode.HALF_UP);
    BigDecimal normalizedComparison = comparison.setScale(2, RoundingMode.HALF_UP);
    BigDecimal change = normalizedCurrent.subtract(normalizedComparison);
    if (normalizedComparison.signum() == 0 && normalizedCurrent.signum() != 0) {
      return new DecimalComparison(
          normalizedCurrent,
          normalizedComparison,
          change,
          null,
          AnalyticsChangeDirection.NOT_AVAILABLE);
    }
    BigDecimal percentage =
        normalizedComparison.signum() == 0
            ? BigDecimal.ZERO.setScale(2)
            : change.multiply(ONE_HUNDRED).divide(normalizedComparison, 2, RoundingMode.HALF_UP);
    return new DecimalComparison(
        normalizedCurrent,
        normalizedComparison,
        change,
        percentage,
        numericDirection(normalizedCurrent, normalizedComparison));
  }

  private AnalyticsChangeDirection numericDirection(long current, long comparison) {
    return numericDirection(BigDecimal.valueOf(current), BigDecimal.valueOf(comparison));
  }

  private AnalyticsChangeDirection numericDirection(BigDecimal current, BigDecimal comparison) {
    int result = current.compareTo(comparison);
    if (result > 0) {
      return AnalyticsChangeDirection.UP;
    }
    if (result < 0) {
      return AnalyticsChangeDirection.DOWN;
    }
    return AnalyticsChangeDirection.UNCHANGED;
  }

  private CurrentOccupancy currentOccupancy(CurrentOccupancyAggregate aggregate) {
    BigDecimal rate = rate(aggregate.occupied(), aggregate.usable());
    return new CurrentOccupancy(
        aggregate.usable(),
        aggregate.occupied(),
        aggregate.available(),
        aggregate.reserved(),
        rate,
        false,
        CURRENT_OCCUPANCY_NOTE);
  }

  private AverageOccupancy averageOccupancy(
      BigDecimal currentAverage, BigDecimal comparisonAverage, long capacity) {
    BigDecimal currentRate = rate(currentAverage, capacity);
    BigDecimal comparisonRate = rate(comparisonAverage, capacity);
    return new AverageOccupancy(
        decimalComparison(currentRate, comparisonRate),
        decimal(currentAverage),
        decimal(comparisonAverage),
        capacity,
        true,
        "CURRENT_USABLE_CAPACITY",
        AVERAGE_OCCUPANCY_NOTE);
  }

  private BigDecimal rate(long numerator, long denominator) {
    return rate(BigDecimal.valueOf(numerator), denominator);
  }

  private BigDecimal rate(BigDecimal numerator, long denominator) {
    if (denominator == 0) {
      return null;
    }
    return numerator
        .multiply(ONE_HUNDRED)
        .divide(BigDecimal.valueOf(denominator), 2, RoundingMode.HALF_UP);
  }

  private BigDecimal decimal(BigDecimal value) {
    return (value == null ? BigDecimal.ZERO : value).setScale(2, RoundingMode.HALF_UP);
  }

  private TrafficAggregate totalTraffic(List<TrafficAggregate> rows) {
    return new TrafficAggregate(
        null,
        rows.stream().mapToLong(TrafficAggregate::entries).sum(),
        rows.stream().mapToLong(TrafficAggregate::exits).sum());
  }

  private TrafficAggregate trafficFor(List<TrafficAggregate> rows, UUID vehicleTypeId) {
    return rows.stream()
        .filter(row -> vehicleTypeId.equals(row.vehicleTypeId()))
        .findFirst()
        .orElse(new TrafficAggregate(vehicleTypeId, 0, 0));
  }

  private CurrentOccupancyAggregate totalOccupancy(List<CurrentOccupancyAggregate> rows) {
    return new CurrentOccupancyAggregate(
        null,
        rows.stream().mapToLong(CurrentOccupancyAggregate::usable).sum(),
        rows.stream().mapToLong(CurrentOccupancyAggregate::occupied).sum(),
        rows.stream().mapToLong(CurrentOccupancyAggregate::available).sum(),
        rows.stream().mapToLong(CurrentOccupancyAggregate::reserved).sum());
  }

  private CurrentOccupancyAggregate occupancyFor(
      List<CurrentOccupancyAggregate> rows, UUID vehicleTypeId) {
    return rows.stream()
        .filter(row -> vehicleTypeId.equals(row.vehicleTypeId()))
        .findFirst()
        .orElse(new CurrentOccupancyAggregate(vehicleTypeId, 0, 0, 0, 0));
  }

  private BigDecimal totalAverage(List<AverageOccupancyAggregate> rows) {
    return rows.stream()
        .filter(row -> row.vehicleTypeId() == null)
        .map(AverageOccupancyAggregate::averageActiveSessions)
        .findFirst()
        .orElse(BigDecimal.ZERO);
  }

  private BigDecimal averageFor(List<AverageOccupancyAggregate> rows, UUID vehicleTypeId) {
    return rows.stream()
        .filter(row -> vehicleTypeId.equals(row.vehicleTypeId()))
        .map(AverageOccupancyAggregate::averageActiveSessions)
        .findFirst()
        .orElse(BigDecimal.ZERO);
  }

  private RevenueBreakdown revenueBreakdown(List<RevenueAggregate> rows, UUID vehicleTypeId) {
    Map<String, BigDecimal> amounts = new HashMap<>();
    rows.stream()
        .filter(row -> vehicleTypeId == null || vehicleTypeId.equals(row.vehicleTypeId()))
        .forEach(
            row ->
                amounts.merge(
                    row.source(),
                    row.amount() == null ? BigDecimal.ZERO : row.amount(),
                    BigDecimal::add));
    BigDecimal payos = money(amounts.get("PAYOS"));
    BigDecimal parkingCash = money(amounts.get("PARKING_CASH"));
    BigDecimal surchargeCash = money(amounts.get("SURCHARGE_CASH"));
    BigDecimal penaltyCash = money(amounts.get("PENALTY_CASH"));
    BigDecimal lostCardFine = money(amounts.get("LOST_CARD_FINE"));
    return new RevenueBreakdown(
        payos,
        parkingCash,
        surchargeCash,
        penaltyCash,
        lostCardFine,
        payos.add(parkingCash).add(surchargeCash).add(penaltyCash).add(lostCardFine));
  }

  private BigDecimal money(BigDecimal value) {
    return value == null ? MONEY_ZERO : value.setScale(2, RoundingMode.HALF_UP);
  }

  private List<VehicleType> responseVehicleTypes(UUID selectedVehicleTypeId) {
    if (selectedVehicleTypeId != null) {
      return List.of(
          vehicleTypeRepository
              .findByIdAndDeletedFalse(selectedVehicleTypeId)
              .filter(VehicleType::isActive)
              .orElseThrow(
                  () -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "VEHICLE_TYPE_NOT_FOUND")));
    }
    return vehicleTypeRepository.findAllByActiveTrueAndDeletedFalseOrderByNameAsc();
  }

  private UUID requireActiveVehicleType(UUID vehicleTypeId) {
    if (vehicleTypeId == null) {
      return null;
    }
    vehicleTypeRepository
        .findByIdAndDeletedFalse(vehicleTypeId)
        .filter(VehicleType::isActive)
        .orElseThrow(
            () -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "VEHICLE_TYPE_NOT_FOUND"));
    return vehicleTypeId;
  }

  private void requireParking(UUID parkingId, UUID tenantId) {
    if (parkingId == null) {
      throw new ApiException(ErrorCode.INVALID_INPUT, "parkingId is required");
    }
    parkingRepository
        .findByIdAndTenantId(parkingId, tenantId)
        .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "PARKING_NOT_FOUND"));
  }

  private UUID currentTenantId() {
    return TenantContext.getTenantId()
        .orElseThrow(() -> new ApiException(ErrorCode.UNAUTHENTICATED));
  }
}
