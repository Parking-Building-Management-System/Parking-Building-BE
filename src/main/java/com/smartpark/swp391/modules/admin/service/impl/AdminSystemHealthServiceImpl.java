package com.smartpark.swp391.modules.admin.service.impl;

import com.amazonaws.services.s3.AmazonS3;
import com.smartpark.swp391.infrastructure.storage.config.MinioStorageProperties;
import com.smartpark.swp391.modules.admin.dto.health.ServiceHealthResponse;
import com.smartpark.swp391.modules.admin.dto.health.SystemErrorResponse;
import com.smartpark.swp391.modules.admin.dto.health.SystemHealthSummaryResponse;
import com.smartpark.swp391.modules.admin.dto.health.TopEndpointResponse;
import com.smartpark.swp391.modules.admin.dto.health.TrafficPointResponse;
import com.smartpark.swp391.modules.admin.service.AdminSystemHealthService;
import com.smartpark.swp391.modules.dashboard.repository.ApiTrafficLogRepository;
import com.smartpark.swp391.modules.identity.enumType.TenantStatus;
import com.smartpark.swp391.modules.identity.repository.SessionRepository;
import com.smartpark.swp391.modules.identity.repository.TenantRepository;
import java.sql.Connection;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import javax.sql.DataSource;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class AdminSystemHealthServiceImpl implements AdminSystemHealthService {

  ApiTrafficLogRepository apiTrafficLogRepository;
  TenantRepository tenantRepository;
  SessionRepository sessionRepository;
  DataSource dataSource;
  ObjectProvider<RedisConnectionFactory> redisConnectionFactoryProvider;
  ObjectProvider<AmazonS3> s3Provider;
  MinioStorageProperties minioStorageProperties;

  ZoneId zoneId = ZoneId.systemDefault();
  long startedAtMillis = System.currentTimeMillis();

  @Override
  public SystemHealthSummaryResponse getSummary() {
    Instant now = Instant.now();
    LocalDateTime to = LocalDateTime.ofInstant(now, zoneId);
    LocalDateTime from = to.minusHours(24);
    Object[] summary = apiTrafficLogRepository.summarize(from, to);
    long totalRequests = number(summary, 0);
    long errorCount = number(summary, 1);
    long avgLatencyMs = number(summary, 2);
    double errorRate = totalRequests == 0 ? 0D : (double) errorCount / totalRequests;
    long activeSessions = sessionRepository.countByRevokedAtIsNullAndExpiredAtAfter(to);

    return SystemHealthSummaryResponse.builder()
        .status(errorRate > 0.2D ? "DEGRADED" : "OPERATIONAL")
        .uptimeSeconds(Duration.ofMillis(System.currentTimeMillis() - startedAtMillis).toSeconds())
        .totalRequests(totalRequests)
        .errorRate(errorRate)
        .avgLatencyMs(avgLatencyMs)
        .activeTenants(tenantRepository.countByStatusAndIsDeletedFalse(TenantStatus.ACTIVE))
        .activeSessions(activeSessions)
        .timestamp(now)
        .build();
  }

  @Override
  public List<ServiceHealthResponse> getServices() {
    return List.of(checkBackend(), checkDatabase(), checkRedis(), checkStorage());
  }

  @Override
  public List<TrafficPointResponse> getTraffic(Instant from, Instant to, String granularity) {
    TimeRange range = range(from, to);
    String bucket = bucket(granularity);
    return apiTrafficLogRepository
        .findTrafficAggregation(range.from(), range.to(), bucket)
        .stream()
        .map(
            row ->
                TrafficPointResponse.builder()
                    .timestamp(toInstant(row[0]))
                    .requestCount(number(row[1]))
                    .errorCount(number(row[2]))
                    .avgLatencyMs(number(row[3]))
                    .build())
        .toList();
  }

  @Override
  public List<TopEndpointResponse> getTopEndpoints(Instant from, Instant to, int limit) {
    TimeRange range = range(from, to);
    int safeLimit = Math.max(1, Math.min(limit, 100));
    return apiTrafficLogRepository
        .findTopEndpoints(range.from(), range.to(), safeLimit)
        .stream()
        .map(
            row ->
                TopEndpointResponse.builder()
                    .method((String) row[0])
                    .path((String) row[1])
                    .requestCount(number(row[2]))
                    .errorCount(number(row[3]))
                    .avgLatencyMs(number(row[4]))
                    .build())
        .toList();
  }

  @Override
  public List<SystemErrorResponse> getErrors(Instant from, Instant to) {
    TimeRange range = range(from, to);
    return apiTrafficLogRepository
        .findRecentErrors(range.from(), range.to())
        .stream()
        .map(
            row ->
                SystemErrorResponse.builder()
                    .timestamp(toInstant(row[0]))
                    .method((String) row[1])
                    .path((String) row[2])
                    .status((int) number(row[3]))
                    .errorCode("HTTP_" + number(row[3]))
                    .message("HTTP status " + number(row[3]))
                    .count(number(row[4]))
                    .build())
        .toList();
  }

  private ServiceHealthResponse checkBackend() {
    return ServiceHealthResponse.builder()
        .name("Backend App")
        .status("OPERATIONAL")
        .latencyMs(0L)
        .lastCheckedAt(Instant.now())
        .message("Application is running")
        .build();
  }

  private ServiceHealthResponse checkDatabase() {
    long start = System.nanoTime();
    try (Connection connection = dataSource.getConnection()) {
      boolean valid = connection.isValid(2);
      return service("Database", valid ? "OPERATIONAL" : "DOWN", start, "Connection checked");
    } catch (Exception e) {
      return service("Database", "DOWN", start, e.getMessage());
    }
  }

  private ServiceHealthResponse checkRedis() {
    long start = System.nanoTime();
    RedisConnectionFactory factory = redisConnectionFactoryProvider.getIfAvailable();
    if (factory == null) {
      return service("Redis Cache", "UNKNOWN", start, "Redis connection factory unavailable");
    }
    try (RedisConnection connection = factory.getConnection()) {
      connection.ping();
      return service("Redis Cache", "OPERATIONAL", start, "PING successful");
    } catch (Exception e) {
      return service("Redis Cache", "DOWN", start, e.getMessage());
    }
  }

  private ServiceHealthResponse checkStorage() {
    long start = System.nanoTime();
    if (!minioStorageProperties.configured()) {
      return service("MinIO Storage", "UNCONFIGURED", start, "MinIO/S3 settings are not configured");
    }
    AmazonS3 s3 = s3Provider.getIfAvailable();
    if (s3 == null) {
      return service("MinIO Storage", "DOWN", start, "MinIO/S3 client is unavailable");
    }
    try {
      boolean exists = s3.doesBucketExistV2(minioStorageProperties.bucket());
      return service(
          "MinIO Storage", exists ? "OPERATIONAL" : "DEGRADED", start, "Bucket check completed");
    } catch (Exception e) {
      return service("MinIO Storage", "DOWN", start, e.getMessage());
    }
  }

  private ServiceHealthResponse service(String name, String status, long startNanos, String message) {
    return ServiceHealthResponse.builder()
        .name(name)
        .status(status)
        .latencyMs(Duration.ofNanos(System.nanoTime() - startNanos).toMillis())
        .lastCheckedAt(Instant.now())
        .message(message)
        .build();
  }

  private TimeRange range(Instant from, Instant to) {
    Instant safeTo = to == null ? Instant.now() : to;
    Instant safeFrom = from == null ? safeTo.minus(Duration.ofHours(24)) : from;
    return new TimeRange(
        LocalDateTime.ofInstant(safeFrom, zoneId), LocalDateTime.ofInstant(safeTo, zoneId));
  }

  private String bucket(String granularity) {
    if ("DAY".equalsIgnoreCase(granularity)) {
      return "day";
    }
    if ("MINUTE".equalsIgnoreCase(granularity)) {
      return "minute";
    }
    return "hour";
  }

  private long number(Object[] values, int index) {
    if (values == null || values.length <= index) {
      return 0L;
    }
    return number(values[index]);
  }

  private long number(Object value) {
    return value instanceof Number number ? number.longValue() : 0L;
  }

  private Instant toInstant(Object value) {
    if (value instanceof java.sql.Timestamp timestamp) {
      return timestamp.toInstant();
    }
    if (value instanceof LocalDateTime localDateTime) {
      return localDateTime.atZone(zoneId).toInstant();
    }
    return Instant.now();
  }

  @EventListener(ApplicationReadyEvent.class)
  void markReady() {}

  private record TimeRange(LocalDateTime from, LocalDateTime to) {}
}
