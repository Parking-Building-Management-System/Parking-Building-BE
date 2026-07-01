package com.smartpark.swp391.modules.settlement.repository;

import com.smartpark.swp391.modules.settlement.entity.StaffCashTransaction;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StaffCashTransactionRepository extends JpaRepository<StaffCashTransaction, UUID> {

  @EntityGraph(attributePaths = {"parkingSession", "penaltyCase"})
  List<StaffCashTransaction> findByTenantIdAndShiftIdOrderByOccurredAtDesc(
      UUID tenantId, UUID shiftId);

  @EntityGraph(attributePaths = {"parkingSession", "penaltyCase"})
  List<StaffCashTransaction> findByTenantIdAndShiftIdOrderByOccurredAtDesc(
      UUID tenantId, UUID shiftId, Pageable pageable);
}
