package com.smartpark.swp391.modules.vehicle.repository;

import com.smartpark.swp391.modules.vehicle.entity.VehicleType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VehicleTypeRepository extends JpaRepository<VehicleType, UUID> {
  boolean existsByCodeIgnoreCase(String code);

  boolean existsByCodeIgnoreCaseAndIdNot(String code, UUID id);

  Optional<VehicleType> findByCodeIgnoreCaseAndDeletedFalse(String code);

  List<VehicleType> findAllByDeletedFalseOrderByNameAsc();
}
