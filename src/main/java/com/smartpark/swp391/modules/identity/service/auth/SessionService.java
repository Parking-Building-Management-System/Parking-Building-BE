package com.smartpark.swp391.modules.identity.service.auth;

import com.smartpark.swp391.modules.identity.entity.Device;
import com.smartpark.swp391.modules.identity.entity.Session;
import com.smartpark.swp391.modules.identity.entity.User;
import java.time.Duration;
import java.util.UUID;

public interface SessionService {
  Session createSession(User user, Device device);

  UUID rotateRefreshJti(UUID sessionId, UUID oldJti);

  void revoke(UUID sessionId, Duration accessTtl);

  void revoke(UUID sessionId, UUID userId, Duration accessTtl);

  void revokeAll(UUID userId);
}
