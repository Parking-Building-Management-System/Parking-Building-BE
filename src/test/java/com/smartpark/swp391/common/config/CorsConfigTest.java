package com.smartpark.swp391.common.config;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

class CorsConfigTest {

  @Test
  void corsConfigurationAllowsProductionAndLocalOriginsWithCredentials() {
    CorsConfig corsConfig = new CorsConfig();
    ReflectionTestUtils.setField(
        corsConfig,
        "allowedOrigins",
        List.of("https://www.hmnm.id.vn", "https://hmnm.id.vn", "http://localhost:3000"));

    CorsConfiguration configuration =
        configuration(corsConfig.corsConfigurationSource(), "https://www.hmnm.id.vn");

    assertThat(configuration.getAllowedOrigins())
        .contains("https://www.hmnm.id.vn", "https://hmnm.id.vn", "http://localhost:3000");
    assertThat(configuration.getAllowCredentials()).isTrue();
    assertThat(configuration.getAllowedMethods())
        .contains("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS");
    assertThat(configuration.checkOrigin("https://www.hmnm.id.vn"))
        .isEqualTo("https://www.hmnm.id.vn");
    assertThat(configuration.checkOrigin("https://evil.example")).isNull();
  }

  private CorsConfiguration configuration(CorsConfigurationSource source, String origin) {
    MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", "/auth/login");
    request.addHeader("Origin", origin);
    request.addHeader("Access-Control-Request-Method", "POST");
    return source.getCorsConfiguration((HttpServletRequest) request);
  }
}
