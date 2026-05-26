package com.smartpark.swp391.modules.manager.specification;

import static org.assertj.core.api.Assertions.assertThat;

import com.smartpark.swp391.modules.identity.entity.Tenant;
import com.smartpark.swp391.modules.identity.entity.User;
import com.smartpark.swp391.modules.identity.enumType.UserStatus;
import com.smartpark.swp391.modules.identity.repository.TenantRepository;
import com.smartpark.swp391.modules.identity.repository.UserRepository;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@DataJpaTest
@Testcontainers(disabledWithoutDocker = true)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ManagerStaffSpecificationsTest {

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

  @Autowired UserRepository userRepository;
  @Autowired TenantRepository tenantRepository;

  @Test
  void listStaffWithoutSearchReturnsTenantStaffOnly() {
    UUID tenantId = tenantId("bcons-plaza");

    Page<User> staff = findStaff(tenantId, null, null);

    assertThat(staff.getContent()).isNotEmpty();
    assertThat(staff.getContent())
        .allSatisfy(
            user -> {
              assertThat(user.getTenant().getId()).isEqualTo(tenantId);
              assertThat(user.isDeleted()).isFalse();
            });
  }

  @Test
  void listStaffWithSearchMatchesUsernameFullNameOrPhone() {
    UUID tenantId = tenantId("bcons-plaza");

    Page<User> staff = findStaff(tenantId, "cashier", null);

    assertThat(staff.getContent()).extracting(User::getUsername).containsExactly(
        "cashier01@bcons-plaza.smartpark.local");
  }

  @Test
  void listStaffWithStatusFiltersByStatus() {
    UUID tenantId = tenantId("bcons-plaza");

    Page<User> staff = findStaff(tenantId, null, UserStatus.ACTIVE);

    assertThat(staff.getContent()).isNotEmpty();
    assertThat(staff.getContent()).allSatisfy(user -> assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE));
  }

  @Test
  void blankSearchIsTreatedAsNoSearchAndDoesNotCrashPostgres() {
    UUID tenantId = tenantId("bcons-plaza");

    Page<User> withoutSearch = findStaff(tenantId, null, null);
    Page<User> blankSearch = findStaff(tenantId, "   ", null);

    assertThat(blankSearch.getTotalElements()).isEqualTo(withoutSearch.getTotalElements());
  }

  @Test
  void managerTenantCannotSeeStaffFromAnotherTenant() {
    UUID bconsTenantId = tenantId("bcons-plaza");

    Page<User> staff = findStaff(bconsTenantId, "fpt", null);

    assertThat(staff.getContent()).noneMatch(user -> user.getUsername().contains("@fpt"));
  }

  private Page<User> findStaff(UUID tenantId, String search, UserStatus status) {
    Specification<User> specification =
        ManagerStaffSpecifications.tenantStaff(tenantId, "STAFF", status)
            .and(ManagerStaffSpecifications.search(search));

    return userRepository.findAll(
        specification, PageRequest.of(0, 50, Sort.by("createdAt").descending()));
  }

  private UUID tenantId(String slug) {
    Tenant tenant = tenantRepository.findBySlug(slug).orElseThrow();
    return tenant.getId();
  }
}
