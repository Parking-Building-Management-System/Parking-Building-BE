package com.smartpark.swp391.modules.identity.service.auth.impl;

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
import com.smartpark.swp391.modules.identity.enumType.StaffPasswordResetStatus;
import com.smartpark.swp391.modules.identity.enumType.UserStatus;
import com.smartpark.swp391.modules.identity.repository.RoleRepository;
import com.smartpark.swp391.modules.identity.repository.StaffPasswordResetRequestRepository;
import com.smartpark.swp391.modules.identity.repository.UserRepository;
import com.smartpark.swp391.modules.identity.service.auth.SessionService;
import com.smartpark.swp391.modules.manager.dto.staff.ManagerPasswordResetCompleteRequest;
import com.smartpark.swp391.modules.manager.dto.staff.ManagerPasswordResetRejectRequest;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class StaffPasswordResetServiceImplTest {

  @Mock StaffPasswordResetRequestRepository requestRepository;
  @Mock UserRepository userRepository;
  @Mock RoleRepository roleRepository;
  @Mock SessionService sessionService;

  PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(4);

  @AfterEach
  void clearTenantContext() {
    TenantContext.clear();
  }

  @Test
  void eligibleActiveStaffCreatesPendingRequest() {
    TestData data = testData();
    when(userRepository.findByUsernameForUpdate(data.staff.getUsername()))
        .thenReturn(Optional.of(data.staff));
    when(roleRepository.findRoleNamesByUserId(data.staff.getId())).thenReturn(List.of("STAFF"));
    when(requestRepository.existsByStaffUserIdAndStatus(
            data.staff.getId(), StaffPasswordResetStatus.PENDING))
        .thenReturn(false);

    service().requestReset(data.staff.getUsername());

    verify(requestRepository)
        .save(
            org.mockito.ArgumentMatchers.argThat(
                request ->
                    request.getStaffUser().equals(data.staff)
                        && request.getTenant().equals(data.tenant)
                        && request.getStatus() == StaffPasswordResetStatus.PENDING
                        && request.getRequestedEmail().equals(data.staff.getUsername())));
  }

  @Test
  void unknownNonStaffInactiveAndDuplicateRequestsDoNotCreateRows() {
    TestData data = testData();
    when(userRepository.findByUsernameForUpdate("unknown@example.com"))
        .thenReturn(Optional.empty());
    service().requestReset("unknown@example.com");

    when(userRepository.findByUsernameForUpdate(data.staff.getUsername()))
        .thenReturn(Optional.of(data.staff));
    when(roleRepository.findRoleNamesByUserId(data.staff.getId()))
        .thenReturn(List.of("PARKING_MANAGER"), List.of("STAFF"), List.of("STAFF"));
    service().requestReset(data.staff.getUsername());

    data.staff.setStatus(UserStatus.INACTIVE);
    service().requestReset(data.staff.getUsername());

    data.staff.setStatus(UserStatus.ACTIVE);
    when(requestRepository.existsByStaffUserIdAndStatus(
            data.staff.getId(), StaffPasswordResetStatus.PENDING))
        .thenReturn(true);
    service().requestReset(data.staff.getUsername());

    verify(requestRepository, never()).save(any());
  }

  @Test
  void completeHashesNewPasswordRevokesSessionsAndMarksRequestCompleted() {
    TestData data = testData();
    TenantContext.setTenantId(data.tenant.getId());
    when(requestRepository.findTenantRequestForUpdate(
            data.tenant.getId(), data.resetRequest.getId()))
        .thenReturn(Optional.of(data.resetRequest));
    when(userRepository.findTenantUserByIdAndRole(data.staff.getId(), data.tenant.getId(), "STAFF"))
        .thenReturn(Optional.of(data.staff));
    when(userRepository.findTenantUserByIdAndRole(
            data.manager.getId(), data.tenant.getId(), "PARKING_MANAGER"))
        .thenReturn(Optional.of(data.manager));
    when(requestRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    String oldHash = data.staff.getPassword();
    var response =
        service()
            .complete(
                data.resetRequest.getId(),
                new ManagerPasswordResetCompleteRequest(
                    "NewSecurePassword@123", "NewSecurePassword@123"),
                data.manager.getId());

    assertThat(data.staff.getPassword()).isNotEqualTo(oldHash);
    assertThat(passwordEncoder.matches("Password@123", data.staff.getPassword())).isFalse();
    assertThat(passwordEncoder.matches("NewSecurePassword@123", data.staff.getPassword())).isTrue();
    assertThat(response.status()).isEqualTo(StaffPasswordResetStatus.COMPLETED);
    assertThat(response.completedAt()).isNotNull();
    verify(sessionService).revokeAll(data.staff.getId());
  }

  @Test
  void completeRejectsMismatchCurrentPasswordAndPreviouslyProcessedRequest() {
    TestData data = testData();
    TenantContext.setTenantId(data.tenant.getId());
    when(requestRepository.findTenantRequestForUpdate(
            data.tenant.getId(), data.resetRequest.getId()))
        .thenReturn(Optional.of(data.resetRequest));
    when(userRepository.findTenantUserByIdAndRole(data.staff.getId(), data.tenant.getId(), "STAFF"))
        .thenReturn(Optional.of(data.staff));
    when(userRepository.findTenantUserByIdAndRole(
            data.manager.getId(), data.tenant.getId(), "PARKING_MANAGER"))
        .thenReturn(Optional.of(data.manager));

    assertThatThrownBy(
            () ->
                service()
                    .complete(
                        data.resetRequest.getId(),
                        new ManagerPasswordResetCompleteRequest(
                            "NewSecurePassword@123", "DifferentPassword@123"),
                        data.manager.getId()))
        .isInstanceOf(ApiException.class)
        .hasMessage("Passwords do not match");

    assertThatThrownBy(
            () ->
                service()
                    .complete(
                        data.resetRequest.getId(),
                        new ManagerPasswordResetCompleteRequest("Password@123", "Password@123"),
                        data.manager.getId()))
        .isInstanceOf(ApiException.class)
        .hasMessage("New password must differ from the current password");

    data.resetRequest.setStatus(StaffPasswordResetStatus.COMPLETED);
    assertThatThrownBy(
            () ->
                service()
                    .reject(
                        data.resetRequest.getId(),
                        new ManagerPasswordResetRejectRequest("Already handled."),
                        data.manager.getId()))
        .isInstanceOf(ApiException.class)
        .hasMessage("PASSWORD_RESET_ALREADY_PROCESSED");
    verify(sessionService, never()).revokeAll(any());
  }

  @Test
  void rejectionLeavesPasswordAndSessionsUnchanged() {
    TestData data = testData();
    TenantContext.setTenantId(data.tenant.getId());
    when(requestRepository.findTenantRequestForUpdate(
            data.tenant.getId(), data.resetRequest.getId()))
        .thenReturn(Optional.of(data.resetRequest));
    when(userRepository.findTenantUserByIdAndRole(data.staff.getId(), data.tenant.getId(), "STAFF"))
        .thenReturn(Optional.of(data.staff));
    when(userRepository.findTenantUserByIdAndRole(
            data.manager.getId(), data.tenant.getId(), "PARKING_MANAGER"))
        .thenReturn(Optional.of(data.manager));
    when(requestRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    String oldHash = data.staff.getPassword();

    var response =
        service()
            .reject(
                data.resetRequest.getId(),
                new ManagerPasswordResetRejectRequest("Employee identity could not be verified."),
                data.manager.getId());

    assertThat(data.staff.getPassword()).isEqualTo(oldHash);
    assertThat(response.status()).isEqualTo(StaffPasswordResetStatus.REJECTED);
    assertThat(response.rejectionReason()).isEqualTo("Employee identity could not be verified.");
    verify(sessionService, never()).revokeAll(any());
  }

  @Test
  void anotherTenantCannotReadOrProcessRequest() {
    TestData data = testData();
    UUID otherTenantId = UUID.randomUUID();
    TenantContext.setTenantId(otherTenantId);
    when(requestRepository.findTenantRequest(otherTenantId, data.resetRequest.getId()))
        .thenReturn(Optional.empty());
    when(requestRepository.findTenantRequestForUpdate(otherTenantId, data.resetRequest.getId()))
        .thenReturn(Optional.empty());

    assertThatThrownBy(() -> service().getRequest(data.resetRequest.getId()))
        .isInstanceOf(ApiException.class)
        .hasMessage("PASSWORD_RESET_REQUEST_NOT_FOUND");
    assertThatThrownBy(
            () ->
                service()
                    .reject(
                        data.resetRequest.getId(),
                        new ManagerPasswordResetRejectRequest("Not this tenant."),
                        data.manager.getId()))
        .isInstanceOf(ApiException.class)
        .hasMessage("PASSWORD_RESET_REQUEST_NOT_FOUND");
  }

  private StaffPasswordResetServiceImpl service() {
    return new StaffPasswordResetServiceImpl(
        requestRepository, userRepository, roleRepository, passwordEncoder, sessionService);
  }

  private TestData testData() {
    Tenant tenant =
        Tenant.builder().name("Tenant").slug("tenant").emailContact("ops@example.com").build();
    tenant.setId(UUID.randomUUID());
    User staff =
        User.builder()
            .tenant(tenant)
            .username("staff@example.com")
            .password(passwordEncoder.encode("Password@123"))
            .fullName("Staff Member")
            .status(UserStatus.ACTIVE)
            .isDeleted(false)
            .build();
    staff.setId(UUID.randomUUID());
    User manager =
        User.builder()
            .tenant(tenant)
            .username("manager@example.com")
            .password(passwordEncoder.encode("Password@123"))
            .fullName("Parking Manager")
            .status(UserStatus.ACTIVE)
            .isDeleted(false)
            .build();
    manager.setId(UUID.randomUUID());
    StaffPasswordResetRequest resetRequest =
        StaffPasswordResetRequest.builder()
            .tenant(tenant)
            .staffUser(staff)
            .requestedEmail(staff.getUsername())
            .status(StaffPasswordResetStatus.PENDING)
            .requestedAt(LocalDateTime.now())
            .build();
    resetRequest.setId(UUID.randomUUID());
    return new TestData(tenant, staff, manager, resetRequest);
  }

  private record TestData(
      Tenant tenant, User staff, User manager, StaffPasswordResetRequest resetRequest) {}
}
