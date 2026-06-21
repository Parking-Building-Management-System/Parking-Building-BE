package com.smartpark.swp391.modules.identity.service.auth.impl;

import com.smartpark.swp391.common.exception.ApiException;
import com.smartpark.swp391.common.exception.ErrorCode;
import com.smartpark.swp391.common.security.config.SessionAuthorityResolver;
import com.smartpark.swp391.infrastructure.cached.redis.model.SessionAuthzCache;
import com.smartpark.swp391.infrastructure.persistence.UuidV7;
import com.smartpark.swp391.modules.identity.dto.authentication.request.AuthenticationRequest;
import com.smartpark.swp391.modules.identity.dto.authentication.response.UserProfileResponse;
import com.smartpark.swp391.modules.identity.dto.token.request.TokenRequest;
import com.smartpark.swp391.modules.identity.dto.token.response.TokenPair;
import com.smartpark.swp391.modules.identity.entity.Device;
import com.smartpark.swp391.modules.identity.entity.Session;
import com.smartpark.swp391.modules.identity.entity.User;
import com.smartpark.swp391.modules.identity.enumType.DeviceStatus;
import com.smartpark.swp391.modules.identity.enumType.TenantStatus;
import com.smartpark.swp391.modules.identity.enumType.UserStatus;
import com.smartpark.swp391.modules.identity.repository.DeviceRepository;
import com.smartpark.swp391.modules.identity.repository.RoleRepository;
import com.smartpark.swp391.modules.identity.repository.SessionRepository;
import com.smartpark.swp391.modules.identity.repository.UserRepository;
import com.smartpark.swp391.modules.identity.service.auth.AuthenticationService;
import com.smartpark.swp391.modules.identity.service.auth.SessionService;
import com.smartpark.swp391.modules.identity.service.token.TokenService;
import com.smartpark.swp391.modules.operation.entity.Kiosk;
import com.smartpark.swp391.modules.operation.enumType.KioskStatus;
import com.smartpark.swp391.modules.operation.repository.KioskStaffRepository;
import com.smartpark.swp391.modules.staff.service.StaffWorkContextService;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@Slf4j
public class AuthenticationServiceImpl implements AuthenticationService {

  private static final String DEV_ROLE = "DEV";

  @Value("${jwt.refreshable-duration}")
  @NonFinal
  long refreshableDurationSeconds;

  @Value("${jwt.valid-duration}")
  @NonFinal
  long accessTokenTtlSeconds;

  UserRepository userRepository;
  SessionRepository sessionRepository;
  DeviceRepository deviceRepository;
  RoleRepository roleRepository;
  PasswordEncoder passwordEncoder;
  TokenService tokenService;
  SessionService sessionService;
  SessionAuthorityResolver sessionAuthorityResolver;
  StaffWorkContextService staffWorkContextService;
  KioskStaffRepository kioskStaffRepository;

  @Override
  @Transactional(noRollbackFor = ApiException.class)
  public TokenPair authenticate(AuthenticationRequest request) {
    // 1. Check Username
    User user =
        userRepository
            .findByUsername(request.username())
            .orElseThrow(() -> new ApiException(ErrorCode.INVALID_INFO));

    // 2. Check Password
    if (!passwordEncoder.matches(request.password(), user.getPassword())) {
      throw new ApiException(ErrorCode.INVALID_INFO);
    }

    if (user.getStatus() != UserStatus.ACTIVE) {
      throw new ApiException(ErrorCode.FORBIDDEN_ACTION);
    }

    List<String> roles = roleRepository.findRoleNamesByUserId(user.getId());
    boolean systemAdmin = roles.contains("SYSTEM_ADMIN");
    boolean parkingManager = roles.contains("PARKING_MANAGER");
    boolean staff = roles.contains("STAFF");
    boolean dev = roles.contains(DEV_ROLE);

    if (!systemAdmin && user.getTenant().getStatus() != TenantStatus.ACTIVE) {
      throw new ApiException(ErrorCode.FORBIDDEN_ACTION);
    }

    // 3. Device Check
    Device device = resolveLoginDevice(user, request, systemAdmin, parkingManager);
    if (staff || dev) {
      ensureStaffDeviceWorkContext(user, device);
    }

    // 4. Info hợp lệ --> tạo session
    Session session = sessionService.createSession(user, device);

    // 5. Tạo Token
    TokenRequest tokenRequest =
        TokenRequest.builder()
            .userId(user.getId())
            .tenantId(user.getTenant().getId())
            .sessionId(session.getId())
            .subject(user.getUsername())
            .refreshJti(session.getRefreshJti())
            .build();

    return tokenService.generateTokenPair(tokenRequest);
  }

