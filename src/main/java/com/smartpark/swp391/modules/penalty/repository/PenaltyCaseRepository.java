package com.smartpark.swp391.modules.penalty.repository;

import com.smartpark.swp391.modules.penalty.entity.PenaltyCase;
import com.smartpark.swp391.modules.penalty.enumType.PenaltyCaseStatus;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PenaltyCaseRepository extends JpaRepository<PenaltyCase, UUID> {

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
