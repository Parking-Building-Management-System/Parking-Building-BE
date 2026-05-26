package com.smartpark.swp391.modules.parking.repository;

import com.smartpark.swp391.modules.parking.entity.Zone;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ZoneRepository extends JpaRepository<Zone, UUID> {
  List<Zone> findAllByFloorIdAndIsDeletedFalseOrderByNameAsc(UUID floorId);

  List<Zone> findAllByParkingIdAndIsDeletedFalseOrderByNameAsc(UUID parkingId);

  long countByFloorIdAndIsDeletedFalse(UUID floorId);

  Optional<Zone> findByIdAndTenantIdAndIsDeletedFalse(UUID id, UUID tenantId);

  Optional<Zone> findByFloorIdAndCodeIgnoreCaseAndIsDeletedFalse(UUID floorId, String code);

  boolean existsByFloorIdAndCodeIgnoreCaseAndIsDeletedFalse(UUID floorId, String code);

  boolean existsByFloorIdAndCodeIgnoreCaseAndIdNotAndIsDeletedFalse(
      UUID floorId, String code, UUID id);

  boolean existsByParkingIdAndCodeIgnoreCaseAndIsDeletedFalse(UUID parkingId, String code);

  boolean existsByParkingIdAndCodeIgnoreCaseAndIdNotAndIsDeletedFalse(
      UUID parkingId, String code, UUID id);
}
