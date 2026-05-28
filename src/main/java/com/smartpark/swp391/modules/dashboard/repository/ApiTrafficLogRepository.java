package com.smartpark.swp391.modules.dashboard.repository;

import com.smartpark.swp391.modules.dashboard.entity.ApiTrafficLog;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ApiTrafficLogRepository extends JpaRepository<ApiTrafficLog, UUID> {

  @Query(
      value =
          """
              SELECT date_trunc(:bucket, occurred_at) AS bucket_start,
                     COUNT(*) AS request_count,
                     SUM(CASE WHEN status_code >= 400 THEN 1 ELSE 0 END) AS error_count,
                     COALESCE(AVG(duration_ms), 0) AS average_duration_ms
              FROM api_traffic_logs
              WHERE occurred_at >= :from AND occurred_at <= :to
              GROUP BY bucket_start
              ORDER BY bucket_start
              """,
      nativeQuery = true)
  List<Object[]> findTrafficAggregation(
      @Param("from") LocalDateTime from,
      @Param("to") LocalDateTime to,
      @Param("bucket") String bucket);

  @Query(
      """
          SELECT COUNT(l), COALESCE(SUM(CASE WHEN l.statusCode >= 400 THEN 1 ELSE 0 END), 0),
                 COALESCE(AVG(l.durationMs), 0)
          FROM ApiTrafficLog l
          WHERE l.occurredAt >= :from AND l.occurredAt <= :to
          """)
  Object[] summarize(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

  @Query(
      value =
          """
              SELECT method, path, COUNT(*) AS request_count,
                     SUM(CASE WHEN status_code >= 400 THEN 1 ELSE 0 END) AS error_count,
                     COALESCE(AVG(duration_ms), 0) AS avg_latency_ms
              FROM api_traffic_logs
              WHERE occurred_at >= :from AND occurred_at <= :to
              GROUP BY method, path
              ORDER BY request_count DESC, error_count DESC, path ASC
              LIMIT :limit
              """,
      nativeQuery = true)
  List<Object[]> findTopEndpoints(
      @Param("from") LocalDateTime from, @Param("to") LocalDateTime to, @Param("limit") int limit);

  @Query(
      value =
          """
              SELECT date_trunc('minute', occurred_at) AS bucket_start, method, path, status_code,
                     COUNT(*) AS count
              FROM api_traffic_logs
              WHERE occurred_at >= :from AND occurred_at <= :to
                AND status_code >= 400
              GROUP BY bucket_start, method, path, status_code
              ORDER BY bucket_start DESC
              LIMIT 100
              """,
      nativeQuery = true)
  List<Object[]> findRecentErrors(
      @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);
}
