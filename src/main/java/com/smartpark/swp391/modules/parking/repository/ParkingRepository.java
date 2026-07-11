package com.smartpark.swp391.modules.parking.repository;

import com.smartpark.swp391.modules.parking.entity.Parking;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ParkingRepository extends JpaRepository<Parking, UUID> {
  long count();

  List<Parking> findAllByOrderByNameAsc();

  List<Parking> findAllByTenantIdOrderByNameAsc(UUID tenantId);

  Optional<Parking> findByCodeIgnoreCase(String code);

  Optional<Parking> findByTenantIdAndCodeIgnoreCase(UUID tenantId, String code);

  Optional<Parking> findByIdAndTenantId(UUID id, UUID tenantId);

  boolean existsByTenantIdAndCodeIgnoreCase(UUID tenantId, String code);

  boolean existsByTenantIdAndCodeIgnoreCaseAndIdNot(UUID tenantId, String code, UUID id);

  @Query(
      value =
          """
          SELECT object_key
          FROM (
              SELECT map_image_url AS object_key FROM floors WHERE parking_id = :parkingId
              UNION ALL
              SELECT entry_image_url FROM parking_sessions WHERE parking_id = :parkingId
              UNION ALL
              SELECT license_plate_image_url FROM parking_sessions WHERE parking_id = :parkingId
              UNION ALL
              SELECT exit_image_url FROM parking_sessions WHERE parking_id = :parkingId
              UNION ALL
              SELECT evidence_image_url FROM parking_penalty_cases WHERE parking_id = :parkingId
              UNION ALL
              SELECT identity_image_url FROM parking_penalty_cases WHERE parking_id = :parkingId
              UNION ALL
              SELECT vehicle_image_url FROM parking_penalty_cases WHERE parking_id = :parkingId
              UNION ALL
              SELECT license_plate_image_url FROM parking_penalty_cases WHERE parking_id = :parkingId
              UNION ALL
              SELECT fii.photo_object_key
              FROM fire_extinguisher_inspections fii
              JOIN fire_extinguishers fe ON fe.id = fii.fire_extinguisher_id
              WHERE fe.parking_id = :parkingId
          ) objects
          WHERE object_key IS NOT NULL
            AND trim(object_key) <> ''
          """,
      nativeQuery = true)
  List<String> findStorageObjectKeysByParkingId(@Param("parkingId") UUID parkingId);

  @Query(
      value =
          """
          SELECT order_code
          FROM payment_intents pi
          JOIN parking_sessions ps ON ps.id = pi.parking_session_id
          WHERE ps.parking_id = :parkingId
          """,
      nativeQuery = true)
  List<Long> findPaymentOrderCodesByParkingId(@Param("parkingId") UUID parkingId);
}
