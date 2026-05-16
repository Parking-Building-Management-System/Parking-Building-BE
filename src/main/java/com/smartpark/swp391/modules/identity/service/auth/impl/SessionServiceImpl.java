package com.smartpark.swp391.modules.identity.service.auth.impl;

import com.smartpark.swp391.common.exception.ApiException;
import com.smartpark.swp391.common.exception.ErrorCode;
import com.smartpark.swp391.infrastructure.cached.redis.service.SessionAuthorityCacheService;
import com.smartpark.swp391.infrastructure.persistence.UuidV7;
import com.smartpark.swp391.modules.identity.entity.Device;
import com.smartpark.swp391.modules.identity.entity.Session;
import com.smartpark.swp391.modules.identity.entity.User;
import com.smartpark.swp391.modules.identity.repository.SessionRepository;
import com.smartpark.swp391.modules.identity.service.auth.SessionService;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Slf4j
public class SessionServiceImpl implements SessionService {

  @Value("${jwt.refreshable-duration}")
  long refreshableDurationSeconds;

  final SessionRepository sessionRepository;
  final SessionAuthorityCacheService sessionAuthorityCacheService;

  @Override
  @Transactional
  public Session createSession(User user, Device device) {
    Session session = new Session();
    session.setUser(user);
    session.setDevice(device);
    session.setExpiredAt(LocalDateTime.now().plusSeconds(refreshableDurationSeconds));
    session.setRefreshJti(UuidV7.random());
    session.setRevokedAt(null);
    return sessionRepository.save(session);
  }

  @Override
  @Transactional
  public UUID rotateRefreshJti(UUID sessionId, UUID oldJti) {
    UUID newJti = UuidV7.random();
    int updated = sessionRepository.rotateRefreshJti(sessionId, oldJti, newJti);
    if (updated != 1) {
      throw new ApiException(ErrorCode.UNAUTHENTICATED);
    }
    return newJti;
  }

  @Override
  @Transactional
  public void revoke(UUID sessionId, Duration accessTtl) {
    if (sessionId == null) throw new ApiException(ErrorCode.UNAUTHENTICATED);
    if (accessTtl == null || accessTtl.isZero() || accessTtl.isNegative()) {
      accessTtl = Duration.ofSeconds(1);
    }

    sessionRepository.revokeIfNotRevoked(sessionId, LocalDateTime.now());

    try {
      sessionAuthorityCacheService.markRevoked(sessionId, accessTtl);
      sessionAuthorityCacheService.clearAuthz(sessionId);
      sessionAuthorityCacheService.clearActive(sessionId);
    } catch (Exception e) {
      log.warn("Revoke redis best-effort failed sessionId={}", sessionId, e);
    }
  }

  @Override
  @Transactional
  public void revokeAll(UUID userId) {
    LocalDateTime now = LocalDateTime.now();
    List<UUID> sessionIds = sessionRepository.findActiveSessionIdsByUserId(userId, now);

    sessionRepository.revokeAllActiveByUserId(userId, now);

    Duration accessTtl = Duration.ofMinutes(15);
    for (UUID sid : sessionIds) {
      try {
        sessionAuthorityCacheService.markRevoked(sid, accessTtl);
        sessionAuthorityCacheService.clearAuthz(sid);
        sessionAuthorityCacheService.clearActive(sid);
      } catch (Exception e) {
        log.warn("RevokeAll redis best-effort failed sessionId={}", sid, e);
      }
    }
  }
}
