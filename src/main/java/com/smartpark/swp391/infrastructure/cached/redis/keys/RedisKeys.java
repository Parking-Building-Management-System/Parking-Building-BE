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

  private static final String SESS_AUTHZ = PREFIX + ":sess:authz:";
  private static final String SESS_REVOKED = PREFIX + ":sess:revoked:";
  private static final String SESS_ACTIVE = PREFIX + ":sess:active:";
  private static final String TENANT_DETAIL = PREFIX + ":tenant:detail:%s";
  private static final String ADMIN_DASHBOARD_STATS = PREFIX + ":admin:dashboard:stats";
  private static final String ADMIN_VEHICLE_TYPES = PREFIX + ":admin:master-data:vehicle-types";

  private static final String RATE_LIMIT_USER = PREFIX + ":ratelimit:user:";
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

  public static String sessionRevoked(UUID sessionId) {
    requireNonBlank(sessionId, "sessionId");
    return SESS_REVOKED + sessionId;
  }

  public static String sessionAuthz(UUID sessionId) {
    requireNonBlank(sessionId, "sessionId");
    return SESS_AUTHZ + sessionId;
  }

  public static String sessionActive(UUID sessionId) {
    requireNonBlank(sessionId, "sessionId");
    return SESS_ACTIVE + sessionId;
  }

  public static String rateLimitUser(UUID userId) {
    requireNonBlank(userId, "userId");
    return RATE_LIMIT_USER + userId;
  }

  public static String rateLimitLogin(String phoneNumber) {
    requireNonBlank(phoneNumber, "phoneNumber");
    return RATE_LIMIT_LOGIN + phoneNumber;
  }

  public static String tenantDetail(UUID tenantId) {
    requireNonBlank(tenantId, "tenantId");
    return String.format(TENANT_DETAIL, tenantId);
  }

  /**
   * Dùng để xóa TOÀN BỘ cache của một khách hàng khi họ bị khóa tài khoản (Suspend/Deactive). Cách
   * dùng: redisJsonCacheSupport.scanAndDelete(RedisKeys.tenantPattern(tenantId));
   */
  public static String tenantPattern(UUID tenantId) {
    requireNonBlank(tenantId, "tenantId");
    return PREFIX + ":tenant:" + tenantId + ":*";
  }

  public static String adminDashboardStats() {
    return ADMIN_DASHBOARD_STATS;
  }

  public static String adminVehicleTypes() {
    return ADMIN_VEHICLE_TYPES;
  }
}
