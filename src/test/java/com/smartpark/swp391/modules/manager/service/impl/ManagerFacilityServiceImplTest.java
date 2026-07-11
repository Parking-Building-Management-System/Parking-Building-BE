package com.smartpark.swp391.modules.manager.service.impl;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.smartpark.swp391.common.exception.ApiException;
import com.smartpark.swp391.infrastructure.cached.redis.service.ManagerFacilityCacheService;
import com.smartpark.swp391.infrastructure.storage.service.StorageService;
import com.smartpark.swp391.infrastructure.tenant.TenantContext;
import com.smartpark.swp391.modules.identity.entity.Tenant;
import com.smartpark.swp391.modules.identity.repository.TenantRepository;
import com.smartpark.swp391.modules.manager.dto.facility.ZoneRequest;
import com.smartpark.swp391.modules.parking.entity.Floor;
import com.smartpark.swp391.modules.parking.entity.Parking;
import com.smartpark.swp391.modules.parking.entity.Zone;
import com.smartpark.swp391.modules.parking.repository.FloorRepository;
import com.smartpark.swp391.modules.parking.repository.ParkingRepository;
import com.smartpark.swp391.modules.parking.repository.SlotRepository;
import com.smartpark.swp391.modules.parking.repository.ZoneRepository;
import com.smartpark.swp391.modules.payment.repository.PaymentWebhookLogRepository;
import com.smartpark.swp391.modules.vehicle.entity.VehicleType;
import com.smartpark.swp391.modules.vehicle.repository.VehicleTypeRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ManagerFacilityServiceImplTest {

  @Mock ParkingRepository parkingRepository;
  @Mock FloorRepository floorRepository;
  @Mock ZoneRepository zoneRepository;
  @Mock SlotRepository slotRepository;
  @Mock VehicleTypeRepository vehicleTypeRepository;
  @Mock TenantRepository tenantRepository;
  @Mock ManagerFacilityCacheService managerFacilityCacheService;
  @Mock PaymentWebhookLogRepository paymentWebhookLogRepository;
  @Mock StorageService storageService;

  @AfterEach
  void clearTenantContext() {
    TenantContext.clear();
  }

  @Test
  void deleteFloorPhysicallyDeletesWhenNoZonesRemain() {
    UUID tenantId = UUID.randomUUID();
    UUID floorId = UUID.randomUUID();
    TenantContext.setTenantId(tenantId);
    Floor floor = floor(floorId, tenantId);
    when(floorRepository.findByIdAndTenantId(floorId, tenantId)).thenReturn(Optional.of(floor));
    when(zoneRepository.countByFloorId(floorId)).thenReturn(0L);

    service().deleteFloor(floorId);

    verify(floorRepository).delete(floor);
  }

  @Test
  void deleteFloorRejectsWhenZonesRemain() {
    UUID tenantId = UUID.randomUUID();
    UUID floorId = UUID.randomUUID();
    TenantContext.setTenantId(tenantId);
    when(floorRepository.findByIdAndTenantId(floorId, tenantId))
        .thenReturn(Optional.of(floor(floorId, tenantId)));
    when(zoneRepository.countByFloorId(floorId)).thenReturn(1L);

    assertThatThrownBy(() -> service().deleteFloor(floorId))
        .isInstanceOf(ApiException.class)
        .hasMessage("Cannot delete a floor that still has zones");

    verify(floorRepository, never()).delete(org.mockito.ArgumentMatchers.any());
  }

  @Test
  void deleteZonePhysicallyDeletesWhenNoSlotsRemain() {
    UUID tenantId = UUID.randomUUID();
    UUID zoneId = UUID.randomUUID();
    TenantContext.setTenantId(tenantId);
    Zone zone =
        Zone.builder()
            .tenant(tenant(tenantId))
            .parking(parking(tenantId))
            .code("A")
            .name("A")
            .capacity(3)
            .build();
    zone.setId(zoneId);
    when(zoneRepository.findByIdAndTenantId(zoneId, tenantId)).thenReturn(Optional.of(zone));
    when(slotRepository.countByZoneId(zoneId)).thenReturn(0L);

    service().deleteZone(zoneId);

    verify(zoneRepository).delete(zone);
  }

  @Test
  void deleteZoneRejectsWhenSlotsRemain() {
    UUID tenantId = UUID.randomUUID();
    UUID zoneId = UUID.randomUUID();
    TenantContext.setTenantId(tenantId);
    Zone zone =
        Zone.builder()
            .tenant(tenant(tenantId))
            .parking(parking(tenantId))
            .code("A")
            .name("A")
            .capacity(3)
            .build();
    zone.setId(zoneId);
    when(zoneRepository.findByIdAndTenantId(zoneId, tenantId)).thenReturn(Optional.of(zone));
    when(slotRepository.countByZoneId(zoneId)).thenReturn(1L);

    assertThatThrownBy(() -> service().deleteZone(zoneId))
        .isInstanceOf(ApiException.class)
        .hasMessage("Cannot delete a zone that still has slots");

    verify(zoneRepository, never()).delete(org.mockito.ArgumentMatchers.any());
  }

  @Test
  void updateZoneRejectsCapacityBelowCurrentPhysicalSlotCount() {
    UUID tenantId = UUID.randomUUID();
    UUID zoneId = UUID.randomUUID();
    TenantContext.setTenantId(tenantId);
    Zone zone =
        Zone.builder()
            .tenant(tenant(tenantId))
            .parking(parking(tenantId))
            .code("A")
            .name("A")
            .capacity(3)
            .build();
    zone.setId(zoneId);
    VehicleType vehicleType = VehicleType.builder().code("CAR").name("Car").build();
    when(zoneRepository.findByIdAndTenantIdForUpdate(zoneId, tenantId))
        .thenReturn(Optional.of(zone));
    when(zoneRepository.existsByParkingIdAndCodeIgnoreCaseAndIdNot(
            zone.getParking().getId(), "A", zoneId))
        .thenReturn(false);
    when(vehicleTypeRepository.findByCodeIgnoreCaseAndDeletedFalse("CAR"))
        .thenReturn(Optional.of(vehicleType));
    when(slotRepository.countByZoneId(zoneId)).thenReturn(3L);

    assertThatThrownBy(
            () -> service().updateZone(zoneId, new ZoneRequest("A", "A", "CAR", 2, null)))
        .isInstanceOf(ApiException.class)
        .hasMessage("Zone capacity cannot be lower than its current slot count (3).");

    verify(zoneRepository, never()).save(zone);
  }

  private ManagerFacilityServiceImpl service() {
    return new ManagerFacilityServiceImpl(
        parkingRepository,
        floorRepository,
        zoneRepository,
        slotRepository,
        vehicleTypeRepository,
        tenantRepository,
        managerFacilityCacheService,
        paymentWebhookLogRepository,
        storageService);
  }

  private Floor floor(UUID floorId, UUID tenantId) {
    Floor floor =
        Floor.builder()
            .tenant(tenant(tenantId))
            .parking(parking(tenantId))
            .code("F1")
            .name("Floor 1")
            .build();
    floor.setId(floorId);
    return floor;
  }

  private Parking parking(UUID tenantId) {
    Parking parking = Parking.builder().tenant(tenant(tenantId)).code("P1").name("P1").build();
    parking.setId(UUID.randomUUID());
    return parking;
  }

  private Tenant tenant(UUID id) {
    Tenant tenant =
        Tenant.builder().name("Tenant").slug("tenant").emailContact("t@example.com").build();
    tenant.setId(id);
    return tenant;
  }
}
