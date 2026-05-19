package com.smartpark.swp391.modules.identity.service.impl;

import com.smartpark.swp391.common.exception.ApiException;
import com.smartpark.swp391.common.exception.ErrorCode;
import com.smartpark.swp391.infrastructure.cached.redis.service.SessionAuthorityCacheService;
import com.smartpark.swp391.infrastructure.cached.redis.service.TenantCacheService;
import com.smartpark.swp391.modules.identity.dto.tenant.request.TenantCreationRequest;
import com.smartpark.swp391.modules.identity.dto.tenant.response.TenantResponse;
import com.smartpark.swp391.modules.identity.entity.Tenant;
import com.smartpark.swp391.modules.identity.enumType.TenantStatus;
import com.smartpark.swp391.modules.identity.mapper.TenantMapper;
import com.smartpark.swp391.modules.identity.repository.SessionRepository;
import com.smartpark.swp391.modules.identity.repository.TenantRepository;
import com.smartpark.swp391.modules.identity.service.TenantService;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class TenantServiceImpl implements TenantService {

  TenantRepository tenantRepository;
  SessionRepository sessionRepository;
  TenantCacheService tenantCacheService;
  SessionAuthorityCacheService sessionAuthorityCacheService;
  TenantMapper tenantMapper;

  @Override
  @Transactional
  public TenantResponse createTenant(TenantCreationRequest request) {
    log.info("Bắt đầu tạo mới khách hàng (Tenant) với slug: {}", request.slug());

    if (tenantRepository.existsBySlug(request.slug())) {
      log.warn("Tạo thất bại - Slug đã tồn tại trên hệ thống: {}", request.slug());
      throw new ApiException(ErrorCode.DUPLICATE_RESOURCE, "Khách hàng đã tồn tại");
    }

    Tenant tenant =
        Tenant.builder()
            .name(request.name())
            .slug(request.slug())
            .emailContact(request.emailContact())
            .status(TenantStatus.ACTIVE)
            .build();

    tenant = tenantRepository.save(tenant);
    log.info("Tạo thành công Tenant ID: [{}] - Tên: {}", tenant.getId(), tenant.getName());

    return tenantMapper.toResponse(tenant);
  }

  @Override
  public TenantResponse getTenantById(UUID id) {
    log.debug("Yêu cầu lấy thông tin Tenant ID: {}", id);

    return tenantCacheService
        .getTenant(id)
        .orElseGet(
            () -> {
              log.info("Cache miss, tiến hành lấy Tenant từ Database - ID: {}", id);
              Tenant tenant =
                  tenantRepository
                      .findById(id)
                      .orElseThrow(
                          () -> {
                            log.error("Không tìm thấy Tenant ID: {}", id);
                            return new ApiException(ErrorCode.RESOURCE_NOT_FOUND);
                          });

              TenantResponse response = tenantMapper.toResponse(tenant);
              tenantCacheService.saveTenant(id, response);

              log.debug("Đã nạp thông tin Tenant ID: {} vào Cache", id);
              return response;
            });
  }

  @Override
  @Transactional
  public TenantResponse updateTenantStatus(UUID id, TenantStatus status) {
    log.info("Nhận yêu cầu cập nhật trạng thái Tenant ID: {} -> {}", id, status);

    Tenant tenant =
        tenantRepository
            .findById(id)
            .orElseThrow(
                () -> {
                  log.error("Cập nhật trạng thái thất bại - Không tìm thấy Tenant ID: {}", id);
                  return new ApiException(ErrorCode.RESOURCE_NOT_FOUND);
                });

    tenant.setStatus(status);
    tenantRepository.save(tenant);
    tenantCacheService.evictTenantData(id);

    if (status == TenantStatus.SUSPENDED) {
      revokeTenantSessions(id);
    }

    log.info("Đã cập nhật trạng thái Tenant ID: {} thành {}", id, status);
    return tenantMapper.toResponse(tenant);
  }

  @Override
  @Transactional
  public void suspendTenant(UUID id) {
    log.info("Nhận yêu cầu đình chỉ (Suspend) Tenant ID: {}", id);

    Tenant tenant =
        tenantRepository
            .findById(id)
            .orElseThrow(
                () -> {
                  log.error("Suspend thất bại - Không tìm thấy Tenant ID: {}", id);
                  return new ApiException(ErrorCode.RESOURCE_NOT_FOUND);
                });

    tenant.setStatus(TenantStatus.SUSPENDED);
    tenantRepository.save(tenant);

    tenantCacheService.evictTenantData(id);
    revokeTenantSessions(id);
    log.info("Đã đình chỉ thành công và dọn sạch Cache/Session của Tenant ID: {}", id);
  }

  @Override
  @Transactional
  public void deleteTenant(UUID id) {
    log.info("Nhận yêu cầu xóa mềm (Delete) Tenant ID: {}", id);

    Tenant tenant =
        tenantRepository
            .findById(id)
            .orElseThrow(
                () -> {
                  log.error("Xóa thất bại - Không tìm thấy Tenant ID: {}", id);
                  return new ApiException(ErrorCode.RESOURCE_NOT_FOUND);
                });

    tenant.setDeleted(true);
    tenantRepository.save(tenant);

    tenantCacheService.evictTenantData(id);
    log.info("Xóa mềm thành công và đã xóa toàn bộ Cache của Tenant ID: {}", id);
  }

  private void revokeTenantSessions(UUID tenantId) {
    LocalDateTime now = LocalDateTime.now();
    List<UUID> sessionIds = sessionRepository.findActiveSessionIdsByTenantId(tenantId, now);
    sessionRepository.revokeAllActiveByTenantId(tenantId, now);

    Duration accessTtl = Duration.ofMinutes(15);
    for (UUID sessionId : sessionIds) {
      try {
        sessionAuthorityCacheService.markRevoked(sessionId, accessTtl);
        sessionAuthorityCacheService.clearAuthz(sessionId);
        sessionAuthorityCacheService.clearActive(sessionId);
      } catch (Exception ex) {
        log.warn("Không revoke được Redis cache cho sessionId={}", sessionId, ex);
      }
    }
  }
}
