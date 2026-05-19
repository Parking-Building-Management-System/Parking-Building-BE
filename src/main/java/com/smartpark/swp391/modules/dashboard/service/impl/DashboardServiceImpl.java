package com.smartpark.swp391.modules.dashboard.service.impl;

import com.smartpark.swp391.common.exception.ApiException;
import com.smartpark.swp391.common.exception.ErrorCode;
import com.smartpark.swp391.modules.dashboard.dto.response.DashboardCountersResponse;
import com.smartpark.swp391.modules.dashboard.dto.response.TrafficChartResponse;
import com.smartpark.swp391.modules.dashboard.dto.response.TrafficPointResponse;
import com.smartpark.swp391.modules.dashboard.enumType.TrafficBucket;
import com.smartpark.swp391.modules.dashboard.repository.ApiTrafficLogRepository;
import com.smartpark.swp391.modules.dashboard.service.DashboardService;
import com.smartpark.swp391.modules.identity.enumType.TenantStatus;
import com.smartpark.swp391.modules.identity.repository.TenantRepository;
import com.smartpark.swp391.modules.parking.enumType.ParkingStatus;
import com.smartpark.swp391.modules.parking.repository.ParkingRepository;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class DashboardServiceImpl implements DashboardService {

  TenantRepository tenantRepository;
  ParkingRepository parkingRepository;
  ApiTrafficLogRepository apiTrafficLogRepository;

  @Override
  @Transactional(readOnly = true)
  public DashboardCountersResponse getCounters() {
    return DashboardCountersResponse.builder()
        .activeTenantCount(tenantRepository.countActiveByStatus(TenantStatus.ACTIVE))
        .activeParkingCount(parkingRepository.countActiveByStatus(ParkingStatus.ACTIVE))
        .build();
  }

  @Override
  @Transactional(readOnly = true)
  public TrafficChartResponse getTraffic(
      LocalDateTime from, LocalDateTime to, TrafficBucket bucket) {
    LocalDateTime normalizedTo = to != null ? to : LocalDateTime.now();
    LocalDateTime normalizedFrom = from != null ? from : normalizedTo.minusDays(1);
    TrafficBucket normalizedBucket = bucket != null ? bucket : TrafficBucket.HOUR;

    if (!normalizedFrom.isBefore(normalizedTo)) {
      throw new ApiException(ErrorCode.INVALID_INPUT, "from phải nhỏ hơn to");
    }

    List<TrafficPointResponse> points =
        apiTrafficLogRepository
            .findTrafficChartRows(normalizedFrom, normalizedTo, normalizedBucket.postgresUnit())
            .stream()
            .map(this::toTrafficPoint)
            .toList();

    return TrafficChartResponse.builder()
        .bucket(normalizedBucket)
        .from(normalizedFrom)
        .to(normalizedTo)
        .points(points)
        .build();
  }

  private TrafficPointResponse toTrafficPoint(Object[] row) {
    return TrafficPointResponse.builder()
        .time(toLocalDateTime(row[0]))
        .requestCount(toLong(row[1]))
        .errorCount(toLong(row[2]))
        .averageDurationMs(toDouble(row[3]))
        .build();
  }

  private LocalDateTime toLocalDateTime(Object value) {
    if (value instanceof LocalDateTime localDateTime) {
      return localDateTime;
    }
    if (value instanceof Timestamp timestamp) {
      return timestamp.toLocalDateTime();
    }
    if (value instanceof Instant instant) {
      return LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
    }
    if (value instanceof OffsetDateTime offsetDateTime) {
      return offsetDateTime.toLocalDateTime();
    }
    throw new ApiException(ErrorCode.UNEXPECTED_ERROR, "Không đọc được mốc thời gian traffic");
  }

  private long toLong(Object value) {
    if (value instanceof Number number) {
      return number.longValue();
    }
    throw new ApiException(ErrorCode.UNEXPECTED_ERROR, "Không đọc được số liệu traffic");
  }

  private double toDouble(Object value) {
    if (value instanceof BigDecimal bigDecimal) {
      return bigDecimal.doubleValue();
    }
    if (value instanceof Number number) {
      return number.doubleValue();
    }
    throw new ApiException(ErrorCode.UNEXPECTED_ERROR, "Không đọc được thời lượng traffic");
  }
}
