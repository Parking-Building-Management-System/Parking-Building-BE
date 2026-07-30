package com.smartpark.swp391.modules.manager.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import com.smartpark.swp391.modules.identity.entity.Tenant;
import com.smartpark.swp391.modules.identity.repository.TenantRepository;
import com.smartpark.swp391.modules.manager.repository.ManagerAnalyticsRepository.AverageOccupancyAggregate;
import com.smartpark.swp391.modules.manager.repository.ManagerAnalyticsRepository.CurrentOccupancyAggregate;
import com.smartpark.swp391.modules.manager.repository.ManagerAnalyticsRepository.RevenueAggregate;
import com.smartpark.swp391.modules.manager.repository.ManagerAnalyticsRepository.TrafficAggregate;
import com.smartpark.swp391.modules.manager.support.AnalyticsTrendGranularity;
import com.smartpark.swp391.modules.parking.entity.Parking;
import com.smartpark.swp391.modules.parking.repository.ParkingRepository;
import com.smartpark.swp391.modules.vehicle.entity.VehicleType;
import com.smartpark.swp391.modules.vehicle.repository.VehicleTypeRepository;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@DataJpaTest
@Import(ManagerAnalyticsRepository.class)
@Testcontainers(disabledWithoutDocker = true)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ManagerAnalyticsRepositoryTest {

  private static final ZoneOffset HCM = ZoneOffset.ofHours(7);
  private static final OffsetDateTime JULY_FROM = OffsetDateTime.of(2026, 7, 1, 0, 0, 0, 0, HCM);
  private static final OffsetDateTime JULY_TO = OffsetDateTime.of(2026, 8, 1, 0, 0, 0, 0, HCM);
  private static final AtomicLong ORDER_CODES = new AtomicLong(9_000_000_000L);

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

  @Autowired ManagerAnalyticsRepository analyticsRepository;
  @Autowired TenantRepository tenantRepository;
  @Autowired ParkingRepository parkingRepository;
  @Autowired VehicleTypeRepository vehicleTypeRepository;
  @Autowired JdbcTemplate jdbc;

  private UUID tenantId;
  private UUID otherTenantId;
  private UUID parkingId;
  private UUID otherParkingId;
  private UUID otherTenantParkingId;
  private UUID carId;
  private UUID motorbikeId;
  private UUID currentCarSessionId;
  private UUID currentMotorbikeSessionId;
  private UUID comparisonCarSessionId;
  private UUID kioskId;
  private UUID staffId;
  private UUID shiftId;

  @BeforeEach
  void setUp() {
    Tenant tenant = tenantRepository.findBySlug("vincom-mega-mall").orElseThrow();
    Tenant otherTenant = tenantRepository.findBySlug("fpt-tower").orElseThrow();
    VehicleType car =
        vehicleTypeRepository.findByCodeIgnoreCaseAndDeletedFalse("CAR").orElseThrow();
    VehicleType motorbike =
        vehicleTypeRepository.findByCodeIgnoreCaseAndDeletedFalse("MOTORBIKE").orElseThrow();
    tenantId = tenant.getId();
    otherTenantId = otherTenant.getId();
    carId = car.getId();
    motorbikeId = motorbike.getId();

    parkingId = insertParking(tenantId, "ANALYTICS");
    otherParkingId = insertParking(tenantId, "ANALYTICS-OTHER");
    otherTenantParkingId = insertParking(otherTenantId, "ANALYTICS-TENANT");

    Facility primary = insertFacility(tenantId, parkingId, carId, "PRIMARY-CAR", "ACTIVE");
    Facility motorcycle =
        insertFacility(tenantId, parkingId, motorbikeId, "PRIMARY-MOTOR", "ACTIVE");
    Facility inactive = insertFacility(tenantId, parkingId, carId, "INACTIVE-CAR", "INACTIVE");
    Facility other = insertFacility(tenantId, otherParkingId, carId, "OTHER-CAR", "ACTIVE");
    Facility otherTenantFacility =
        insertFacility(otherTenantId, otherTenantParkingId, carId, "TENANT-CAR", "ACTIVE");

    insertCapacitySlots(primary, motorcycle, inactive);
    insertSessionFixtures(primary, motorcycle, other, otherTenantFacility);
    resolveCashReferences();
    insertRevenueFixtures();
  }

  @Test
  void trafficUsesHcmHalfOpenBoundariesAndIsolatesTenantParkingAndVehicleType() {
    List<TrafficAggregate> all =
        analyticsRepository.traffic(tenantId, parkingId, null, JULY_FROM, JULY_TO);
    List<TrafficAggregate> cars =
        analyticsRepository.traffic(tenantId, parkingId, carId, JULY_FROM, JULY_TO);
    List<TrafficAggregate> wrongTenant =
        analyticsRepository.traffic(otherTenantId, parkingId, null, JULY_FROM, JULY_TO);

    assertThat(all).extracting(TrafficAggregate::entries).containsExactlyInAnyOrder(2L, 1L);
    assertThat(all).extracting(TrafficAggregate::exits).containsExactlyInAnyOrder(2L, 1L);
    assertThat(cars)
        .singleElement()
        .satisfies(
            row -> {
              assertThat(row.vehicleTypeId()).isEqualTo(carId);
              assertThat(row.entries()).isEqualTo(2);
              assertThat(row.exits()).isEqualTo(2);
            });
    assertThat(wrongTenant).isEmpty();

    var trend =
        analyticsRepository.trafficTrend(
            tenantId, parkingId, null, JULY_FROM, JULY_TO, AnalyticsTrendGranularity.DAY);
    assertThat(trend)
        .anySatisfy(
            point -> {
              assertThat(point.bucketStart()).isEqualTo(JULY_FROM);
              assertThat(point.entries()).isEqualTo(1);
              assertThat(point.exits()).isEqualTo(1);
            });
  }

  @Test
  void revenueUsesPaidPayosAndCashLedgerWithoutSessionTotalOrAdjustments() {
    List<RevenueAggregate> current =
        analyticsRepository.revenue(tenantId, parkingId, null, JULY_FROM, JULY_TO);
    List<RevenueAggregate> comparison =
        analyticsRepository.revenue(
            tenantId, parkingId, null, JULY_FROM.minusYears(1), JULY_TO.minusYears(1));
    List<RevenueAggregate> carOnly =
        analyticsRepository.revenue(tenantId, parkingId, carId, JULY_FROM, JULY_TO);
    List<RevenueAggregate> motorbikeOnly =
        analyticsRepository.revenue(tenantId, parkingId, motorbikeId, JULY_FROM, JULY_TO);

    assertThat(sum(current)).isEqualByComparingTo("165.00");
    assertThat(amount(current, "PAYOS")).isEqualByComparingTo("111.00");
    assertThat(amount(current, "PARKING_CASH")).isEqualByComparingTo("33.00");
    assertThat(amount(current, "SURCHARGE_CASH")).isEqualByComparingTo("5.00");
    assertThat(amount(current, "PENALTY_CASH")).isEqualByComparingTo("7.00");
    assertThat(amount(current, "LOST_CARD_FINE")).isEqualByComparingTo("9.00");
    assertThat(current).noneMatch(row -> "ADJUSTMENT".equals(row.source()));
    assertThat(sum(comparison)).isEqualByComparingTo("90.00");
    assertThat(sum(carOnly)).isEqualByComparingTo("141.00");
    assertThat(sum(motorbikeOnly)).isEqualByComparingTo("24.00");
  }

  @Test
  void currentOccupancyExcludesDeletedUnavailableAndInactiveZoneSlots() {
    List<CurrentOccupancyAggregate> all =
        analyticsRepository.currentOccupancy(tenantId, parkingId, null);
    List<CurrentOccupancyAggregate> cars =
        analyticsRepository.currentOccupancy(tenantId, parkingId, carId);

    assertThat(all).hasSize(2);
    assertThat(all.stream().mapToLong(CurrentOccupancyAggregate::usable).sum()).isEqualTo(7);
    assertThat(all.stream().mapToLong(CurrentOccupancyAggregate::occupied).sum()).isEqualTo(3);
    assertThat(all.stream().mapToLong(CurrentOccupancyAggregate::available).sum()).isEqualTo(3);
    assertThat(all.stream().mapToLong(CurrentOccupancyAggregate::reserved).sum()).isEqualTo(1);
    assertThat(cars)
        .singleElement()
        .satisfies(
            row -> {
              assertThat(row.usable()).isEqualTo(5);
              assertThat(row.occupied()).isEqualTo(2);
              assertThat(row.available()).isEqualTo(2);
              assertThat(row.reserved()).isEqualTo(1);
            });
  }

  @Test
  void averageOccupancyUsesHourlySessionOverlapAndCurrentVehicleFiltering() {
    OffsetDateTime dayFrom = OffsetDateTime.of(2026, 7, 15, 0, 0, 0, 0, HCM);
    OffsetDateTime dayTo = dayFrom.plusDays(1);

    List<AverageOccupancyAggregate> all =
        analyticsRepository.averageOccupancy(tenantId, parkingId, null, dayFrom, dayTo);
    List<AverageOccupancyAggregate> cars =
        analyticsRepository.averageOccupancy(tenantId, parkingId, carId, dayFrom, dayTo);

    assertThat(average(all, null))
        .isCloseTo(new BigDecimal("0.125"), within(new BigDecimal("0.0001")));
    assertThat(average(all, carId))
        .isCloseTo(new BigDecimal("0.083333"), within(new BigDecimal("0.0001")));
    assertThat(average(all, motorbikeId))
        .isCloseTo(new BigDecimal("0.041667"), within(new BigDecimal("0.0001")));
    assertThat(average(cars, null))
        .isCloseTo(new BigDecimal("0.083333"), within(new BigDecimal("0.0001")));
    assertThat(cars).noneMatch(row -> motorbikeId.equals(row.vehicleTypeId()));
  }

  @Test
  void peakHoursUseHcmLocalHoursAndReturnAtMostThreePerVehicleType() {
    var peaks = analyticsRepository.peakHours(tenantId, parkingId, null, JULY_FROM, JULY_TO);

    assertThat(peaks)
        .filteredOn(row -> carId.equals(row.vehicleTypeId()))
        .hasSizeLessThanOrEqualTo(3)
        .anySatisfy(row -> assertThat(row.hour()).isEqualTo(8));
    assertThat(peaks)
        .filteredOn(row -> motorbikeId.equals(row.vehicleTypeId()))
        .anySatisfy(
            row -> {
              assertThat(row.hour()).isEqualTo(8);
              assertThat(row.entryCount()).isEqualTo(1);
            });
  }

  private UUID insertParking(UUID ownerTenantId, String codePrefix) {
    UUID id = UUID.randomUUID();
    jdbc.update(
        """
        INSERT INTO parkings (
          id, tenant_id, code, name, total_capacity, status, is_deleted
        ) VALUES (?, ?, ?, ?, 0, 'ACTIVE', false)
        """,
        id,
        ownerTenantId,
        codePrefix + "-" + id.toString().substring(0, 8),
        "Analytics test parking");
    return id;
  }

  private Facility insertFacility(
      UUID ownerTenantId,
      UUID ownerParkingId,
      UUID vehicleTypeId,
      String codePrefix,
      String status) {
    UUID zoneId = UUID.randomUUID();
    jdbc.update(
        """
        INSERT INTO zones (
          id, tenant_id, parking_id, vehicle_type_id, code, name, capacity, status, is_deleted
        ) VALUES (?, ?, ?, ?, ?, ?, 0, ?, false)
        """,
        zoneId,
        ownerTenantId,
        ownerParkingId,
        vehicleTypeId,
        codePrefix + "-" + zoneId,
        codePrefix,
        status);
    UUID slotId = insertSlot(ownerTenantId, ownerParkingId, zoneId, "AVAILABLE", false);
    return new Facility(ownerTenantId, ownerParkingId, vehicleTypeId, zoneId, slotId);
  }

  private void insertCapacitySlots(Facility car, Facility motorcycle, Facility inactive) {
    jdbc.update("DELETE FROM slots WHERE id = ?", car.sessionSlotId());
    insertSlot(car.tenantId(), car.parkingId(), car.zoneId(), "OCCUPIED", false);
    car =
        car.withSessionSlot(
            insertSlot(car.tenantId(), car.parkingId(), car.zoneId(), "OCCUPIED", false));
    insertSlot(car.tenantId(), car.parkingId(), car.zoneId(), "AVAILABLE", false);
    insertSlot(car.tenantId(), car.parkingId(), car.zoneId(), "AVAILABLE", false);
    insertSlot(car.tenantId(), car.parkingId(), car.zoneId(), "RESERVED", false);
    insertSlot(car.tenantId(), car.parkingId(), car.zoneId(), "MAINTENANCE", false);
    insertSlot(car.tenantId(), car.parkingId(), car.zoneId(), "LOCKED", false);
    insertSlot(car.tenantId(), car.parkingId(), car.zoneId(), "OCCUPIED", true);
    insertSlot(car.tenantId(), otherParkingId, car.zoneId(), "OCCUPIED", false);

    jdbc.update("UPDATE slots SET status = 'OCCUPIED' WHERE id = ?", motorcycle.sessionSlotId());
    insertSlot(
        motorcycle.tenantId(), motorcycle.parkingId(), motorcycle.zoneId(), "AVAILABLE", false);
    jdbc.update("UPDATE slots SET status = 'AVAILABLE' WHERE id = ?", inactive.sessionSlotId());

    primaryCarFacility = car;
  }

  private Facility primaryCarFacility;

  private UUID insertSlot(
      UUID ownerTenantId, UUID ownerParkingId, UUID zoneId, String status, boolean deleted) {
    UUID id = UUID.randomUUID();
    String code = "S-" + id;
    jdbc.update(
        """
        INSERT INTO slots (
          id, tenant_id, parking_id, zone_id, code, slot_number, status, is_deleted
        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        """,
        id,
        ownerTenantId,
        ownerParkingId,
        zoneId,
        code,
        code,
        status,
        deleted);
    return id;
  }

  private void insertSessionFixtures(
      Facility initialCar,
      Facility motorcycle,
      Facility otherParking,
      Facility otherTenantParking) {
    Facility car = primaryCarFacility == null ? initialCar : primaryCarFacility;
    OffsetDateTime justBefore = JULY_FROM.minusMinutes(1);
    OffsetDateTime exactEnd = JULY_TO;
    insertSession(car, carId, justBefore.minusHours(1), justBefore, new BigDecimal("9999.00"));
    currentCarSessionId =
        insertSession(
            car,
            carId,
            JULY_FROM,
            JULY_FROM.plusHours(1).plusMinutes(30),
            new BigDecimal("9999.00"));
    insertSession(
        car,
        carId,
        OffsetDateTime.of(2026, 7, 15, 8, 15, 0, 0, HCM),
        OffsetDateTime.of(2026, 7, 15, 9, 10, 0, 0, HCM),
        BigDecimal.ZERO);
    currentMotorbikeSessionId =
        insertSession(
            motorcycle,
            motorbikeId,
            OffsetDateTime.of(2026, 7, 15, 8, 30, 0, 0, HCM),
            OffsetDateTime.of(2026, 7, 15, 8, 45, 0, 0, HCM),
            BigDecimal.ZERO);
    insertSession(car, carId, exactEnd, exactEnd.plusHours(1), BigDecimal.ZERO);
    comparisonCarSessionId =
        insertSession(
            car,
            carId,
            JULY_FROM.minusYears(1).plusDays(2),
            JULY_FROM.minusYears(1).plusDays(2).plusHours(1),
            BigDecimal.ZERO);
    insertSession(
        otherParking,
        carId,
        JULY_FROM.plusDays(3),
        JULY_FROM.plusDays(3).plusHours(1),
        BigDecimal.ZERO);
    insertSession(
        otherTenantParking,
        carId,
        JULY_FROM.plusDays(4),
        JULY_FROM.plusDays(4).plusHours(1),
        BigDecimal.ZERO);
  }

  private UUID insertSession(
      Facility facility,
      UUID vehicleTypeId,
      OffsetDateTime checkIn,
      OffsetDateTime checkOut,
      BigDecimal totalAmount) {
    UUID id = UUID.randomUUID();
    jdbc.update(
        """
        INSERT INTO parking_sessions (
          id, tenant_id, parking_id, zone_id, slot_id, vehicle_type_id,
          license_plate, check_in_at, check_out_at, status, total_amount
        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 'COMPLETED', ?)
        """,
        id,
        facility.tenantId(),
        facility.parkingId(),
        facility.zoneId(),
        facility.sessionSlotId(),
        vehicleTypeId,
        "AN-" + id.toString().substring(0, 8),
        checkIn,
        checkOut,
        totalAmount);
    return id;
  }

  private void resolveCashReferences() {
    Parking seededParking =
        parkingRepository.findByTenantIdAndCodeIgnoreCase(tenantId, "VINCOM-DK").orElseThrow();
    kioskId =
        jdbc.queryForObject(
            "SELECT id FROM kiosk WHERE tenant_id = ? AND parking_id = ? LIMIT 1",
            UUID.class,
            tenantId,
            seededParking.getId());
    staffId =
        jdbc.queryForObject(
            "SELECT staff_user_id FROM kiosk_staff WHERE tenant_id = ? LIMIT 1",
            UUID.class,
            tenantId);
    UUID operationalShiftId =
        jdbc.queryForObject(
            "SELECT shift_id FROM kiosk_staff WHERE tenant_id = ? LIMIT 1", UUID.class, tenantId);
    shiftId = UUID.randomUUID();
    jdbc.update(
        """
        INSERT INTO staff_cash_shifts (
          id, tenant_id, parking_id, kiosk_id, staff_id, opened_at, closed_at, status,
          staff_name_snapshot, staff_username_snapshot
        ) VALUES (?, ?, ?, ?, ?, ?, ?, 'CLOSED', 'Analytics Staff', 'analytics-staff')
        """,
        shiftId,
        tenantId,
        parkingId,
        kioskId,
        staffId,
        JULY_FROM,
        JULY_FROM.plusHours(8));
    assertThat(operationalShiftId).isNotNull();
  }

  private void insertRevenueFixtures() {
    insertIntent(
        tenantId, currentCarSessionId, "PAID", "PAYOS", false, new BigDecimal("100.00"), JULY_FROM);
    insertIntent(
        tenantId,
        currentMotorbikeSessionId,
        "PAID",
        "PAYOS",
        false,
        new BigDecimal("11.00"),
        JULY_FROM.plusHours(1));
    insertIntent(
        tenantId,
        currentCarSessionId,
        "CANCELLED",
        "PAYOS",
        false,
        new BigDecimal("500.00"),
        JULY_FROM.plusMinutes(1));
    insertIntent(
        tenantId,
        currentCarSessionId,
        "PAID",
        "PAYOS",
        true,
        new BigDecimal("600.00"),
        JULY_FROM.plusMinutes(2));
    insertIntent(
        tenantId,
        currentCarSessionId,
        "PAID",
        "MOMO",
        false,
        new BigDecimal("700.00"),
        JULY_FROM.plusMinutes(3));
    insertIntent(
        tenantId,
        comparisonCarSessionId,
        "PAID",
        "PAYOS",
        false,
        new BigDecimal("80.00"),
        JULY_FROM.minusYears(1).plusDays(2));
    insertIntent(
        tenantId, currentCarSessionId, "PAID", "PAYOS", false, new BigDecimal("900.00"), JULY_TO);

    insertCash("PARKING_CASH", "20.00", JULY_FROM.plusHours(2), currentCarSessionId, parkingId);
    insertCash(
        "PARKING_CASH", "13.00", JULY_FROM.plusHours(3), currentMotorbikeSessionId, parkingId);
    insertCash("SURCHARGE_CASH", "5.00", JULY_FROM.plusHours(2), currentCarSessionId, parkingId);
    insertCash("PENALTY_CASH", "7.00", JULY_FROM.plusHours(2), currentCarSessionId, parkingId);
    insertCash("LOST_CARD_FINE", "9.00", JULY_FROM.plusHours(2), currentCarSessionId, parkingId);
    insertCash("ADJUSTMENT", "1000.00", JULY_FROM.plusHours(2), currentCarSessionId, parkingId);
    insertCash(
        "PARKING_CASH",
        "10.00",
        JULY_FROM.minusYears(1).plusDays(2),
        comparisonCarSessionId,
        parkingId);
    insertCash(
        "PARKING_CASH", "800.00", JULY_FROM.plusHours(2), currentCarSessionId, otherParkingId);
    insertCash("PENALTY_CASH", "901.00", JULY_TO, currentCarSessionId, parkingId);
  }

  private void insertIntent(
      UUID ownerTenantId,
      UUID sessionId,
      String status,
      String provider,
      boolean deleted,
      BigDecimal amount,
      OffsetDateTime paidAt) {
    jdbc.update(
        """
        INSERT INTO payment_intents (
          id, tenant_id, parking_session_id, order_code, amount, currency,
          status, provider, paid_at, is_deleted
        ) VALUES (?, ?, ?, ?, ?, 'VND', ?, ?, ?, ?)
        """,
        UUID.randomUUID(),
        ownerTenantId,
        sessionId,
        ORDER_CODES.incrementAndGet(),
        amount,
        status,
        provider,
        paidAt,
        deleted);
  }

  private void insertCash(
      String type,
      String amount,
      OffsetDateTime occurredAt,
      UUID sessionId,
      UUID transactionParkingId) {
    jdbc.update(
        """
        INSERT INTO staff_cash_transactions (
          id, tenant_id, shift_id, parking_id, kiosk_id, staff_id,
          parking_session_id, type, amount, occurred_at, source
        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'NORMAL_EXIT')
        """,
        UUID.randomUUID(),
        tenantId,
        shiftId,
        transactionParkingId,
        kioskId,
        staffId,
        sessionId,
        type,
        new BigDecimal(amount),
        occurredAt);
  }

  private BigDecimal sum(List<RevenueAggregate> rows) {
    return rows.stream().map(RevenueAggregate::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  private BigDecimal amount(List<RevenueAggregate> rows, String source) {
    return rows.stream()
        .filter(row -> source.equals(row.source()))
        .map(RevenueAggregate::amount)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  private BigDecimal average(List<AverageOccupancyAggregate> rows, UUID vehicleTypeId) {
    return rows.stream()
        .filter(
            row ->
                vehicleTypeId == null
                    ? row.vehicleTypeId() == null
                    : vehicleTypeId.equals(row.vehicleTypeId()))
        .map(AverageOccupancyAggregate::averageActiveSessions)
        .findFirst()
        .orElseThrow();
  }

  private record Facility(
      UUID tenantId, UUID parkingId, UUID vehicleTypeId, UUID zoneId, UUID sessionSlotId) {
    Facility withSessionSlot(UUID value) {
      return new Facility(tenantId, parkingId, vehicleTypeId, zoneId, value);
    }
  }
}
