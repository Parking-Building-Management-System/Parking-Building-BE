package com.smartpark.swp391.modules.identity.repository;

import com.smartpark.swp391.modules.identity.entity.Session;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SessionRepository extends JpaRepository<Session, UUID> {

  // Cập nhật JTI mới khi user gọi hàm Refresh Token
  @Modifying
  @Query(
      """
                UPDATE Session s
                SET s.refreshJti = :newJti, s.updatedAt = CURRENT_TIMESTAMP
                WHERE s.id = :sessionId AND s.refreshJti = :oldJti
                AND s.revokedAt IS NULL AND s.expiredAt > CURRENT_TIMESTAMP
            """)
  int rotateRefreshJti(
      @Param("sessionId") UUID sessionId,
      @Param("oldJti") UUID oldJti,
      @Param("newJti") UUID newJti);

  // Dùng cho Fallback nếu Redis sập
  @Query(
      """
                SELECT (COUNT(s) > 0) FROM Session s
                WHERE s.id = :sessionId AND s.revokedAt IS NULL AND s.expiredAt > :now
            """)
  boolean isSessionActive(@Param("sessionId") UUID sessionId, @Param("now") LocalDateTime now);

  // Logout 1 thiết bị
  @Modifying
  @Query(
      """
                UPDATE Session s SET s.revokedAt = :now
                WHERE s.id = :sessionId AND s.revokedAt IS NULL
            """)
  int revokeIfNotRevoked(@Param("sessionId") UUID sessionId, @Param("now") LocalDateTime now);

  // Logout 1 thiết bị, ràng buộc đúng chủ session để tránh revoke nhầm user.
  @Modifying
  @Query(
      """
                UPDATE Session s SET s.revokedAt = :now
                WHERE s.id = :sessionId AND s.user.id = :userId AND s.revokedAt IS NULL
            """)
  int revokeIfNotRevokedByUser(
      @Param("sessionId") UUID sessionId,
      @Param("userId") UUID userId,
      @Param("now") LocalDateTime now);

  // Logout tất cả thiết bị (Force Logout)
  @Modifying
  @Query(
      """
                UPDATE Session s SET s.revokedAt = :now
                WHERE s.user.id = :userId AND s.revokedAt IS NULL AND s.expiredAt > :now
            """)
  int revokeAllActiveByUserId(@Param("userId") UUID userId, @Param("now") LocalDateTime now);

  // Tìm các ID đang active để gọi Redis xóa cache
  @Query(
      """
                SELECT s.id FROM Session s
                WHERE s.user.id = :userId AND s.revokedAt IS NULL AND s.expiredAt > :now
            """)
  List<UUID> findActiveSessionIdsByUserId(
      @Param("userId") UUID userId, @Param("now") LocalDateTime now);

  @Query(
      """
                SELECT s.id FROM Session s
                WHERE s.user.tenant.id = :tenantId AND s.revokedAt IS NULL AND s.expiredAt > :now
            """)
  List<UUID> findActiveSessionIdsByTenantId(
      @Param("tenantId") UUID tenantId, @Param("now") LocalDateTime now);

  @Modifying
  @Query(
      """
                UPDATE Session s SET s.revokedAt = :now
                WHERE s.user.tenant.id = :tenantId AND s.revokedAt IS NULL AND s.expiredAt > :now
            """)
  int revokeAllActiveByTenantId(@Param("tenantId") UUID tenantId, @Param("now") LocalDateTime now);

  @Query(
      value =
          """
          SELECT s
          FROM Session s
          JOIN FETCH s.device d
          JOIN FETCH d.user
          LEFT JOIN FETCH d.kiosk k
          LEFT JOIN FETCH k.parking
          WHERE s.id = :sessionId
            AND s.user.id = :userId
            AND s.revokedAt IS NULL
            AND s.expiredAt > :now
          """)
  java.util.Optional<Session> findActiveByIdAndUserIdWithDevice(
      @Param("sessionId") UUID sessionId,
      @Param("userId") UUID userId,
      @Param("now") LocalDateTime now);

  long countByRevokedAtIsNullAndExpiredAtAfter(LocalDateTime now);

  @Query(
      value =
          """
          SELECT s
          FROM Session s
          JOIN FETCH s.user u
          JOIN FETCH u.tenant t
          JOIN FETCH s.device d
          WHERE (:tenantId IS NULL OR t.id = :tenantId)
            AND (:role IS NULL OR EXISTS (
              SELECT 1 FROM UserRole ur JOIN ur.role r
              WHERE ur.user.id = u.id AND r.name = :role
            ))
            AND (
              :status IS NULL
              OR (:status = 'ACTIVE' AND s.revokedAt IS NULL AND s.expiredAt > :now)
              OR (:status = 'REVOKED' AND s.revokedAt IS NOT NULL)
              OR (:status = 'EXPIRED' AND s.revokedAt IS NULL AND s.expiredAt <= :now)
            )
          """,
      countQuery =
          """
          SELECT COUNT(s)
          FROM Session s
          JOIN s.user u
          JOIN u.tenant t
          WHERE (:tenantId IS NULL OR t.id = :tenantId)
            AND (:role IS NULL OR EXISTS (
              SELECT 1 FROM UserRole ur JOIN ur.role r
              WHERE ur.user.id = u.id AND r.name = :role
            ))
            AND (
              :status IS NULL
              OR (:status = 'ACTIVE' AND s.revokedAt IS NULL AND s.expiredAt > :now)
              OR (:status = 'REVOKED' AND s.revokedAt IS NOT NULL)
              OR (:status = 'EXPIRED' AND s.revokedAt IS NULL AND s.expiredAt <= :now)
            )
          """)
  Page<Session> findAdminSessions(
      @Param("tenantId") UUID tenantId,
      @Param("role") String role,
      @Param("status") String status,
      @Param("now") LocalDateTime now,
      Pageable pageable);
}
