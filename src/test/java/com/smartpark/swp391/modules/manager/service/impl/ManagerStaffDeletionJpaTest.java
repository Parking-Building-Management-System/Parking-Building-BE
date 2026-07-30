package com.smartpark.swp391.modules.manager.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import com.smartpark.swp391.common.exception.ApiException;
import com.smartpark.swp391.infrastructure.cached.redis.service.SessionAuthorityCacheService;
import com.smartpark.swp391.infrastructure.tenant.TenantContext;
import com.smartpark.swp391.modules.identity.entity.Device;
import com.smartpark.swp391.modules.identity.entity.Session;
import com.smartpark.swp391.modules.identity.entity.StaffPasswordResetRequest;
import com.smartpark.swp391.modules.identity.entity.Tenant;
import com.smartpark.swp391.modules.identity.entity.User;
import com.smartpark.swp391.modules.identity.entity.UserRole;
import com.smartpark.swp391.modules.identity.enumType.DeviceStatus;
import com.smartpark.swp391.modules.identity.enumType.StaffPasswordResetStatus;
import com.smartpark.swp391.modules.identity.enumType.UserStatus;
import com.smartpark.swp391.modules.identity.repository.DeviceRepository;
import com.smartpark.swp391.modules.identity.repository.RoleRepository;
import com.smartpark.swp391.modules.identity.repository.SessionRepository;
import com.smartpark.swp391.modules.identity.repository.StaffPasswordResetRequestRepository;
import com.smartpark.swp391.modules.identity.repository.TenantRepository;
import com.smartpark.swp391.modules.identity.repository.UserRepository;
import com.smartpark.swp391.modules.identity.repository.UserRoleRepository;
import com.smartpark.swp391.modules.identity.service.auth.impl.SessionServiceImpl;
import com.smartpark.swp391.modules.manager.service.ManagerStaffService;
import com.smartpark.swp391.modules.operation.entity.Kiosk;
import com.smartpark.swp391.modules.operation.entity.KioskStaff;
import com.smartpark.swp391.modules.operation.repository.KioskRepository;
import com.smartpark.swp391.modules.operation.repository.KioskStaffRepository;
import com.smartpark.swp391.modules.parking.entity.Parking;
import com.smartpark.swp391.modules.parking.repository.ParkingRepository;
import com.smartpark.swp391.modules.settlement.entity.StaffCashShift;
import com.smartpark.swp391.modules.settlement.enumType.StaffCashShiftStatus;
import com.smartpark.swp391.modules.settlement.repository.StaffCashShiftRepository;
import com.smartpark.swp391.modules.settlement.service.ManagerShiftSettlementService;
import com.smartpark.swp391.modules.settlement.service.StaffCashSettlementMapper;
import com.smartpark.swp391.modules.settlement.service.impl.ManagerShiftSettlementServiceImpl;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@DataJpaTest
@Testcontainers(disabledWithoutDocker = true)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({
  ManagerStaffServiceImpl.class,
  SessionServiceImpl.class,
  ManagerShiftSettlementServiceImpl.class,
  StaffCashSettlementMapper.class,
  ManagerStaffDeletionJpaTest.TestBeans.class
})
class ManagerStaffDeletionJpaTest {

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

  @Autowired ManagerStaffService managerStaffService;
  @Autowired ManagerShiftSettlementService managerShiftSettlementService;
  @Autowired UserRepository userRepository;
  @Autowired UserRoleRepository userRoleRepository;
  @Autowired RoleRepository roleRepository;
  @Autowired TenantRepository tenantRepository;
  @Autowired ParkingRepository parkingRepository;
  @Autowired KioskRepository kioskRepository;
  @Autowired KioskStaffRepository kioskStaffRepository;
  @Autowired DeviceRepository deviceRepository;
  @Autowired SessionRepository sessionRepository;
  @Autowired StaffPasswordResetRequestRepository passwordResetRequestRepository;
  @Autowired StaffCashShiftRepository staffCashShiftRepository;
  @Autowired EntityManager entityManager;
  @Autowired JdbcTemplate jdbcTemplate;

  @AfterEach
  void clearTenantContext() {
    TenantContext.clear();
  }

