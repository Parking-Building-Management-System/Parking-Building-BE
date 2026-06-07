package com.smartpark.swp391.modules.manager.controller;

import com.smartpark.swp391.common.exception.ErrorCode;
import com.smartpark.swp391.common.response.ApiResponse;
import com.smartpark.swp391.common.response.PageResponse;
import com.smartpark.swp391.modules.manager.dto.pricing.ManagerPricingRulePreviewRequest;
import com.smartpark.swp391.modules.manager.dto.pricing.ManagerPricingRuleRequest;
import com.smartpark.swp391.modules.manager.dto.pricing.ManagerPricingRuleResponse;
import com.smartpark.swp391.modules.manager.dto.pricing.ManagerPricingRuleStatusRequest;
import com.smartpark.swp391.modules.manager.service.ManagerPricingRuleService;
import com.smartpark.swp391.modules.manager.support.ManagerTenantContext;
import com.smartpark.swp391.modules.pricing.dto.PricingQuoteResponse;
import com.smartpark.swp391.modules.pricing.enumType.PricingRuleStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
@RequestMapping("/manager/pricing/rules")
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@PreAuthorize("hasRole('PARKING_MANAGER')")
@SecurityRequirement(name = "bearerAuth")
@Tag(
    name = "Manager Pricing",
    description = "PARKING_MANAGER pricing rule configuration and quote preview APIs")
public class ManagerPricingRuleController {

  ManagerPricingRuleService managerPricingRuleService;
  ManagerTenantContext managerTenantContext;

  @GetMapping
  @Operation(summary = "List pricing rules")
  public ResponseEntity<ApiResponse<PageResponse<ManagerPricingRuleResponse>>> getRules(
      @RequestParam(required = false) UUID parkingId,
      @RequestParam(required = false) UUID vehicleTypeId,
      @RequestParam(required = false) PricingRuleStatus status,
      @RequestParam(defaultValue = "0") @Min(0) int page,
      @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
      @AuthenticationPrincipal Jwt jwt) {
    return ok(
        "/manager/pricing/rules",
        managerTenantContext.call(
            jwt,
            () ->
                managerPricingRuleService.getRules(parkingId, vehicleTypeId, status, page, size)));
  }

  @PostMapping
  @Operation(
      summary = "Create pricing rule",
      description =
          "Actor: PARKING_MANAGER. Creates a tenant pricing rule for a vehicle type and optional"
              + " parking scope. The rule controls checkout quote, PayOS amount, and exit grace"
              + " period.")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Pricing rule created"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid price blocks, duplicate active rule, or inactive vehicle type"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthenticated"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "PARKING_MANAGER role required"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Parking or vehicle type not found")
  })
  public ResponseEntity<ApiResponse<ManagerPricingRuleResponse>> createRule(
      @Valid @RequestBody ManagerPricingRuleRequest request, @AuthenticationPrincipal Jwt jwt) {
    return ok(
        "/manager/pricing/rules",
        managerTenantContext.call(jwt, () -> managerPricingRuleService.createRule(request)));
  }

  @GetMapping("/{id}")
  @Operation(summary = "Get pricing rule")
  public ResponseEntity<ApiResponse<ManagerPricingRuleResponse>> getRule(
      @PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
    return ok(
        "/manager/pricing/rules/" + id,
        managerTenantContext.call(jwt, () -> managerPricingRuleService.getRule(id)));
  }

  @PutMapping("/{id}")
  @Operation(summary = "Update pricing rule")
  public ResponseEntity<ApiResponse<ManagerPricingRuleResponse>> updateRule(
      @PathVariable UUID id,
      @Valid @RequestBody ManagerPricingRuleRequest request,
      @AuthenticationPrincipal Jwt jwt) {
    return ok(
        "/manager/pricing/rules/" + id,
        managerTenantContext.call(jwt, () -> managerPricingRuleService.updateRule(id, request)));
  }

  @PatchMapping("/{id}/status")
  @Operation(summary = "Update pricing rule status")
  public ResponseEntity<ApiResponse<ManagerPricingRuleResponse>> updateStatus(
      @PathVariable UUID id,
      @Valid @RequestBody ManagerPricingRuleStatusRequest request,
      @AuthenticationPrincipal Jwt jwt) {
    return ok(
        "/manager/pricing/rules/" + id + "/status",
        managerTenantContext.call(jwt, () -> managerPricingRuleService.updateStatus(id, request)));
  }

  @DeleteMapping("/{id}")
  @Operation(summary = "Soft-delete pricing rule")
  public ResponseEntity<ApiResponse<Void>> deleteRule(
      @PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
    managerTenantContext.run(jwt, () -> managerPricingRuleService.deleteRule(id));
    return ok("/manager/pricing/rules/" + id, null);
  }

  @PostMapping("/{id}/preview")
  @Operation(
      summary = "Preview pricing quote",
      description =
          "Actor: PARKING_MANAGER. Calculates a quote from the selected pricing rule and sample"
              + " check-in/check-out times without modifying sessions or payment data.")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Pricing preview calculated"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid time range or inactive pricing rule"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthenticated"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "PARKING_MANAGER role required"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Pricing rule not found")
  })
  public ResponseEntity<ApiResponse<PricingQuoteResponse>> preview(
      @PathVariable UUID id,
      @Valid @RequestBody ManagerPricingRulePreviewRequest request,
      @AuthenticationPrincipal Jwt jwt) {
    return ok(
        "/manager/pricing/rules/" + id + "/preview",
        managerTenantContext.call(jwt, () -> managerPricingRuleService.preview(id, request)));
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
