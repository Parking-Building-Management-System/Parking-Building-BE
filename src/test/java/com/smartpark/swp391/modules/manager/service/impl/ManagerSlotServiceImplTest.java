package com.smartpark.swp391.modules.manager.service.impl;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.smartpark.swp391.common.exception.ApiException;
import com.smartpark.swp391.infrastructure.cached.redis.service.ManagerFacilityCacheService;
import com.smartpark.swp391.infrastructure.tenant.TenantContext;
import com.smartpark.swp391.modules.identity.entity.Tenant;
import com.smartpark.swp391.modules.identity.repository.TenantRepository;
import com.smartpark.swp391.modules.manager.dto.slot.SlotBulkStatusRequest;
import com.smartpark.swp391.modules.parking.entity.Parking;
import com.smartpark.swp391.modules.parking.entity.Slot;
import com.smartpark.swp391.modules.parking.enumType.SlotStatus;
import com.smartpark.swp391.modules.parking.repository.FloorRepository;
import com.smartpark.swp391.modules.parking.repository.ParkingRepository;
import com.smartpark.swp391.modules.parking.repository.SlotRepository;
import com.smartpark.swp391.modules.parking.repository.ZoneRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ManagerSlotServiceImplTest {

  @Mock SlotRepository slotRepository;
  @Mock ParkingRepository parkingRepository;
  @Mock FloorRepository floorRepository;
  @Mock ZoneRepository zoneRepository;
  @Mock TenantRepository tenantRepository;
  @Mock ManagerFacilityCacheService managerFacilityCacheService;

  @AfterEach
  void clearTenantContext() {
    TenantContext.clear();
  }

  @Test
  void bulkUpdateStatusRejectsCrossTenantSlotIds() {
    UUID tenantId = UUID.randomUUID();
    UUID otherTenantId = UUID.randomUUID();
    UUID slotId = UUID.randomUUID();
    TenantContext.setTenantId(tenantId);

    Slot crossTenantSlot =
        Slot.builder()
            .tenant(tenant(otherTenantId))
            .parking(Parking.builder().tenant(tenant(otherTenantId)).code("P1").name("P1").build())
            .code("A1")
            .slotNumber("A1")
            .status(SlotStatus.AVAILABLE)
            .build();
    crossTenantSlot.setId(slotId);

    when(slotRepository.findAllById(List.of(slotId))).thenReturn(List.of(crossTenantSlot));

    assertThatThrownBy(
            () ->
                service()
                    .bulkUpdateStatus(
                        new SlotBulkStatusRequest(List.of(slotId), SlotStatus.MAINTENANCE)))
        .isInstanceOf(ApiException.class)
        .hasMessage("One or more slots were not found");

    verify(slotRepository, never())
        .bulkUpdateStatus(tenantId, List.of(slotId), SlotStatus.MAINTENANCE);
  }

  private ManagerSlotServiceImpl service() {
    return new ManagerSlotServiceImpl(
        slotRepository,
        parkingRepository,
        floorRepository,
        zoneRepository,
        tenantRepository,
        managerFacilityCacheService);
  }

  private Tenant tenant(UUID tenantId) {
    Tenant tenant =
        Tenant.builder().name("Tenant").slug("tenant").emailContact("t@example.com").build();
    tenant.setId(tenantId);
    return tenant;
  }
}
