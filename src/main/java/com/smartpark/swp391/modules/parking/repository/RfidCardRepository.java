package com.smartpark.swp391.modules.parking.repository;

import com.smartpark.swp391.modules.parking.entity.RfidCard;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RfidCardRepository extends JpaRepository<RfidCard, UUID> {

  Optional<RfidCard> findByTenantIdAndCodeIgnoreCase(UUID tenantId, String code);
}
