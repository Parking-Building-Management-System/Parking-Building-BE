package com.smartpark.swp391.modules.manager.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.smartpark.swp391.infrastructure.cached.redis.service.ManagerFacilityCacheService;
import com.smartpark.swp391.infrastructure.tenant.TenantContext;
import com.smartpark.swp391.modules.identity.entity.Tenant;
import com.smartpark.swp391.modules.identity.repository.TenantRepository;
import com.smartpark.swp391.modules.manager.dto.slot.SlotRequest;
import com.smartpark.swp391.modules.manager.dto.slot.SlotResponse;
import com.smartpark.swp391.modules.operation.repository.ParkingSessionRepository;
import com.smartpark.swp391.modules.parking.entity.Floor;
import com.smartpark.swp391.modules.parking.entity.Parking;
import com.smartpark.swp391.modules.parking.entity.Zone;
import com.smartpark.swp391.modules.parking.enumType.SlotStatus;
import com.smartpark.swp391.modules.parking.repository.FloorRepository;
import com.smartpark.swp391.modules.parking.repository.ParkingRepository;
import com.smartpark.swp391.modules.parking.repository.SlotRepository;
import com.smartpark.swp391.modules.parking.repository.ZoneRepository;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
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
  @Autowired PlatformTransactionManager transactionManager;

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
            .findByTenantIdAndCodeIgnoreCase(tenant.getId(), "BCONS-PLAZA")
            .orElseThrow();
    List<Floor> floors =
        floorRepository.findAllByParkingIdOrderByDisplayOrderAscNameAsc(parking.getId());
    Floor floor = floors.getFirst();
    Zone zone = zoneRepository.findAllByFloorIdOrderByNameAsc(floor.getId()).getFirst();

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
        parkingRepository.findByTenantIdAndCodeIgnoreCase(fpt.getId(), "FPT-TOWER").orElseThrow();
    TenantContext.setTenantId(bcons.getId());

    var result = service().getSlots(fptParking.getId(), null, null, null, null, false, 0, 20);

    assertThat(result.content()).isEmpty();
  }

  @Test
  void concurrentCreatesCannotExceedZoneCapacity() throws Exception {
    TestZone testZone = createCapacityOneZone();
    CountDownLatch start = new CountDownLatch(1);

    try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
      Future<CreateOutcome> first =
          executor.submit(() -> createSlotInNewTransaction(testZone, "CAP-01", start));
      Future<CreateOutcome> second =
          executor.submit(() -> createSlotInNewTransaction(testZone, "CAP-02", start));
      start.countDown();

      List<CreateOutcome> outcomes = List.of(first.get(), second.get());
      assertThat(outcomes)
          .containsExactlyInAnyOrder(CreateOutcome.CREATED, CreateOutcome.CAPACITY_REJECTED);
    }

    long slotCount =
        requiresNewTransaction().execute(status -> slotRepository.countByZoneId(testZone.zoneId()));
    assertThat(slotCount).isEqualTo(1);
  }

  private CreateOutcome createSlotInNewTransaction(
      TestZone testZone, String slotCode, CountDownLatch start) {
    try {
      start.await();
      TenantContext.setTenantId(testZone.tenantId());
      requiresNewTransaction()
          .execute(
              status -> {
                service()
                    .createSlot(
                        testZone.zoneId(),
                        new SlotRequest(slotCode, slotCode, SlotStatus.AVAILABLE));
                return null;
              });
      return CreateOutcome.CREATED;
    } catch (com.smartpark.swp391.common.exception.ApiException exception) {
      assertThat(exception).hasMessage("Zone capacity reached. Capacity: 1, existing slots: 1.");
      return CreateOutcome.CAPACITY_REJECTED;
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new AssertionError("Concurrent capacity test was interrupted", exception);
    } finally {
      TenantContext.clear();
    }
  }

  private TestZone createCapacityOneZone() {
    return requiresNewTransaction()
        .execute(
            status -> {
              Tenant tenant = tenant("bcons-plaza");
              String suffix = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
              Parking parking =
                  parkingRepository.save(
                      Parking.builder()
                          .tenant(tenant)
                          .code("CAP-" + suffix)
                          .name("Capacity test " + suffix)
                          .build());
              Floor floor =
                  floorRepository.save(
                      Floor.builder()
                          .tenant(tenant)
                          .parking(parking)
                          .code("F-" + suffix)
                          .name("Capacity floor")
                          .build());
              Zone zone =
                  zoneRepository.save(
                      Zone.builder()
                          .tenant(tenant)
                          .parking(parking)
                          .floor(floor)
                          .code("Z-" + suffix)
                          .name("Capacity zone")
                          .capacity(1)
                          .build());
              return new TestZone(tenant.getId(), zone.getId());
            });
  }

  private TransactionTemplate requiresNewTransaction() {
    TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
    transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    return transactionTemplate;
  }

  private ManagerSlotServiceImpl service() {
    return new ManagerSlotServiceImpl(
        slotRepository,
        parkingRepository,
        floorRepository,
        zoneRepository,
        tenantRepository,
        mock(ManagerFacilityCacheService.class),
        mock(ParkingSessionRepository.class));
  }

  private Tenant tenant(String slug) {
    return tenantRepository.findBySlug(slug).orElseThrow();
  }

  private enum CreateOutcome {
    CREATED,
    CAPACITY_REJECTED
  }

  private record TestZone(UUID tenantId, UUID zoneId) {}
}
