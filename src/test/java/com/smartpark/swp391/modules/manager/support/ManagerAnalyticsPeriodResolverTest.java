package com.smartpark.swp391.modules.manager.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.smartpark.swp391.common.exception.ApiException;
import com.smartpark.swp391.modules.manager.dto.analytics.ManagerAnalyticsQuery;
import com.smartpark.swp391.modules.manager.enumType.ManagerAnalyticsComparison;
import com.smartpark.swp391.modules.manager.enumType.ManagerAnalyticsPeriodType;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ManagerAnalyticsPeriodResolverTest {

  private static final UUID PARKING_ID = UUID.randomUUID();
  private final ManagerAnalyticsPeriodResolver resolver = new ManagerAnalyticsPeriodResolver();

  @Test
  void resolvesJulyAndSameMonthLastYearWithHcmHalfOpenBoundaries() {
    ResolvedAnalyticsPeriod result =
        resolver.resolve(
            query(ManagerAnalyticsPeriodType.MONTH, 2026, 7, null, null, null), today());

    assertThat(result.currentPeriod().from())
        .isEqualTo(OffsetDateTime.of(2026, 7, 1, 0, 0, 0, 0, ZoneOffset.ofHours(7)));
    assertThat(result.currentPeriod().to())
        .isEqualTo(OffsetDateTime.of(2026, 8, 1, 0, 0, 0, 0, ZoneOffset.ofHours(7)));
    assertThat(result.currentPeriod().label()).isEqualTo("July 2026");
    assertThat(result.comparisonPeriod().from())
        .isEqualTo(OffsetDateTime.of(2025, 7, 1, 0, 0, 0, 0, ZoneOffset.ofHours(7)));
    assertThat(result.comparisonPeriod().to())
        .isEqualTo(OffsetDateTime.of(2025, 8, 1, 0, 0, 0, 0, ZoneOffset.ofHours(7)));
    assertThat(result.comparisonPeriod().label()).isEqualTo("July 2025");
    assertThat(result.currentPeriod().timeZone()).isEqualTo("Asia/Ho_Chi_Minh");
    assertThat(result.trendGranularity()).isEqualTo(AnalyticsTrendGranularity.DAY);
  }

  @Test
  void resolvesQuarterAndIsoWeekAgainstPriorYear() {
    ResolvedAnalyticsPeriod quarter =
        resolver.resolve(
            query(ManagerAnalyticsPeriodType.QUARTER, 2026, null, 1, null, null), today());
    ResolvedAnalyticsPeriod week =
        resolver.resolve(
            new ManagerAnalyticsQuery(
                PARKING_ID,
                ManagerAnalyticsPeriodType.WEEK,
                LocalDate.of(2026, 7, 30),
                null,
                null,
                null,
                null,
                null,
                ManagerAnalyticsComparison.SAME_PERIOD_LAST_YEAR,
                null),
            today());

    assertThat(quarter.currentPeriod().label()).isEqualTo("Q1 2026");
    assertThat(quarter.currentFrom()).isEqualTo(LocalDate.of(2026, 1, 1));
    assertThat(quarter.currentToExclusive()).isEqualTo(LocalDate.of(2026, 4, 1));
    assertThat(quarter.comparisonFrom()).isEqualTo(LocalDate.of(2025, 1, 1));
    assertThat(quarter.comparisonToExclusive()).isEqualTo(LocalDate.of(2025, 4, 1));
    assertThat(week.currentFrom().getDayOfWeek()).isEqualTo(java.time.DayOfWeek.MONDAY);
    assertThat(week.comparisonFrom().getDayOfWeek()).isEqualTo(java.time.DayOfWeek.MONDAY);
    assertThat(week.currentPeriod().label()).isEqualTo("Week 31, 2026");
    assertThat(week.comparisonPeriod().label()).isEqualTo("Week 31, 2025");
  }

  @Test
  void shiftsLeapDayCustomRangeSafelyByCalendarYear() {
    ResolvedAnalyticsPeriod result =
        resolver.resolve(
            query(
                ManagerAnalyticsPeriodType.CUSTOM,
                null,
                null,
                null,
                LocalDate.of(2024, 2, 29),
                LocalDate.of(2024, 2, 29)),
            today());

    assertThat(result.currentFrom()).isEqualTo(LocalDate.of(2024, 2, 29));
    assertThat(result.currentToExclusive()).isEqualTo(LocalDate.of(2024, 3, 1));
    assertThat(result.comparisonFrom()).isEqualTo(LocalDate.of(2023, 2, 28));
    assertThat(result.comparisonToExclusive()).isEqualTo(LocalDate.of(2023, 3, 1));
  }

  @Test
  void defaultsToCurrentMonthAndRejectsInvalidCustomRanges() {
    ManagerAnalyticsQuery defaults =
        new ManagerAnalyticsQuery(PARKING_ID, null, null, null, null, null, null, null, null, null);
    ManagerAnalyticsQuery invalid =
        query(
            ManagerAnalyticsPeriodType.CUSTOM,
            null,
            null,
            null,
            LocalDate.of(2026, 8, 1),
            LocalDate.of(2026, 7, 1));

    assertThat(resolver.resolve(defaults, today()).currentPeriod().label()).isEqualTo("July 2026");
    assertThatThrownBy(() -> resolver.resolve(invalid, today())).isInstanceOf(ApiException.class);
  }

  private ManagerAnalyticsQuery query(
      ManagerAnalyticsPeriodType type,
      Integer year,
      Integer month,
      Integer quarter,
      LocalDate from,
      LocalDate to) {
    return new ManagerAnalyticsQuery(
        PARKING_ID,
        type,
        null,
        year,
        month,
        quarter,
        from,
        to,
        ManagerAnalyticsComparison.SAME_PERIOD_LAST_YEAR,
        null);
  }

  private LocalDate today() {
    return LocalDate.of(2026, 7, 30);
  }
}
