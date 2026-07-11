package com.smartpark.swp391.modules.parking.repository;

import com.smartpark.swp391.modules.parking.entity.Zone;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ZoneRepository extends JpaRepository<Zone, UUID> {
  List<Zone> findAllByFloorIdOrderByNameAsc(UUID floorId);

  List<Zone> findAllByParkingIdOrderByNameAsc(UUID parkingId);

  long countByFloorId(UUID floorId);

  Optional<Zone> findByIdAndTenantId(UUID id, UUID tenantId);

  Optional<Zone> findByFloorIdAndCodeIgnoreCase(UUID floorId, String code);

  boolean existsByFloorIdAndCodeIgnoreCase(UUID floorId, String code);

  boolean existsByFloorIdAndCodeIgnoreCaseAndIdNot(UUID floorId, String code, UUID id);

  boolean existsByParkingIdAndCodeIgnoreCase(UUID parkingId, String code);

  boolean existsByParkingIdAndCodeIgnoreCaseAndIdNot(UUID parkingId, String code, UUID id);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT z FROM Zone z WHERE z.id = :id AND z.tenant.id = :tenantId")
  Optional<Zone> findByIdAndTenantIdForUpdate(
      @Param("id") UUID id, @Param("tenantId") UUID tenantId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query(
      """
      SELECT z
      FROM Zone z
      WHERE z.tenant.id = :tenantId
        AND z.id IN :zoneIds
      ORDER BY z.id ASC
      """)
  List<Zone> findAllByIdInAndTenantIdForUpdate(
      @Param("zoneIds") List<UUID> zoneIds, @Param("tenantId") UUID tenantId);
}
