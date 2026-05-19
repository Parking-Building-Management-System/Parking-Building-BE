package com.smartpark.swp391.modules.vehicle.repository;

import com.smartpark.swp391.modules.vehicle.entity.VehicleType;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VehicleTypeRepository extends JpaRepository<VehicleType, UUID> {

  boolean existsByCode(String code);

  @Query(
      """
          SELECT (COUNT(v) > 0) FROM VehicleType v
          WHERE v.code = :code AND v.id <> :id
      """)
  boolean existsByCodeExceptId(@Param("code") String code, @Param("id") UUID id);
}
