package com.smartpark.swp391.modules.manager.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.smartpark.swp391.common.exception.ApiException;
import com.smartpark.swp391.infrastructure.tenant.TenantContext;
import com.smartpark.swp391.modules.identity.entity.StaffPasswordResetRequest;
import com.smartpark.swp391.modules.identity.entity.Tenant;
import com.smartpark.swp391.modules.identity.entity.User;
import com.smartpark.swp391.modules.identity.enumType.DeviceStatus;
import com.smartpark.swp391.modules.identity.enumType.StaffPasswordResetStatus;
import com.smartpark.swp391.modules.identity.enumType.UserStatus;
import com.smartpark.swp391.modules.identity.repository.DeviceRepository;
import com.smartpark.swp391.modules.identity.repository.RoleRepository;
import com.smartpark.swp391.modules.identity.repository.StaffPasswordResetRequestRepository;
import com.smartpark.swp391.modules.identity.repository.TenantRepository;
import com.smartpark.swp391.modules.identity.repository.UserRepository;
import com.smartpark.swp391.modules.identity.repository.UserRoleRepository;
import com.smartpark.swp391.modules.identity.service.auth.SessionService;
import com.smartpark.swp391.modules.operation.entity.KioskStaff;
import com.smartpark.swp391.modules.operation.repository.KioskStaffRepository;
import com.smartpark.swp391.modules.settlement.entity.StaffCashShift;
import com.smartpark.swp391.modules.settlement.repository.StaffCashShiftRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class ManagerStaffServiceImplTest {

  @Mock UserRepository userRepository;
  @Mock RoleRepository roleRepository;
  @Mock UserRoleRepository userRoleRepository;
  @Mock TenantRepository tenantRepository;
  @Mock DeviceRepository deviceRepository;
  @Mock KioskStaffRepository kioskStaffRepository;
  @Mock StaffCashShiftRepository staffCashShiftRepository;
  @Mock StaffPasswordResetRequestRepository passwordResetRequestRepository;
  @Mock PasswordEncoder passwordEncoder;
  @Mock SessionService sessionService;

  Tenant tenant;
  User staff;
  User manager;

  @BeforeEach
  void setUp() {
    tenant = Tenant.builder().name("Tenant").slug("tenant").emailContact("ops@example.com").build();
    tenant.setId(UUID.randomUUID());
    staff =
        User.builder()
            .tenant(tenant)
            .username("staff@example.com")
            .password("hash")
            .fullName("Staff Member")
            .status(UserStatus.ACTIVE)
            .isDeleted(false)
            .build();
    staff.setId(UUID.randomUUID());
    manager =
        User.builder()
            .tenant(tenant)
            .username("manager@example.com")
            .password("hash")
            .fullName("Manager")
            .status(UserStatus.ACTIVE)
            .isDeleted(false)
            .build();
    manager.setId(UUID.randomUUID());
    TenantContext.setTenantId(tenant.getId());
  }

  @AfterEach
  void clearTenantContext() {
    TenantContext.clear();
  }

  @Test
  void deleteStaffDeactivatesAccessRejectsPendingResetAndPreservesTheUserRow() {
    StaffPasswordResetRequest resetRequest =
        StaffPasswordResetRequest.builder()
            .tenant(tenant)
            .staffUser(staff)
            .requestedEmail(staff.getUsername())
            .status(StaffPasswordResetStatus.PENDING)
            .build();
    stubTargetLookup();
    when(passwordResetRequestRepository.findPendingForStaffForUpdate(tenant.getId(), staff.getId()))
        .thenReturn(Optional.of(resetRequest));
    when(staffCashShiftRepository.findOpenForStaffForUpdate(tenant.getId(), staff.getId()))
        .thenReturn(Optional.empty());

    service().deleteStaff(staff.getId(), manager.getId());

    assertThat(staff.getStatus()).isEqualTo(UserStatus.INACTIVE);
    assertThat(staff.isDeleted()).isTrue();
    assertThat(resetRequest.getStatus()).isEqualTo(StaffPasswordResetStatus.REJECTED);
    assertThat(resetRequest.getReviewedByManager()).isSameAs(manager);
    assertThat(resetRequest.getReviewedAt()).isNotNull();
    assertThat(resetRequest.getRejectionReason())
        .isEqualTo("Staff account deleted by parking manager.");
    verify(kioskStaffRepository).deactivateActiveAssignmentsForStaff(tenant.getId(), staff.getId());
    verify(deviceRepository).suspendAndDetachByUserId(staff.getId(), DeviceStatus.SUSPENDED);
    verify(sessionService).revokeAll(staff.getId());
    verify(userRepository).save(staff);
    verify(userRepository, never()).delete(any(User.class));
    verify(kioskStaffRepository, never()).delete(any(KioskStaff.class));
  }

  @Test
  void deleteStaffWithOpenCashShiftReturnsTheRequiredConflictWithoutCleanup() {
    stubTargetLookup();
    when(passwordResetRequestRepository.findPendingForStaffForUpdate(tenant.getId(), staff.getId()))
        .thenReturn(Optional.empty());
    when(staffCashShiftRepository.findOpenForStaffForUpdate(tenant.getId(), staff.getId()))
        .thenReturn(Optional.of(StaffCashShift.builder().staff(staff).build()));

    assertThatThrownBy(() -> service().deleteStaff(staff.getId(), manager.getId()))
        .isInstanceOf(ApiException.class)
        .hasMessage(
            "Staff account cannot be deleted while a cash shift is open. Close the shift first.");

    assertThat(staff.getStatus()).isEqualTo(UserStatus.ACTIVE);
    assertThat(staff.isDeleted()).isFalse();
    verify(kioskStaffRepository, never()).deactivateActiveAssignmentsForStaff(any(), any());
    verify(deviceRepository, never()).suspendAndDetachByUserId(any(), any());
    verify(sessionService, never()).revokeAll(any());
  }

  @Test
  void deleteStaffRejectsPrivilegedTargets() {
    when(userRepository.findTenantUserByIdAndRole(staff.getId(), tenant.getId(), "STAFF"))
        .thenReturn(Optional.of(staff));
    when(roleRepository.findRoleNamesByUserId(staff.getId()))
        .thenReturn(List.of("STAFF", "PARKING_MANAGER"));

    assertThatThrownBy(() -> service().deleteStaff(staff.getId(), manager.getId()))
        .isInstanceOf(ApiException.class)
        .hasMessage("Privileged accounts cannot be deleted as Staff");

    verify(userRepository, never()).findTenantUserByIdAndRoleForUpdate(any(), any(), any());
    verify(sessionService, never()).revokeAll(any());
  }

  @Test
  void deleteStaffDoesNotResolveCrossTenantOrNonStaffTargets() {
    when(userRepository.findTenantUserByIdAndRole(staff.getId(), tenant.getId(), "STAFF"))
        .thenReturn(Optional.empty());

    assertThatThrownBy(() -> service().deleteStaff(staff.getId(), manager.getId()))
        .isInstanceOf(ApiException.class)
        .hasMessage("Staff not found");

    verify(sessionService, never()).revokeAll(any());
  }

  private void stubTargetLookup() {
    when(userRepository.findTenantUserByIdAndRole(staff.getId(), tenant.getId(), "STAFF"))
        .thenReturn(Optional.of(staff));
    when(roleRepository.findRoleNamesByUserId(staff.getId())).thenReturn(List.of("STAFF"));
    when(userRepository.findTenantUserByIdAndRole(
            manager.getId(), tenant.getId(), "PARKING_MANAGER"))
        .thenReturn(Optional.of(manager));
    when(userRepository.findTenantUserByIdAndRoleForUpdate(staff.getId(), tenant.getId(), "STAFF"))
        .thenReturn(Optional.of(staff));
  }

  private ManagerStaffServiceImpl service() {
    return new ManagerStaffServiceImpl(
        userRepository,
        roleRepository,
        userRoleRepository,
        tenantRepository,
        deviceRepository,
        kioskStaffRepository,
        staffCashShiftRepository,
        passwordResetRequestRepository,
        passwordEncoder,
        sessionService);
  }
}
