package com.smartpark.swp391.common.security.config;

import com.smartpark.swp391.common.exception.ApiException;
import com.smartpark.swp391.infrastructure.cached.redis.model.SessionAuthzCache;
import com.smartpark.swp391.infrastructure.cached.redis.service.SessionAuthorityCacheService;
import com.smartpark.swp391.modules.identity.service.auth.AuthorityLoader;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

// Orchestrates the retrieval of user authorization data. Implements a Cache-Aside pattern with
// Redis, mapping the cache TTL dynamically to the JWT's expiration time to ensure synchronized
// lifecycle management.
@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
// this file implement the logic to answer the question
// "How Spring Security get the Role and Permission of request ?. In the JWT
// just contain sub
// (phoneNumber) and session_id"
// Logic of system:
// Using session_id in JWT --> Look up in redis
// Check guard to know that session is active, if not --> Fail
// if HIT CACHED --> Get info
// if MISS CACHED --> Query Database --> Put Redis --> Get Info
public class SessionAuthorityResolver {
  SessionAuthorityCacheService sessionAuthorityCacheService;
  AuthorityLoader authorityLoader;
  SessionGuardService sessionGuardService;

  // Logic to reuse jwtExp for redis ttl (not hardcode):
  // Assume that now is 0:00h, jwtExp expired at 0:10 --> ttl is 10p
  // At every request, get jwtExp --> calculate ttl --> put redis
  // When jwtExp cannot used --> redis auto revoke (next request will get from DB)

  public SessionAuthzCache resolve(UUID sessionId, UUID userId, Instant jwtExp) {
    // 1. Dynamic TTL Calculation: Align Redis TTL with JWT Expiration
    Duration ttl = Duration.between(Instant.now(), jwtExp);
    if (ttl.isNegative() || ttl.isZero()) {
      ttl = Duration.ofSeconds(1);
    }

    // 2. The Guard: Hard check if session is forcefully revoked or inactive
    sessionGuardService.ensureActive(sessionId, ttl);
    try {
      // 3. Cache HIT: Return immediately
      SessionAuthzCache cached = sessionAuthorityCacheService.get(sessionId).orElse(null);
      if (cached != null) {
        return cached;
      }
      // 4. Cache MISS: Hit the Database, hydrate Cache, and return
      SessionAuthzCache loaded = authorityLoader.load(userId);
      sessionAuthorityCacheService.put(sessionId, loaded, ttl);
      return loaded;

    } catch (ApiException e) {
      throw e;

    } catch (Exception ex) {
      // 5. Fault Tolerance: If Redis is down, fallback to direct DB lookup
      // Ensures the application remains functional during cache outages
      log.warn("AUTHZ cache FAIL -> fallback DB sessionId={} userId={}.", sessionId, userId, ex);
      return authorityLoader.load(userId);
    }
  }
}
