package com.smartpark.swp391.modules.admin.controller;

import com.smartpark.swp391.common.exception.ErrorCode;
import com.smartpark.swp391.common.response.ApiResponse;
import com.smartpark.swp391.modules.admin.dto.health.ServiceHealthResponse;
import com.smartpark.swp391.modules.admin.dto.health.SystemErrorResponse;
import com.smartpark.swp391.modules.admin.dto.health.SystemHealthSummaryResponse;
import com.smartpark.swp391.modules.admin.dto.health.TopEndpointResponse;
import com.smartpark.swp391.modules.admin.dto.health.TrafficPointResponse;
import com.smartpark.swp391.modules.admin.service.AdminSystemHealthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Instant;
import java.util.List;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/system-health")
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@PreAuthorize("hasRole('SYSTEM_ADMIN')")
@SecurityRequirement(name = "bearerAuth")
@Tag(
    name = "System Admin Health",
    description = "SYSTEM_ADMIN platform health, traffic, endpoint, and error monitoring APIs")
public class AdminSystemHealthController {

  AdminSystemHealthService adminSystemHealthService;

  @GetMapping("/summary")
  @Operation(
      summary = "Get health summary",
      description =
          "Actor: SYSTEM_ADMIN. Returns aggregate API traffic, error rate, and service health"
              + " indicators for the dashboard. Read-only.")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Health summary loaded"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthenticated"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "SYSTEM_ADMIN role required")
  })
  public ResponseEntity<ApiResponse<SystemHealthSummaryResponse>> getSummary() {
    return ok("/admin/system-health/summary", adminSystemHealthService.getSummary());
  }

  @GetMapping("/services")
  @Operation(
      summary = "Get service health list",
      description =
          "Actor: SYSTEM_ADMIN. Checks backend dependencies such as database, Redis, and storage"
              + " readiness where available. Read-only.")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Service health loaded"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthenticated"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "SYSTEM_ADMIN role required")
  })
  public ResponseEntity<ApiResponse<List<ServiceHealthResponse>>> getServices() {
    return ok("/admin/system-health/services", adminSystemHealthService.getServices());
  }

  @GetMapping("/traffic")
  @Operation(
      summary = "Get API traffic timeline",
      description =
          "Actor: SYSTEM_ADMIN. Aggregates api_traffic_logs by time range and granularity"
              + " for dashboard charts. Read-only.")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Traffic timeline loaded"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid date range or granularity"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthenticated"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "SYSTEM_ADMIN role required")
  })
  public ResponseEntity<ApiResponse<List<TrafficPointResponse>>> getTraffic(
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
          Instant from,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
          Instant to,
      @RequestParam(defaultValue = "HOUR") String granularity) {
    return ok(
        "/admin/system-health/traffic",
        adminSystemHealthService.getTraffic(from, to, granularity));
  }

  @GetMapping("/top-endpoints")
  @Operation(
      summary = "Get top endpoints",
      description =
          "Actor: SYSTEM_ADMIN. Returns the busiest endpoints in the selected time window from"
              + " api_traffic_logs. Read-only.")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Top endpoints loaded"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid date range or limit"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthenticated"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "SYSTEM_ADMIN role required")
  })
  public ResponseEntity<ApiResponse<List<TopEndpointResponse>>> getTopEndpoints(
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
          Instant from,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
          Instant to,
      @RequestParam(defaultValue = "10") int limit) {
    return ok(
        "/admin/system-health/top-endpoints",
        adminSystemHealthService.getTopEndpoints(from, to, limit));
  }

  @GetMapping("/errors")
  @Operation(
      summary = "Get recent API errors",
      description =
          "Actor: SYSTEM_ADMIN. Lists error responses captured from api_traffic_logs in the"
              + " selected time window. Read-only.")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Errors loaded"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid date range"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthenticated"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "SYSTEM_ADMIN role required")
  })
  public ResponseEntity<ApiResponse<List<SystemErrorResponse>>> getErrors(
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
          Instant from,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
          Instant to) {
    return ok("/admin/system-health/errors", adminSystemHealthService.getErrors(from, to));
  }

  private <T> ResponseEntity<ApiResponse<T>> ok(String path, T result) {
    return ResponseEntity.ok(
        ApiResponse.<T>builder()
            .code(ErrorCode.SUCCESS.getCode())
            .message(ErrorCode.SUCCESS.getDefaultMessage())
            .result(result)
            .timestamp(Instant.now())
            .path(path)
            .build());
  }
}
