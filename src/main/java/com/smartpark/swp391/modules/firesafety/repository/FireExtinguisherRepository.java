package com.smartpark.swp391.modules.firesafety.repository;

import com.smartpark.swp391.modules.firesafety.entity.FireExtinguisher;
import com.smartpark.swp391.modules.firesafety.enumType.FireExtinguisherStatus;
import com.smartpark.swp391.modules.firesafety.enumType.FireExtinguisherType;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FireExtinguisherRepository extends JpaRepository<FireExtinguisher, UUID> {

  Optional<FireExtinguisher> findByIdAndTenantIdAndDeletedFalse(UUID id, UUID tenantId);

  boolean existsByTenantIdAndCodeIgnoreCaseAndDeletedFalse(UUID tenantId, String code);

  boolean existsByTenantIdAndCodeIgnoreCaseAndIdNotAndDeletedFalse(
      UUID tenantId, String code, UUID id);

  @Query(
      """
          SELECT fe
          FROM FireExtinguisher fe
          JOIN FETCH fe.parking p
          JOIN FETCH fe.floor f
          LEFT JOIN FETCH fe.zone z
          WHERE fe.tenant.id = :tenantId
            AND fe.deleted = false
            AND (:parkingId IS NULL OR p.id = :parkingId)
            AND (:floorId IS NULL OR f.id = :floorId)
            AND (:zoneId IS NULL OR z.id = :zoneId)
            AND (:status IS NULL OR fe.status = :status)
            AND (:type IS NULL OR fe.type = :type)
            AND (:expiringUntil IS NULL OR fe.expiryDate <= :expiringUntil)
            AND (
              :search IS NULL OR
              lower(fe.code) LIKE lower(concat('%', :search, '%')) OR
              lower(coalesce(fe.locationDescription, '')) LIKE lower(concat('%', :search, '%'))
            )
          """)
  Page<FireExtinguisher> search(
      @Param("tenantId") UUID tenantId,
      @Param("parkingId") UUID parkingId,
      @Param("floorId") UUID floorId,
      @Param("zoneId") UUID zoneId,
      @Param("status") FireExtinguisherStatus status,
      @Param("type") FireExtinguisherType type,
      @Param("search") String search,
      @Param("expiringUntil") LocalDate expiringUntil,
      Pageable pageable);

  @Query(
      """
          SELECT fe
          FROM FireExtinguisher fe
          JOIN FETCH fe.parking p
          JOIN FETCH fe.floor f
          LEFT JOIN FETCH fe.zone z
          WHERE fe.tenant.id = :tenantId
            AND fe.deleted = false
            AND f.id = :floorId
          ORDER BY fe.code ASC
          """)
  List<FireExtinguisher> findMapItemsByTenantIdAndFloorId(
      @Param("tenantId") UUID tenantId, @Param("floorId") UUID floorId);

  @Query(
      """
          SELECT fe.status, count(fe)
          FROM FireExtinguisher fe
          WHERE fe.tenant.id = :tenantId
            AND fe.deleted = false
          GROUP BY fe.status
          """)
  List<Object[]> countByStatus(@Param("tenantId") UUID tenantId);

  long countByTenantIdAndDeletedFalse(UUID tenantId);

  long countByTenantIdAndDeletedFalseAndNextInspectionAtLessThanEqual(
      UUID tenantId, LocalDateTime now);

  long countByTenantIdAndDeletedFalseAndExpiryDateBetween(
      UUID tenantId, LocalDate start, LocalDate end);

  @Query(
      """
          SELECT fe
          FROM FireExtinguisher fe
          JOIN FETCH fe.parking p
          JOIN FETCH fe.floor f
          LEFT JOIN FETCH fe.zone z
          WHERE fe.tenant.id = :tenantId
            AND fe.deleted = false
            AND p.id = :parkingId
            AND (:floorId IS NULL OR f.id = :floorId)
            AND (:status IS NULL OR fe.status = :status)
            AND (
              fe.nextInspectionAt IS NOT NULL
              AND fe.nextInspectionAt <= :now
              OR fe.expiryDate IS NOT NULL
              AND fe.expiryDate <= :expiringUntil
            )
          ORDER BY fe.nextInspectionAt ASC NULLS LAST, fe.expiryDate ASC NULLS LAST, fe.code ASC
          """)
  List<FireExtinguisher> findDueForStaffParking(
      @Param("tenantId") UUID tenantId,
      @Param("parkingId") UUID parkingId,
      @Param("floorId") UUID floorId,
      @Param("status") FireExtinguisherStatus status,
      @Param("now") LocalDateTime now,
      @Param("expiringUntil") LocalDate expiringUntil);
}
