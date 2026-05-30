package com.smartpark.swp391.modules.vehicle.repository;

import com.smartpark.swp391.modules.vehicle.entity.VehicleType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface VehicleTypeRepository extends JpaRepository<VehicleType, String> {

  @Query(
      """
      SELECT v FROM VehicleType v
      WHERE v.isDeleted = false
        AND (:searchKey IS NULL OR :searchKey = ''
             OR LOWER(v.name) LIKE LOWER(CONCAT('%', :searchKey, '%')))
      """)
  Page<VehicleType> findAllActive(
      @Param("searchKey") String searchKey, Pageable pageable);

  boolean existsByNameIgnoreCaseAndIsDeletedFalse(String name);
}
