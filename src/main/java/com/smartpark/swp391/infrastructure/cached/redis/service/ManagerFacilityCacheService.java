package com.smartpark.swp391.infrastructure.cached.redis.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartpark.swp391.infrastructure.cached.redis.helper.RedisJsonCacheSupport;
import com.smartpark.swp391.infrastructure.cached.redis.keys.RedisKeys;
import com.smartpark.swp391.modules.manager.dto.topology.ParkingTopologyResponse;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class ManagerFacilityCacheService extends RedisJsonCacheSupport {

  private static final Duration TOPOLOGY_TTL = Duration.ofMinutes(10);

  public ManagerFacilityCacheService(StringRedisTemplate redis, ObjectMapper redisObjectMapper) {
    super(redis, redisObjectMapper);
  }

  public Optional<ParkingTopologyResponse> getTopology(UUID tenantId, UUID parkingId) {
    return readRawValue(RedisKeys.tenantParkingTopology(tenantId, parkingId))
        .flatMap(json -> deserialize(json, ParkingTopologyResponse.class));
  }

  public void saveTopology(UUID tenantId, UUID parkingId, ParkingTopologyResponse response) {
    serialize(response)
        .ifPresent(
            json ->
                writeValue(
                    RedisKeys.tenantParkingTopology(tenantId, parkingId), json, TOPOLOGY_TTL));
  }

  public void evictTopology(UUID tenantId, UUID parkingId) {
    deleteKey(RedisKeys.tenantParkingTopology(tenantId, parkingId));
  }
}
