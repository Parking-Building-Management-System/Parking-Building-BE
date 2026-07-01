package com.smartpark.swp391.modules.manager.controller;

import com.smartpark.swp391.common.exception.ErrorCode;
import com.smartpark.swp391.common.response.ApiResponse;
import com.smartpark.swp391.common.response.PageResponse;
import com.smartpark.swp391.modules.manager.dto.penalty.ManagerPenaltyRuleRequest;
import com.smartpark.swp391.modules.manager.dto.penalty.ManagerPenaltyRuleResponse;
import com.smartpark.swp391.modules.manager.dto.penalty.ManagerPenaltyRuleStatusRequest;
import com.smartpark.swp391.modules.manager.service.ManagerPenaltyRuleService;
import com.smartpark.swp391.modules.manager.support.ManagerTenantContext;
import com.smartpark.swp391.modules.penalty.enumType.PenaltyRuleStatus;
import com.smartpark.swp391.modules.penalty.enumType.PenaltyType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/manager/penalty-rules")
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@PreAuthorize("hasRole('PARKING_MANAGER')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Manager Penalty Rules", description = "PARKING_MANAGER fine configuration APIs")
public class ManagerPenaltyRuleController {

  ManagerPenaltyRuleService managerPenaltyRuleService;
  ManagerTenantContext managerTenantContext;

  @GetMapping
  @Operation(summary = "List penalty rules")
  public ResponseEntity<ApiResponse<PageResponse<ManagerPenaltyRuleResponse>>> getRules(
      @RequestParam(required = false) UUID parkingId,
      @RequestParam(required = false) PenaltyType type,
      @RequestParam(required = false) PenaltyRuleStatus status,
      @RequestParam(defaultValue = "0") @Min(0) int page,
      @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
      @AuthenticationPrincipal Jwt jwt) {
    return ok(
        "/manager/penalty-rules",
        managerTenantContext.call(
            jwt, () -> managerPenaltyRuleService.getRules(parkingId, type, status, page, size)));
  }

  @PostMapping
  @Operation(summary = "Create penalty rule")
  public ResponseEntity<ApiResponse<ManagerPenaltyRuleResponse>> createRule(
      @Valid @RequestBody ManagerPenaltyRuleRequest request, @AuthenticationPrincipal Jwt jwt) {
    return ok(
        "/manager/penalty-rules",
        managerTenantContext.call(jwt, () -> managerPenaltyRuleService.createRule(request)));
  }

  @GetMapping("/{id}")
  @Operation(summary = "Get penalty rule")
  public ResponseEntity<ApiResponse<ManagerPenaltyRuleResponse>> getRule(
      @PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
    return ok(
        "/manager/penalty-rules/" + id,
        managerTenantContext.call(jwt, () -> managerPenaltyRuleService.getRule(id)));
  }

  @PutMapping("/{id}")
  @Operation(summary = "Update penalty rule")
  public ResponseEntity<ApiResponse<ManagerPenaltyRuleResponse>> updateRule(
      @PathVariable UUID id,
      @Valid @RequestBody ManagerPenaltyRuleRequest request,
      @AuthenticationPrincipal Jwt jwt) {
    return ok(
        "/manager/penalty-rules/" + id,
        managerTenantContext.call(jwt, () -> managerPenaltyRuleService.updateRule(id, request)));
  }

  @PatchMapping("/{id}/status")
  @Operation(summary = "Update penalty rule status")
  public ResponseEntity<ApiResponse<ManagerPenaltyRuleResponse>> updateStatus(
      @PathVariable UUID id,
      @Valid @RequestBody ManagerPenaltyRuleStatusRequest request,
      @AuthenticationPrincipal Jwt jwt) {
    return ok(
        "/manager/penalty-rules/" + id + "/status",
        managerTenantContext.call(jwt, () -> managerPenaltyRuleService.updateStatus(id, request)));
  }

  @DeleteMapping("/{id}")
  @Operation(summary = "Soft-delete penalty rule")
  public ResponseEntity<ApiResponse<Void>> deleteRule(
      @PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
    managerTenantContext.run(jwt, () -> managerPenaltyRuleService.deleteRule(id));
    return ok("/manager/penalty-rules/" + id, null);
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
