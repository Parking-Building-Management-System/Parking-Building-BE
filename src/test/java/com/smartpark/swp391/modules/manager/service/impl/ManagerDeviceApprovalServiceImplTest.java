package com.smartpark.swp391.modules.manager.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.smartpark.swp391.common.exception.ApiException;
import com.smartpark.swp391.infrastructure.tenant.TenantContext;
import com.smartpark.swp391.modules.identity.entity.Device;
import com.smartpark.swp391.modules.identity.entity.Tenant;
import com.smartpark.swp391.modules.identity.entity.User;
import com.smartpark.swp391.modules.identity.enumType.DeviceStatus;
import com.smartpark.swp391.modules.identity.repository.DeviceRepository;
import com.smartpark.swp391.modules.manager.dto.device.DeviceApprovalRequest;
import com.smartpark.swp391.modules.operation.entity.Kiosk;
import com.smartpark.swp391.modules.operation.repository.KioskRepository;
import com.smartpark.swp391.modules.operation.repository.KioskStaffRepository;
import com.smartpark.swp391.modules.parking.entity.Parking;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ManagerDeviceApprovalServiceImplTest {

  @Mock DeviceRepository deviceRepository;
  @Mock KioskRepository kioskRepository;
  @Mock KioskStaffRepository kioskStaffRepository;

  @AfterEach
  void clearTenantContext() {
    TenantContext.clear();
  }

  @Test
  void approveWithNullExpiresAtStoresPermanentApproval() {
    TestFixture fixture = fixture();
    when(deviceRepository.save(any(Device.class))).thenAnswer(invocation -> invocation.getArgument(0));

    var result =
        service()
            .approve(
                fixture.deviceId,
                new DeviceApprovalRequest(fixture.kioskId, null),
                fixture.managerUserId);

    assertThat(result.status()).isEqualTo(DeviceStatus.APPROVED);
    assertThat(result.kioskId()).isEqualTo(fixture.kioskId);
    assertThat(result.expiresAt()).isNull();
  }

  @Test
  void approveWithFutureExpiresAtSucceeds() {
    TestFixture fixture = fixture();
    LocalDateTime future = LocalDateTime.now().plusHours(1);
    when(deviceRepository.save(any(Device.class))).thenAnswer(invocation -> invocation.getArgument(0));

    var result =
        service()
            .approve(
                fixture.deviceId,
                new DeviceApprovalRequest(fixture.kioskId, future),
                fixture.managerUserId);

    assertThat(result.status()).isEqualTo(DeviceStatus.APPROVED);
    assertThat(result.expiresAt()).isEqualTo(future);
  }

  @Test
  void approveWithPastExpiresAtFails() {
    TestFixture fixture = fixture();

    assertThatThrownBy(
            () ->
                service()
                    .approve(
                        fixture.deviceId,
                        new DeviceApprovalRequest(fixture.kioskId, LocalDateTime.now().minusSeconds(1)),
                        fixture.managerUserId))
        .isInstanceOf(ApiException.class)
        .hasMessage("DEVICE_APPROVAL_EXPIRES_AT_MUST_BE_FUTURE");
  }

  private ManagerDeviceApprovalServiceImpl service() {
    return new ManagerDeviceApprovalServiceImpl(
        deviceRepository, kioskRepository, kioskStaffRepository);
  }

  private TestFixture fixture() {
    UUID tenantId = UUID.randomUUID();
    UUID deviceId = UUID.randomUUID();
    UUID kioskId = UUID.randomUUID();
    UUID staffId = UUID.randomUUID();
    UUID managerUserId = UUID.randomUUID();
    UUID parkingId = UUID.randomUUID();
    TenantContext.setTenantId(tenantId);

    Tenant tenant = Tenant.builder().name("Tenant").slug("tenant").emailContact("t@example.com").build();
    tenant.setId(tenantId);
    User staff = User.builder().tenant(tenant).username("staff").fullName("Staff").password("x").build();
    staff.setId(staffId);
    Parking parking = Parking.builder().tenant(tenant).code("P1").name("Parking 1").build();
    parking.setId(parkingId);
    Kiosk kiosk = Kiosk.builder().tenant(tenant).parking(parking).code("K1").name("Kiosk").build();
    kiosk.setId(kioskId);
    Device device =
        Device.builder()
            .user(staff)
            .fingerprint("fp")
            .label("Device")
            .status(DeviceStatus.PENDING)
            .build();
    device.setId(deviceId);

    lenient()
        .when(deviceRepository.findTenantDeviceById(deviceId, tenantId))
        .thenReturn(Optional.of(device));
    lenient()
        .when(kioskRepository.findTenantKioskById(kioskId, tenantId))
        .thenReturn(Optional.of(kiosk));
    lenient()
        .when(kioskStaffRepository.existsActiveAssignment(tenantId, kioskId, staffId))
        .thenReturn(true);

    return new TestFixture(deviceId, kioskId, managerUserId);
  }

  private record TestFixture(UUID deviceId, UUID kioskId, UUID managerUserId) {}
}
