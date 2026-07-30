package com.smartpark.swp391.modules.manager.support;

import com.smartpark.swp391.common.exception.ApiException;
import com.smartpark.swp391.common.exception.ErrorCode;
import com.smartpark.swp391.modules.manager.dto.analytics.ManagerAnalyticsOverviewResponse.Period;
import com.smartpark.swp391.modules.manager.dto.analytics.ManagerAnalyticsQuery;
import com.smartpark.swp391.modules.manager.enumType.ManagerAnalyticsComparison;
import com.smartpark.swp391.modules.manager.enumType.ManagerAnalyticsPeriodType;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Month;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.time.temporal.IsoFields;
import java.time.temporal.TemporalAdjusters;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class ManagerAnalyticsPeriodResolver {

  public static final ZoneId ANALYTICS_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
  private static final int MAX_CUSTOM_DAYS = 366;
  private static final DateTimeFormatter DATE_LABEL = DateTimeFormatter.ofPattern("dd MMM yyyy");

  public ResolvedAnalyticsPeriod resolve(ManagerAnalyticsQuery query) {
    return resolve(query, LocalDate.now(ANALYTICS_ZONE));
  }

  public ResolvedAnalyticsPeriod resolve(ManagerAnalyticsQuery query, LocalDate today) {
    if (query == null || query.parkingId() == null) {
      throw invalid("parkingId is required");
    }
    ManagerAnalyticsComparison comparison =
        query.comparison() == null
            ? ManagerAnalyticsComparison.SAME_PERIOD_LAST_YEAR
            : query.comparison();
    if (comparison != ManagerAnalyticsComparison.SAME_PERIOD_LAST_YEAR) {
      throw invalid("Unsupported analytics comparison");
    }

    ManagerAnalyticsPeriodType type =
        query.periodType() == null ? ManagerAnalyticsPeriodType.MONTH : query.periodType();
    DateRange current = resolveCurrentRange(query, type, today);
    DateRange previous = comparisonRange(current, type);

    return new ResolvedAnalyticsPeriod(
        current.from(),
        current.toExclusive(),
        previous.from(),
        previous.toExclusive(),
        toResponse(current),
        toResponse(previous),
        granularity(type, current),
        ANALYTICS_ZONE);
  }

  private DateRange resolveCurrentRange(
      ManagerAnalyticsQuery query, ManagerAnalyticsPeriodType type, LocalDate today) {
    return switch (type) {
      case DAY -> day(query.date() == null ? today : query.date());
      case WEEK -> week(query.date() == null ? today : query.date());
      case MONTH ->
          month(
              query.year() == null ? today.getYear() : query.year(),
              query.month() == null ? today.getMonthValue() : query.month());
      case QUARTER ->
          quarter(
              query.year() == null ? today.getYear() : query.year(),
              query.quarter() == null ? ((today.getMonthValue() - 1) / 3) + 1 : query.quarter());
      case YEAR -> year(query.year() == null ? today.getYear() : query.year());
      case CUSTOM -> custom(query.from(), query.to());
    };
  }

  private DateRange comparisonRange(DateRange current, ManagerAnalyticsPeriodType type) {
    if (type == ManagerAnalyticsPeriodType.WEEK) {
      int week = current.from().get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
      int weekYear = current.from().get(IsoFields.WEEK_BASED_YEAR) - 1;
      int availableWeeks = LocalDate.of(weekYear, 12, 28).get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
      LocalDate comparisonFrom =
          LocalDate.of(weekYear, 1, 4)
              .with(IsoFields.WEEK_OF_WEEK_BASED_YEAR, Math.min(week, availableWeeks))
              .with(ChronoField.DAY_OF_WEEK, DayOfWeek.MONDAY.getValue());
      return new DateRange(
          comparisonFrom,
          comparisonFrom.plusWeeks(1),
          "Week " + Math.min(week, availableWeeks) + ", " + weekYear);
    }
    LocalDate comparisonFrom = current.from().minusYears(1);
    LocalDate comparisonToExclusive =
        type == ManagerAnalyticsPeriodType.CUSTOM
            ? current.toExclusive().minusDays(1).minusYears(1).plusDays(1)
            : current.toExclusive().minusYears(1);
    return new DateRange(
        comparisonFrom, comparisonToExclusive, label(type, comparisonFrom, comparisonToExclusive));
  }

  private DateRange day(LocalDate date) {
    return new DateRange(date, date.plusDays(1), DATE_LABEL.format(date));
  }

  private DateRange week(LocalDate date) {
    LocalDate start = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    int week = start.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
    int weekYear = start.get(IsoFields.WEEK_BASED_YEAR);
    return new DateRange(start, start.plusWeeks(1), "Week " + week + ", " + weekYear);
  }

  private DateRange month(int year, int month) {
    requireYear(year);
    if (month < 1 || month > 12) {
      throw invalid("month must be between 1 and 12");
    }
    LocalDate start = LocalDate.of(year, month, 1);
    return new DateRange(start, start.plusMonths(1), labelMonth(start));
  }

  private DateRange quarter(int year, int quarter) {
    requireYear(year);
    if (quarter < 1 || quarter > 4) {
      throw invalid("quarter must be between 1 and 4");
    }
    LocalDate start = LocalDate.of(year, ((quarter - 1) * 3) + 1, 1);
    return new DateRange(start, start.plusMonths(3), "Q" + quarter + " " + year);
  }

  private DateRange year(int year) {
    requireYear(year);
    LocalDate start = LocalDate.of(year, 1, 1);
    return new DateRange(start, start.plusYears(1), String.valueOf(year));
  }

  private DateRange custom(LocalDate from, LocalDate to) {
    if (from == null || to == null) {
      throw invalid("from and to are required for CUSTOM periods");
    }
    if (to.isBefore(from)) {
      throw invalid("to must be on or after from");
    }
    long days = ChronoUnit.DAYS.between(from, to) + 1;
    if (days > MAX_CUSTOM_DAYS) {
      throw invalid("CUSTOM period cannot exceed " + MAX_CUSTOM_DAYS + " days");
    }
    return new DateRange(from, to.plusDays(1), customLabel(from, to));
  }

  private AnalyticsTrendGranularity granularity(
      ManagerAnalyticsPeriodType type, DateRange current) {
    return switch (type) {
      case DAY -> AnalyticsTrendGranularity.HOUR;
      case WEEK, MONTH -> AnalyticsTrendGranularity.DAY;
      case QUARTER -> AnalyticsTrendGranularity.WEEK;
      case YEAR -> AnalyticsTrendGranularity.MONTH;
      case CUSTOM -> {
        long days = ChronoUnit.DAYS.between(current.from(), current.toExclusive());
        if (days == 1) {
          yield AnalyticsTrendGranularity.HOUR;
        }
        if (days <= 45) {
          yield AnalyticsTrendGranularity.DAY;
        }
        if (days <= 120) {
          yield AnalyticsTrendGranularity.WEEK;
        }
        yield AnalyticsTrendGranularity.MONTH;
      }
    };
  }

  private Period toResponse(DateRange range) {
    return new Period(
        range.from().atStartOfDay(ANALYTICS_ZONE).toOffsetDateTime(),
        range.toExclusive().atStartOfDay(ANALYTICS_ZONE).toOffsetDateTime(),
        range.label(),
        ANALYTICS_ZONE.getId());
  }

  private String label(ManagerAnalyticsPeriodType type, LocalDate from, LocalDate toExclusive) {
    return switch (type) {
      case DAY -> DATE_LABEL.format(from);
      case MONTH -> labelMonth(from);
      case QUARTER -> "Q" + (((from.getMonthValue() - 1) / 3) + 1) + " " + from.getYear();
      case YEAR -> String.valueOf(from.getYear());
      case CUSTOM -> customLabel(from, toExclusive.minusDays(1));
      case WEEK -> throw new IllegalStateException("Week labels are resolved separately");
    };
  }

  private String labelMonth(LocalDate date) {
    return Month.of(date.getMonthValue()).getDisplayName(TextStyle.FULL, Locale.ENGLISH)
        + " "
        + date.getYear();
  }

  private String customLabel(LocalDate from, LocalDate inclusiveTo) {
    return DATE_LABEL.format(from) + " – " + DATE_LABEL.format(inclusiveTo);
  }

  private void requireYear(int year) {
    if (year < 1970 || year > 2200) {
      throw invalid("year must be between 1970 and 2200");
    }
  }

  private ApiException invalid(String message) {
    return new ApiException(ErrorCode.INVALID_INPUT, message);
  }

  private record DateRange(LocalDate from, LocalDate toExclusive, String label) {}
}
