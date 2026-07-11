package com.smartpark.swp391.modules.penalty.repository;

import com.smartpark.swp391.modules.penalty.entity.PenaltyCase;
import com.smartpark.swp391.modules.penalty.enumType.PenaltyCaseStatus;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PenaltyCaseRepository
    extends JpaRepository<PenaltyCase, UUID>, PenaltyCaseRepositoryCustom {

  long countByTenantIdAndOffenderSessionIdAndStatus(
      UUID tenantId, UUID offenderSessionId, PenaltyCaseStatus status);

  @Query(
      """
      SELECT pc
      FROM PenaltyCase pc
      LEFT JOIN FETCH pc.rule rule
      LEFT JOIN FETCH pc.victimSession victim
      LEFT JOIN FETCH pc.offenderSession offender
      LEFT JOIN FETCH pc.reportedSlot reportedSlot
      LEFT JOIN FETCH pc.reassignedSlot reassignedSlot
      LEFT JOIN FETCH pc.reviewedByStaff reviewer
      WHERE pc.tenant.id = :tenantId
        AND pc.parking.id = :parkingId
        AND pc.id = :id
        AND pc.type = com.smartpark.swp391.modules.penalty.enumType.PenaltyType.OCCUPIED_ASSIGNED_SLOT
        AND pc.reportedFromPwa = true
      """)
  java.util.Optional<PenaltyCase> findViolationReportDetail(
      @Param("tenantId") UUID tenantId, @Param("parkingId") UUID parkingId, @Param("id") UUID id);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query(
      """
      SELECT pc
      FROM PenaltyCase pc
      WHERE pc.tenant.id = :tenantId
        AND pc.parking.id = :parkingId
        AND pc.id = :id
        AND pc.type = com.smartpark.swp391.modules.penalty.enumType.PenaltyType.OCCUPIED_ASSIGNED_SLOT
        AND pc.reportedFromPwa = true
      """)
  java.util.Optional<PenaltyCase> findViolationReportForUpdate(
      @Param("tenantId") UUID tenantId, @Param("parkingId") UUID parkingId, @Param("id") UUID id);

  @Query(
      """
          SELECT pc
          FROM PenaltyCase pc
          LEFT JOIN FETCH pc.rule r
          LEFT JOIN FETCH pc.reportedSlot rs
          LEFT JOIN FETCH pc.reassignedSlot ns
          WHERE pc.tenant.id = :tenantId
            AND pc.targetSession.id = :sessionId
            AND pc.status IN :statuses
          ORDER BY pc.createdAt ASC
          """)
  List<PenaltyCase> findByTargetSessionAndStatuses(
      @Param("tenantId") UUID tenantId,
      @Param("sessionId") UUID sessionId,
      @Param("statuses") Collection<PenaltyCaseStatus> statuses);

  @Query(
      """
          SELECT pc
          FROM PenaltyCase pc
          LEFT JOIN FETCH pc.rule r
          LEFT JOIN FETCH pc.reportedSlot rs
          LEFT JOIN FETCH pc.reassignedSlot ns
          WHERE pc.tenant.id = :tenantId
            AND pc.id = :id
          """)
  java.util.Optional<PenaltyCase> findDetailByTenantIdAndId(
      @Param("tenantId") UUID tenantId, @Param("id") UUID id);
}
