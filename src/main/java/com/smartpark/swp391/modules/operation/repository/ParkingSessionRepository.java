package com.smartpark.swp391.modules.operation.repository;

import com.smartpark.swp391.modules.operation.entity.ParkingSession;
import com.smartpark.swp391.modules.operation.enumType.ParkingSessionStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ParkingSessionRepository extends JpaRepository<ParkingSession, UUID> {

  boolean existsByTenantIdAndRfidCardIdAndStatus(
      UUID tenantId, UUID rfidCardId, ParkingSessionStatus status);

  @Query(
      """
          SELECT ps
          FROM ParkingSession ps
          JOIN FETCH ps.parking p
          JOIN FETCH ps.zone z
          JOIN FETCH ps.slot s
          LEFT JOIN FETCH s.floor f
          JOIN FETCH ps.rfidCard c
          WHERE c.id = :rfidCardId
            AND ps.status = :status
          ORDER BY ps.checkInAt DESC
          """)
  List<ParkingSession> findActiveByRfidCardId(
      @Param("rfidCardId") UUID rfidCardId,
      @Param("status") ParkingSessionStatus status,
      Pageable pageable);
}
