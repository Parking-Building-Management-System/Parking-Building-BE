package com.smartpark.swp391.modules.parking.repository;

import com.smartpark.swp391.modules.parking.entity.Slot;
import com.smartpark.swp391.modules.parking.enumType.SlotStatus;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface SlotRepository extends JpaRepository<Slot, UUID>, JpaSpecificationExecutor<Slot> {
  long countByParkingIdAndIsDeletedFalse(UUID parkingId);

  long countByTenantIdAndIsDeletedFalse(UUID tenantId);

  long countByZoneIdAndIsDeletedFalse(UUID zoneId);

  Optional<Slot> findByZoneIdAndCodeIgnoreCaseAndIsDeletedFalse(UUID zoneId, String code);

  Optional<Slot> findByIdAndTenantIdAndIsDeletedFalse(UUID id, UUID tenantId);

  boolean existsByZoneIdAndCodeIgnoreCaseAndIsDeletedFalse(UUID zoneId, String code);

  boolean existsByZoneIdAndCodeIgnoreCaseAndIdNotAndIsDeletedFalse(
      UUID zoneId, String code, UUID id);

  @Query(
      """
          SELECT s
          FROM Slot s
          JOIN FETCH s.parking p
          JOIN FETCH s.zone z
          LEFT JOIN FETCH s.floor f
          WHERE s.tenant.id = :tenantId
            AND s.isDeleted = false
          ORDER BY p.name ASC, z.name ASC, s.code ASC
          """)
  List<Slot> findAllForExport(@Param("tenantId") UUID tenantId);

  Page<Slot> findAll(Pageable pageable);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query(
      """
          SELECT s
          FROM Slot s
          JOIN FETCH s.parking p
          JOIN FETCH s.zone z
          LEFT JOIN FETCH z.vehicleType vt
          WHERE s.tenant.id = :tenantId
            AND p.id = :parkingId
            AND s.status = :status
            AND s.isDeleted = false
            AND z.isDeleted = false
          ORDER BY LOWER(COALESCE(z.name, '')), LOWER(z.code), LOWER(s.code)
          """)
  List<Slot> findFirstAvailableForCheckIn(
      @Param("tenantId") UUID tenantId,
      @Param("parkingId") UUID parkingId,
      @Param("status") SlotStatus status,
      Pageable pageable);

  @Modifying
  @Query(
      """
          UPDATE Slot s
          SET s.status = :status, s.updatedAt = CURRENT_TIMESTAMP
          WHERE s.id IN :ids AND s.tenant.id = :tenantId AND s.isDeleted = false
          """)
  int bulkUpdateStatus(
      @Param("tenantId") UUID tenantId,
      @Param("ids") Collection<UUID> ids,
      @Param("status") SlotStatus status);
}
