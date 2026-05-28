package com.smartpark.swp391.modules.identity.repository;

import com.smartpark.swp391.modules.identity.entity.Device;
import com.smartpark.swp391.modules.identity.enumType.DeviceStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DeviceRepository extends JpaRepository<Device, UUID> {

  // Check xem máy này của user này đã từng login chưa
  @Query("SELECT d FROM Device d WHERE d.user.id = :userId AND d.fingerprint = :fingerprint")
  Optional<Device> findByUserIdAndFingerprint(
      @Param("userId") UUID userId, @Param("fingerprint") String fingerprint);

  long countByUserIdAndStatus(UUID userId, DeviceStatus status);

  @Query(
      value =
          """
          SELECT d
          FROM Device d
          JOIN FETCH d.user u
          JOIN UserRole ur ON ur.user.id = u.id
          JOIN Role r ON r.id = ur.role.id
          WHERE u.tenant.id = :tenantId
            AND d.status = :status
            AND r.name = 'STAFF'
          ORDER BY d.createdAt DESC
          """)
  List<Device> findByTenantIdAndStatus(
      @Param("tenantId") UUID tenantId, @Param("status") DeviceStatus status);

  @Query(
      value =
          """
          SELECT d
          FROM Device d
          JOIN FETCH d.user u
          JOIN UserRole ur ON ur.user.id = u.id
          JOIN Role r ON r.id = ur.role.id
          LEFT JOIN FETCH d.kiosk k
          LEFT JOIN FETCH k.parking
          WHERE d.id = :id
            AND u.tenant.id = :tenantId
            AND r.name = 'STAFF'
          """)
  Optional<Device> findTenantDeviceById(@Param("id") UUID id, @Param("tenantId") UUID tenantId);

  @Query(
      value =
          """
          SELECT d
          FROM Device d
          JOIN FETCH d.user u
          JOIN FETCH u.tenant t
          LEFT JOIN FETCH d.kiosk k
          WHERE (:tenantId IS NULL OR t.id = :tenantId)
            AND (:status IS NULL OR d.status = :status)
          """,
      countQuery =
          """
          SELECT COUNT(d)
          FROM Device d
          JOIN d.user u
          JOIN u.tenant t
          WHERE (:tenantId IS NULL OR t.id = :tenantId)
            AND (:status IS NULL OR d.status = :status)
          """)
  Page<Device> findAdminDevices(
      @Param("tenantId") UUID tenantId,
      @Param("status") DeviceStatus status,
      Pageable pageable);
}
