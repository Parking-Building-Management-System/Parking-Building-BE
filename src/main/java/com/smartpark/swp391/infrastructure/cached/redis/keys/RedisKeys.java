package com.smartpark.swp391.infrastructure.cached.redis.keys;

import java.util.Objects;
import java.util.UUID;

// This file defines the keys used in StringRedisTemplate<Key,Value>
// Kinh nghiệm làm multi-tenant SaaS: gắn tenantId vào redisKey mặc dù key đó không cần. Khi khách
// hàng (tenant) hết hạn license --> quét redis theo tenantId và kick session. Thay vì phải query DB
// và kick.
public final class RedisKeys {
  private RedisKeys() {}

  private static final String PREFIX = "smartpark";

  private static final String SESS_AUTHZ = PREFIX + ":tenant:%s:sess:authz:";
  private static final String SESS_REVOKED = PREFIX + ":tenant:%s:sess:revoked:";
  private static final String SESS_ACTIVE = PREFIX + ":tenant:%s:sess:active:";
  private static final String RATE_LIMIT_USER = PREFIX + ":tenant:%s:ratelimit:user:";

  private static final String RATE_LIMIT_LOGIN = PREFIX + ":ratelimit:login:";

  // Helper kiểm tra UUID
  private static void requireNonBlank(UUID value, String name) {
    if (Objects.isNull(value)) {
      throw new IllegalArgumentException(name + " must not be null");
    }
  }

  // Helper kiểm tra String
  private static void requireNonBlank(String value, String name) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(name + " must not be blank");
    }
  }

  public static String sessionRevoked(UUID tenantId, UUID sessionId) {
    requireNonBlank(tenantId, "tenantId");
    requireNonBlank(sessionId, "sessionId");
    return String.format(SESS_REVOKED, tenantId) + sessionId;
  }

  public static String sessionAuthz(UUID tenantId, UUID sessionId) {
    requireNonBlank(tenantId, "tenantId");
    requireNonBlank(sessionId, "sessionId");
    return String.format(SESS_AUTHZ, tenantId) + sessionId;
  }

  public static String sessionActive(UUID tenantId, UUID sessionId) {
    requireNonBlank(tenantId, "tenantId");
    requireNonBlank(sessionId, "sessionId");
    return String.format(SESS_ACTIVE, tenantId) + sessionId;
  }

  public static String rateLimitUser(UUID tenantId, UUID userId) {
    requireNonBlank(tenantId, "tenantId");
    requireNonBlank(userId, "userId");
    return String.format(RATE_LIMIT_USER, tenantId) + userId;
  }

  public static String rateLimitLogin(String phoneNumber) {
    requireNonBlank(phoneNumber, "phoneNumber");
    return RATE_LIMIT_LOGIN + phoneNumber;
  }

  /**
   * Dùng để xóa TOÀN BỘ cache của một khách hàng khi họ bị khóa tài khoản (Suspend/Deactive). Cách
   * dùng: redisJsonCacheSupport.scanAndDelete(RedisKeys.tenantPattern(tenantId));
   */
  public static String tenantPattern(UUID tenantId) {
    requireNonBlank(tenantId, "tenantId");
    return PREFIX + ":tenant:" + tenantId + ":*";
  }
}
