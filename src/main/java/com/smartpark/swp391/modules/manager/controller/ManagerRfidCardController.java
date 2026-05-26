package com.smartpark.swp391.modules.manager.controller;

import com.smartpark.swp391.common.exception.ErrorCode;
import com.smartpark.swp391.common.response.ApiResponse;
import com.smartpark.swp391.common.response.PageResponse;
import com.smartpark.swp391.modules.manager.dto.rfid.RfidCardGenerateRequest;
import com.smartpark.swp391.modules.manager.dto.rfid.RfidCardGenerateResponse;
import com.smartpark.swp391.modules.manager.dto.rfid.RfidCardResponse;
import com.smartpark.swp391.modules.manager.dto.rfid.RfidCardStatusRequest;
import com.smartpark.swp391.modules.manager.service.ManagerRfidCardService;
import com.smartpark.swp391.modules.manager.support.ManagerTenantContext;
import com.smartpark.swp391.modules.parking.enumType.RfidCardStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/manager/rfid-cards")
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@PreAuthorize("hasRole('PARKING_MANAGER')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Manager RFID Cards", description = "PARKING_MANAGER RFID card pool APIs")
public class ManagerRfidCardController {

  ManagerRfidCardService managerRfidCardService;
  ManagerTenantContext managerTenantContext;

  @GetMapping
  @Operation(
      summary = "List RFID cards",
      description = "Lists current tenant RFID cards with optional status filter.",
      responses =
          @io.swagger.v3.oas.annotations.responses.ApiResponse(
              responseCode = "200",
              content = @Content(schema = @Schema(implementation = PageResponse.class))))
  public ResponseEntity<ApiResponse<PageResponse<RfidCardResponse>>> getCards(
      @RequestParam(required = false) RfidCardStatus status,
      @RequestParam(defaultValue = "0") @Min(0) int page,
      @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
      @AuthenticationPrincipal Jwt jwt) {
    return ok(
        "/manager/rfid-cards",
        managerTenantContext.call(
            jwt, () -> managerRfidCardService.getCards(status, page, size)));
  }

  @PostMapping("/generate")
  @Operation(
      summary = "Generate RFID card pool",
      description = "Generates idempotent ACTIVE RFID cards for the current tenant.")
  public ResponseEntity<ApiResponse<RfidCardGenerateResponse>> generateCards(
      @Valid @RequestBody(required = false) RfidCardGenerateRequest request,
      @AuthenticationPrincipal Jwt jwt) {
    return ok(
        "/manager/rfid-cards/generate",
        managerTenantContext.call(jwt, () -> managerRfidCardService.generateCards(request)));
  }

  @PatchMapping("/{id}/status")
  @Operation(summary = "Update RFID status", description = "Updates one tenant RFID card status.")
  public ResponseEntity<ApiResponse<RfidCardResponse>> updateStatus(
      @PathVariable UUID id,
      @Valid @RequestBody RfidCardStatusRequest request,
      @AuthenticationPrincipal Jwt jwt) {
    return ok(
        "/manager/rfid-cards/" + id + "/status",
        managerTenantContext.call(jwt, () -> managerRfidCardService.updateStatus(id, request)));
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
