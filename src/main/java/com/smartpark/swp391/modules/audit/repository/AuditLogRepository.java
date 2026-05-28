package com.smartpark.swp391.modules.audit.repository;

import com.smartpark.swp391.modules.audit.entity.AuditLog;
import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

  @Query(
      value =
          """
          SELECT a
          FROM AuditLog a
          JOIN FETCH a.tenant t
          LEFT JOIN FETCH a.actorUser u
          WHERE (:actorId IS NULL OR u.id = :actorId)
            AND (:role IS NULL OR a.actorRole = :role OR EXISTS (
              SELECT 1 FROM UserRole ur JOIN ur.role r
              WHERE ur.user.id = u.id AND r.name = :role
            ))
            AND (:severity IS NULL OR a.severity = :severity)
            AND (:from IS NULL OR a.occurredAt >= :from)
            AND (:to IS NULL OR a.occurredAt <= :to)
          """,
      countQuery =
          """
          SELECT COUNT(a)
          FROM AuditLog a
          LEFT JOIN a.actorUser u
          WHERE (:actorId IS NULL OR u.id = :actorId)
            AND (:role IS NULL OR a.actorRole = :role OR EXISTS (
              SELECT 1 FROM UserRole ur JOIN ur.role r
              WHERE ur.user.id = u.id AND r.name = :role
            ))
            AND (:severity IS NULL OR a.severity = :severity)
            AND (:from IS NULL OR a.occurredAt >= :from)
            AND (:to IS NULL OR a.occurredAt <= :to)
          """)
  Page<AuditLog> searchAdminAuditLogs(
      @Param("actorId") UUID actorId,
      @Param("role") String role,
      @Param("severity") String severity,
      @Param("from") LocalDateTime from,
      @Param("to") LocalDateTime to,
      Pageable pageable);
}
