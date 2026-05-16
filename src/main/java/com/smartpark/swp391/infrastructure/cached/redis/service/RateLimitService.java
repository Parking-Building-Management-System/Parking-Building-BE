package com.smartpark.swp391.infrastructure.cached.redis.service;

import jakarta.annotation.PostConstruct;
import java.time.Instant;
import java.util.Collections;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class RateLimitService {

  StringRedisTemplate redisTemplate;
  DefaultRedisScript<Boolean> redisScript = new DefaultRedisScript<>();

  @PostConstruct
  public void init() {
    redisScript.setScriptSource(
        new ResourceScriptSource(new ClassPathResource("scripts/token-bucket.lua")));
    redisScript.setResultType(Boolean.class);
  }

  /**
   * @param fullKey: Key đầy đủ (bao gồm cả prefix)
   * @param capacity: Dung lượng bucket
   * @param rate: Tốc độ hồi phục (token/giây)
   * @param tokensRequested: Số token muốn lấy
   */
  public boolean allowRequest(String fullKey, long capacity, double rate, long tokensRequested) {
    // Dùng giây (Epoch Second)
    long now = Instant.now().getEpochSecond();

    try {
      Boolean allowed =
          redisTemplate.execute(
              redisScript,
              Collections.singletonList(fullKey),
              String.valueOf(rate), // Chuyển double thành String (Lua sẽ hiểu "0.0833")
              String.valueOf(capacity),
              String.valueOf(now),
              String.valueOf(tokensRequested));
      return Boolean.TRUE.equals(allowed);
    } catch (Exception e) {
      log.error("Lỗi Rate Limit Redis với key {}", fullKey, e);
      // Fail-open: Nếu Redis lỗi, cho phép request đi qua
      return true;
    }
  }
}
