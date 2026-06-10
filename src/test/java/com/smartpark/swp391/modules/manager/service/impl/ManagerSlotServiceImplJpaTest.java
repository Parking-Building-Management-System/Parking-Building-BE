package com.smartpark.swp391.modules.manager.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.smartpark.swp391.infrastructure.cached.redis.service.ManagerFacilityCacheService;
import com.smartpark.swp391.infrastructure.tenant.TenantContext;
import com.smartpark.swp391.modules.identity.entity.Tenant;
import com.smartpark.swp391.modules.identity.repository.TenantRepository;
import com.smartpark.swp391.modules.manager.dto.slot.SlotResponse;
import com.smartpark.swp391.modules.parking.entity.Floor;
import com.smartpark.swp391.modules.parking.entity.Parking;
import com.smartpark.swp391.modules.parking.entity.Zone;
import com.smartpark.swp391.modules.parking.enumType.SlotStatus;
import com.smartpark.swp391.modules.parking.repository.FloorRepository;
import com.smartpark.swp391.modules.parking.repository.ParkingRepository;
import com.smartpark.swp391.modules.parking.repository.SlotRepository;
import com.smartpark.swp391.modules.parking.repository.ZoneRepository;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@DataJpaTest
@Testcontainers(disabledWithoutDocker = true)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ManagerSlotServiceImplJpaTest {

  @Container
  static final PostgreSQLContainer<?> POSTGRES =
      new PostgreSQLContainer<>("postgres:16-alpine")
          .withDatabaseName("smartpark-test")
          .withUsername("test")
          .withPassword("test");

  @DynamicPropertySource
  static void databaseProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
    registry.add("spring.datasource.username", POSTGRES::getUsername);
    registry.add("spring.datasource.password", POSTGRES::getPassword);
    registry.add("spring.flyway.enabled", () -> "true");
  }

  @Autowired SlotRepository slotRepository;
  @Autowired ParkingRepository parkingRepository;
  @Autowired FloorRepository floorRepository;
  @Autowired ZoneRepository zoneRepository;
  @Autowired TenantRepository tenantRepository;

  @AfterEach
  void clearTenantContext() {
    TenantContext.clear();
  }

  @Test
  void getSlotsFiltersByParkingFloorZoneStatusAndSearchWithinTenant() {
    Tenant tenant = tenant("bcons-plaza");
    TenantContext.setTenantId(tenant.getId());
    Parking parking =
        parkingRepository
            .findByTenantIdAndCodeIgnoreCaseAndIsDeletedFalse(tenant.getId(), "BCONS-PLAZA")
            .orElseThrow();
    List<Floor> floors =
        floorRepository.findAllByParkingIdAndDeletedFalseOrderByDisplayOrderAscNameAsc(
            parking.getId());
    Floor floor = floors.getFirst();
    Zone zone =
        zoneRepository.findAllByFloorIdAndIsDeletedFalseOrderByNameAsc(floor.getId()).getFirst();

    List<SlotResponse> allTenantSlots =
        service().getSlots(null, null, null, null, null, false, 0, 100).content();
    List<SlotResponse> parkingSlots =
        service().getSlots(parking.getId(), null, null, null, null, false, 0, 100).content();
    List<SlotResponse> floorSlots =
        service().getSlots(null, floor.getId(), null, null, null, false, 0, 100).content();
    List<SlotResponse> combinedSlots =
        service()
            .getSlots(parking.getId(), floor.getId(), null, null, null, false, 0, 100)
            .content();
    List<SlotResponse> zoneSlots =
        service().getSlots(null, null, zone.getId(), null, null, false, 0, 100).content();
    List<SlotResponse> availableCodeSlots =
        service()
            .getSlots(
                parking.getId(), floor.getId(), null, SlotStatus.AVAILABLE, "C-", false, 0, 100)
            .content();

    assertThat(allTenantSlots).isNotEmpty();
    assertThat(allTenantSlots)
        .allSatisfy(slot -> assertThat(slot.parkingId()).isEqualTo(parking.getId()));
    assertThat(parkingSlots)
        .allSatisfy(slot -> assertThat(slot.parkingId()).isEqualTo(parking.getId()));
    assertThat(floorSlots).allSatisfy(slot -> assertThat(slot.floorId()).isEqualTo(floor.getId()));
    assertThat(combinedSlots)
        .allSatisfy(
            slot -> {
              assertThat(slot.parkingId()).isEqualTo(parking.getId());
              assertThat(slot.floorId()).isEqualTo(floor.getId());
            });
    assertThat(zoneSlots).allSatisfy(slot -> assertThat(slot.zoneId()).isEqualTo(zone.getId()));
    assertThat(availableCodeSlots)
        .allSatisfy(
            slot -> {
              assertThat(slot.status()).isEqualTo(SlotStatus.AVAILABLE);
              assertThat(slot.code()).contains("C-");
            });
  }

  @Test
  void getSlotsDoesNotLeakCrossTenantParking() {
    Tenant bcons = tenant("bcons-plaza");
    Tenant fpt = tenant("fpt-tower");
    Parking fptParking =
        parkingRepository
            .findByTenantIdAndCodeIgnoreCaseAndIsDeletedFalse(fpt.getId(), "FPT-TOWER")
            .orElseThrow();
    TenantContext.setTenantId(bcons.getId());

    var result = service().getSlots(fptParking.getId(), null, null, null, null, false, 0, 20);

    assertThat(result.content()).isEmpty();
  }

  private ManagerSlotServiceImpl service() {
    return new ManagerSlotServiceImpl(
        slotRepository,
        parkingRepository,
        floorRepository,
        zoneRepository,
        tenantRepository,
        mock(ManagerFacilityCacheService.class));
  }

  private Tenant tenant(String slug) {
    return tenantRepository.findBySlug(slug).orElseThrow();
  }
}
