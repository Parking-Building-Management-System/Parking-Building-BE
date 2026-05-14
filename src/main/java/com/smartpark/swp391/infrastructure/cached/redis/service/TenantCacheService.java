package com.smartpark.swp391.infrastructure.cached.redis.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartpark.swp391.infrastructure.cached.redis.helper.RedisJsonCacheSupport;
import com.smartpark.swp391.infrastructure.cached.redis.keys.RedisKeys;
import com.smartpark.swp391.modules.identity.dto.tenant.response.TenantResponse;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class TenantCacheService extends RedisJsonCacheSupport {

  public TenantCacheService(StringRedisTemplate redis, ObjectMapper redisObjectMapper) {
    super(redis, redisObjectMapper);
  }

  public Optional<TenantResponse> getTenant(UUID id) {
    String cacheKey = RedisKeys.tenantDetail(id);
    return readRawValue(cacheKey).flatMap(json -> deserialize(json, TenantResponse.class));
  }

  public void saveTenant(UUID id, TenantResponse response) {
    String cacheKey = RedisKeys.tenantDetail(id);
    serialize(response).ifPresent(json -> writeValue(cacheKey, json, Duration.ofHours(1)));
  }

  public void evictTenantData(UUID id) {
    deleteKey(RedisKeys.tenantDetail(id));
    scanAndDelete(RedisKeys.tenantPattern(id));
  }
}