  @Test
  void deletionCleansActiveAccessAndKeepsSettlementIdentityReadable() {
    DeletionData data = createDeletionData(StaffCashShiftStatus.CLOSED);
    TenantContext.setTenantId(data.tenant().getId());

    managerStaffService.deleteStaff(data.staff().getId(), data.manager().getId());
    entityManager.flush();
    entityManager.clear();

    assertThat(userRepository.findByUsername(data.staff().getUsername())).isEmpty();
    assertThat(
            jdbcTemplate.queryForObject(
                "SELECT status FROM users WHERE id = ?", String.class, data.staff().getId()))
        .isEqualTo(UserStatus.INACTIVE.name());
    assertThat(
            jdbcTemplate.queryForObject(
                "SELECT is_deleted FROM users WHERE id = ?", Boolean.class, data.staff().getId()))
        .isTrue();

    Device device = deviceRepository.findById(data.device().getId()).orElseThrow();
    assertThat(device.getStatus()).isEqualTo(DeviceStatus.SUSPENDED);
    assertThat(device.getKiosk()).isNull();
    assertThat(kioskStaffRepository.findById(data.assignment().getId()).orElseThrow().isActive())
        .isFalse();
    assertThat(sessionRepository.findById(data.session().getId()).orElseThrow().getRevokedAt())
        .isNotNull();
    assertThat(
            passwordResetRequestRepository
                .findById(data.resetRequest().getId())
                .orElseThrow()
                .getStatus())
        .isEqualTo(StaffPasswordResetStatus.REJECTED);

    var settlement = managerShiftSettlementService.getSettlement(data.shift().getId()).shift();
    assertThat(settlement.staffId()).isEqualTo(data.staff().getId());
    assertThat(settlement.staffName()).isEqualTo(data.staff().getFullName());
    assertThat(settlement.staffUsername()).isEqualTo(data.staff().getUsername());
    assertThat(staffCashShiftRepository.existsById(data.shift().getId())).isTrue();
    assertThat(countById("staff_cash_transactions", data.cashTransactionId())).isOne();
    assertThat(countById("parking_penalty_cases", data.penaltyCaseId())).isOne();
    assertThat(countById("fire_extinguisher_inspections", data.fireInspectionId())).isOne();
    assertThat(countById("audit_logs", data.auditLogId())).isOne();

    assertThatThrownBy(
            () -> managerStaffService.deleteStaff(data.staff().getId(), data.manager().getId()))
        .isInstanceOf(ApiException.class)
        .hasMessage("Staff not found");
  }

  @Test
  void openCashShiftBlocksDeletionWithoutChangingAccess() {
    DeletionData data = createDeletionData(StaffCashShiftStatus.OPEN);
    TenantContext.setTenantId(data.tenant().getId());

    assertThatThrownBy(
            () -> managerStaffService.deleteStaff(data.staff().getId(), data.manager().getId()))
        .isInstanceOf(ApiException.class)
        .hasMessage(
            "Staff account cannot be deleted while a cash shift is open. Close the shift first.");

    assertThat(data.staff().getStatus()).isEqualTo(UserStatus.ACTIVE);
    assertThat(data.staff().isDeleted()).isFalse();
    assertThat(data.device().getStatus()).isEqualTo(DeviceStatus.APPROVED);
    assertThat(data.assignment().isActive()).isTrue();
    assertThat(data.session().getRevokedAt()).isNull();
    assertThat(data.resetRequest().getStatus()).isEqualTo(StaffPasswordResetStatus.PENDING);
  }

  @Test
  void tenantAndRoleBoundariesHideInvalidTargets() {
    Tenant bcons = tenantRepository.findBySlug("bcons-plaza").orElseThrow();
    User bconsManager =
        userRepository.findByUsername("manager@bcons.smartpark.local").orElseThrow();
    User otherTenantStaff =
        userRepository.findByUsername("staff@fpt.smartpark.local").orElseThrow();
    TenantContext.setTenantId(bcons.getId());

    assertThatThrownBy(
            () -> managerStaffService.deleteStaff(otherTenantStaff.getId(), bconsManager.getId()))
        .isInstanceOf(ApiException.class)
        .hasMessage("Staff not found");
    assertThatThrownBy(
            () -> managerStaffService.deleteStaff(bconsManager.getId(), bconsManager.getId()))
        .isInstanceOf(ApiException.class)
        .hasMessage("Staff not found");
  }

