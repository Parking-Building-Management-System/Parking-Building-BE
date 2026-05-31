package com.smartpark.swp391.modules.pricing.repository;

import com.smartpark.swp391.modules.pricing.entity.PricingRule;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PricingRuleRepository
    extends JpaRepository<PricingRule, UUID>, JpaSpecificationExecutor<PricingRule> {

  Optional<PricingRule> findByIdAndTenantIdAndDeletedFalse(UUID id, UUID tenantId);

  @Query(
      """
          SELECT pr
          FROM PricingRule pr
          JOIN FETCH pr.vehicleType vt
          LEFT JOIN FETCH pr.parking p
          WHERE pr.tenant.id = :tenantId
            AND pr.id = :id
            AND pr.deleted = false
          """)
  Optional<PricingRule> findDetailByIdAndTenantId(
      @Param("id") UUID id, @Param("tenantId") UUID tenantId);

  @Query(
      """
          SELECT pr
          FROM PricingRule pr
          JOIN FETCH pr.vehicleType vt
          LEFT JOIN FETCH pr.parking p
          WHERE pr.tenant.id = :tenantId
            AND pr.parking.id = :parkingId
            AND pr.vehicleType.id = :vehicleTypeId
            AND pr.status = com.smartpark.swp391.modules.pricing.enumType.PricingRuleStatus.ACTIVE
            AND pr.deleted = false
          """)
  List<PricingRule> findActiveParkingRules(
      @Param("tenantId") UUID tenantId,
      @Param("parkingId") UUID parkingId,
      @Param("vehicleTypeId") UUID vehicleTypeId);

  @Query(
      """
          SELECT pr
          FROM PricingRule pr
          JOIN FETCH pr.vehicleType vt
          LEFT JOIN FETCH pr.parking p
          WHERE pr.tenant.id = :tenantId
            AND pr.parking IS NULL
            AND pr.vehicleType.id = :vehicleTypeId
            AND pr.status = com.smartpark.swp391.modules.pricing.enumType.PricingRuleStatus.ACTIVE
            AND pr.deleted = false
          """)
  List<PricingRule> findActiveTenantDefaultRules(
      @Param("tenantId") UUID tenantId, @Param("vehicleTypeId") UUID vehicleTypeId);

  @Query(
      """
          SELECT (COUNT(pr) > 0)
          FROM PricingRule pr
          WHERE pr.tenant.id = :tenantId
            AND (:excludedId IS NULL OR pr.id <> :excludedId)
            AND (
              (:parkingId IS NULL AND pr.parking IS NULL)
              OR (:parkingId IS NOT NULL AND pr.parking.id = :parkingId)
            )
            AND pr.vehicleType.id = :vehicleTypeId
            AND pr.status = com.smartpark.swp391.modules.pricing.enumType.PricingRuleStatus.ACTIVE
            AND pr.deleted = false
          """)
  boolean existsActiveScope(
      @Param("tenantId") UUID tenantId,
      @Param("parkingId") UUID parkingId,
      @Param("vehicleTypeId") UUID vehicleTypeId,
      @Param("excludedId") UUID excludedId);
}
