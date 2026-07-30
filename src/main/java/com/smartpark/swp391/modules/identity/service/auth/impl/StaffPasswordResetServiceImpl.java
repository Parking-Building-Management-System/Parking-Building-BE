package com.smartpark.swp391.modules.identity.service.auth.impl;

import com.smartpark.swp391.common.exception.ApiException;
import com.smartpark.swp391.common.exception.ErrorCode;
import com.smartpark.swp391.common.response.PageResponse;
import com.smartpark.swp391.infrastructure.tenant.TenantContext;
import com.smartpark.swp391.modules.identity.entity.StaffPasswordResetRequest;
import com.smartpark.swp391.modules.identity.entity.User;
import com.smartpark.swp391.modules.identity.enumType.StaffPasswordResetStatus;
import com.smartpark.swp391.modules.identity.enumType.UserStatus;
import com.smartpark.swp391.modules.identity.repository.RoleRepository;
import com.smartpark.swp391.modules.identity.repository.StaffPasswordResetRequestRepository;
import com.smartpark.swp391.modules.identity.repository.UserRepository;
import com.smartpark.swp391.modules.identity.service.auth.SessionService;
import com.smartpark.swp391.modules.identity.service.auth.StaffPasswordResetService;
import com.smartpark.swp391.modules.manager.dto.staff.ManagerPasswordResetCompleteRequest;
import com.smartpark.swp391.modules.manager.dto.staff.ManagerPasswordResetRejectRequest;
import com.smartpark.swp391.modules.manager.dto.staff.ManagerPasswordResetResponse;
import com.smartpark.swp391.modules.manager.specification.ManagerPasswordResetRequestSpecifications;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class StaffPasswordResetServiceImpl implements StaffPasswordResetService {

  private static final String STAFF_ROLE = "STAFF";
  private static final String MANAGER_ROLE = "PARKING_MANAGER";

  StaffPasswordResetRequestRepository requestRepository;
  UserRepository userRepository;
  RoleRepository roleRepository;
  PasswordEncoder passwordEncoder;
  SessionService sessionService;

  @Override
  @Transactional
  public void requestReset(String normalizedEmail) {
    if (normalizedEmail == null || normalizedEmail.isBlank()) {
      return;
    }

    User staff = userRepository.findByUsernameForUpdate(normalizedEmail).orElse(null);
    if (!isEligibleStaff(staff)) {
      return;
    }
    if (requestRepository.existsByStaffUserIdAndStatus(
        staff.getId(), StaffPasswordResetStatus.PENDING)) {
      return;
    }

    requestRepository.save(
        StaffPasswordResetRequest.builder()
            .tenant(staff.getTenant())
            .staffUser(staff)
            .requestedEmail(normalizedEmail)
            .status(StaffPasswordResetStatus.PENDING)
            .requestedAt(LocalDateTime.now())
            .build());
  }

  @Override
  @Transactional(readOnly = true)
  public PageResponse<ManagerPasswordResetResponse> getRequests(
      StaffPasswordResetStatus status,
      String search,
      LocalDateTime from,
      LocalDateTime to,
      int page,
      int size) {
    validateDateRange(from, to);
    var pageable = PageRequest.of(page, size, Sort.by("requestedAt").descending());
    var requestPage =
        requestRepository.findAll(
            ManagerPasswordResetRequestSpecifications.filtered(
                currentTenantId(), status, search, from, to),
            pageable);

    return new PageResponse<>(
        requestPage.getContent().stream().map(this::toResponse).toList(),
        requestPage.getNumber(),
        requestPage.getSize(),
        requestPage.getTotalElements(),
        requestPage.getTotalPages());
  }

  @Override
  @Transactional(readOnly = true)
  public ManagerPasswordResetResponse getRequest(UUID id) {
    return toResponse(
        requestRepository
            .findTenantRequest(currentTenantId(), id)
            .orElseThrow(
                () ->
                    new ApiException(
                        ErrorCode.RESOURCE_NOT_FOUND, "PASSWORD_RESET_REQUEST_NOT_FOUND")));
  }

  @Override
  @Transactional
  public ManagerPasswordResetResponse complete(
      UUID id, ManagerPasswordResetCompleteRequest request, UUID managerUserId) {
    UUID tenantId = currentTenantId();
    StaffPasswordResetRequest resetRequest = getPendingRequestForUpdate(tenantId, id);
    User staff = requireTenantRoleUser(resetRequest.getStaffUser().getId(), tenantId, STAFF_ROLE);
    User manager = requireTenantRoleUser(managerUserId, tenantId, MANAGER_ROLE);

    if (!request.newPassword().equals(request.confirmPassword())) {
      throw new ApiException(ErrorCode.INVALID_INPUT, "Passwords do not match");
    }
    if (passwordEncoder.matches(request.newPassword(), staff.getPassword())) {
      throw new ApiException(
          ErrorCode.INVALID_INPUT, "New password must differ from the current password");
    }

    LocalDateTime now = LocalDateTime.now();
    staff.setPassword(passwordEncoder.encode(request.newPassword()));
    resetRequest.setStatus(StaffPasswordResetStatus.COMPLETED);
    resetRequest.setReviewedAt(now);
    resetRequest.setReviewedByManager(manager);
    resetRequest.setCompletedAt(now);
    resetRequest.setRejectionReason(null);

    userRepository.save(staff);
    StaffPasswordResetRequest saved = requestRepository.save(resetRequest);
    sessionService.revokeAll(staff.getId());
    return toResponse(saved);
  }

  @Override
  @Transactional
  public ManagerPasswordResetResponse reject(
      UUID id, ManagerPasswordResetRejectRequest request, UUID managerUserId) {
    UUID tenantId = currentTenantId();
    StaffPasswordResetRequest resetRequest = getPendingRequestForUpdate(tenantId, id);
    requireTenantRoleUser(resetRequest.getStaffUser().getId(), tenantId, STAFF_ROLE);
    User manager = requireTenantRoleUser(managerUserId, tenantId, MANAGER_ROLE);
    String reason = request.reason().trim();
    if (reason.length() < 3) {
      throw new ApiException(ErrorCode.INVALID_INPUT, "Rejection reason is too short");
    }

    resetRequest.setStatus(StaffPasswordResetStatus.REJECTED);
    resetRequest.setReviewedAt(LocalDateTime.now());
    resetRequest.setReviewedByManager(manager);
    resetRequest.setCompletedAt(null);
    resetRequest.setRejectionReason(reason);
    return toResponse(requestRepository.save(resetRequest));
  }

  private boolean isEligibleStaff(User user) {
    if (user == null || user.isDeleted() || user.getStatus() != UserStatus.ACTIVE) {
      return false;
    }
    List<String> roleNames = roleRepository.findRoleNamesByUserId(user.getId());
    return roleNames.contains(STAFF_ROLE);
  }

  private StaffPasswordResetRequest getPendingRequestForUpdate(UUID tenantId, UUID id) {
    StaffPasswordResetRequest request =
        requestRepository
            .findTenantRequestForUpdate(tenantId, id)
            .orElseThrow(
                () ->
                    new ApiException(
                        ErrorCode.RESOURCE_NOT_FOUND, "PASSWORD_RESET_REQUEST_NOT_FOUND"));
    if (request.getStatus() != StaffPasswordResetStatus.PENDING) {
      throw new ApiException(ErrorCode.DUPLICATE_RESOURCE, "PASSWORD_RESET_ALREADY_PROCESSED");
    }
    return request;
  }

  private User requireTenantRoleUser(UUID userId, UUID tenantId, String roleName) {
    return userRepository
        .findTenantUserByIdAndRole(userId, tenantId, roleName)
        .orElseThrow(
            () ->
                new ApiException(
                    ErrorCode.RESOURCE_NOT_FOUND,
                    roleName.equals(STAFF_ROLE) ? "Staff not found" : "Manager not found"));
  }

  private ManagerPasswordResetResponse toResponse(StaffPasswordResetRequest request) {
    User staff = request.getStaffUser();
    User reviewer = request.getReviewedByManager();
    return ManagerPasswordResetResponse.builder()
        .id(request.getId())
        .staffId(staff.getId())
        .staffFullName(staff.getFullName())
        .staffUsername(staff.getUsername())
        .staffStatus(staff.getStatus())
        .requestedEmail(request.getRequestedEmail())
        .requestedAt(request.getRequestedAt())
        .status(request.getStatus())
        .reviewedAt(request.getReviewedAt())
        .reviewedById(reviewer == null ? null : reviewer.getId())
        .reviewedByName(reviewer == null ? null : reviewer.getFullName())
        .completedAt(request.getCompletedAt())
        .rejectionReason(request.getRejectionReason())
        .build();
  }

  private void validateDateRange(LocalDateTime from, LocalDateTime to) {
    if (from != null && to != null && to.isBefore(from)) {
      throw new ApiException(ErrorCode.INVALID_INPUT, "to must not be before from");
    }
  }

  private UUID currentTenantId() {
    return TenantContext.getTenantId()
        .orElseThrow(() -> new ApiException(ErrorCode.UNAUTHENTICATED));
  }
}