  private Device resolveLoginDevice(
      User user, AuthenticationRequest request, boolean systemAdmin, boolean parkingManager) {
    Device device =
        deviceRepository
            .findByUserIdAndFingerprint(user.getId(), request.deviceFingerprint())
            .orElse(null);

    if (systemAdmin) {
      return device == null ? createApprovedDevice(user, request) : device;
    }

    if (parkingManager) {
      return resolveManagerDevice(user, request, device);
    }

    if (device == null) {
      // Máy lạ hoắc -> Lưu xuống DB với trạng thái PENDING chờ duyệt
      Device newDevice =
          Device.builder()
              .user(user)
              .fingerprint(request.deviceFingerprint())
              .label(request.deviceLabel())
              .status(DeviceStatus.PENDING)
              .build();
      deviceRepository.save(newDevice);
      throw new ApiException(ErrorCode.DEVICE_NOT_TRUST);
    }

    ensureApprovedDevice(device);
    return device;
  }

  private Device resolveManagerDevice(
      User user, AuthenticationRequest request, Device existingDevice) {
    if (existingDevice != null && existingDevice.getStatus() == DeviceStatus.APPROVED) {
      return existingDevice;
    }

    long approvedDeviceCount =
        deviceRepository.countByUserIdAndStatus(user.getId(), DeviceStatus.APPROVED);

    if (approvedDeviceCount == 0) {
      if (existingDevice == null) {
        return createApprovedDevice(user, request);
      }

      existingDevice.setStatus(DeviceStatus.APPROVED);
      existingDevice.setLabel(request.deviceLabel());
      existingDevice.setApprovedBy(user.getId());
      existingDevice.setApprovedAt(LocalDateTime.now());
      existingDevice.setExpiresAt(null);
      return deviceRepository.save(existingDevice);
    }

    if (existingDevice == null) {
      Device pendingDevice =
          Device.builder()
              .user(user)
              .fingerprint(request.deviceFingerprint())
              .label(request.deviceLabel())
              .status(DeviceStatus.PENDING)
              .build();
      deviceRepository.save(pendingDevice);
    }

    if (existingDevice != null && existingDevice.getStatus() == DeviceStatus.SUSPENDED) {
      throw new ApiException(ErrorCode.FORBIDDEN_ACTION);
    }
    throw new ApiException(ErrorCode.DEVICE_NOT_TRUST);
  }

  private Device createApprovedDevice(User user, AuthenticationRequest request) {
    Device device =
        Device.builder()
            .user(user)
            .fingerprint(request.deviceFingerprint())
            .label(request.deviceLabel())
            .status(DeviceStatus.APPROVED)
            .approvedBy(user.getId())
            .approvedAt(LocalDateTime.now())
            .build();
    return deviceRepository.save(device);
  }

  private void ensureApprovedDevice(Device device) {
    if (device.getStatus() == DeviceStatus.SUSPENDED) {
      throw new ApiException(ErrorCode.FORBIDDEN_ACTION);
    }
    if (device.getStatus() != DeviceStatus.APPROVED) {
      throw new ApiException(ErrorCode.DEVICE_NOT_TRUST);
    }
  }

  private void ensureStaffDeviceWorkContext(User user, Device device) {
    Kiosk kiosk = device.getKiosk();
    if (kiosk == null) {
      throw new ApiException(ErrorCode.DEVICE_NOT_TRUST, "KIOSK_CONTEXT_REQUIRED");
    }
    if (device.getExpiresAt() != null && device.getExpiresAt().isBefore(LocalDateTime.now())) {
      throw new ApiException(ErrorCode.DEVICE_NOT_TRUST);
    }
    if (kiosk.getStatus() != KioskStatus.ACTIVE) {
      throw new ApiException(ErrorCode.FORBIDDEN_ACTION, "Kiosk is not active");
    }
    if (!user.getTenant().getId().equals(kiosk.getTenant().getId())) {
      throw new ApiException(ErrorCode.FORBIDDEN_ACTION);
    }
    if (!kioskStaffRepository.existsActiveAssignment(
        user.getTenant().getId(), kiosk.getId(), user.getId())) {
      throw new ApiException(ErrorCode.FORBIDDEN_ACTION, "STAFF_NOT_ASSIGNED_TO_KIOSK");
    }
  }

