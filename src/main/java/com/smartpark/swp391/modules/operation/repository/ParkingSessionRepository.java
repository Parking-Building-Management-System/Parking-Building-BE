package com.smartpark.swp391.modules.operation.repository;

import com.smartpark.swp391.modules.operation.entity.ParkingSession;
import com.smartpark.swp391.modules.operation.enumType.ParkingSessionStatus;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ParkingSessionRepository extends JpaRepository<ParkingSession, UUID> {

  boolean existsByTenantIdAndRfidCardIdAndStatus(
      UUID tenantId, UUID rfidCardId, ParkingSessionStatus status);
}
