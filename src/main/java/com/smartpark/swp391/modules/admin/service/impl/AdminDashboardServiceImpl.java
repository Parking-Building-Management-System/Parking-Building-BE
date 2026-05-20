package com.smartpark.swp391.modules.admin.service.impl;

import com.smartpark.swp391.infrastructure.cached.redis.service.AdminPortalCacheService;
import com.smartpark.swp391.modules.admin.dto.dashboard.AdminDashboardStatsResponse;
import com.smartpark.swp391.modules.admin.dto.dashboard.AdminTrafficPointResponse;
import com.smartpark.swp391.modules.admin.service.AdminDashboardService;
import com.smartpark.swp391.modules.dashboard.repository.ApiTrafficLogRepository;
import com.smartpark.swp391.modules.identity.enumType.TenantStatus;
import com.smartpark.swp391.modules.identity.repository.TenantRepository;
import com.smartpark.swp391.modules.parking.repository.ParkingRepository;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class AdminDashboardServiceImpl implements AdminDashboardService {

  TenantRepository tenantRepository;
  ParkingRepository parkingRepository;
  ApiTrafficLogRepository apiTrafficLogRepository;
  AdminPortalCacheService adminPortalCacheService;

  @Override
  @Transactional(readOnly = true)
  public AdminDashboardStatsResponse getStats() {
    return adminPortalCacheService.getDashboardStats().orElseGet(this::loadStats);
  }

  private AdminDashboardStatsResponse loadStats() {
    LocalDateTime now = LocalDateTime.now();
    LocalDateTime from = now.minusDays(7);

    AdminDashboardStatsResponse response =
        AdminDashboardStatsResponse.builder()
            .activeTenantCount(tenantRepository.countByStatusAndIsDeletedFalse(TenantStatus.ACTIVE))
            .parkingCount(parkingRepository.countByIsDeletedFalse())
            .traffic(
                apiTrafficLogRepository.findTrafficAggregation(from, now, "day").stream()
                    .map(this::toTrafficPoint)
                    .toList())
            .build();

    adminPortalCacheService.saveDashboardStats(response);
    return response;
  }

  private AdminTrafficPointResponse toTrafficPoint(Object[] row) {
    return AdminTrafficPointResponse.builder()
        .bucketStart(toLocalDateTime(row[0]))
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
    throw new IllegalStateException("Unsupported traffic bucket type: " + value);
  }

  private long toLong(Object value) {
    return value instanceof Number number ? number.longValue() : 0;
  }

  private double toDouble(Object value) {
    if (value instanceof BigDecimal bigDecimal) {
      return bigDecimal.doubleValue();
    }
    return value instanceof Number number ? number.doubleValue() : 0;
  }
}