  @Override
  @Transactional
  public TokenPair refresh(String refreshToken) {
    if (refreshToken == null || refreshToken.isBlank()) {
      throw new ApiException(ErrorCode.UNAUTHENTICATED);
    }
    TokenRequest extracted = tokenService.extractToken(refreshToken);
    UUID sessionId = extracted.getSessionId();
    UUID oldJti = extracted.getRefreshJti();

    Session session =
        sessionRepository
            .findById(sessionId)
            .orElseThrow(() -> new ApiException(ErrorCode.UNAUTHENTICATED));

    if (session.getRevokedAt() != null || session.getExpiredAt().isBefore(LocalDateTime.now())) {
      throw new ApiException(ErrorCode.UNAUTHENTICATED);
    }
    if (!session.getUser().getId().equals(extracted.getUserId())) {
      throw new ApiException(ErrorCode.UNAUTHENTICATED);
    }

    UUID newJti = UuidV7.random();
    int updated = sessionRepository.rotateRefreshJti(sessionId, oldJti, newJti);
    if (updated != 1) {
      throw new ApiException(ErrorCode.UNAUTHENTICATED);
    }

    TokenRequest tokenReq =
        TokenRequest.builder()
            .userId(extracted.getUserId())
            .tenantId(extracted.getTenantId())
            .sessionId(sessionId)
            .subject(extracted.getSubject())
            .refreshJti(newJti)
            .build();

    String accessToken = tokenService.generateAccessToken(tokenReq);
    Date refreshExpiry =
        Date.from(session.getExpiredAt().atZone(ZoneId.systemDefault()).toInstant());
    String refreshTokenNew = tokenService.generateRefreshToken(tokenReq, refreshExpiry);

    long refreshTtlRemaining =
        Math.max(0, Duration.between(LocalDateTime.now(), session.getExpiredAt()).getSeconds());

    return TokenPair.builder()
        .accessToken(accessToken)
        .refreshToken(refreshTokenNew)
        .accessTtl(accessTokenTtlSeconds)
        .refreshTtl(refreshTtlRemaining)
        .build();
  }

  @Override
  @Transactional
  public void logout(UUID sessionId, UUID userId) {
    if (sessionId == null || userId == null) {
      throw new ApiException(ErrorCode.UNAUTHENTICATED);
    }
    Duration accessTtl = Duration.ofSeconds(accessTokenTtlSeconds);
    sessionService.revoke(sessionId, userId, accessTtl);
  }

  @Override
  @Transactional
  public void logoutAll(UUID userId) {
    sessionService.revokeAll(userId);
  }

  @Override
  @Transactional
  public void forceLogout(UUID targetUserId) {
    sessionService.revokeAll(targetUserId);
  }

  @Override
  @Transactional(readOnly = true)
  public UserProfileResponse getMyProfile() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (!(auth instanceof JwtAuthenticationToken jwtAuth)) {
      throw new ApiException(ErrorCode.UNAUTHENTICATED);
    }

    Jwt jwt = jwtAuth.getToken();
    UUID userId = UUID.fromString(jwt.getClaimAsString("user_id"));
    UUID sessionId = UUID.fromString(jwt.getClaimAsString("session_id"));
    UUID tenantId = UUID.fromString(jwt.getClaimAsString("tenant_id"));

    SessionAuthzCache authzCache =
        sessionAuthorityResolver.resolve(sessionId, userId, jwt.getExpiresAt());

    User user =
        userRepository
            .findById(userId)
            .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND));

    return UserProfileResponse.builder()
        .id(user.getId())
        .tenantId(tenantId)
        .username(user.getUsername())
        .fullName(user.getFullName())
        .phone(user.getPhone())
        .roles(authzCache.roles())
        .permissions(authzCache.permissions())
        .workContext(
            authzCache.roles().contains("STAFF")
                ? staffWorkContextService.resolveContext(sessionId, userId, tenantId).orElse(null)
                : null)
        .build();
  }
}
