package com.smartpark.swp391.infrastructure.cached.redis.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartpark.swp391.infrastructure.cached.redis.helper.RedisJsonCacheSupport;
import com.smartpark.swp391.infrastructure.cached.redis.keys.RedisKeys;
import com.smartpark.swp391.modules.admin.dto.dashboard.AdminDashboardStatsResponse;
import com.smartpark.swp391.modules.admin.dto.masterdata.AdminVehicleTypeResponse;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class AdminPortalCacheService extends RedisJsonCacheSupport {

  public AdminPortalCacheService(StringRedisTemplate redis, ObjectMapper redisObjectMapper) {
    super(redis, redisObjectMapper);
  }

  public Optional<AdminDashboardStatsResponse> getDashboardStats() {
    return readRawValue(RedisKeys.adminDashboardStats())
        .flatMap(json -> deserialize(json, AdminDashboardStatsResponse.class));
  }

  public void saveDashboardStats(AdminDashboardStatsResponse response) {
    serialize(response)
        .ifPresent(
            json -> writeValue(RedisKeys.adminDashboardStats(), json, Duration.ofMinutes(10)));
  }

  public void evictDashboardStats() {
    deleteKey(RedisKeys.adminDashboardStats());
  }

  public Optional<List<AdminVehicleTypeResponse>> getVehicleTypes() {
    return readRawValue(RedisKeys.adminVehicleTypes())
        .flatMap(json -> deserialize(json, new TypeReference<List<AdminVehicleTypeResponse>>() {}));
  }

  public void saveVehicleTypes(List<AdminVehicleTypeResponse> response) {
    serialize(response)
        .ifPresent(json -> redis.opsForValue().set(RedisKeys.adminVehicleTypes(), json));
  }

  public void evictVehicleTypes() {
    deleteKey(RedisKeys.adminVehicleTypes());
  }
}
