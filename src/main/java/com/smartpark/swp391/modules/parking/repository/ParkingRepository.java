package com.smartpark.swp391.modules.parking.repository;

import com.smartpark.swp391.modules.parking.entity.Parking;
import com.smartpark.swp391.modules.parking.enumType.ParkingStatus;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ParkingRepository extends JpaRepository<Parking, UUID> {
  @Query("SELECT COUNT(p) FROM Parking p WHERE p.status = :status AND p.isDeleted = false")
  long countActiveByStatus(@Param("status") ParkingStatus status);
}
