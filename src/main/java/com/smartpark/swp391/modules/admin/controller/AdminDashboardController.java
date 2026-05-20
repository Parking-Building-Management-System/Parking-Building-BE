package com.smartpark.swp391.modules.admin.controller;

import com.smartpark.swp391.common.exception.ErrorCode;
import com.smartpark.swp391.common.response.ApiResponse;
import com.smartpark.swp391.modules.admin.dto.dashboard.AdminDashboardStatsResponse;
import com.smartpark.swp391.modules.admin.service.AdminDashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/dashboard")
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@PreAuthorize("hasRole('SYSTEM_ADMIN')")
@SecurityRequirement(name = "bearerAuth")
@Tag(
    name = "Admin Dashboard",
    description = "SYSTEM_ADMIN dashboard counters and traffic telemetry")
public class AdminDashboardController {

  AdminDashboardService adminDashboardService;

  @GetMapping("/stats")
  @Operation(
      summary = "Get global dashboard stats",
      description =
          "Returns active tenant count, total parking count, and API traffic telemetry aggregated"
              + " by day. The payload is cached in Redis for 10 minutes.")
  @io.swagger.v3.oas.annotations.responses.ApiResponse(
      responseCode = "200",
      description = "Dashboard stats loaded successfully",
      content = @Content(schema = @Schema(implementation = AdminDashboardStatsResponse.class)))
  public ResponseEntity<ApiResponse<AdminDashboardStatsResponse>> getStats() {
    return ResponseEntity.ok(
        ApiResponse.<AdminDashboardStatsResponse>builder()
            .code(ErrorCode.SUCCESS.getCode())
            .message(ErrorCode.SUCCESS.getDefaultMessage())
            .result(adminDashboardService.getStats())
            .timestamp(Instant.now())
            .path("/admin/dashboard/stats")
            .build());
  }
}
