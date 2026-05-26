package com.smartpark.swp391.modules.operation.repository;

import com.smartpark.swp391.modules.operation.entity.Kiosk;
import com.smartpark.swp391.modules.operation.enumType.KioskStatus;
import com.smartpark.swp391.modules.operation.enumType.KioskType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface KioskRepository extends JpaRepository<Kiosk, UUID> {

  @Query(
      """
          SELECT k
          FROM Kiosk k
          JOIN FETCH k.parking p
          WHERE k.tenant.id = :tenantId
            AND (:parkingId IS NULL OR p.id = :parkingId)
            AND (:status IS NULL OR k.status = :status)
            AND (:type IS NULL OR k.type = :type)
          ORDER BY p.name ASC, k.name ASC
          """)
  List<Kiosk> findTenantKiosks(
      @Param("tenantId") UUID tenantId,
      @Param("parkingId") UUID parkingId,
      @Param("status") KioskStatus status,
      @Param("type") KioskType type);

  @Query(
      """
          SELECT k
          FROM Kiosk k
          JOIN FETCH k.parking p
          WHERE k.id = :id
            AND k.tenant.id = :tenantId
          """)
  Optional<Kiosk> findTenantKioskById(@Param("id") UUID id, @Param("tenantId") UUID tenantId);

  boolean existsByTenantIdAndParkingIdAndCodeIgnoreCase(UUID tenantId, UUID parkingId, String code);
}
