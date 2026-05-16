package com.smartpark.swp391.common.security.annotation;

import com.smartpark.swp391.common.exception.ApiException;
import com.smartpark.swp391.common.exception.ErrorCode;
import com.smartpark.swp391.infrastructure.cached.redis.keys.RedisKeys;
import com.smartpark.swp391.infrastructure.cached.redis.service.RateLimitService;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class RateLimitAspect {

  RateLimitService rateLimitService;

  // Optimize: Sử dụng spEl thay vì Reflection
  ExpressionParser parser = new SpelExpressionParser();
  ParameterNameDiscoverer nameDiscoverer = new DefaultParameterNameDiscoverer();

  @Around("@annotation(rateLimit)")
  public Object handleRateLimit(ProceedingJoinPoint joinPoint, RateLimit rateLimit)
      throws Throwable {

    String keySuffix = resolveKey(joinPoint, rateLimit);
    String fullKey = "";

    switch (rateLimit.type()) {
      case USER_ID:
        try {
          fullKey = RedisKeys.rateLimitUser(UUID.fromString(keySuffix));
        } catch (IllegalArgumentException e) {
          log.error("Invalid User UUID in JWT: {}", keySuffix);
          throw new ApiException(ErrorCode.UNAUTHENTICATED);
        }
        break;

      case REQUEST_FIELD:
        fullKey = RedisKeys.rateLimitLogin(keySuffix);
        break;

      default:
        return joinPoint.proceed();
    }

    long capacity = rateLimit.limit();
    double refillRate = (double) capacity / rateLimit.duration();

    boolean allowed = rateLimitService.allowRequest(fullKey, capacity, refillRate, 1);

    if (!allowed) {
      log.warn("Rate limit exceeded for key: {}", fullKey);
      throw new ApiException(ErrorCode.TOO_MANY_REQUESTS);
    }

    return joinPoint.proceed();
  }

  private String resolveKey(ProceedingJoinPoint joinPoint, RateLimit rateLimit) {
    switch (rateLimit.type()) {
      case USER_ID:
        return getUserIdFromSecurityContext();

      case REQUEST_FIELD:
        return getFieldFromArgs(joinPoint, rateLimit.fieldName());

      default:
        return "unknown";
    }
  }

  private String getUserIdFromSecurityContext() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

    if (authentication == null || !authentication.isAuthenticated()) {
      throw new ApiException(ErrorCode.UNAUTHENTICATED);
    }

    if (authentication instanceof JwtAuthenticationToken jwtAuth) {
      String userId = jwtAuth.getToken().getClaimAsString("user_id");
      if (userId != null) {
        return userId;
      }
    }

    log.warn("Cannot find user_id in SecurityContext");
    throw new ApiException(ErrorCode.UNAUTHENTICATED);
  }

  // Với spEl, dev có thể linh hoạt truyền biến ở tầng controller
  private String getFieldFromArgs(ProceedingJoinPoint joinPoint, String spelExpression) {
    MethodSignature signature = (MethodSignature) joinPoint.getSignature();
    Object[] args = joinPoint.getArgs();

    // Lấy tên thật của các tham số (ví dụ: ["payload", "request", "id"])
    String[] parameterNames = nameDiscoverer.getParameterNames(signature.getMethod());

    if (parameterNames == null) {
      return "unknown";
    }

    StandardEvaluationContext context = new StandardEvaluationContext();
    for (int i = 0; i < parameterNames.length; i++) {
      context.setVariable(parameterNames[i], args[i]);
    }

    try {
      // Thực thi cú pháp SpEL (VD: "#payload.phoneNumber")
      Object value = parser.parseExpression(spelExpression).getValue(context);
      return value != null ? value.toString() : "unknown";
    } catch (Exception e) {
      log.error("Failed to parse SpEL expression: {}", spelExpression, e);
      return "unknown";
    }
  }
}
