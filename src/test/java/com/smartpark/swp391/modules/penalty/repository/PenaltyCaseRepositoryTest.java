package com.smartpark.swp391.modules.penalty.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.smartpark.swp391.modules.identity.entity.Tenant;
import com.smartpark.swp391.modules.identity.repository.TenantRepository;
import com.smartpark.swp391.modules.parking.entity.Parking;
import com.smartpark.swp391.modules.parking.repository.ParkingRepository;
import com.smartpark.swp391.modules.penalty.entity.PenaltyCase;
import com.smartpark.swp391.modules.penalty.enumType.PenaltyCaseStatus;
import com.smartpark.swp391.modules.penalty.enumType.PenaltyType;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
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
class PenaltyCaseRepositoryTest {

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

  @Autowired PenaltyCaseRepository penaltyCaseRepository;
  @Autowired TenantRepository tenantRepository;
  @Autowired ParkingRepository parkingRepository;
  @Autowired EntityManager entityManager;

  private Tenant currentTenant;
  private Parking currentParking;
  private PenaltyCase reportedCase;
  private PenaltyCase appliedCase;

  @BeforeEach
  void setUp() {
    currentTenant = tenantRepository.findBySlug("bcons-plaza").orElseThrow();
    currentParking =
        parkingRepository
            .findByTenantIdAndCodeIgnoreCase(currentTenant.getId(), "BCONS-PLAZA")
            .orElseThrow();
    Tenant otherTenant = tenantRepository.findBySlug("fpt-tower").orElseThrow();
    Parking otherTenantParking =
        parkingRepository
            .findByTenantIdAndCodeIgnoreCase(otherTenant.getId(), "FPT-TOWER")
            .orElseThrow();
    Parking otherCurrentTenantParking =
        parkingRepository.save(
            Parking.builder()
                .tenant(currentTenant)
                .code("OTHER-" + System.nanoTime())
                .name("Other current tenant parking")
                .build());

    reportedCase =
        saveReport(
            currentTenant,
            currentParking,
            "51A-AbC-12345",
            PenaltyCaseStatus.REPORTED,
            LocalDateTime.now().minusMinutes(5));
    appliedCase =
        saveReport(
            currentTenant,
            currentParking,
            "51A-XYZ-67890",
            PenaltyCaseStatus.APPLIED,
            LocalDateTime.now().minusMinutes(10));
    saveReport(
        currentTenant,
        otherCurrentTenantParking,
        "51A-OTHER-00001",
        PenaltyCaseStatus.REPORTED,
        LocalDateTime.now().minusMinutes(15));
    saveReport(
        otherTenant,
        otherTenantParking,
        "29A-TENANT-00001",
        PenaltyCaseStatus.REPORTED,
        LocalDateTime.now().minusMinutes(20));
    entityManager.flush();
    setCreatedAt(reportedCase, LocalDateTime.now().minusMinutes(5));
    setCreatedAt(appliedCase, LocalDateTime.now().minusMinutes(10));
    entityManager.clear();
  }

  @Test
  void noSearchNullBlankAndWhitespaceReturnCurrentParkingReportsWithoutPostgresGrammarErrors() {
    List<PenaltyCase> withoutSearch = findReports(null, null);
    List<PenaltyCase> blankSearch = findReports(null, "");
    List<PenaltyCase> whitespaceSearch = findReports(null, "   ");

    assertThat(withoutSearch)
        .extracting(PenaltyCase::getId)
        .containsExactlyInAnyOrder(reportedCase.getId(), appliedCase.getId());
    assertThat(blankSearch)
        .extracting(PenaltyCase::getId)
        .containsExactlyInAnyOrderElementsOf(
            withoutSearch.stream().map(PenaltyCase::getId).toList());
    assertThat(whitespaceSearch)
        .extracting(PenaltyCase::getId)
        .containsExactlyInAnyOrderElementsOf(
            withoutSearch.stream().map(PenaltyCase::getId).toList());
  }

