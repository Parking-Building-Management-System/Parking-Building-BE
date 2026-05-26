package com.smartpark.swp391.modules.manager.controller;

import com.smartpark.swp391.common.exception.ErrorCode;
import com.smartpark.swp391.common.response.ApiResponse;
import com.smartpark.swp391.modules.manager.dto.slot.SlotRequest;
import com.smartpark.swp391.modules.manager.dto.slot.SlotResponse;
import com.smartpark.swp391.modules.manager.dto.slot.SlotStatusRequest;
import com.smartpark.swp391.modules.manager.service.ManagerSlotService;
import com.smartpark.swp391.modules.manager.support.ManagerTenantContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/manager")
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@PreAuthorize("hasRole('PARKING_MANAGER')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Manager Slots", description = "PARKING_MANAGER slot management APIs")
public class ManagerSlotManagementController {

  ManagerSlotService managerSlotService;
  ManagerTenantContext managerTenantContext;

  @PostMapping("/zones/{zoneId}/slots")
  @Operation(summary = "Create slot", description = "Creates a slot under a tenant zone.")
  public ResponseEntity<ApiResponse<SlotResponse>> createSlot(
      @PathVariable UUID zoneId,
      @Valid @RequestBody SlotRequest request,
      @AuthenticationPrincipal Jwt jwt) {
    return ok(
        "/manager/zones/" + zoneId + "/slots",
        managerTenantContext.call(jwt, () -> managerSlotService.createSlot(zoneId, request)));
  }

  @GetMapping("/slots/{id}")
  @Operation(summary = "Get slot", description = "Gets one tenant slot by id.")
  public ResponseEntity<ApiResponse<SlotResponse>> getSlot(
      @PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
    return ok(
        "/manager/slots/" + id,
        managerTenantContext.call(jwt, () -> managerSlotService.getSlot(id)));
  }

  @PutMapping("/slots/{id}")
  @Operation(summary = "Update slot", description = "Updates one tenant slot by id.")
  public ResponseEntity<ApiResponse<SlotResponse>> updateSlot(
      @PathVariable UUID id,
      @Valid @RequestBody SlotRequest request,
      @AuthenticationPrincipal Jwt jwt) {
    return ok(
        "/manager/slots/" + id,
        managerTenantContext.call(jwt, () -> managerSlotService.updateSlot(id, request)));
  }

  @DeleteMapping("/slots/{id}")
  @Operation(summary = "Delete slot", description = "Soft-deletes one tenant slot by id.")
  public ResponseEntity<ApiResponse<Void>> deleteSlot(
      @PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
    managerTenantContext.run(jwt, () -> managerSlotService.deleteSlot(id));
    return ok("/manager/slots/" + id, null);
  }

  @PatchMapping("/slots/{id}/status")
  @Operation(summary = "Update slot status", description = "Updates one tenant slot status.")
  public ResponseEntity<ApiResponse<SlotResponse>> updateSlotStatus(
      @PathVariable UUID id,
      @Valid @RequestBody SlotStatusRequest request,
      @AuthenticationPrincipal Jwt jwt) {
    return ok(
        "/manager/slots/" + id + "/status",
        managerTenantContext.call(jwt, () -> managerSlotService.updateSlotStatus(id, request)));
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
