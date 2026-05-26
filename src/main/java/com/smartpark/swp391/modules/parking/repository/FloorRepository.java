package com.smartpark.swp391.modules.parking.repository;

import com.smartpark.swp391.modules.parking.entity.Floor;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FloorRepository extends JpaRepository<Floor, UUID> {
  List<Floor> findAllByParkingIdAndDeletedFalseOrderByDisplayOrderAscNameAsc(UUID parkingId);

  Optional<Floor> findByIdAndTenantIdAndDeletedFalse(UUID id, UUID tenantId);

  Optional<Floor> findByParkingIdAndCodeIgnoreCaseAndDeletedFalse(UUID parkingId, String code);

  boolean existsByParkingIdAndCodeIgnoreCaseAndDeletedFalse(UUID parkingId, String code);

  boolean existsByParkingIdAndCodeIgnoreCaseAndIdNotAndDeletedFalse(
      UUID parkingId, String code, UUID id);
}
