package com.smartpark.swp391.modules.parking.repository;

import com.smartpark.swp391.modules.parking.entity.Parking;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ParkingRepository extends JpaRepository<Parking, UUID> {
  long countByIsDeletedFalse();

  List<Parking> findAllByIsDeletedFalseOrderByNameAsc();

  Optional<Parking> findByCodeIgnoreCaseAndIsDeletedFalse(String code);
}
