package com.smartpark.swp391.modules.parking.repository;

import com.smartpark.swp391.modules.parking.entity.Parking;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ParkingRepository extends JpaRepository<Parking, UUID> {
  long countByIsDeletedFalse();

  List<Parking> findAllByIsDeletedFalseOrderByNameAsc();

  List<Parking> findAllByTenantIdAndIsDeletedFalseOrderByNameAsc(UUID tenantId);

  Optional<Parking> findByCodeIgnoreCaseAndIsDeletedFalse(String code);

  Optional<Parking> findByTenantIdAndCodeIgnoreCaseAndIsDeletedFalse(UUID tenantId, String code);

  Optional<Parking> findByIdAndTenantIdAndIsDeletedFalse(UUID id, UUID tenantId);

  boolean existsByTenantIdAndCodeIgnoreCaseAndIsDeletedFalse(UUID tenantId, String code);

  boolean existsByTenantIdAndCodeIgnoreCaseAndIdNotAndIsDeletedFalse(
      UUID tenantId, String code, UUID id);
}
