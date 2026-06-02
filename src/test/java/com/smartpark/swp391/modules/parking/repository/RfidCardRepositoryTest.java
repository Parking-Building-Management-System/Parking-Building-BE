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
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
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

  @Test
  void availableCardsIncludeOnlyActiveCardsAndExcludeActiveSessionsInParking() {
    Tenant tenant = tenantRepository.findBySlug("bcons-plaza").orElseThrow();
    Parking parking =
        parkingRepository
            .findByTenantIdAndCodeIgnoreCaseAndIsDeletedFalse(tenant.getId(), "BCONS-PLAZA")
            .orElseThrow();
    RfidCard availableCard =
        rfidCardRepository
            .findByTenantIdAndCodeIgnoreCase(tenant.getId(), "BCONS-0001")
            .orElseThrow();
    RfidCard inactiveCard =
        rfidCardRepository
            .findByTenantIdAndCodeIgnoreCase(tenant.getId(), "BCONS-0002")
            .orElseThrow();
    RfidCard usedCard =
        rfidCardRepository
            .findByTenantIdAndCodeIgnoreCase(tenant.getId(), "BCONS-0003")
            .orElseThrow();
    inactiveCard.setStatus(RfidCardStatus.INACTIVE);
    rfidCardRepository.save(inactiveCard);
    createActiveSession(tenant, parking, usedCard);

    var cards =
        rfidCardRepository.findAvailableForStaffParking(
            tenant.getId(),
            parking.getId(),
            RfidCardStatus.ACTIVE,
            "BCONS-000",
            PageRequest.of(0, 20));

    assertThat(cards).extracting(RfidCard::getCode).contains(availableCard.getCode());
    assertThat(cards).extracting(RfidCard::getCode).doesNotContain(inactiveCard.getCode());
    assertThat(cards).extracting(RfidCard::getCode).doesNotContain(usedCard.getCode());
  }

  private void createActiveSession(Tenant tenant, Parking parking, RfidCard card) {
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
            .licensePlate("51A-12345")
            .checkInAt(LocalDateTime.now())
            .status(ParkingSessionStatus.ACTIVE)
            .build();
    parkingSessionRepository.save(session);
  }
}
