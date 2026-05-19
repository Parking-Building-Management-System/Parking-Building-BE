package com.smartpark.swp391.modules.dashboard.controller;

import com.smartpark.swp391.common.exception.ErrorCode;
import com.smartpark.swp391.common.response.ApiResponse;
import com.smartpark.swp391.modules.dashboard.dto.response.DashboardCountersResponse;
import com.smartpark.swp391.modules.dashboard.dto.response.TrafficChartResponse;
import com.smartpark.swp391.modules.dashboard.enumType.TrafficBucket;
import com.smartpark.swp391.modules.dashboard.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Instant;
import java.time.LocalDateTime;
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
@RequestMapping("/dashboard")
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@PreAuthorize("hasRole('SYSTEM_ADMIN')")
@Tag(name = "Dashboard", description = "Dashboard tổng cho System Admin")
@SecurityRequirement(name = "bearerAuth")
public class DashboardController {

  DashboardService dashboardService;

  @GetMapping("/counters")
  @Operation(summary = "Đếm số Tenant active và bãi xe active trên toàn hệ thống")
  public ResponseEntity<ApiResponse<DashboardCountersResponse>> getCounters() {
    return ResponseEntity.ok(
        ApiResponse.<DashboardCountersResponse>builder()
            .code(ErrorCode.SUCCESS.getCode())
            .message("Lấy thống kê counters thành công")
            .result(dashboardService.getCounters())
            .timestamp(Instant.now())
            .path("/dashboard/counters")
            .build());
  }

  @GetMapping("/traffic")
  @Operation(summary = "Lấy dữ liệu biểu đồ traffic request API theo thời gian")
  public ResponseEntity<ApiResponse<TrafficChartResponse>> getTraffic(
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
          LocalDateTime from,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
          LocalDateTime to,
      @RequestParam(defaultValue = "HOUR") TrafficBucket bucket) {
    return ResponseEntity.ok(
        ApiResponse.<TrafficChartResponse>builder()
            .code(ErrorCode.SUCCESS.getCode())
            .message("Lấy dữ liệu traffic thành công")
            .result(dashboardService.getTraffic(from, to, bucket))
            .timestamp(Instant.now())
            .path("/dashboard/traffic")
            .build());
  }
}
