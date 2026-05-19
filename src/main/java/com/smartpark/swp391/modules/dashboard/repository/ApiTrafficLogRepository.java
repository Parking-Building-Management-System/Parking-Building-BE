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
              SELECT
                  date_trunc(:bucket, occurred_at) AS bucket_start,
                  COUNT(*) AS request_count,
                  COUNT(*) FILTER (WHERE status_code >= 400) AS error_count,
                  COALESCE(ROUND(AVG(duration_ms)::numeric, 2), 0) AS average_duration_ms
              FROM api_traffic_logs
              WHERE occurred_at >= :from AND occurred_at <= :to
              GROUP BY bucket_start
              ORDER BY bucket_start
          """,
      nativeQuery = true)
  List<Object[]> findTrafficChartRows(
      @Param("from") LocalDateTime from,
      @Param("to") LocalDateTime to,
      @Param("bucket") String bucket);
}
