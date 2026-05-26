package com.smartpark.swp391.modules.operation.repository;

import com.smartpark.swp391.modules.operation.entity.KioskStaff;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface KioskStaffRepository extends JpaRepository<KioskStaff, UUID> {

  interface KioskStaffCountView {
    UUID getKioskId();

    long getAssignedStaffCount();
  }

  @Query(
      """
          SELECT ks
          FROM KioskStaff ks
          JOIN FETCH ks.staffUser u
          WHERE ks.tenant.id = :tenantId
            AND ks.kiosk.id = :kioskId
            AND ks.active = true
          ORDER BY u.fullName ASC
          """)
  List<KioskStaff> findActiveByTenantAndKiosk(
      @Param("tenantId") UUID tenantId, @Param("kioskId") UUID kioskId);

  @Query(
      """
          SELECT ks
          FROM KioskStaff ks
          WHERE ks.tenant.id = :tenantId
            AND ks.kiosk.id = :kioskId
            AND ks.staffUser.id = :staffId
            AND ks.shift IS NULL
          """)
  Optional<KioskStaff> findNoShiftAssignment(
      @Param("tenantId") UUID tenantId,
      @Param("kioskId") UUID kioskId,
      @Param("staffId") UUID staffId);

  @Query(
      """
          SELECT COUNT(ks) > 0
          FROM KioskStaff ks
          WHERE ks.tenant.id = :tenantId
            AND ks.kiosk.id = :kioskId
            AND ks.staffUser.id = :staffId
            AND ks.active = true
          """)
  boolean existsActiveAssignment(
      @Param("tenantId") UUID tenantId,
      @Param("kioskId") UUID kioskId,
      @Param("staffId") UUID staffId);

  @Query(
      """
          SELECT ks.kiosk.id AS kioskId,
                 COUNT(ks.id) AS assignedStaffCount
          FROM KioskStaff ks
          WHERE ks.tenant.id = :tenantId
            AND ks.kiosk.id IN :kioskIds
            AND ks.active = true
          GROUP BY ks.kiosk.id
          """)
  List<KioskStaffCountView> countActiveAssignmentsByKioskIds(
      @Param("tenantId") UUID tenantId, @Param("kioskIds") Collection<UUID> kioskIds);

  @Query(
      """
          SELECT COUNT(ks.id)
          FROM KioskStaff ks
          WHERE ks.tenant.id = :tenantId
            AND ks.kiosk.id = :kioskId
            AND ks.active = true
          """)
  long countActiveAssignments(
      @Param("tenantId") UUID tenantId, @Param("kioskId") UUID kioskId);

  @Modifying
  @Query(
      """
          UPDATE KioskStaff ks
          SET ks.active = false,
              ks.updatedAt = CURRENT_TIMESTAMP
          WHERE ks.tenant.id = :tenantId
            AND ks.kiosk.id = :kioskId
            AND ks.staffUser.id = :staffId
            AND ks.active = true
          """)
  int deactivateAssignments(
      @Param("tenantId") UUID tenantId,
      @Param("kioskId") UUID kioskId,
      @Param("staffId") UUID staffId);
}
