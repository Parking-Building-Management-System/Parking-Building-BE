package com.smartpark.swp391.modules.dashboard.filter;

import com.smartpark.swp391.modules.dashboard.entity.ApiTrafficLog;
import com.smartpark.swp391.modules.dashboard.repository.ApiTrafficLogRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Set;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Slf4j
@Component
@Order(Ordered.LOWEST_PRECEDENCE)
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class ApiTrafficLoggingFilter extends OncePerRequestFilter {

  private static final Set<String> EXCLUDED_PREFIXES =
      Set.of(
          "/dashboard",
          "/swagger-ui",
          "/v3/api-docs",
          "/actuator",
          "/internal/healthz",
          "/api/v1/internal/healthz");

  ApiTrafficLogRepository apiTrafficLogRepository;

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    String path = request.getRequestURI();
    return EXCLUDED_PREFIXES.stream().anyMatch(path::startsWith);
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    long startedAt = System.nanoTime();

    try {
      filterChain.doFilter(request, response);
    } finally {
      long durationMs = (System.nanoTime() - startedAt) / 1_000_000;
      persistTrafficLog(request, response, durationMs);
    }
  }

  private void persistTrafficLog(
      HttpServletRequest request, HttpServletResponse response, long durationMs) {
    try {
      ApiTrafficLog logEntry =
          ApiTrafficLog.builder()
              .method(request.getMethod())
              .path(request.getRequestURI())
              .statusCode(response.getStatus())
              .durationMs(durationMs)
              .occurredAt(LocalDateTime.now())
              .build();
      apiTrafficLogRepository.save(logEntry);
    } catch (Exception ex) {
      log.warn("Không ghi được API traffic log cho path={}", request.getRequestURI(), ex);
    }
  }
}
