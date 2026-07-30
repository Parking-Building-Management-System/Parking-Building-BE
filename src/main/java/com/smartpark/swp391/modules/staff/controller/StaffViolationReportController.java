package com.smartpark.swp391.modules.staff.controller;

import com.smartpark.swp391.common.exception.ErrorCode;
import com.smartpark.swp391.common.response.ApiResponse;
import com.smartpark.swp391.modules.penalty.enumType.PenaltyCaseStatus;
import com.smartpark.swp391.modules.staff.dto.violation.PendingViolationReportCountResponse;
import com.smartpark.swp391.modules.staff.dto.violation.ViolationReportApproveRequest;
import com.smartpark.swp391.modules.staff.dto.violation.ViolationReportRejectRequest;
import com.smartpark.swp391.modules.staff.dto.violation.ViolationReportResponse;
import com.smartpark.swp391.modules.staff.service.StaffViolationReportService;
import com.smartpark.swp391.modules.staff.support.StaffTenantContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/staff/violation-reports")
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@PreAuthorize("hasRole('STAFF')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Staff Violation Reports", description = "STAFF occupied-slot report review APIs")
public class StaffViolationReportController {

  StaffViolationReportService staffViolationReportService;
  StaffTenantContext staffTenantContext;

  @GetMapping("/pending-count")
  @Operation(summary = "Count occupied-slot reports awaiting review in the current kiosk parking")
  public ResponseEntity<ApiResponse<PendingViolationReportCountResponse>> getPendingCount(
      @AuthenticationPrincipal Jwt jwt) {
    return ok(
        "/staff/violation-reports/pending-count",
        staffTenantContext.call(jwt, staffViolationReportService::getPendingCount));
  }

  @GetMapping
  @Operation(summary = "List occupied-slot reports in the current kiosk parking")
  public ResponseEntity<ApiResponse<List<ViolationReportResponse>>> getReports(
      @RequestParam(required = false) PenaltyCaseStatus status,
      @RequestParam(required = false) String reportedPlate,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
          LocalDateTime from,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
          LocalDateTime to,
      @AuthenticationPrincipal Jwt jwt) {
    return ok(
        "/staff/violation-reports",
        staffTenantContext.call(
            jwt, () -> staffViolationReportService.getReports(status, reportedPlate, from, to)));
  }

  @GetMapping("/{id}")
  @Operation(summary = "Get occupied-slot report review detail")
  public ResponseEntity<ApiResponse<ViolationReportResponse>> getReport(
      @PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
    return ok(
        "/staff/violation-reports/" + id,
        staffTenantContext.call(jwt, () -> staffViolationReportService.getReport(id)));
  }

  @PostMapping("/{id}/approve")
  @Operation(summary = "Approve an occupied-slot report and apply its configured penalty")
  public ResponseEntity<ApiResponse<ViolationReportResponse>> approve(
      @PathVariable UUID id,
      @Valid @RequestBody ViolationReportApproveRequest request,
      @AuthenticationPrincipal Jwt jwt) {
    return ok(
        "/staff/violation-reports/" + id + "/approve",
        staffTenantContext.call(jwt, () -> staffViolationReportService.approve(id, request)));
  }

  @PostMapping("/{id}/reject")
  @Operation(summary = "Reject an occupied-slot report without applying a penalty")
  public ResponseEntity<ApiResponse<ViolationReportResponse>> reject(
      @PathVariable UUID id,
      @Valid @RequestBody ViolationReportRejectRequest request,
      @AuthenticationPrincipal Jwt jwt) {
    return ok(
        "/staff/violation-reports/" + id + "/reject",
        staffTenantContext.call(jwt, () -> staffViolationReportService.reject(id, request)));
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
