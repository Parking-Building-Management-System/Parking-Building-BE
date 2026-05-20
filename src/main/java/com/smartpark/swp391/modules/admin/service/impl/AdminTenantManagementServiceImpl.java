package com.smartpark.swp391.modules.admin.service.impl;

import com.smartpark.swp391.common.exception.ApiException;
import com.smartpark.swp391.common.exception.ErrorCode;
import com.smartpark.swp391.common.response.PageResponse;
import com.smartpark.swp391.infrastructure.cached.redis.service.AdminPortalCacheService;
import com.smartpark.swp391.infrastructure.cached.redis.service.SessionAuthorityCacheService;
import com.smartpark.swp391.infrastructure.cached.redis.service.TenantCacheService;
import com.smartpark.swp391.modules.admin.dto.tenant.AdminTenantProvisionRequest;
import com.smartpark.swp391.modules.admin.dto.tenant.AdminTenantStatusResponse;
import com.smartpark.swp391.modules.admin.dto.tenant.AdminTenantSummaryResponse;
import com.smartpark.swp391.modules.admin.service.AdminTenantManagementService;
import com.smartpark.swp391.modules.identity.entity.Role;
import com.smartpark.swp391.modules.identity.entity.Tenant;
import com.smartpark.swp391.modules.identity.entity.User;
import com.smartpark.swp391.modules.identity.entity.UserRole;
import com.smartpark.swp391.modules.identity.enumType.TenantStatus;
import com.smartpark.swp391.modules.identity.enumType.UserStatus;
import com.smartpark.swp391.modules.identity.repository.RoleRepository;
import com.smartpark.swp391.modules.identity.repository.SessionRepository;
import com.smartpark.swp391.modules.identity.repository.TenantRepository;
import com.smartpark.swp391.modules.identity.repository.UserRepository;
import com.smartpark.swp391.modules.identity.repository.UserRoleRepository;
import java.text.Normalizer;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class AdminTenantManagementServiceImpl implements AdminTenantManagementService {
  private static final String PARKING_MANAGER_ROLE = "PARKING_MANAGER";

  TenantRepository tenantRepository;
  UserRepository userRepository;
  RoleRepository roleRepository;
  UserRoleRepository userRoleRepository;
  SessionRepository sessionRepository;
  TenantCacheService tenantCacheService;
  AdminPortalCacheService adminPortalCacheService;
  SessionAuthorityCacheService sessionAuthorityCacheService;
  PasswordEncoder passwordEncoder;

  @Value("${jwt.valid-duration}")
  long accessTokenTtlSeconds;

  @Override
  @Transactional(readOnly = true)
  public PageResponse<AdminTenantSummaryResponse> getTenants(int page, int size) {
    Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
    var tenantPage = tenantRepository.findAll(pageable);

    return new PageResponse<>(
        tenantPage.getContent().stream().map(this::toTenantSummary).toList(),
        tenantPage.getNumber(),
        tenantPage.getSize(),
        tenantPage.getTotalElements(),
        tenantPage.getTotalPages());
  }

  @Override
  @Transactional
  public AdminTenantSummaryResponse provisionTenant(AdminTenantProvisionRequest request) {
    String slug = uniqueSlug(slugify(request.companyName()));
    String managerEmail = request.managerEmail().trim().toLowerCase(Locale.ROOT);

    if (userRepository.existsByUsername(managerEmail)) {
      throw new ApiException(ErrorCode.DUPLICATE_RESOURCE, "Email quản lý đã tồn tại");
    }

    Tenant tenant =
        tenantRepository.save(
            Tenant.builder()
                .name(request.companyName().trim())
                .slug(slug)
                .emailContact(managerEmail)
                .status(TenantStatus.ACTIVE)
                .build());

    User manager =
        userRepository.save(
            User.builder()
                .tenant(tenant)
                .username(managerEmail)
                .password(passwordEncoder.encode(request.initialPassword()))
                .fullName(request.companyName().trim() + " Manager")
                .status(UserStatus.ACTIVE)
                .build());

    Role parkingManagerRole =
        roleRepository
            .findByName(PARKING_MANAGER_ROLE)
            .orElseThrow(
                () -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Thiếu role PARKING_MANAGER"));

    userRoleRepository.save(UserRole.builder().user(manager).role(parkingManagerRole).build());
    adminPortalCacheService.evictDashboardStats();
    return toTenantSummary(tenant);
  }

  @Override
  @Transactional
  public AdminTenantStatusResponse toggleTenantStatus(UUID id) {
    Tenant tenant =
        tenantRepository
            .findById(id)
            .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND));

    TenantStatus nextStatus =
        tenant.getStatus() == TenantStatus.ACTIVE ? TenantStatus.SUSPENDED : TenantStatus.ACTIVE;

    tenant.setStatus(nextStatus);
    tenantRepository.save(tenant);
    tenantCacheService.evictTenantData(id);
    adminPortalCacheService.evictDashboardStats();

    if (nextStatus == TenantStatus.SUSPENDED) {
      revokeTenantSessions(id);
    }

    return AdminTenantStatusResponse.builder().id(id).status(nextStatus).build();
  }

  private void revokeTenantSessions(UUID tenantId) {
    LocalDateTime now = LocalDateTime.now();
    List<UUID> sessionIds = sessionRepository.findActiveSessionIdsByTenantId(tenantId, now);
    sessionRepository.revokeAllActiveByTenantId(tenantId, now);

    Duration accessTtl = Duration.ofSeconds(Math.max(1, accessTokenTtlSeconds));
    for (UUID sessionId : sessionIds) {
      sessionAuthorityCacheService.markRevoked(sessionId, accessTtl);
      sessionAuthorityCacheService.clearAuthz(sessionId);
      sessionAuthorityCacheService.clearActive(sessionId);
    }
  }

  private AdminTenantSummaryResponse toTenantSummary(Tenant tenant) {
    return AdminTenantSummaryResponse.builder()
        .id(tenant.getId())
        .name(tenant.getName())
        .slug(tenant.getSlug())
        .emailContact(tenant.getEmailContact())
        .status(tenant.getStatus())
        .createdAt(tenant.getCreatedAt())
        .build();
  }

  private String uniqueSlug(String baseSlug) {
    String slug = baseSlug;
    int suffix = 2;
    while (tenantRepository.existsBySlug(slug)) {
      slug = baseSlug + "-" + suffix;
      suffix++;
    }
    return slug;
  }

  private String slugify(String value) {
    String normalized =
        Normalizer.normalize(value.trim().toLowerCase(Locale.ROOT), Normalizer.Form.NFD);
    String ascii = Pattern.compile("\\p{M}").matcher(normalized).replaceAll("");
    String slug = ascii.replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
    return slug.isBlank() ? "tenant" : slug;
  }
}
