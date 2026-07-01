package com.smartpark.swp391.modules.settlement.repository;

import com.smartpark.swp391.modules.settlement.entity.StaffCashShift;
import com.smartpark.swp391.modules.settlement.enumType.StaffCashShiftStatus;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StaffCashShiftRepository
    extends JpaRepository<StaffCashShift, UUID>, JpaSpecificationExecutor<StaffCashShift> {

  @EntityGraph(attributePaths = {"parking", "kiosk", "staff"})
  Optional<StaffCashShift> findFirstByTenantIdAndStaffIdAndStatusOrderByOpenedAtDesc(
      UUID tenantId, UUID staffId, StaffCashShiftStatus status);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query(
      """
          SELECT shift
          FROM StaffCashShift shift
          JOIN FETCH shift.parking
          JOIN FETCH shift.kiosk
          JOIN FETCH shift.staff
          WHERE shift.tenant.id = :tenantId
            AND shift.staff.id = :staffId
            AND shift.status = com.smartpark.swp391.modules.settlement.enumType.StaffCashShiftStatus.OPEN
          ORDER BY shift.openedAt DESC
          """)
  Optional<StaffCashShift> findOpenForStaffForUpdate(
      @Param("tenantId") UUID tenantId, @Param("staffId") UUID staffId);

  @EntityGraph(attributePaths = {"parking", "kiosk", "staff"})
  Optional<StaffCashShift> findByTenantIdAndId(UUID tenantId, UUID id);
}