  private DeletionData createDeletionData(StaffCashShiftStatus shiftStatus) {
    Tenant tenant = tenantRepository.findBySlug("bcons-plaza").orElseThrow();
    User manager = userRepository.findByUsername("manager@bcons.smartpark.local").orElseThrow();
    Parking parking =
        parkingRepository.findAllByTenantIdOrderByNameAsc(tenant.getId()).stream()
            .findFirst()
            .orElseThrow();
    Kiosk kiosk =
        kioskRepository.findTenantKiosks(tenant.getId(), parking.getId(), null, null).stream()
            .findFirst()
            .orElseThrow();
    String suffix = UUID.randomUUID().toString();
    User staff =
        userRepository.save(
            User.builder()
                .tenant(tenant)
                .username("delete-" + suffix + "@smartpark.local")
                .password("password-hash")
                .fullName("Deletion Test Staff")
                .status(UserStatus.ACTIVE)
                .isDeleted(false)
                .build());
    userRoleRepository.save(
        UserRole.builder()
            .user(staff)
            .role(roleRepository.findByName("STAFF").orElseThrow())
            .build());
    KioskStaff assignment =
        kioskStaffRepository.save(
            KioskStaff.builder()
                .tenant(tenant)
                .kiosk(kiosk)
                .staffUser(staff)
                .assignedAt(LocalDateTime.now())
                .active(true)
                .build());
    Device device =
        deviceRepository.save(
            Device.builder()
                .user(staff)
                .fingerprint("delete-device-" + suffix)
                .label("Deletion test device")
                .status(DeviceStatus.APPROVED)
                .kiosk(kiosk)
                .build());
    Session session =
        sessionRepository.save(
            Session.builder()
                .user(staff)
                .device(device)
                .refreshJti(UUID.randomUUID())
                .expiredAt(LocalDateTime.now().plusHours(1))
                .build());
    StaffPasswordResetRequest resetRequest =
        passwordResetRequestRepository.save(
            StaffPasswordResetRequest.builder()
                .tenant(tenant)
                .staffUser(staff)
                .requestedEmail(staff.getUsername())
                .status(StaffPasswordResetStatus.PENDING)
                .requestedAt(LocalDateTime.now())
                .build());
    StaffCashShift shift =
        staffCashShiftRepository.save(
            StaffCashShift.builder()
                .tenant(tenant)
                .parking(parking)
                .kiosk(kiosk)
                .staff(staff)
                .staffNameSnapshot(staff.getFullName())
                .staffUsernameSnapshot(staff.getUsername())
                .openedAt(LocalDateTime.now().minusHours(1))
                .closedAt(shiftStatus == StaffCashShiftStatus.CLOSED ? LocalDateTime.now() : null)
                .status(shiftStatus)
                .expectedCashAmount(BigDecimal.ZERO)
                .onlineAmount(BigDecimal.ZERO)
                .cashParkingAmount(BigDecimal.ZERO)
                .surchargeCashAmount(BigDecimal.ZERO)
                .penaltyCashAmount(BigDecimal.ZERO)
                .lostCardCashAmount(BigDecimal.ZERO)
                .transactionCount(0)
                .build());
    entityManager.flush();
    UUID cashTransactionId = UUID.randomUUID();
    jdbcTemplate.update(
        """
        INSERT INTO staff_cash_transactions (
            id, tenant_id, shift_id, parking_id, kiosk_id, staff_id,
            type, amount, occurred_at, source
        ) VALUES (?, ?, ?, ?, ?, ?, 'ADJUSTMENT', 0, CURRENT_TIMESTAMP, 'NORMAL_EXIT')
        """,
        cashTransactionId,
        tenant.getId(),
        shift.getId(),
        parking.getId(),
        kiosk.getId(),
        staff.getId());
    UUID penaltyCaseId = UUID.randomUUID();
    jdbcTemplate.update(
        """
        INSERT INTO parking_penalty_cases (
            id, tenant_id, parking_id, type, amount, currency, status,
            reported_from_pwa, reported_by_staff_id, reviewed_by_staff_id, reviewed_at
        ) VALUES (?, ?, ?, 'OTHER', 0, 'VND', 'APPLIED', FALSE, ?, ?, CURRENT_TIMESTAMP)
        """,
        penaltyCaseId,
        tenant.getId(),
        parking.getId(),
        staff.getId(),
        staff.getId());
    UUID extinguisherId =
        jdbcTemplate.queryForObject(
            "SELECT id FROM fire_extinguishers WHERE tenant_id = ? LIMIT 1",
            UUID.class,
            tenant.getId());
    UUID fireInspectionId = UUID.randomUUID();
    jdbcTemplate.update(
        """
        INSERT INTO fire_extinguisher_inspections (
            id, tenant_id, fire_extinguisher_id, inspected_by, result, inspected_at
        ) VALUES (?, ?, ?, ?, 'OK', CURRENT_TIMESTAMP)
        """,
        fireInspectionId,
        tenant.getId(),
        extinguisherId,
        staff.getId());
    UUID auditLogId = UUID.randomUUID();
    jdbcTemplate.update(
        """
        INSERT INTO audit_logs (
            id, tenant_id, actor_user_id, action, resource_type, occurred_at
        ) VALUES (?, ?, ?, 'TEST_HISTORY', 'STAFF', CURRENT_TIMESTAMP)
        """,
        auditLogId,
        tenant.getId(),
        staff.getId());
    return new DeletionData(
        tenant,
        manager,
        staff,
        kiosk,
        device,
        assignment,
        session,
        resetRequest,
        shift,
        cashTransactionId,
        penaltyCaseId,
        fireInspectionId,
        auditLogId);
  }

  private int countById(String table, UUID id) {
    return jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM " + table + " WHERE id = ?", Integer.class, id);
  }

  private record DeletionData(
      Tenant tenant,
      User manager,
      User staff,
      Kiosk kiosk,
      Device device,
      KioskStaff assignment,
      Session session,
      StaffPasswordResetRequest resetRequest,
      StaffCashShift shift,
      UUID cashTransactionId,
      UUID penaltyCaseId,
      UUID fireInspectionId,
      UUID auditLogId) {}

  @TestConfiguration
  static class TestBeans {

    @Bean
    PasswordEncoder passwordEncoder() {
      return new BCryptPasswordEncoder(4);
    }

    @Bean
    SessionAuthorityCacheService sessionAuthorityCacheService() {
      return mock(SessionAuthorityCacheService.class);
    }
  }
}
