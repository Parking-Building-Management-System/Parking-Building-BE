package com.smartpark.swp391.modules.manager.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.smartpark.swp391.common.exception.ApiException;
import com.smartpark.swp391.infrastructure.tenant.TenantContext;
import com.smartpark.swp391.modules.manager.dto.analytics.ManagerAnalyticsOverviewResponse;
import com.smartpark.swp391.modules.manager.dto.analytics.ManagerAnalyticsQuery;
import com.smartpark.swp391.modules.manager.enumType.AnalyticsChangeDirection;
import com.smartpark.swp391.modules.manager.enumType.ManagerAnalyticsComparison;
import com.smartpark.swp391.modules.manager.enumType.ManagerAnalyticsPeriodType;
import com.smartpark.swp391.modules.manager.repository.ManagerAnalyticsRepository;
import com.smartpark.swp391.modules.manager.repository.ManagerAnalyticsRepository.AverageOccupancyAggregate;
import com.smartpark.swp391.modules.manager.repository.ManagerAnalyticsRepository.CurrentOccupancyAggregate;
import com.smartpark.swp391.modules.manager.repository.ManagerAnalyticsRepository.PeakHourAggregate;
import com.smartpark.swp391.modules.manager.repository.ManagerAnalyticsRepository.RevenueAggregate;
import com.smartpark.swp391.modules.manager.repository.ManagerAnalyticsRepository.TrafficAggregate;
import com.smartpark.swp391.modules.manager.repository.ManagerAnalyticsRepository.TrafficBucket;
import com.smartpark.swp391.modules.manager.support.ManagerAnalyticsPeriodResolver;
import com.smartpark.swp391.modules.parking.entity.Parking;
import com.smartpark.swp391.modules.parking.repository.ParkingRepository;
import com.smartpark.swp391.modules.vehicle.entity.VehicleType;
import com.smartpark.swp391.modules.vehicle.repository.VehicleTypeRepository;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ManagerAnalyticsServiceImplTest {

  @Mock ManagerAnalyticsRepository analyticsRepository;
  @Mock ParkingRepository parkingRepository;
  @Mock VehicleTypeRepository vehicleTypeRepository;

  private ManagerAnalyticsServiceImpl service;
  private UUID tenantId;
  private UUID parkingId;
  private UUID vehicleTypeId;
  private VehicleType vehicleType;

  @BeforeEach
  void setUp() {
    tenantId = UUID.randomUUID();
    parkingId = UUID.randomUUID();
    vehicleTypeId = UUID.randomUUID();
    TenantContext.setTenantId(tenantId);
    service =
        new ManagerAnalyticsServiceImpl(
            analyticsRepository,
            new ManagerAnalyticsPeriodResolver(),
            parkingRepository,
            vehicleTypeRepository);
    vehicleType =
        VehicleType.builder()
            .id(vehicleTypeId)
            .code("CAR")
            .name("Car")
            .active(true)
            .deleted(false)
            .build();
  }

  @AfterEach
  void tearDown() {
    TenantContext.clear();
  }

  @Test
  void aggregatesCanonicalMetricsAndRepresentsUnavailableComparisonsSafely() {
    when(parkingRepository.findByIdAndTenantId(parkingId, tenantId))
        .thenReturn(Optional.of(Parking.builder().id(parkingId).build()));
    when(vehicleTypeRepository.findAllByActiveTrueAndDeletedFalseOrderByNameAsc())
        .thenReturn(List.of(vehicleType));
    when(analyticsRepository.traffic(eq(tenantId), eq(parkingId), eq(null), any(), any()))
        .thenReturn(List.of(new TrafficAggregate(vehicleTypeId, 5, 4)))
        .thenReturn(List.of(new TrafficAggregate(vehicleTypeId, 0, 2)));
    when(analyticsRepository.revenue(eq(tenantId), eq(parkingId), eq(null), any(), any()))
        .thenReturn(
            List.of(
                new RevenueAggregate(vehicleTypeId, "PAYOS", new BigDecimal("100.00")),
                new RevenueAggregate(vehicleTypeId, "PARKING_CASH", new BigDecimal("20.00"))))
        .thenReturn(List.of(new RevenueAggregate(vehicleTypeId, "PAYOS", new BigDecimal("50.00"))));
    when(analyticsRepository.currentOccupancy(tenantId, parkingId, null))
        .thenReturn(List.of(new CurrentOccupancyAggregate(vehicleTypeId, 5, 2, 2, 1)));
    when(analyticsRepository.averageOccupancy(eq(tenantId), eq(parkingId), eq(null), any(), any()))
        .thenReturn(List.of(new AverageOccupancyAggregate(null, new BigDecimal("1.50"))))
        .thenReturn(List.of(new AverageOccupancyAggregate(null, new BigDecimal("1.00"))));
    OffsetDateTime currentBucket = OffsetDateTime.of(2026, 7, 1, 0, 0, 0, 0, ZoneOffset.ofHours(7));
    OffsetDateTime comparisonBucket = currentBucket.minusYears(1);
    when(analyticsRepository.trafficTrend(
            eq(tenantId), eq(parkingId), eq(null), any(), any(), any()))
        .thenReturn(List.of(new TrafficBucket(currentBucket, 2, 1)))
        .thenReturn(List.of(new TrafficBucket(comparisonBucket, 1, 1)));
    when(analyticsRepository.peakHours(eq(tenantId), eq(parkingId), eq(null), any(), any()))
        .thenReturn(List.of(new PeakHourAggregate(vehicleTypeId, 8, 3, 2)));

    ManagerAnalyticsOverviewResponse result = service.getOverview(monthQuery());

    assertThat(result.entries().current()).isEqualTo(5);
    assertThat(result.entries().comparison()).isZero();
    assertThat(result.entries().changePercent()).isNull();
    assertThat(result.entries().direction()).isEqualTo(AnalyticsChangeDirection.NOT_AVAILABLE);
    assertThat(result.exits().direction()).isEqualTo(AnalyticsChangeDirection.UP);
    assertThat(result.revenue().currentBreakdown().total()).isEqualByComparingTo("120.00");
    assertThat(result.revenue().comparisonBreakdown().total()).isEqualByComparingTo("50.00");
    assertThat(result.currentOccupancy().occupancyRate()).isEqualByComparingTo("40.00");
    assertThat(result.currentOccupancy().reservedSlots()).isEqualTo(1);
    assertThat(result.currentOccupancy().comparisonAvailable()).isFalse();
    assertThat(result.averageOccupancy().averageOccupancyRate().current())
        .isEqualByComparingTo("30.00");
    assertThat(result.averageOccupancy().averageOccupancyRate().comparison())
        .isEqualByComparingTo("20.00");
    assertThat(result.averageOccupancy().approximation()).isTrue();
    assertThat(result.trafficTrend()).hasSize(31);
    assertThat(result.trafficTrend().getFirst().entries()).isEqualTo(2);
    assertThat(result.comparisonTrafficTrend()).hasSize(31);
    assertThat(result.byVehicleType())
        .singleElement()
        .satisfies(
            row -> {
              assertThat(row.code()).isEqualTo("CAR");
              assertThat(row.revenue().current()).isEqualByComparingTo("120.00");
            });
    assertThat(result.peakHours())
        .singleElement()
        .satisfies(peak -> assertThat(peak.label()).isEqualTo("08:00–09:00"));
  }

  @Test
  void rejectsParkingOutsideCurrentTenantBeforeRunningAnalytics() {
    when(parkingRepository.findByIdAndTenantId(parkingId, tenantId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.getOverview(monthQuery())).isInstanceOf(ApiException.class);
    verifyNoInteractions(analyticsRepository);
  }

  private ManagerAnalyticsQuery monthQuery() {
    return new ManagerAnalyticsQuery(
        parkingId,
        ManagerAnalyticsPeriodType.MONTH,
        null,
        2026,
        7,
        null,
        null,
        null,
        ManagerAnalyticsComparison.SAME_PERIOD_LAST_YEAR,
        null);
  }
}
