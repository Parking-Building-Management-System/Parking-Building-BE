package com.smartpark.swp391.modules.settlement.controller;

import com.smartpark.swp391.common.exception.ErrorCode;
import com.smartpark.swp391.common.response.ApiResponse;
import com.smartpark.swp391.modules.settlement.dto.StaffCashSettlementPreviewResponse;
import com.smartpark.swp391.modules.settlement.dto.StaffCashShiftCloseRequest;
import com.smartpark.swp391.modules.settlement.dto.StaffCashShiftResponse;
import com.smartpark.swp391.modules.settlement.dto.StaffCurrentCashShiftResponse;
import com.smartpark.swp391.modules.settlement.service.StaffCashShiftService;
import com.smartpark.swp391.modules.staff.support.StaffTenantContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/staff/shifts")
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@PreAuthorize("hasRole('STAFF')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Staff Cash Shifts", description = "STAFF blind cash settlement APIs")
public class StaffCashShiftController {

  StaffCashShiftService staffCashShiftService;
  StaffTenantContext staffTenantContext;

  @PostMapping("/start")
  @Operation(summary = "Start a staff cash shift")
  public ResponseEntity<ApiResponse<StaffCashShiftResponse>> startShift(
      @AuthenticationPrincipal Jwt jwt) {
    return ok(
        "/staff/shifts/start", staffTenantContext.call(jwt, staffCashShiftService::startShift));
  }

  @GetMapping("/current")
  @Operation(summary = "Get current open staff cash shift")
  public ResponseEntity<ApiResponse<StaffCurrentCashShiftResponse>> getCurrentShift(
      @AuthenticationPrincipal Jwt jwt) {
    return ok(
        "/staff/shifts/current",
        staffTenantContext.call(jwt, staffCashShiftService::getCurrentShift));
  }

  @GetMapping("/current/settlement-preview")
  @Operation(summary = "Preview live settlement totals for current staff cash shift")
  public ResponseEntity<ApiResponse<StaffCashSettlementPreviewResponse>> getSettlementPreview(
      @AuthenticationPrincipal Jwt jwt) {
    return ok(
        "/staff/shifts/current/settlement-preview",
        staffTenantContext.call(jwt, staffCashShiftService::getCurrentSettlementPreview));
  }

  @PostMapping("/current/close")
  @Operation(summary = "Blind close current staff cash shift")
  public ResponseEntity<ApiResponse<StaffCashShiftResponse>> closeCurrentShift(
      @Valid @RequestBody StaffCashShiftCloseRequest request, @AuthenticationPrincipal Jwt jwt) {
    return ok(
        "/staff/shifts/current/close",
        staffTenantContext.call(jwt, () -> staffCashShiftService.closeCurrentShift(request)));
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
