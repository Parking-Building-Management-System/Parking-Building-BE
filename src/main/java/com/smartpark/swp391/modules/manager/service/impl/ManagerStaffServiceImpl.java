package com.smartpark.swp391.modules.manager.service.impl;

import com.smartpark.swp391.common.exception.ApiException;
import com.smartpark.swp391.common.exception.ErrorCode;
import com.smartpark.swp391.common.response.PageResponse;
import com.smartpark.swp391.infrastructure.tenant.TenantContext;
import com.smartpark.swp391.modules.identity.entity.Role;
import com.smartpark.swp391.modules.identity.entity.Tenant;
import com.smartpark.swp391.modules.identity.entity.User;
import com.smartpark.swp391.modules.identity.entity.UserRole;
import com.smartpark.swp391.modules.identity.enumType.UserStatus;
import com.smartpark.swp391.modules.identity.repository.RoleRepository;
import com.smartpark.swp391.modules.identity.repository.TenantRepository;
import com.smartpark.swp391.modules.identity.repository.UserRepository;
import com.smartpark.swp391.modules.identity.repository.UserRoleRepository;
import com.smartpark.swp391.modules.identity.service.auth.SessionService;
import com.smartpark.swp391.modules.manager.dto.staff.ManagerStaffCreateRequest;
import com.smartpark.swp391.modules.manager.dto.staff.ManagerStaffPasswordResetRequest;
import com.smartpark.swp391.modules.manager.dto.staff.ManagerStaffResponse;
import com.smartpark.swp391.modules.manager.dto.staff.ManagerStaffStatusRequest;
import com.smartpark.swp391.modules.manager.dto.staff.ManagerStaffUpdateRequest;
import com.smartpark.swp391.modules.manager.service.ManagerStaffService;
import com.smartpark.swp391.modules.manager.specification.ManagerStaffSpecifications;
import java.util.Locale;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class ManagerStaffServiceImpl implements ManagerStaffService {

  private static final String STAFF_ROLE = "STAFF";

  UserRepository userRepository;
  RoleRepository roleRepository;
  UserRoleRepository userRoleRepository;
  TenantRepository tenantRepository;
  PasswordEncoder passwordEncoder;
  SessionService sessionService;

  @Override
  @Transactional(readOnly = true)
  public PageResponse<ManagerStaffResponse> getStaff(
      String search, UserStatus status, int page, int size) {
    var pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
    Specification<User> specification =
        ManagerStaffSpecifications.tenantStaff(currentTenantId(), STAFF_ROLE, status)
            .and(ManagerStaffSpecifications.search(search));
    var staffPage =
        userRepository.findAll(specification, pageable);

    return new PageResponse<>(
        staffPage.getContent().stream().map(this::toResponse).toList(),
        staffPage.getNumber(),
        staffPage.getSize(),
        staffPage.getTotalElements(),
        staffPage.getTotalPages());
  }

  @Override
  @Transactional
  public ManagerStaffResponse createStaff(ManagerStaffCreateRequest request, UUID managerUserId) {
    String username = normalizeUsername(request.username());
    if (userRepository.existsByUsername(username)) {
      throw new ApiException(ErrorCode.DUPLICATE_RESOURCE, "Username already exists");
    }

    String rawPassword = resolvePassword(request);
    Tenant tenant = tenantRepository.getReferenceById(currentTenantId());
    Role staffRole =
        roleRepository
            .findByName(STAFF_ROLE)
            .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Missing STAFF role"));

    User staff =
        userRepository.save(
            User.builder()
                .tenant(tenant)
                .username(username)
                .password(passwordEncoder.encode(rawPassword))
                .fullName(request.fullName().trim())
                .phone(trimToNull(request.phone()))
                .status(request.status() == null ? UserStatus.ACTIVE : request.status())
                .createdBy(managerUserId)
                .isDeleted(false)
                .build());

    userRoleRepository.save(UserRole.builder().user(staff).role(staffRole).build());
    return toResponse(staff);
  }

  @Override
  @Transactional(readOnly = true)
  public ManagerStaffResponse getStaffById(UUID id) {
    return toResponse(getStaffOrThrow(id));
  }

  @Override
  @Transactional
  public ManagerStaffResponse updateStaff(UUID id, ManagerStaffUpdateRequest request) {
    User staff = getStaffOrThrow(id);
    staff.setFullName(request.fullName().trim());
    staff.setPhone(trimToNull(request.phone()));
    if (request.status() != null && staff.getStatus() != request.status()) {
      staff.setStatus(request.status());
      revokeIfInactive(staff);
    }

    return toResponse(userRepository.save(staff));
  }

  @Override
  @Transactional
  public ManagerStaffResponse updateStatus(UUID id, ManagerStaffStatusRequest request) {
    User staff = getStaffOrThrow(id);
    staff.setStatus(request.status());
    revokeIfInactive(staff);
    return toResponse(userRepository.save(staff));
  }

  @Override
  @Transactional
  public ManagerStaffResponse resetPassword(UUID id, ManagerStaffPasswordResetRequest request) {
    User staff = getStaffOrThrow(id);
    staff.setPassword(passwordEncoder.encode(request.newPassword()));
    sessionService.revokeAll(staff.getId());
    return toResponse(userRepository.save(staff));
  }

  private User getStaffOrThrow(UUID id) {
    return userRepository
        .findTenantUserByIdAndRole(id, currentTenantId(), STAFF_ROLE)
        .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Staff not found"));
  }

  private void revokeIfInactive(User staff) {
    if (staff.getStatus() != UserStatus.ACTIVE) {
      sessionService.revokeAll(staff.getId());
    }
  }

  private ManagerStaffResponse toResponse(User user) {
    return ManagerStaffResponse.builder()
        .id(user.getId())
        .username(user.getUsername())
        .fullName(user.getFullName())
        .phone(user.getPhone())
        .status(user.getStatus())
        .createdAt(user.getCreatedAt())
        .updatedAt(user.getUpdatedAt())
        .build();
  }

  private String resolvePassword(ManagerStaffCreateRequest request) {
    String password =
        request.password() == null || request.password().isBlank()
            ? request.initialPassword()
            : request.password();
    if (password == null || password.isBlank()) {
      throw new ApiException(ErrorCode.INVALID_INPUT, "password or initialPassword is required");
    }
    return password;
  }

  private String normalizeUsername(String username) {
    if (username == null || username.isBlank()) {
      throw new ApiException(ErrorCode.INVALID_INPUT, "Username must not be blank");
    }
    return username.trim().toLowerCase(Locale.ROOT);
  }

  private String trimToNull(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return value.trim();
  }

  private UUID currentTenantId() {
    return TenantContext.getTenantId()
        .orElseThrow(() -> new ApiException(ErrorCode.UNAUTHENTICATED));
  }
}
