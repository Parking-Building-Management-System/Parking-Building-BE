package com.smartpark.swp391.modules.identity.repository;

import com.smartpark.swp391.modules.identity.entity.Device;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DeviceRepository extends JpaRepository<Device, UUID> {

  // Check xem máy này của user này đã từng login chưa
  @Query("SELECT d FROM Device d WHERE d.user.id = :userId AND d.fingerprint = :fingerprint")
  Optional<Device> findByUserIdAndFingerprint(
      @Param("userId") UUID userId, @Param("fingerprint") String fingerprint);
}
