package com.smartpark.swp391.modules.firesafety.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.smartpark.swp391.modules.firesafety.entity.FireExtinguisher;
import com.smartpark.swp391.modules.identity.entity.Tenant;
import com.smartpark.swp391.modules.identity.repository.TenantRepository;
import java.util.Locale;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
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
class FireExtinguisherRepositoryTest {

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

  @Autowired FireExtinguisherRepository fireExtinguisherRepository;
  @Autowired TenantRepository tenantRepository;

  @Test
  void listWithoutSearchReturnsTenantExtinguishersAndDoesNotCrashPostgres() {
    UUID tenantId = tenantId("bcons-plaza");

    var extinguishers =
        fireExtinguisherRepository.findByFilters(
            tenantId, null, null, null, null, null, null, pageable());

    assertThat(extinguishers.getContent()).isNotEmpty();
    assertThat(extinguishers.getContent())
        .allSatisfy(
            extinguisher -> assertThat(extinguisher.getTenant().getId()).isEqualTo(tenantId));
  }

  @Test
  void searchByTextMatchesCodeCaseInsensitively() {
    UUID tenantId = tenantId("bcons-plaza");
    FireExtinguisher sample =
        fireExtinguisherRepository
            .findByFilters(tenantId, null, null, null, null, null, null, pageable())
            .getContent()
            .getFirst();
    String search = sample.getCode().toLowerCase(Locale.ROOT);

    var extinguishers =
        fireExtinguisherRepository.searchByText(
            tenantId, null, null, null, null, null, search, null, pageable());

    assertThat(extinguishers.getContent())
        .extracting(FireExtinguisher::getCode)
        .contains(sample.getCode());
  }

  @Test
  void searchByTextMatchesLocationDescriptionCaseInsensitively() {
    UUID tenantId = tenantId("bcons-plaza");

    var extinguishers =
        fireExtinguisherRepository.searchByText(
            tenantId, null, null, null, null, null, "elevator", null, pageable());

    assertThat(extinguishers.getContent()).isNotEmpty();
    assertThat(extinguishers.getContent())
        .allSatisfy(
            extinguisher ->
                assertThat(extinguisher.getLocationDescription()).containsIgnoringCase("elevator"));
  }

  private PageRequest pageable() {
    return PageRequest.of(0, 50, Sort.by("code").ascending());
  }

  private UUID tenantId(String slug) {
    Tenant tenant = tenantRepository.findBySlug(slug).orElseThrow();
    return tenant.getId();
  }
}
