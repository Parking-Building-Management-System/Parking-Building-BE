package com.smartpark.swp391.modules.penalty.repository;

import com.smartpark.swp391.modules.penalty.entity.PenaltyRule;
import com.smartpark.swp391.modules.penalty.enumType.PenaltyRuleStatus;
import com.smartpark.swp391.modules.penalty.enumType.PenaltyType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PenaltyRuleRepository
    extends JpaRepository<PenaltyRule, UUID>, JpaSpecificationExecutor<PenaltyRule> {

  @Query(
      """
          SELECT pr
          FROM PenaltyRule pr
          LEFT JOIN FETCH pr.parking p
          WHERE pr.tenant.id = :tenantId
            AND pr.id = :id
            AND pr.deleted = false
          """)
  Optional<PenaltyRule> findDetailByIdAndTenantId(
      @Param("id") UUID id, @Param("tenantId") UUID tenantId);

  @Query(
      """
          SELECT pr
          FROM PenaltyRule pr
          LEFT JOIN FETCH pr.parking p
          WHERE pr.tenant.id = :tenantId
            AND pr.parking.id = :parkingId
            AND pr.type = :type
            AND pr.status = com.smartpark.swp391.modules.penalty.enumType.PenaltyRuleStatus.ACTIVE
            AND pr.deleted = false
          """)
  List<PenaltyRule> findActiveParkingRules(
      @Param("tenantId") UUID tenantId,
      @Param("parkingId") UUID parkingId,
      @Param("type") PenaltyType type);

  @Query(
      """
          SELECT pr
          FROM PenaltyRule pr
          LEFT JOIN FETCH pr.parking p
          WHERE pr.tenant.id = :tenantId
            AND pr.parking IS NULL
            AND pr.type = :type
            AND pr.status = com.smartpark.swp391.modules.penalty.enumType.PenaltyRuleStatus.ACTIVE
            AND pr.deleted = false
          """)
  List<PenaltyRule> findActiveTenantDefaultRules(
      @Param("tenantId") UUID tenantId, @Param("type") PenaltyType type);

  @Query(
      """
          SELECT (COUNT(pr) > 0)
          FROM PenaltyRule pr
          WHERE pr.tenant.id = :tenantId
            AND (:excludedId IS NULL OR pr.id <> :excludedId)
            AND (
              (:parkingId IS NULL AND pr.parking IS NULL)
              OR (:parkingId IS NOT NULL AND pr.parking.id = :parkingId)
            )
            AND pr.type = :type
            AND pr.status = :status
            AND pr.deleted = false
          """)
  boolean existsActiveScope(
      @Param("tenantId") UUID tenantId,
      @Param("parkingId") UUID parkingId,
      @Param("type") PenaltyType type,
      @Param("status") PenaltyRuleStatus status,
      @Param("excludedId") UUID excludedId);
}
