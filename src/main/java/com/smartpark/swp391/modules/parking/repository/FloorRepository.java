package com.smartpark.swp391.modules.parking.repository;

import com.smartpark.swp391.modules.parking.entity.Floor;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface FloorRepository extends JpaRepository<Floor, UUID> {
  List<Floor> findAllByParkingIdOrderByDisplayOrderAscNameAsc(UUID parkingId);

  Optional<Floor> findByIdAndTenantId(UUID id, UUID tenantId);

  Optional<Floor> findByParkingIdAndCodeIgnoreCase(UUID parkingId, String code);

  boolean existsByParkingIdAndCodeIgnoreCase(UUID parkingId, String code);

  boolean existsByParkingIdAndCodeIgnoreCaseAndIdNot(UUID parkingId, String code, UUID id);

  @Query(
      """
          SELECT f
          FROM Floor f
          JOIN FETCH f.tenant t
          JOIN FETCH f.parking p
          ORDER BY t.slug ASC, p.name ASC, f.displayOrder ASC, f.name ASC
          """)
  List<Floor> findAllForDemoSeed();
}
