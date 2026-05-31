package com.smartpark.swp391.common.security.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartpark.swp391.common.exception.ErrorCode;
import com.smartpark.swp391.common.response.ApiResponse;
import com.smartpark.swp391.common.security.jwt.CustomJwtDecoder;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.client.RestTemplate;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class SecurityConfig {

  CustomJwtDecoder customJwtDecoder;
  JwtAuthenticationConverter jwtAuthenticationConverter;
  ObjectMapper objectMapper;

  private static final String[] SWAGGER_ENDPOINTS = {
    "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html"
  };

  private static final String[] PUBLIC_AUTH_ENDPOINTS = {
    "/auth/login", "/auth/refresh", "/internal/healthz"
  };

  private static final String[] PUBLIC_PWA_ENDPOINTS = {"/pwa/**"};

  private static final String[] PUBLIC_PAYMENT_WEBHOOK_ENDPOINTS = {"/payments/webhooks/payos"};

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder(10);
  }

  @Bean
  public RestTemplate restTemplate() {
    return new RestTemplate();
  }

  @Bean
  @Order(1)
  public SecurityFilterChain publicAuthChain(HttpSecurity http) throws Exception {
    http.securityMatcher(PUBLIC_AUTH_ENDPOINTS)
        .cors(Customizer.withDefaults())
        .csrf(AbstractHttpConfigurer::disable)
        .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
        .oauth2ResourceServer(AbstractHttpConfigurer::disable);

    return http.build();
  }

  @Bean
  @Order(2)
  public SecurityFilterChain apiChain(HttpSecurity http) throws Exception {
    http.securityMatcher("/**");

    http.cors(Customizer.withDefaults()).csrf(AbstractHttpConfigurer::disable);

    http.authorizeHttpRequests(
        auth ->
            auth.requestMatchers(SWAGGER_ENDPOINTS)
                .permitAll()
                .requestMatchers(PUBLIC_AUTH_ENDPOINTS)
                .permitAll()
                .requestMatchers(PUBLIC_PWA_ENDPOINTS)
                .permitAll()
                .requestMatchers(PUBLIC_PAYMENT_WEBHOOK_ENDPOINTS)
                .permitAll()
                .requestMatchers("/ws/**")
                .permitAll()
                .requestMatchers(HttpMethod.OPTIONS, "/**")
                .permitAll()
                .anyRequest()
                .authenticated());

    http.exceptionHandling(
        ex ->
            ex.authenticationEntryPoint(
                    (req, res, exAuth) -> {
                      res.setStatus(HttpStatus.UNAUTHORIZED.value());
                      res.setContentType("application/json");
                      objectMapper.writeValue(
                          res.getOutputStream(),
                          ApiResponse.builder()
                              .code(ErrorCode.UNAUTHENTICATED.getCode())
                              .message(ErrorCode.UNAUTHENTICATED.getDefaultMessage())
                              .path(req.getRequestURI())
                              .timestamp(Instant.now())
                              .build());
                    })
                .accessDeniedHandler(
                    (req, res, exDenied) -> {
                      res.setStatus(HttpStatus.FORBIDDEN.value());
                      res.setContentType("application/json");
                      objectMapper.writeValue(
                          res.getOutputStream(),
                          ApiResponse.builder()
                              .code(ErrorCode.FORBIDDEN_ACTION.getCode())
                              .message(ErrorCode.FORBIDDEN_ACTION.getDefaultMessage())
                              .path(req.getRequestURI())
                              .timestamp(Instant.now())
                              .build());
                    }));

    http.oauth2ResourceServer(
        oauth2 ->
            oauth2.jwt(
                jwt ->
                    jwt.decoder(customJwtDecoder)
                        .jwtAuthenticationConverter(jwtAuthenticationConverter)));

    return http.build();
  }

  @Bean
  @Order(3)
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http.authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
    return http.build();
  }
}
