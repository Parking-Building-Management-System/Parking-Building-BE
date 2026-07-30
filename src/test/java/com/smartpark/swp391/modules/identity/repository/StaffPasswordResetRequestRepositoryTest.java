package com.smartpark.swp391.modules.identity.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.smartpark.swp391.modules.identity.entity.StaffPasswordResetRequest;
import com.smartpark.swp391.modules.identity.entity.Tenant;
import com.smartpark.swp391.modules.identity.entity.User;
import com.smartpark.swp391.modules.identity.enumType.StaffPasswordResetStatus;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@DataJpaTest
@Testcontainers(disabledWithoutDocker = true)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class StaffPasswordResetRequestRepositoryTest {

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

  @Autowired StaffPasswordResetRequestRepository requestRepository;
  @Autowired UserRepository userRepository;
  @Autowired TenantRepository tenantRepository;
  @Autowired JdbcTemplate jdbcTemplate;

  @Test
  void partialUniqueIndexAllowsOnlyOnePendingRequestPerStaff() {
    User staff = userRepository.findByUsername("staff@vincom.smartpark.local").orElseThrow();
    requestRepository.saveAndFlush(pendingRequest(staff));

    assertThatThrownBy(() -> requestRepository.saveAndFlush(pendingRequest(staff)))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void completedRequestAllowsANewPendingRequestForTheSameStaff() {
    User staff = userRepository.findByUsername("staff@fpt.smartpark.local").orElseThrow();
    User manager = userRepository.findByUsername("manager@fpt.smartpark.local").orElseThrow();
    StaffPasswordResetRequest completed = pendingRequest(staff);
    completed.setStatus(StaffPasswordResetStatus.COMPLETED);
    completed.setReviewedAt(LocalDateTime.now());
    completed.setReviewedByManager(manager);
    completed.setCompletedAt(LocalDateTime.now());
    requestRepository.saveAndFlush(completed);

    StaffPasswordResetRequest pending = requestRepository.saveAndFlush(pendingRequest(staff));

    assertThat(pending.getStatus()).isEqualTo(StaffPasswordResetStatus.PENDING);
  }

  @Test
  void tenantForeignKeyRejectsMismatchedStaffTenant() {
    User staff = userRepository.findByUsername("staff@vincom.smartpark.local").orElseThrow();
    Tenant otherTenant = tenantRepository.findBySlug("fpt-tower").orElseThrow();
    StaffPasswordResetRequest request = pendingRequest(staff);
    request.setTenant(otherTenant);

    assertThatThrownBy(() -> requestRepository.saveAndFlush(request))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void roleConstraintRejectsANonStaffTarget() {
    User manager = userRepository.findByUsername("manager@vincom.smartpark.local").orElseThrow();

    assertThatThrownBy(() -> requestRepository.saveAndFlush(pendingRequest(manager)))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void requestTableHasNoPlaintextPasswordColumn() {
    Integer passwordColumns =
        jdbcTemplate.queryForObject(
            """
            SELECT COUNT(*)
            FROM information_schema.columns
            WHERE table_name = 'staff_password_reset_requests'
              AND column_name ILIKE '%password%'
            """,
            Integer.class);

    assertThat(passwordColumns).isZero();
  }

  private StaffPasswordResetRequest pendingRequest(User staff) {
    return StaffPasswordResetRequest.builder()
        .tenant(staff.getTenant())
        .staffUser(staff)
        .requestedEmail(staff.getUsername())
        .status(StaffPasswordResetStatus.PENDING)
        .requestedAt(LocalDateTime.now())
        .build();
  }
}
