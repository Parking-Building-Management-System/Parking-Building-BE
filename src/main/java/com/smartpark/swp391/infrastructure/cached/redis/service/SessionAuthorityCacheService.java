package com.smartpark.swp391.infrastructure.cached.redis.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartpark.swp391.infrastructure.cached.redis.keys.RedisKeys;
import com.smartpark.swp391.infrastructure.cached.redis.model.SessionAuthzCache;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@AllArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class SessionAuthorityCacheService {

  StringRedisTemplate redis;
  ObjectMapper objectMapper;

  public Optional<SessionAuthzCache> get(UUID sessionId) {
    if (sessionId == null) {
      return Optional.empty();
    }

    String key = RedisKeys.sessionAuthz(sessionId);
    return readRawValue(key).flatMap(this::deserializeSessionAuthz);
  }

  public boolean put(UUID sessionId, SessionAuthzCache value, Duration ttl) {
    if (sessionId == null || value == null || !isValidTtl(ttl)) {
      return false;
    }

    String key = RedisKeys.sessionAuthz(sessionId);
    return serialize(value).map(json -> writeValue(key, json, ttl)).orElse(false);
  }

  public boolean markRevoked(UUID sessionId, Duration ttl) {
    if (sessionId == null || !isValidTtl(ttl)) {
      return false;
    }

    String key = RedisKeys.sessionRevoked(sessionId);
    try {
      redis.opsForValue().set(key, "1", ttl);
      return true;
    } catch (DataAccessException e) {
      log.warn("Ghi đè revoked-marker thất bại cho key={}", key, e);
      return false;
    }
  }

  public boolean isRevoked(UUID sessionId) {
    if (sessionId == null) {
      return false;
    }

    String key = RedisKeys.sessionRevoked(sessionId);
    try {
      return Boolean.TRUE.equals(redis.hasKey(key));
    } catch (DataAccessException e) {
      log.warn("Đọc revoked-marker thất bại cho key={}", key, e);
      return false;
    }
  }

  public boolean markActive(UUID sessionId, Duration ttl) {
    if (sessionId == null || !isValidTtl(ttl)) {
      return false;
    }

    String key = RedisKeys.sessionActive(sessionId);
    try {
      redis.opsForValue().set(key, "1", ttl);
      return true;
    } catch (DataAccessException e) {
      log.warn("Ghi active-marker thất bại cho key={}", key, e);
      return false;
    }
  }

  public boolean isActive(UUID sessionId) {
    if (sessionId == null) {
      return false;
    }

    String key = RedisKeys.sessionActive(sessionId);
    try {
      return Boolean.TRUE.equals(redis.hasKey(key));
    } catch (DataAccessException e) {
      log.warn("Đọc active-marker thất bại cho key={}", key, e);
      return false;
    }
  }

  public boolean clearActive(UUID sessionId) {
    if (sessionId == null) {
      return false;
    }
    String key = RedisKeys.sessionActive(sessionId);
    try {
      redis.delete(key);
      return true;
    } catch (DataAccessException e) {
      log.warn("Xóa active-marker thất bại cho key={}", key, e);
      return false;
    }
  }

  public boolean clearAuthz(UUID sessionId) {
    if (sessionId == null) {
      return false;
    }
    String key = RedisKeys.sessionAuthz(sessionId);
    try {
      redis.delete(key);
      return true;
    } catch (DataAccessException e) {
      log.warn("Xóa authz-cache thất bại cho key={}", key, e);
      return false;
    }
  }

  private boolean isValidTtl(Duration ttl) {
    return ttl != null && !ttl.isZero() && !ttl.isNegative();
  }

  private Optional<String> readRawValue(String key) {
    try {
      String json = redis.opsForValue().get(key);
      if (json == null || json.isBlank()) {
        return Optional.empty();
      }
      return Optional.of(json);
    } catch (DataAccessException e) {
      log.warn("Redis sập hoặc lỗi khi đọc key={}", key, e);
      return Optional.empty();
    }
  }

  private Optional<SessionAuthzCache> deserializeSessionAuthz(String json) {
    try {
      return Optional.of(objectMapper.readValue(json, SessionAuthzCache.class));
    } catch (JsonProcessingException e) {
      log.error("Lỗi parse JSON ngược về SessionAuthzCache", e);
      return Optional.empty();
    }
  }

  private boolean writeValue(String key, String json, Duration ttl) {
    try {
      redis.opsForValue().set(key, json, ttl);
      return true;
    } catch (DataAccessException e) {
      log.warn("Ghi JSON vào Redis thất bại cho key={}", key, e);
      return false;
    }
  }

  private Optional<String> serialize(SessionAuthzCache value) {
    try {
      return Optional.of(objectMapper.writeValueAsString(value));
    } catch (JsonProcessingException e) {
      log.error("Lỗi chuyển đổi Object sang JSON: {}", value, e);
      return Optional.empty();
    }
  }
}
