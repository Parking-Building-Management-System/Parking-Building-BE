package com.smartpark.swp391.modules.parking.repository;

import com.smartpark.swp391.modules.parking.entity.RfidCard;
import com.smartpark.swp391.modules.parking.enumType.RfidCardStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RfidCardRepository extends JpaRepository<RfidCard, UUID> {

  Optional<RfidCard> findByTenantIdAndCodeIgnoreCase(UUID tenantId, String code);

  Optional<RfidCard> findByIdAndTenantId(UUID id, UUID tenantId);

  Optional<RfidCard> findByQrToken(String qrToken);

  Page<RfidCard> findAllByTenantId(UUID tenantId, Pageable pageable);

  Page<RfidCard> findAllByTenantIdAndStatus(
      UUID tenantId, RfidCardStatus status, Pageable pageable);

  long countByTenantIdAndCodeIgnoreCase(UUID tenantId, String code);

  boolean existsByQrToken(String qrToken);

  @Query(
      """
          SELECT c
          FROM RfidCard c
          WHERE c.tenant.id = :tenantId
            AND c.status = :status
            AND (:search IS NULL OR lower(c.code) LIKE lower(concat('%', :search, '%')))
            AND NOT EXISTS (
              SELECT 1
              FROM ParkingSession ps
              WHERE ps.tenant.id = :tenantId
                AND ps.parking.id = :parkingId
                AND ps.rfidCard.id = c.id
                AND ps.status = com.smartpark.swp391.modules.operation.enumType.ParkingSessionStatus.ACTIVE
            )
          ORDER BY c.code ASC
          """)
  List<RfidCard> findAvailableForStaffParking(
      @Param("tenantId") UUID tenantId,
      @Param("parkingId") UUID parkingId,
      @Param("status") RfidCardStatus status,
      @Param("search") String search,
      Pageable pageable);
}
