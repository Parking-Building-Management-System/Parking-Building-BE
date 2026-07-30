package com.smartpark.swp391.modules.manager.repository;

import com.smartpark.swp391.modules.manager.support.AnalyticsTrendGranularity;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class ManagerAnalyticsRepository {

  private static final String LOCAL_TIME_ZONE = "Asia/Ho_Chi_Minh";

  NamedParameterJdbcTemplate jdbcTemplate;

  public List<TrafficAggregate> traffic(
      UUID tenantId, UUID parkingId, UUID vehicleTypeId, OffsetDateTime from, OffsetDateTime to) {
    String vehiclePredicate = vehiclePredicate("ps", vehicleTypeId);
    String sql =
        """
        SELECT ps.vehicle_type_id,
               COUNT(*) FILTER (
                   WHERE ps.check_in_at >= :fromInclusive
                     AND ps.check_in_at < :toExclusive
               ) AS entry_count,
               COUNT(*) FILTER (
                   WHERE ps.check_out_at >= :fromInclusive
                     AND ps.check_out_at < :toExclusive
               ) AS exit_count
        FROM parking_sessions ps
        WHERE ps.tenant_id = :tenantId
          AND ps.parking_id = :parkingId
        """
            + vehiclePredicate
            + """
          AND (
                (ps.check_in_at >= :fromInclusive AND ps.check_in_at < :toExclusive)
             OR (ps.check_out_at >= :fromInclusive AND ps.check_out_at < :toExclusive)
          )
        GROUP BY ps.vehicle_type_id
        """;
    return jdbcTemplate.query(
        sql,
        periodParameters(tenantId, parkingId, vehicleTypeId, from, to),
        (rs, rowNum) ->
            new TrafficAggregate(
                uuid(rs, "vehicle_type_id"), rs.getLong("entry_count"), rs.getLong("exit_count")));
  }

  public List<TrafficBucket> trafficTrend(
      UUID tenantId,
      UUID parkingId,
      UUID vehicleTypeId,
      OffsetDateTime from,
      OffsetDateTime to,
      AnalyticsTrendGranularity granularity) {
    String vehiclePredicate = vehiclePredicate("ps", vehicleTypeId);
    String localBucket =
        "(date_trunc('"
            + granularity.sqlValue()
            + "', %s AT TIME ZONE '"
            + LOCAL_TIME_ZONE
            + "') AT TIME ZONE '"
            + LOCAL_TIME_ZONE
            + "')";
    String sql =
        """
        WITH events AS (
          SELECT
        """
            + localBucket.formatted("ps.check_in_at")
            + """
              AS bucket_start,
              1::bigint AS entry_count,
              0::bigint AS exit_count
          FROM parking_sessions ps
          WHERE ps.tenant_id = :tenantId
            AND ps.parking_id = :parkingId
        """
            + vehiclePredicate
            + """
            AND ps.check_in_at >= :fromInclusive
            AND ps.check_in_at < :toExclusive

          UNION ALL

          SELECT
        """
            + localBucket.formatted("ps.check_out_at")
            + """
              AS bucket_start,
              0::bigint AS entry_count,
              1::bigint AS exit_count
          FROM parking_sessions ps
          WHERE ps.tenant_id = :tenantId
            AND ps.parking_id = :parkingId
        """
            + vehiclePredicate
            + """
            AND ps.check_out_at >= :fromInclusive
            AND ps.check_out_at < :toExclusive
        )
        SELECT bucket_start,
               SUM(entry_count)::bigint AS entry_count,
               SUM(exit_count)::bigint AS exit_count
        FROM events
        GROUP BY bucket_start
        ORDER BY bucket_start
        """;
    return jdbcTemplate.query(
        sql,
        periodParameters(tenantId, parkingId, vehicleTypeId, from, to),
        (rs, rowNum) ->
            new TrafficBucket(
                rs.getObject("bucket_start", OffsetDateTime.class),
                rs.getLong("entry_count"),
                rs.getLong("exit_count")));
  }

  public List<RevenueAggregate> revenue(
      UUID tenantId, UUID parkingId, UUID vehicleTypeId, OffsetDateTime from, OffsetDateTime to) {
    String paymentVehiclePredicate = vehiclePredicate("ps", vehicleTypeId);
    String cashVehiclePredicate = vehiclePredicate("cash_session", vehicleTypeId);
    String sql =
        """
        WITH revenue_events AS (
          SELECT ps.vehicle_type_id,
                 'PAYOS'::varchar AS source,
                 pi.amount
          FROM payment_intents pi
          JOIN parking_sessions ps
            ON ps.id = pi.parking_session_id
           AND ps.tenant_id = pi.tenant_id
          WHERE pi.tenant_id = :tenantId
            AND ps.parking_id = :parkingId
            AND pi.status = 'PAID'
            AND pi.provider = 'PAYOS'
            AND pi.is_deleted = false
            AND pi.paid_at >= :fromInclusive
            AND pi.paid_at < :toExclusive
        """
            + paymentVehiclePredicate
            + """

          UNION ALL

          SELECT cash_session.vehicle_type_id,
                 cash.type AS source,
                 cash.amount
          FROM staff_cash_transactions cash
          LEFT JOIN parking_sessions cash_session
            ON cash_session.id = cash.parking_session_id
           AND cash_session.tenant_id = cash.tenant_id
           AND cash_session.parking_id = cash.parking_id
          WHERE cash.tenant_id = :tenantId
            AND cash.parking_id = :parkingId
            AND cash.type IN (
              'PARKING_CASH',
              'SURCHARGE_CASH',
              'PENALTY_CASH',
              'LOST_CARD_FINE'
            )
            AND cash.occurred_at >= :fromInclusive
            AND cash.occurred_at < :toExclusive
        """
            + cashVehiclePredicate
            + """
        )
        SELECT vehicle_type_id, source, COALESCE(SUM(amount), 0) AS amount
        FROM revenue_events
        GROUP BY vehicle_type_id, source
        ORDER BY vehicle_type_id NULLS LAST, source
        """;
    return jdbcTemplate.query(
        sql,
        periodParameters(tenantId, parkingId, vehicleTypeId, from, to),
        (rs, rowNum) ->
            new RevenueAggregate(
                uuid(rs, "vehicle_type_id"), rs.getString("source"), rs.getBigDecimal("amount")));
  }

  public List<CurrentOccupancyAggregate> currentOccupancy(
      UUID tenantId, UUID parkingId, UUID vehicleTypeId) {
    String vehiclePredicate = vehiclePredicate("z", vehicleTypeId);
    String sql =
        """
        SELECT z.vehicle_type_id,
               COUNT(*) FILTER (
                 WHERE s.status IN ('AVAILABLE', 'OCCUPIED', 'RESERVED')
               ) AS usable_slots,
               COUNT(*) FILTER (WHERE s.status = 'OCCUPIED') AS occupied_slots,
               COUNT(*) FILTER (WHERE s.status = 'AVAILABLE') AS available_slots,
               COUNT(*) FILTER (WHERE s.status = 'RESERVED') AS reserved_slots
        FROM zones z
        JOIN slots s
          ON s.zone_id = z.id
         AND s.tenant_id = z.tenant_id
         AND s.parking_id = z.parking_id
         AND s.is_deleted = false
        WHERE z.tenant_id = :tenantId
          AND z.parking_id = :parkingId
          AND z.status = 'ACTIVE'
          AND z.is_deleted = false
        """
            + vehiclePredicate
            + """
        GROUP BY z.vehicle_type_id
        ORDER BY z.vehicle_type_id NULLS LAST
        """;
    MapSqlParameterSource parameters =
        new MapSqlParameterSource().addValue("tenantId", tenantId).addValue("parkingId", parkingId);
    if (vehicleTypeId != null) {
      parameters.addValue("vehicleTypeId", vehicleTypeId);
    }
    return jdbcTemplate.query(
        sql,
        parameters,
        (rs, rowNum) ->
            new CurrentOccupancyAggregate(
                uuid(rs, "vehicle_type_id"),
                rs.getLong("usable_slots"),
                rs.getLong("occupied_slots"),
                rs.getLong("available_slots"),
                rs.getLong("reserved_slots")));
  }

  public List<AverageOccupancyAggregate> averageOccupancy(
      UUID tenantId, UUID parkingId, UUID vehicleTypeId, OffsetDateTime from, OffsetDateTime to) {
    String requestedTypePredicate = vehicleTypeId == null ? "" : " AND vt.id = :vehicleTypeId\n";
    String sessionVehiclePredicate = vehiclePredicate("ps", vehicleTypeId);
    String sql =
        """
        WITH buckets AS (
          SELECT bucket_start
          FROM generate_series(
            CAST(:fromInclusive AS timestamptz),
            CAST(:toExclusive AS timestamptz) - INTERVAL '1 hour',
            INTERVAL '1 hour'
          ) AS bucket_start
        ),
        requested_types AS (
          SELECT vt.id
          FROM vehicle_types vt
          WHERE vt.is_active = true
            AND vt.is_deleted = false
        """
            + requestedTypePredicate
            + """
          UNION ALL
          SELECT NULL::uuid
        ),
        relevant_sessions AS (
          SELECT ps.id, ps.vehicle_type_id, ps.check_in_at, ps.check_out_at
          FROM parking_sessions ps
          WHERE ps.tenant_id = :tenantId
            AND ps.parking_id = :parkingId
            AND ps.check_in_at < :toExclusive
            AND (ps.check_out_at IS NULL OR ps.check_out_at >= :fromInclusive)
        """
            + sessionVehiclePredicate
            + """
        ),
        bucket_counts AS (
          SELECT requested_types.id AS vehicle_type_id,
                 buckets.bucket_start,
                 COUNT(relevant_sessions.id)::numeric AS active_sessions
          FROM requested_types
          CROSS JOIN buckets
          LEFT JOIN relevant_sessions
            ON relevant_sessions.check_in_at < buckets.bucket_start + INTERVAL '1 hour'
           AND (
                relevant_sessions.check_out_at IS NULL
                OR relevant_sessions.check_out_at >= buckets.bucket_start
           )
           AND (
                requested_types.id IS NULL
                OR relevant_sessions.vehicle_type_id = requested_types.id
           )
          GROUP BY requested_types.id, buckets.bucket_start
        )
        SELECT vehicle_type_id,
               COALESCE(AVG(active_sessions), 0) AS average_active_sessions
        FROM bucket_counts
        GROUP BY vehicle_type_id
        ORDER BY vehicle_type_id NULLS LAST
        """;
    return jdbcTemplate.query(
        sql,
        periodParameters(tenantId, parkingId, vehicleTypeId, from, to),
        (rs, rowNum) ->
            new AverageOccupancyAggregate(
                uuid(rs, "vehicle_type_id"), rs.getBigDecimal("average_active_sessions")));
  }

  public List<PeakHourAggregate> peakHours(
      UUID tenantId, UUID parkingId, UUID vehicleTypeId, OffsetDateTime from, OffsetDateTime to) {
    String vehiclePredicate = vehiclePredicate("ps", vehicleTypeId);
    String checkInHour =
        "EXTRACT(HOUR FROM ps.check_in_at AT TIME ZONE '" + LOCAL_TIME_ZONE + "')::integer";
    String checkOutHour =
        "EXTRACT(HOUR FROM ps.check_out_at AT TIME ZONE '" + LOCAL_TIME_ZONE + "')::integer";
    String sql =
        """
        WITH events AS (
          SELECT ps.vehicle_type_id,
        """
            + checkInHour
            + """
              AS local_hour,
                 1::bigint AS entry_count,
                 0::bigint AS exit_count
          FROM parking_sessions ps
          WHERE ps.tenant_id = :tenantId
            AND ps.parking_id = :parkingId
        """
            + vehiclePredicate
            + """
            AND ps.check_in_at >= :fromInclusive
            AND ps.check_in_at < :toExclusive

          UNION ALL

          SELECT ps.vehicle_type_id,
        """
            + checkOutHour
            + """
              AS local_hour,
                 0::bigint AS entry_count,
                 1::bigint AS exit_count
          FROM parking_sessions ps
          WHERE ps.tenant_id = :tenantId
            AND ps.parking_id = :parkingId
        """
            + vehiclePredicate
            + """
            AND ps.check_out_at >= :fromInclusive
            AND ps.check_out_at < :toExclusive
        ),
        hourly AS (
          SELECT vehicle_type_id,
                 local_hour,
                 SUM(entry_count)::bigint AS entry_count,
                 SUM(exit_count)::bigint AS exit_count
          FROM events
          GROUP BY vehicle_type_id, local_hour
        ),
        ranked AS (
          SELECT hourly.*,
                 ROW_NUMBER() OVER (
                   PARTITION BY vehicle_type_id
                   ORDER BY entry_count + exit_count DESC, local_hour ASC
                 ) AS position
          FROM hourly
        )
        SELECT vehicle_type_id, local_hour, entry_count, exit_count
        FROM ranked
        WHERE position <= 3
        ORDER BY vehicle_type_id, position
        """;
    return jdbcTemplate.query(
        sql,
        periodParameters(tenantId, parkingId, vehicleTypeId, from, to),
        (rs, rowNum) ->
            new PeakHourAggregate(
                uuid(rs, "vehicle_type_id"),
                rs.getInt("local_hour"),
                rs.getLong("entry_count"),
                rs.getLong("exit_count")));
  }

  private MapSqlParameterSource periodParameters(
      UUID tenantId, UUID parkingId, UUID vehicleTypeId, OffsetDateTime from, OffsetDateTime to) {
    MapSqlParameterSource parameters =
        new MapSqlParameterSource()
            .addValue("tenantId", tenantId)
            .addValue("parkingId", parkingId)
            .addValue("fromInclusive", from)
            .addValue("toExclusive", to);
    if (vehicleTypeId != null) {
      parameters.addValue("vehicleTypeId", vehicleTypeId);
    }
    return parameters;
  }

  private String vehiclePredicate(String alias, UUID vehicleTypeId) {
    return vehicleTypeId == null ? "" : " AND " + alias + ".vehicle_type_id = :vehicleTypeId\n";
  }

  private UUID uuid(ResultSet resultSet, String column) throws SQLException {
    return resultSet.getObject(column, UUID.class);
  }

  public record TrafficAggregate(UUID vehicleTypeId, long entries, long exits) {}

  public record TrafficBucket(OffsetDateTime bucketStart, long entries, long exits) {}

  public record RevenueAggregate(UUID vehicleTypeId, String source, BigDecimal amount) {}

  public record CurrentOccupancyAggregate(
      UUID vehicleTypeId, long usable, long occupied, long available, long reserved) {}

  public record AverageOccupancyAggregate(UUID vehicleTypeId, BigDecimal averageActiveSessions) {}

  public record PeakHourAggregate(UUID vehicleTypeId, int hour, long entryCount, long exitCount) {}
}
