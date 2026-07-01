package com.smartpark.swp391.modules.settlement.controller;

import com.smartpark.swp391.common.exception.ErrorCode;
import com.smartpark.swp391.common.response.ApiResponse;
import com.smartpark.swp391.common.response.PageResponse;
import com.smartpark.swp391.modules.manager.support.ManagerTenantContext;
import com.smartpark.swp391.modules.settlement.dto.ManagerShiftSettlementDetailResponse;
import com.smartpark.swp391.modules.settlement.dto.ManagerShiftSettlementListItemResponse;
import com.smartpark.swp391.modules.settlement.enumType.StaffCashShiftStatus;
import com.smartpark.swp391.modules.settlement.service.ManagerShiftSettlementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.Instant;
import java.time.LocalDateTime;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/manager/shifts/settlements")
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@PreAuthorize("hasRole('PARKING_MANAGER')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Manager Shift Settlements", description = "PARKING_MANAGER cash settlement APIs")
public class ManagerShiftSettlementController {

  ManagerShiftSettlementService managerShiftSettlementService;
  ManagerTenantContext managerTenantContext;

  @GetMapping
  @Operation(summary = "List staff shift settlements")
  public ResponseEntity<ApiResponse<PageResponse<ManagerShiftSettlementListItemResponse>>>
      getSettlements(
          @RequestParam(required = false) UUID parkingId,
          @RequestParam(required = false) UUID staffId,
          @RequestParam(required = false) StaffCashShiftStatus status,
          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
              LocalDateTime from,
          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
              LocalDateTime to,
          @RequestParam(defaultValue = "0") @Min(0) int page,
          @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
          @AuthenticationPrincipal Jwt jwt) {
    return ok(
        "/manager/shifts/settlements",
        managerTenantContext.call(
            jwt,
            () ->
                managerShiftSettlementService.getSettlements(
                    parkingId, staffId, status, from, to, page, size)));
  }

  @GetMapping("/{id}")
  @Operation(summary = "Get staff shift settlement detail")
  public ResponseEntity<ApiResponse<ManagerShiftSettlementDetailResponse>> getSettlement(
      @PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
    return ok(
        "/manager/shifts/settlements/" + id,
        managerTenantContext.call(jwt, () -> managerShiftSettlementService.getSettlement(id)));
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
