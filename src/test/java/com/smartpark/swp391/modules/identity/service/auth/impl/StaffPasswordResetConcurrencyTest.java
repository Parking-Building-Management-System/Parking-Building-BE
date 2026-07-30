package com.smartpark.swp391.modules.identity.service.auth.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.smartpark.swp391.common.exception.ApiException;
import com.smartpark.swp391.infrastructure.tenant.TenantContext;
import com.smartpark.swp391.modules.identity.entity.StaffPasswordResetRequest;
import com.smartpark.swp391.modules.identity.entity.User;
import com.smartpark.swp391.modules.identity.enumType.StaffPasswordResetStatus;
import com.smartpark.swp391.modules.identity.repository.StaffPasswordResetRequestRepository;
import com.smartpark.swp391.modules.identity.repository.UserRepository;
import com.smartpark.swp391.modules.identity.service.auth.SessionService;
import com.smartpark.swp391.modules.identity.service.auth.StaffPasswordResetService;
import com.smartpark.swp391.modules.manager.dto.staff.ManagerPasswordResetCompleteRequest;
import com.smartpark.swp391.modules.manager.dto.staff.ManagerPasswordResetRejectRequest;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@DataJpaTest
@Testcontainers(disabledWithoutDocker = true)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({StaffPasswordResetServiceImpl.class, StaffPasswordResetConcurrencyTest.TestBeans.class})
class StaffPasswordResetConcurrencyTest {

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

  @Autowired StaffPasswordResetService service;
  @Autowired StaffPasswordResetRequestRepository requestRepository;
  @Autowired UserRepository userRepository;

  @Test
  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  void concurrentCompleteAndRejectAllowOnlyOneOutcome() throws Exception {
    User staff = userRepository.findByUsername("staff@bcons.smartpark.local").orElseThrow();
    User manager = userRepository.findByUsername("manager@bcons.smartpark.local").orElseThrow();
    StaffPasswordResetRequest resetRequest =
        requestRepository.saveAndFlush(
            StaffPasswordResetRequest.builder()
                .tenant(staff.getTenant())
                .staffUser(staff)
                .requestedEmail(staff.getUsername())
                .status(StaffPasswordResetStatus.PENDING)
                .requestedAt(LocalDateTime.now())
                .build());
    CountDownLatch start = new CountDownLatch(1);

    Callable<String> complete =
        () ->
            process(
                start,
                staff,
                () ->
                    service
                        .complete(
                            resetRequest.getId(),
                            new ManagerPasswordResetCompleteRequest(
                                "ConcurrentPassword@123", "ConcurrentPassword@123"),
                            manager.getId())
                        .status()
                        .name());
    Callable<String> reject =
        () ->
            process(
                start,
                staff,
                () ->
                    service
                        .reject(
                            resetRequest.getId(),
                            new ManagerPasswordResetRejectRequest("Identity verification failed."),
                            manager.getId())
                        .status()
                        .name());

    try (var executor = Executors.newFixedThreadPool(2)) {
      var results = List.of(executor.submit(complete), executor.submit(reject));
      start.countDown();
      List<String> outcomes = results.stream().map(this::getResult).toList();

      assertThat(outcomes)
          .filteredOn(
              outcome ->
                  outcome.equals(StaffPasswordResetStatus.COMPLETED.name())
                      || outcome.equals(StaffPasswordResetStatus.REJECTED.name()))
          .hasSize(1);
      assertThat(outcomes).contains("PASSWORD_RESET_ALREADY_PROCESSED");
    }

    StaffPasswordResetRequest saved =
        requestRepository.findById(resetRequest.getId()).orElseThrow();
    assertThat(saved.getStatus())
        .isIn(StaffPasswordResetStatus.COMPLETED, StaffPasswordResetStatus.REJECTED);
  }

  private String process(
      CountDownLatch start, User staff, java.util.concurrent.Callable<String> action)
      throws Exception {
    start.await();
    TenantContext.setTenantId(staff.getTenant().getId());
    try {
      return action.call();
    } catch (ApiException exception) {
      return exception.getMessage();
    } finally {
      TenantContext.clear();
    }
  }

  private String getResult(java.util.concurrent.Future<String> result) {
    try {
      return result.get();
    } catch (Exception exception) {
      throw new AssertionError(exception);
    }
  }

  @TestConfiguration
  static class TestBeans {

    @Bean
    PasswordEncoder passwordEncoder() {
      return new BCryptPasswordEncoder(4);
    }

    @Bean
    SessionService sessionService() {
      return mock(SessionService.class);
    }
  }
}