  @Test
  void searchIsCaseInsensitiveAndStatusFiltersWorkWithOrWithoutSearch() {
    assertThat(findReports(null, "abc-123"))
        .extracting(PenaltyCase::getId)
        .containsExactly(reportedCase.getId());
    assertThat(findReports(PenaltyCaseStatus.REPORTED, null))
        .extracting(PenaltyCase::getId)
        .containsExactly(reportedCase.getId());
    assertThat(findReports(PenaltyCaseStatus.REPORTED, "ABC"))
        .extracting(PenaltyCase::getId)
        .containsExactly(reportedCase.getId());
  }

  @Test
  void filtersDatesAndExcludesOtherParkingAndTenantReports() {
    List<PenaltyCase> reports =
        penaltyCaseRepository.findViolationReports(
            currentTenant.getId(),
            currentParking.getId(),
            null,
            null,
            LocalDateTime.now().minusMinutes(7),
            LocalDateTime.now());

    assertThat(reports).extracting(PenaltyCase::getId).containsExactly(reportedCase.getId());
  }

  @Test
  void pendingViolationCountIncludesOnlyReportedPwaOccupiedSlotCasesInCurrentScope() {
    saveCase(
        currentTenant,
        currentParking,
        PenaltyType.OCCUPIED_ASSIGNED_SLOT,
        PenaltyCaseStatus.REPORTED,
        false);
    saveCase(
        currentTenant, currentParking, PenaltyType.LOST_CARD, PenaltyCaseStatus.REPORTED, true);
    saveCase(
        currentTenant,
        currentParking,
        PenaltyType.OCCUPIED_ASSIGNED_SLOT,
        PenaltyCaseStatus.REJECTED,
        true);
    saveCase(
        currentTenant,
        currentParking,
        PenaltyType.OCCUPIED_ASSIGNED_SLOT,
        PenaltyCaseStatus.COLLECTED,
        true);
    entityManager.flush();
    entityManager.clear();

    assertThat(
            penaltyCaseRepository.countPendingViolationReports(
                currentTenant.getId(), currentParking.getId()))
        .isEqualTo(1);
  }

  private List<PenaltyCase> findReports(PenaltyCaseStatus status, String search) {
    return penaltyCaseRepository.findViolationReports(
        currentTenant.getId(), currentParking.getId(), status, search, null, null);
  }

  private PenaltyCase saveReport(
      Tenant tenant,
      Parking parking,
      String plate,
      PenaltyCaseStatus status,
      LocalDateTime createdAt) {
    PenaltyCase penaltyCase =
        PenaltyCase.builder()
            .tenant(tenant)
            .parking(parking)
            .type(PenaltyType.OCCUPIED_ASSIGNED_SLOT)
            .amount(BigDecimal.ZERO)
            .status(status)
            .offenderLicensePlate(plate)
            .reportedFromPwa(true)
            .build();
    penaltyCase.setCreatedAt(createdAt);
    return penaltyCaseRepository.save(penaltyCase);
  }

  private PenaltyCase saveCase(
      Tenant tenant,
      Parking parking,
      PenaltyType type,
      PenaltyCaseStatus status,
      boolean reportedFromPwa) {
    return penaltyCaseRepository.save(
        PenaltyCase.builder()
            .tenant(tenant)
            .parking(parking)
            .type(type)
            .amount(BigDecimal.ZERO)
            .status(status)
            .reportedFromPwa(reportedFromPwa)
            .build());
  }

  private void setCreatedAt(PenaltyCase penaltyCase, LocalDateTime createdAt) {
    entityManager
        .createNativeQuery(
            "UPDATE parking_penalty_cases SET created_at = :createdAt WHERE id = :id")
        .setParameter("createdAt", createdAt)
        .setParameter("id", penaltyCase.getId())
        .executeUpdate();
  }
}
