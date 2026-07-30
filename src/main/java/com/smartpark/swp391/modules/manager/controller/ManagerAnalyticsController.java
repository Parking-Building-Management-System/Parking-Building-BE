package com.smartpark.swp391.modules.manager.controller;

import com.smartpark.swp391.common.exception.ErrorCode;
import com.smartpark.swp391.common.response.ApiResponse;
import com.smartpark.swp391.modules.manager.dto.analytics.ManagerAnalyticsOverviewResponse;
import com.smartpark.swp391.modules.manager.dto.analytics.ManagerAnalyticsQuery;
import com.smartpark.swp391.modules.manager.enumType.ManagerAnalyticsComparison;
import com.smartpark.swp391.modules.manager.enumType.ManagerAnalyticsPeriodType;
import com.smartpark.swp391.modules.manager.service.ManagerAnalyticsService;
import com.smartpark.swp391.modules.manager.support.ManagerTenantContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/manager/analytics")
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@PreAuthorize("hasRole('PARKING_MANAGER')")
@SecurityRequirement(name = "bearerAuth")
@Tag(
    name = "Manager Analytics",
    description = "Tenant-scoped operational traffic, revenue, and occupancy reporting")
public class ManagerAnalyticsController {

  ManagerAnalyticsService managerAnalyticsService;
  ManagerTenantContext managerTenantContext;

  @GetMapping("/overview")
  @Operation(
      summary = "Get Manager operational analytics",
      description =
          "Returns tenant- and parking-scoped traffic, canonical revenue, current occupancy,"
              + " approximate historical occupancy, trends, vehicle-type splits, and local peak"
              + " hours. Period boundaries are half-open and use Asia/Ho_Chi_Minh.",
      responses =
          @io.swagger.v3.oas.annotations.responses.ApiResponse(
              responseCode = "200",
              content =
                  @Content(
                      schema = @Schema(implementation = ManagerAnalyticsOverviewResponse.class))))
  public ResponseEntity<ApiResponse<ManagerAnalyticsOverviewResponse>> overview(
      @RequestParam UUID parkingId,
      @RequestParam(defaultValue = "MONTH") ManagerAnalyticsPeriodType periodType,
      @RequestParam(required = false)
          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          @Parameter(description = "Selected date for DAY or anchor date for ISO WEEK")
          LocalDate date,
      @RequestParam(required = false) Integer year,
      @RequestParam(required = false) Integer month,
      @RequestParam(required = false) Integer quarter,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
      @RequestParam(defaultValue = "SAME_PERIOD_LAST_YEAR") ManagerAnalyticsComparison comparison,
      @RequestParam(required = false) UUID vehicleTypeId,
      @AuthenticationPrincipal Jwt jwt) {
    ManagerAnalyticsQuery query =
        new ManagerAnalyticsQuery(
            parkingId, periodType, date, year, month, quarter, from, to, comparison, vehicleTypeId);
    ManagerAnalyticsOverviewResponse result =
        managerTenantContext.call(jwt, () -> managerAnalyticsService.getOverview(query));
    return ResponseEntity.ok(
        ApiResponse.<ManagerAnalyticsOverviewResponse>builder()
            .code(ErrorCode.SUCCESS.getCode())
            .message(ErrorCode.SUCCESS.getDefaultMessage())
            .result(result)
            .timestamp(Instant.now())
            .path("/manager/analytics/overview")
            .build());
  }
}
