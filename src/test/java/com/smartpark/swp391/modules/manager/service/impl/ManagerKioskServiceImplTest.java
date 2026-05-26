package com.smartpark.swp391.modules.manager.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.smartpark.swp391.infrastructure.tenant.TenantContext;
import com.smartpark.swp391.modules.identity.entity.Tenant;
import com.smartpark.swp391.modules.identity.repository.TenantRepository;
import com.smartpark.swp391.modules.identity.repository.UserRepository;
import com.smartpark.swp391.modules.manager.dto.kiosk.ManagerKioskResponse;
import com.smartpark.swp391.modules.operation.entity.Kiosk;
import com.smartpark.swp391.modules.operation.enumType.KioskStatus;
import com.smartpark.swp391.modules.operation.enumType.KioskType;
import com.smartpark.swp391.modules.operation.repository.KioskRepository;
import com.smartpark.swp391.modules.operation.repository.KioskStaffRepository;
import com.smartpark.swp391.modules.parking.entity.Parking;
import com.smartpark.swp391.modules.parking.repository.ParkingRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ManagerKioskServiceImplTest {

  @Mock KioskRepository kioskRepository;
  @Mock KioskStaffRepository kioskStaffRepository;
  @Mock ParkingRepository parkingRepository;
  @Mock UserRepository userRepository;
  @Mock TenantRepository tenantRepository;

  @AfterEach
  void clearTenantContext() {
    TenantContext.clear();
  }

  @Test
  void getKiosksIncludesActiveAssignedStaffCount() {
    UUID tenantId = UUID.randomUUID();
    UUID parkingId = UUID.randomUUID();
    UUID kioskId = UUID.randomUUID();
    TenantContext.setTenantId(tenantId);

    Parking parking = Parking.builder().code("P1").name("Parking 1").build();
    parking.setId(parkingId);
    Kiosk kiosk =
        Kiosk.builder()
            .tenant(Tenant.builder().build())
            .parking(parking)
            .code("P1-GATE")
            .name("Gate")
            .type(KioskType.ENTRY)
            .status(KioskStatus.ACTIVE)
            .build();
    kiosk.setId(kioskId);

    when(kioskRepository.findTenantKiosks(tenantId, null, null, null)).thenReturn(List.of(kiosk));
    when(kioskStaffRepository.countActiveAssignmentsByKioskIds(eq(tenantId), eq(List.of(kioskId))))
        .thenReturn(List.of(countView(kioskId, 1)));

    ManagerKioskServiceImpl service =
        new ManagerKioskServiceImpl(
            kioskRepository,
            kioskStaffRepository,
            parkingRepository,
            userRepository,
            tenantRepository);

    List<ManagerKioskResponse> result = service.getKiosks(null, null, null);

    assertThat(result).hasSize(1);
    assertThat(result.getFirst().assignedStaffCount()).isEqualTo(1);
  }

  private KioskStaffRepository.KioskStaffCountView countView(UUID kioskId, long count) {
    return new KioskStaffRepository.KioskStaffCountView() {
      @Override
      public UUID getKioskId() {
        return kioskId;
      }

      @Override
      public long getAssignedStaffCount() {
        return count;
      }
    };
  }
}
