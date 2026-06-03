package com.smartpark.swp391.modules.firesafety.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.smartpark.swp391.modules.firesafety.entity.FireExtinguisherInspection;
import com.smartpark.swp391.modules.firesafety.enumType.FireInspectionResult;
import com.smartpark.swp391.modules.identity.entity.Tenant;
import com.smartpark.swp391.modules.identity.repository.TenantRepository;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@DataJpaTest
@Testcontainers(disabledWithoutDocker = true)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class FireExtinguisherInspectionRepositoryTest {

  @Container
  static final PostgreSQLContainer<?> POSTGRES =
      new PostgreSQLContainer<>("postgres:16-alpine")
          .withDatabaseName("smartpark-test")
          .withUsername("test")
          .withPassword("test");

  @DynamicPropertySource
  static void databaseProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", () -> POSTGRES.getJdbcUrl() + "&sslmode=disable");
    registry.add("spring.datasource.username", POSTGRES::getUsername);
    registry.add("spring.datasource.password", POSTGRES::getPassword);
    registry.add("spring.flyway.enabled", () -> "true");
  }

  @Autowired FireExtinguisherInspectionRepository inspectionRepository;
  @Autowired TenantRepository tenantRepository;
  @Autowired TestEntityManager entityManager;

  @Test
  void searchLogsWithoutFiltersReturnsTenantRowsAndDoesNotCrashPostgres() {
    UUID tenantId = tenantId("bcons-plaza");

    var logs = inspectionRepository.searchLogs(tenantId, null, null, null, null, null, null, page());

    assertThat(logs.getContent()).isNotEmpty();
    assertThat(logs.getContent())
        .allSatisfy(inspection -> assertThat(inspection.getTenant().getId()).isEqualTo(tenantId));
  }

  @Test
  void searchLogsWithNullFilterParamsDoesNotBindAmbiguousPostgresDates() {
    UUID tenantId = tenantId("bcons-plaza");

    var logs = inspectionRepository.searchLogs(tenantId, null, null, null, null, null, null, page());

    assertThat(logs.getContent()).isNotEmpty();
  }

  @Test
  void searchLogsFiltersByResult() {
    UUID tenantId = tenantId("bcons-plaza");

    var logs =
        inspectionRepository.searchLogs(
            tenantId, null, null, null, FireInspectionResult.OK, null, null, page());

    assertThat(logs.getContent()).isNotEmpty();
    assertThat(logs.getContent())
        .allSatisfy(inspection -> assertThat(inspection.getResult()).isEqualTo(FireInspectionResult.OK));
  }

  @Test
  void searchLogsFiltersByDateRange() {
    UUID tenantId = tenantId("bcons-plaza");
    FireExtinguisherInspection sample = sampleInspection(tenantId);
    LocalDateTime from = sample.getInspectedAt().minusSeconds(1);
    LocalDateTime to = sample.getInspectedAt().plusSeconds(1);

    var logs = inspectionRepository.searchLogs(tenantId, null, null, null, null, from, to, page());

    assertThat(logs.getContent()).isNotEmpty();
    assertThat(logs.getContent())
        .allSatisfy(
            inspection -> {
              assertThat(inspection.getInspectedAt()).isAfterOrEqualTo(from);
              assertThat(inspection.getInspectedAt()).isBeforeOrEqualTo(to);
            });
  }

  @Test
  void searchLogsFiltersByParkingAndFloorWithinTenant() {
    UUID tenantId = tenantId("bcons-plaza");
    FireExtinguisherInspection sample = sampleInspection(tenantId);
    UUID parkingId = sample.getFireExtinguisher().getParking().getId();
    UUID floorId = sample.getFireExtinguisher().getFloor().getId();

    var logs =
        inspectionRepository.searchLogs(
            tenantId, null, parkingId, floorId, null, null, null, page());

    assertThat(logs.getContent()).isNotEmpty();
    assertThat(logs.getContent())
        .allSatisfy(
            inspection -> {
              assertThat(inspection.getTenant().getId()).isEqualTo(tenantId);
              assertThat(inspection.getFireExtinguisher().getParking().getId()).isEqualTo(parkingId);
              assertThat(inspection.getFireExtinguisher().getFloor().getId()).isEqualTo(floorId);
            });
  }

  @Test
  void searchLogsReturnsPhotoObjectKey() {
    UUID tenantId = tenantId("bcons-plaza");
    FireExtinguisherInspection sample = sampleInspection(tenantId);
    String objectKey = "tenants/" + tenantId + "/fire-inspections/staff/sample.jpg";
    sample.setPhotoObjectKey(objectKey);
    inspectionRepository.saveAndFlush(sample);
    entityManager.clear();

    var logs =
        inspectionRepository.searchLogs(
            tenantId,
            sample.getFireExtinguisher().getId(),
            null,
            null,
            null,
            null,
            null,
            page());

    assertThat(logs.getContent()).extracting(FireExtinguisherInspection::getPhotoObjectKey)
        .contains(objectKey);
  }

  private FireExtinguisherInspection sampleInspection(UUID tenantId) {
    return inspectionRepository.searchLogs(tenantId, null, null, null, null, null, null, page())
        .getContent()
        .getFirst();
  }

  private PageRequest page() {
    return PageRequest.of(0, 50, Sort.by("inspectedAt").descending());
  }

  private UUID tenantId(String slug) {
    Tenant tenant = tenantRepository.findBySlug(slug).orElseThrow();
    return tenant.getId();
  }
}
