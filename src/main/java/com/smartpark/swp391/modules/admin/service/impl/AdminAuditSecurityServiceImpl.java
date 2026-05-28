package com.smartpark.swp391.modules.admin.service.impl;

import com.smartpark.swp391.common.exception.ApiException;
import com.smartpark.swp391.common.exception.ErrorCode;
import com.smartpark.swp391.common.response.PageResponse;
import com.smartpark.swp391.infrastructure.cached.redis.service.SessionAuthorityCacheService;
import com.smartpark.swp391.modules.admin.dto.security.AdminDeviceResponse;
import com.smartpark.swp391.modules.admin.dto.security.AdminSessionResponse;
import com.smartpark.swp391.modules.admin.dto.security.AuditLogResponse;
import com.smartpark.swp391.modules.admin.dto.security.ForceLogoutResponse;
import com.smartpark.swp391.modules.admin.dto.security.RevokeDeviceResponse;
import com.smartpark.swp391.modules.admin.dto.security.RevokeSessionResponse;
import com.smartpark.swp391.modules.admin.dto.security.SecurityActionRequest;
import com.smartpark.swp391.modules.admin.service.AdminAuditSecurityService;
import com.smartpark.swp391.modules.audit.entity.AuditLog;
import com.smartpark.swp391.modules.audit.repository.AuditLogRepository;
import com.smartpark.swp391.modules.identity.entity.Device;
import com.smartpark.swp391.modules.identity.entity.Session;
import com.smartpark.swp391.modules.identity.entity.User;
import com.smartpark.swp391.modules.identity.enumType.DeviceStatus;
import com.smartpark.swp391.modules.identity.repository.DeviceRepository;
import com.smartpark.swp391.modules.identity.repository.SessionRepository;
import com.smartpark.swp391.modules.identity.repository.UserRepository;
import com.smartpark.swp391.modules.identity.repository.UserRoleRepository;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class AdminAuditSecurityServiceImpl implements AdminAuditSecurityService {

  AuditLogRepository auditLogRepository;
  SessionRepository sessionRepository;
  DeviceRepository deviceRepository;
  UserRepository userRepository;
  UserRoleRepository userRoleRepository;
  SessionAuthorityCacheService sessionAuthorityCacheService;
  ZoneId zoneId = ZoneId.systemDefault();

  @Override
  @Transactional(readOnly = true)
  public PageResponse<AuditLogResponse> getAuditLogs(
      UUID actorId, String role, String severity, Instant from, Instant to, int page, int size) {
    Page<AuditLog> result =
        auditLogRepository.searchAdminAuditLogs(
            actorId,
            blankToNull(role),
            blankToNull(severity),
            toLocal(from),
            toLocal(to),
            PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "occurredAt")));
    return toPage(result.map(this::toAuditLogResponse));
  }

  @Override
  @Transactional(readOnly = true)
  public PageResponse<AdminSessionResponse> getSessions(
      UUID tenantId, String role, String status, int page, int size) {
    Page<Session> result =
        sessionRepository.findAdminSessions(
            tenantId,
            blankToNull(role),
            blankToNull(status),
            LocalDateTime.now(),
            PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
    return toPage(result.map(this::toSessionResponse));
  }

  @Override
  @Transactional
  public ForceLogoutResponse forceLogout(UUID userId, SecurityActionRequest request) {
    User user =
        userRepository
            .findById(userId)
            .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "User not found"));
    LocalDateTime now = LocalDateTime.now();
    List<UUID> sessionIds = sessionRepository.findActiveSessionIdsByUserId(userId, now);
    int revoked = sessionRepository.revokeAllActiveByUserId(userId, now);
    evictSessionCaches(sessionIds);
    audit(user, "FORCE_LOGOUT_USER", "USER", userId, "WARN", reason(request), null);
    return ForceLogoutResponse.builder().userId(userId).revokedSessionCount(revoked).build();
  }

  @Override
  @Transactional
  public RevokeSessionResponse revokeSession(UUID sessionId, SecurityActionRequest request) {
    Session session =
        sessionRepository
            .findById(sessionId)
            .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Session not found"));
    int revoked = sessionRepository.revokeIfNotRevoked(sessionId, LocalDateTime.now());
    if (revoked > 0) {
      evictSessionCaches(List.of(sessionId));
      audit(
          session.getUser(),
          "REVOKE_SESSION",
          "SESSION",
          sessionId,
          "WARN",
          reason(request),
          session.getDevice().getFingerprint());
    }
    return RevokeSessionResponse.builder().sessionId(sessionId).revoked(revoked > 0).build();
  }

  @Override
  @Transactional(readOnly = true)
  public PageResponse<AdminDeviceResponse> getDevices(UUID tenantId, String status, int page, int size) {
    DeviceStatus deviceStatus = parseDeviceStatus(status);
    Page<Device> result =
        deviceRepository.findAdminDevices(
            tenantId,
            deviceStatus,
            PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
    return toPage(result.map(this::toDeviceResponse));
  }

  @Override
  @Transactional
  public RevokeDeviceResponse revokeDevice(UUID deviceId, SecurityActionRequest request) {
    Device device =
        deviceRepository
            .findById(deviceId)
            .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Device not found"));
    device.setStatus(DeviceStatus.SUSPENDED);
    deviceRepository.save(device);

    LocalDateTime now = LocalDateTime.now();
    List<UUID> sessionIds = sessionRepository.findActiveSessionIdsByUserId(device.getUser().getId(), now);
    int revoked = sessionRepository.revokeAllActiveByUserId(device.getUser().getId(), now);
    evictSessionCaches(sessionIds);
    audit(
        device.getUser(),
        "REVOKE_DEVICE",
        "DEVICE",
        deviceId,
        "WARN",
        reason(request),
        device.getFingerprint());
    return RevokeDeviceResponse.builder()
        .deviceId(deviceId)
        .status(device.getStatus())
        .revokedSessionCount(revoked)
        .build();
  }

  private AuditLogResponse toAuditLogResponse(AuditLog auditLog) {
    User actor = auditLog.getActorUser();
    String role = auditLog.getActorRole();
    if ((role == null || role.isBlank()) && actor != null) {
      role = primaryRole(actor.getId());
    }
    return AuditLogResponse.builder()
        .id(auditLog.getId())
        .tenantId(auditLog.getTenant() == null ? null : auditLog.getTenant().getId())
        .tenantName(auditLog.getTenant() == null ? null : auditLog.getTenant().getName())
        .actorId(actor == null ? null : actor.getId())
        .actorUsername(actor == null ? null : actor.getUsername())
        .actorRole(role)
        .action(auditLog.getAction())
        .resourceType(auditLog.getResourceType())
        .resourceId(auditLog.getResourceId())
        .severity(auditLog.getSeverity())
        .reason(auditLog.getReason())
        .ipAddress(auditLog.getIpAddress())
        .deviceFingerprint(maskFingerprint(auditLog.getDeviceFingerprint()))
        .createdAt(auditLog.getOccurredAt())
        .build();
  }

  private AdminSessionResponse toSessionResponse(Session session) {
    User user = session.getUser();
    return AdminSessionResponse.builder()
        .id(session.getId())
        .userId(user.getId())
        .username(user.getUsername())
        .role(primaryRole(user.getId()))
        .tenantId(user.getTenant().getId())
        .tenantName(user.getTenant().getName())
        .deviceId(session.getDevice().getId())
        .deviceLabel(session.getDevice().getLabel())
        .status(sessionStatus(session))
        .createdAt(session.getCreatedAt())
        .expiresAt(session.getExpiredAt())
        .revokedAt(session.getRevokedAt())
        .build();
  }

  private AdminDeviceResponse toDeviceResponse(Device device) {
    User user = device.getUser();
    return AdminDeviceResponse.builder()
        .id(device.getId())
        .userId(user.getId())
        .username(user.getUsername())
        .tenantId(user.getTenant().getId())
        .tenantName(user.getTenant().getName())
        .fingerprint(maskFingerprint(device.getFingerprint()))
        .label(device.getLabel())
        .status(device.getStatus())
        .kioskId(device.getKiosk() == null ? null : device.getKiosk().getId())
        .kioskName(device.getKiosk() == null ? null : device.getKiosk().getName())
        .approvedBy(device.getApprovedBy())
        .approvedAt(device.getApprovedAt())
        .expiresAt(device.getExpiresAt())
        .createdAt(device.getCreatedAt())
        .updatedAt(device.getUpdatedAt())
        .build();
  }

  private void audit(
      User actor,
      String action,
      String resourceType,
      UUID resourceId,
      String severity,
      String reason,
      String deviceFingerprint) {
    auditLogRepository.save(
        AuditLog.builder()
            .tenant(actor.getTenant())
            .actorUser(actor)
            .actorRole(primaryRole(actor.getId()))
            .action(action)
            .resourceType(resourceType)
            .resourceId(resourceId)
            .severity(severity)
            .reason(reason)
            .deviceFingerprint(deviceFingerprint)
            .occurredAt(LocalDateTime.now())
            .build());
  }

  private String primaryRole(UUID userId) {
    return userRoleRepository.findAllByUserIdWithRole(userId).stream()
        .map(userRole -> userRole.getRole().getName())
        .findFirst()
        .orElse(null);
  }

  private String sessionStatus(Session session) {
    if (session.getRevokedAt() != null) {
      return "REVOKED";
    }
    return session.getExpiredAt().isAfter(LocalDateTime.now()) ? "ACTIVE" : "EXPIRED";
  }

  private void evictSessionCaches(List<UUID> sessionIds) {
    for (UUID sessionId : sessionIds) {
      try {
        sessionAuthorityCacheService.markRevoked(sessionId, Duration.ofMinutes(15));
        sessionAuthorityCacheService.clearAuthz(sessionId);
        sessionAuthorityCacheService.clearActive(sessionId);
      } catch (Exception e) {
        log.warn("Admin session cache eviction failed for sessionId={}", sessionId, e);
      }
    }
  }

  private DeviceStatus parseDeviceStatus(String status) {
    if (status == null || status.isBlank()) {
      return null;
    }
    return DeviceStatus.valueOf(status.trim().toUpperCase(java.util.Locale.ROOT));
  }

  private String reason(SecurityActionRequest request) {
    return request == null || request.reason() == null || request.reason().isBlank()
        ? null
        : request.reason().trim();
  }

  private String maskFingerprint(String value) {
    if (value == null || value.length() <= 12) {
      return value;
    }
    return value.substring(0, 6) + "..." + value.substring(value.length() - 4);
  }

  private LocalDateTime toLocal(Instant instant) {
    return instant == null ? null : LocalDateTime.ofInstant(instant, zoneId);
  }

  private String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }

  private <T> PageResponse<T> toPage(Page<T> page) {
    return new PageResponse<>(
        page.getContent(),
        page.getNumber(),
        page.getSize(),
        page.getTotalElements(),
        page.getTotalPages());
  }
}
