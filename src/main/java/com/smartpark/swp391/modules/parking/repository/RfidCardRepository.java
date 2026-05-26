package com.smartpark.swp391.modules.parking.repository;

import com.smartpark.swp391.modules.parking.entity.RfidCard;
import com.smartpark.swp391.modules.parking.enumType.RfidCardStatus;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RfidCardRepository extends JpaRepository<RfidCard, UUID> {

  Optional<RfidCard> findByTenantIdAndCodeIgnoreCase(UUID tenantId, String code);

  Optional<RfidCard> findByIdAndTenantId(UUID id, UUID tenantId);

  Page<RfidCard> findAllByTenantId(UUID tenantId, Pageable pageable);

  Page<RfidCard> findAllByTenantIdAndStatus(UUID tenantId, RfidCardStatus status, Pageable pageable);

  long countByTenantIdAndCodeIgnoreCase(UUID tenantId, String code);
}
