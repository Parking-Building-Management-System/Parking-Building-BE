package com.smartpark.swp391.modules.manager.controller;

import com.smartpark.swp391.common.exception.ErrorCode;
import com.smartpark.swp391.common.response.ApiResponse;
import com.smartpark.swp391.common.response.PageResponse;
import com.smartpark.swp391.modules.manager.dto.slot.SlotBulkStatusRequest;
import com.smartpark.swp391.modules.manager.dto.slot.SlotBulkStatusResponse;
import com.smartpark.swp391.modules.manager.dto.slot.SlotImportResponse;
import com.smartpark.swp391.modules.manager.dto.slot.SlotResponse;
import com.smartpark.swp391.modules.manager.service.ManagerSlotService;
import com.smartpark.swp391.modules.manager.support.ManagerTenantContext;
import com.smartpark.swp391.modules.parking.enumType.SlotStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Validated
@RestController
@RequestMapping("/manager/slots")
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@PreAuthorize("hasRole('PARKING_MANAGER')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Manager Slots", description = "PARKING_MANAGER slot operations")
public class ManagerSlotController {

  private static final MediaType EXCEL_MEDIA_TYPE =
      MediaType.parseMediaType("application/vnd.ms-excel");

  ManagerSlotService managerSlotService;
  ManagerTenantContext managerTenantContext;

  @GetMapping
  @Operation(
      summary = "Search slots",
      description =
          "Returns tenant slots with pagination and strict filters by zoneId, status, and slotCode."
              + " Use exact=true for exact code match; otherwise slotCode is a LIKE search.")
  @io.swagger.v3.oas.annotations.responses.ApiResponse(
      responseCode = "200",
      description = "Slots loaded successfully",
      content = @Content(schema = @Schema(implementation = PageResponse.class)))
  public ResponseEntity<ApiResponse<PageResponse<SlotResponse>>> getSlots(
      @RequestParam(required = false) UUID zoneId,
      @RequestParam(required = false) SlotStatus status,
      @RequestParam(required = false) String slotCode,
      @RequestParam(defaultValue = "false") boolean exact,
      @RequestParam(defaultValue = "0") @Min(0) int page,
      @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
      @AuthenticationPrincipal Jwt jwt) {
    return ok(
        "/manager/slots",
        managerTenantContext.call(
            jwt, () -> managerSlotService.getSlots(zoneId, status, slotCode, exact, page, size)));
  }

  @PatchMapping("/bulk-status")
  @Operation(
      summary = "Bulk update slot status",
      description = "Updates tenant slots to AVAILABLE, MAINTENANCE, or LOCKED in one transaction.")
  public ResponseEntity<ApiResponse<SlotBulkStatusResponse>> bulkUpdateStatus(
      @Valid @RequestBody SlotBulkStatusRequest request, @AuthenticationPrincipal Jwt jwt) {
    return ok(
        "/manager/slots/bulk-status",
        managerTenantContext.call(jwt, () -> managerSlotService.bulkUpdateStatus(request)));
  }

  @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @Operation(
      summary = "Import slots",
      description =
          "Imports slots from Excel. Required headers are parkingCode, floorCode, zoneCode, and"
              + " slotCode. Optional headers are slotNumber and status.")
  public ResponseEntity<ApiResponse<SlotImportResponse>> importSlots(
      @RequestParam("file") MultipartFile file, @AuthenticationPrincipal Jwt jwt) {
    return ok(
        "/manager/slots/import",
        managerTenantContext.call(jwt, () -> managerSlotService.importSlots(file)));
  }

  @GetMapping(value = "/export", produces = "application/vnd.ms-excel")
  @Operation(summary = "Export slots", description = "Exports all tenant slots as an Excel file.")
  public ResponseEntity<byte[]> exportSlots(@AuthenticationPrincipal Jwt jwt) {
    byte[] body = managerTenantContext.call(jwt, managerSlotService::exportSlots);
    String filename = "smartpark-slots-" + LocalDate.now() + ".xlsx";

    return ResponseEntity.ok()
        .contentType(EXCEL_MEDIA_TYPE)
        .header(
            HttpHeaders.CONTENT_DISPOSITION,
            ContentDisposition.attachment().filename(filename).build().toString())
        .body(body);
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
