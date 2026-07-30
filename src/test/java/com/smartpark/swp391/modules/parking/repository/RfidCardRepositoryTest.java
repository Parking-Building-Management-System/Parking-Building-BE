package com.smartpark.swp391.modules.parking.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.smartpark.swp391.modules.identity.entity.Tenant;
import com.smartpark.swp391.modules.identity.repository.TenantRepository;
import com.smartpark.swp391.modules.operation.entity.ParkingSession;
import com.smartpark.swp391.modules.operation.enumType.ParkingSessionStatus;
import com.smartpark.swp391.modules.operation.repository.ParkingSessionRepository;
import com.smartpark.swp391.modules.parking.entity.Parking;
import com.smartpark.swp391.modules.parking.entity.RfidCard;
import com.smartpark.swp391.modules.parking.entity.Slot;
import com.smartpark.swp391.modules.parking.enumType.RfidCardStatus;
import com.smartpark.swp391.modules.vehicle.entity.VehicleType;
import com.smartpark.swp391.modules.vehicle.repository.VehicleTypeRepository;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@DataJpaTest
@Testcontainers(disabledWithoutDocker = true)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class RfidCardRepositoryTest {

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

  @Autowired RfidCardRepository rfidCardRepository;
  @Autowired ParkingRepository parkingRepository;
  @Autowired SlotRepository slotRepository;
  @Autowired TenantRepository tenantRepository;
  @Autowired VehicleTypeRepository vehicleTypeRepository;
  @Autowired ParkingSessionRepository parkingSessionRepository;
  @Autowired JdbcTemplate jdbcTemplate;

  @Test
  void activeUnusedCardIsReturnedWhileLostInactiveAndBlockedCardsAreExcluded() {
    Tenant tenant = tenantRepository.findBySlug("bcons-plaza").orElseThrow();
    String prefix = uniquePrefix("STATUS");
    RfidCard activeCard = createCard(tenant, prefix + "-ACTIVE", RfidCardStatus.ACTIVE);
    RfidCard lostCard = createCard(tenant, prefix + "-LOST", RfidCardStatus.LOST);
    RfidCard inactiveCard = createCard(tenant, prefix + "-INACTIVE", RfidCardStatus.INACTIVE);
    RfidCard blockedCard = createCard(tenant, prefix + "-BLOCKED", RfidCardStatus.BLOCKED);

    var cards =
        rfidCardRepository.searchAvailableForStaffParking(
            tenant.getId(), RfidCardStatus.ACTIVE, prefix, PageRequest.of(0, 20));

    assertThat(cards).extracting(RfidCard::getId).containsExactly(activeCard.getId());
    assertThat(cards)
        .extracting(RfidCard::getId)
        .doesNotContain(lostCard.getId(), inactiveCard.getId(), blockedCard.getId());
  }

  @Test
  void nonCanonicalSuspendedStatusIsExcludedByActiveOnlyPredicate() {
    Tenant tenant = tenantRepository.findBySlug("bcons-plaza").orElseThrow();
    String code = uniquePrefix("SUSPENDED");
    UUID id = UUID.randomUUID();
    jdbcTemplate.update(
        """
        INSERT INTO rfid_cards (id, tenant_id, code, uid, qr_token, status, activated_at)
        VALUES (?, ?, ?, ?, ?, 'SUSPENDED', CURRENT_TIMESTAMP)
        """,
        id,
        tenant.getId(),
        code,
        "UID-" + id,
        "QR-" + id);

    var cards =
        rfidCardRepository.searchAvailableForStaffParking(
            tenant.getId(), RfidCardStatus.ACTIVE, code, PageRequest.of(0, 20));

    assertThat(cards).isEmpty();
  }

  @Test
  void activeCardWithActiveSessionAtAnotherParkingIsExcluded() {
    Tenant tenant = tenantRepository.findBySlug("vincom-mega-mall").orElseThrow();
    Parking otherParking =
        parkingRepository
            .findByTenantIdAndCodeIgnoreCase(tenant.getId(), "VINCOM-TD")
            .orElseThrow();
    RfidCard usedCard = createCard(tenant, uniquePrefix("ACTIVE-SESSION"), RfidCardStatus.ACTIVE);
    createSession(tenant, otherParking, usedCard, ParkingSessionStatus.ACTIVE);

    var cards =
        rfidCardRepository.searchAvailableForStaffParking(
            tenant.getId(), RfidCardStatus.ACTIVE, usedCard.getCode(), PageRequest.of(0, 20));

    assertThat(cards).isEmpty();
  }

  @Test
  void activeCardWithCompletedHistoricalSessionIsAvailableAgain() {
    Tenant tenant = tenantRepository.findBySlug("vincom-mega-mall").orElseThrow();
    Parking parking =
        parkingRepository
            .findByTenantIdAndCodeIgnoreCase(tenant.getId(), "VINCOM-DK")
            .orElseThrow();
    RfidCard card = createCard(tenant, uniquePrefix("COMPLETED"), RfidCardStatus.ACTIVE);
    createSession(tenant, parking, card, ParkingSessionStatus.COMPLETED);

    var cards =
        rfidCardRepository.searchAvailableForStaffParking(
            tenant.getId(), RfidCardStatus.ACTIVE, card.getCode(), PageRequest.of(0, 20));

    assertThat(cards).extracting(RfidCard::getId).containsExactly(card.getId());
  }

  @Test
  void availableCardsAreTenantScoped() {
    Tenant tenant = tenantRepository.findBySlug("bcons-plaza").orElseThrow();
    Tenant otherTenant = tenantRepository.findBySlug("fpt-tower").orElseThrow();
    String code = uniquePrefix("TENANT");
    RfidCard tenantCard = createCard(tenant, code, RfidCardStatus.ACTIVE);
    RfidCard otherTenantCard = createCard(otherTenant, code, RfidCardStatus.ACTIVE);

    var cards =
        rfidCardRepository.searchAvailableForStaffParking(
            tenant.getId(), RfidCardStatus.ACTIVE, code, PageRequest.of(0, 20));

    assertThat(cards).extracting(RfidCard::getId).containsExactly(tenantCard.getId());
    assertThat(cards).extracting(RfidCard::getId).doesNotContain(otherTenantCard.getId());
  }

  @Test
  void availableCardSearchSupportsExactAndPartialCode() {
    Tenant tenant = tenantRepository.findBySlug("bcons-plaza").orElseThrow();
    String prefix = uniquePrefix("SEARCH");
    RfidCard card = createCard(tenant, prefix + "-EXACT", RfidCardStatus.ACTIVE);

    var exact =
        rfidCardRepository.searchAvailableForStaffParking(
            tenant.getId(), RfidCardStatus.ACTIVE, card.getCode(), PageRequest.of(0, 20));
    var partial =
        rfidCardRepository.searchAvailableForStaffParking(
            tenant.getId(), RfidCardStatus.ACTIVE, prefix, PageRequest.of(0, 20));

    assertThat(exact).extracting(RfidCard::getId).containsExactly(card.getId());
    assertThat(partial).extracting(RfidCard::getId).containsExactly(card.getId());
  }

  @Test
  void availableCardSearchIsCaseInsensitive() {
    Tenant tenant = tenantRepository.findBySlug("bcons-plaza").orElseThrow();
    RfidCard card = createCard(tenant, uniquePrefix("CASE") + "-MiXeD", RfidCardStatus.ACTIVE);

    var cards =
        rfidCardRepository.searchAvailableForStaffParking(
            tenant.getId(),
            RfidCardStatus.ACTIVE,
            card.getCode().toLowerCase(),
            PageRequest.of(0, 20));

    assertThat(cards).extracting(RfidCard::getId).containsExactly(card.getId());
  }

  @Test
  void availableCardsWithoutSearchUseDeterministicCodeOrdering() {
    Tenant tenant = tenantRepository.findBySlug("bcons-plaza").orElseThrow();
    String prefix = uniquePrefix("ORDER");
    RfidCard cardC = createCard(tenant, prefix + "-C", RfidCardStatus.ACTIVE);
    RfidCard cardA = createCard(tenant, prefix + "-A", RfidCardStatus.ACTIVE);
    RfidCard cardB = createCard(tenant, prefix + "-B", RfidCardStatus.ACTIVE);

    var cards =
        rfidCardRepository.searchAvailableForStaffParking(
            tenant.getId(), RfidCardStatus.ACTIVE, prefix, PageRequest.of(0, 20));

    assertThat(cards)
        .extracting(RfidCard::getId)
        .containsExactly(cardA.getId(), cardB.getId(), cardC.getId());
  }

  @Test
  void availableCardsWithoutSearchReturnNormalResults() {
    Tenant tenant = tenantRepository.findBySlug("vincom-mega-mall").orElseThrow();

    var cards =
        rfidCardRepository.findAvailableForStaffParking(
            tenant.getId(), RfidCardStatus.ACTIVE, PageRequest.of(0, 5));

    assertThat(cards).isNotEmpty();
  }

  @Test
  void managerSearchIsCaseInsensitivePaginatedStatusFilteredAndTenantScoped() {
    Tenant tenant = tenantRepository.findBySlug("bcons-plaza").orElseThrow();
    Tenant otherTenant = tenantRepository.findBySlug("fpt-tower").orElseThrow();
    RfidCard lostCard =
        rfidCardRepository
            .findByTenantIdAndCodeIgnoreCase(tenant.getId(), "BCONS-0001")
            .orElseThrow();
    lostCard.setStatus(RfidCardStatus.LOST);
    rfidCardRepository.saveAndFlush(lostCard);
    var firstPage =
        rfidCardRepository.searchByTenantId(
            tenant.getId(), "bcons-00", PageRequest.of(0, 2, Sort.by("code").ascending()));

    assertThat(firstPage.getContent()).hasSize(2);
    assertThat(firstPage.getTotalElements()).isGreaterThan(2);
    assertThat(firstPage.getTotalPages()).isGreaterThan(1);
    assertThat(firstPage.getContent())
        .extracting(RfidCard::getCode)
        .isSorted()
        .allMatch(code -> code.toLowerCase().contains("bcons-00"));
    assertThat(firstPage.getContent())
        .allMatch(card -> card.getTenant().getId().equals(tenant.getId()));

    var uidSearch =
        rfidCardRepository.searchByTenantId(
            tenant.getId(),
            lostCard.getUid().toLowerCase(),
            PageRequest.of(0, 10, Sort.by("code").ascending()));
    assertThat(uidSearch.getContent())
        .extracting(RfidCard::getId)
        .containsExactly(lostCard.getId());

    var combined =
        rfidCardRepository.searchByTenantIdAndStatus(
            tenant.getId(),
            RfidCardStatus.LOST,
            "bcons-000",
            PageRequest.of(0, 10, Sort.by("code").ascending()));
    assertThat(combined.getContent()).extracting(RfidCard::getId).containsExactly(lostCard.getId());

    var otherTenantResults =
        rfidCardRepository.searchByTenantId(
            otherTenant.getId(), "bcons-0001", PageRequest.of(0, 10, Sort.by("code").ascending()));
    assertThat(otherTenantResults).isEmpty();
  }

  private RfidCard createCard(Tenant tenant, String code, RfidCardStatus status) {
    String uniqueValue = UUID.randomUUID().toString();
    return rfidCardRepository.saveAndFlush(
        RfidCard.builder()
            .tenant(tenant)
            .code(code)
            .uid("UID-" + uniqueValue)
            .qrToken("QR-" + uniqueValue)
            .status(status)
            .activatedAt(LocalDateTime.now())
            .build());
  }

  private String uniquePrefix(String purpose) {
    return "TEST-" + purpose + "-" + UUID.randomUUID().toString().substring(0, 8);
  }

  private void createSession(
      Tenant tenant, Parking parking, RfidCard card, ParkingSessionStatus status) {
    Slot slot =
        slotRepository
            .findFirstAvailableForCheckIn(
                tenant.getId(),
                parking.getId(),
                com.smartpark.swp391.modules.parking.enumType.SlotStatus.AVAILABLE,
                PageRequest.of(0, 1))
            .getFirst();
    VehicleType vehicleType =
        vehicleTypeRepository.findByCodeIgnoreCaseAndDeletedFalse("CAR").orElseThrow();
    ParkingSession session =
        ParkingSession.builder()
            .tenant(tenant)
            .parking(parking)
            .zone(slot.getZone())
            .slot(slot)
            .rfidCard(card)
            .vehicleType(vehicleType)
            .licensePlate("TEST-" + UUID.randomUUID().toString().substring(0, 8))
            .checkInAt(LocalDateTime.now())
            .checkOutAt(status == ParkingSessionStatus.COMPLETED ? LocalDateTime.now() : null)
            .status(status)
            .build();
    parkingSessionRepository.saveAndFlush(session);
  }
}
