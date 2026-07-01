package com.smartpark.swp391.modules.pwa.controller;

import com.smartpark.swp391.common.exception.ErrorCode;
import com.smartpark.swp391.common.response.ApiResponse;
import com.smartpark.swp391.modules.pwa.dto.CardActiveSessionResponse;
import com.smartpark.swp391.modules.pwa.dto.CardCheckoutQuoteResponse;
import com.smartpark.swp391.modules.pwa.dto.report.OccupiedSlotReportRequest;
import com.smartpark.swp391.modules.pwa.dto.report.OccupiedSlotReportResponse;
import com.smartpark.swp391.modules.pwa.dto.report.PwaReportUploadRequest;
import com.smartpark.swp391.modules.pwa.dto.report.PwaReportUploadResponse;
import com.smartpark.swp391.modules.pwa.service.PwaCardSessionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/pwa/cards")
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@Tag(name = "PWA Card Session", description = "Public card QR active session guide APIs")
public class PwaCardSessionController {

  PwaCardSessionService pwaCardSessionService;

  @GetMapping("/{qrToken}/active-session")
  @Operation(summary = "Resolve active parking session by RFID card QR token")
  public ResponseEntity<ApiResponse<CardActiveSessionResponse>> getActiveSession(
      @PathVariable String qrToken) {
    return ResponseEntity.ok(
        ApiResponse.<CardActiveSessionResponse>builder()
            .code(ErrorCode.SUCCESS.getCode())
            .message(ErrorCode.SUCCESS.getDefaultMessage())
            .result(pwaCardSessionService.getActiveSession(qrToken))
            .timestamp(Instant.now())
            .path("/pwa/cards/" + qrToken + "/active-session")
            .build());
  }

  @GetMapping("/{qrToken}/checkout-quote")
  @Operation(summary = "Resolve checkout quote by RFID card QR token")
  public ResponseEntity<ApiResponse<CardCheckoutQuoteResponse>> getCheckoutQuote(
      @PathVariable String qrToken) {
    return ResponseEntity.ok(
        ApiResponse.<CardCheckoutQuoteResponse>builder()
            .code(ErrorCode.SUCCESS.getCode())
            .message(ErrorCode.SUCCESS.getDefaultMessage())
            .result(pwaCardSessionService.getCheckoutQuote(qrToken))
            .timestamp(Instant.now())
            .path("/pwa/cards/" + qrToken + "/checkout-quote")
            .build());
  }

  @PostMapping("/{qrToken}/reports/upload-url")
  @Operation(summary = "Create presigned upload URL for a PWA report photo")
  public ResponseEntity<ApiResponse<PwaReportUploadResponse>> createReportUpload(
      @PathVariable String qrToken, @Valid @RequestBody PwaReportUploadRequest request) {
    return ResponseEntity.ok(
        ApiResponse.<PwaReportUploadResponse>builder()
            .code(ErrorCode.SUCCESS.getCode())
            .message(ErrorCode.SUCCESS.getDefaultMessage())
            .result(pwaCardSessionService.createReportUpload(qrToken, request))
            .timestamp(Instant.now())
            .path("/pwa/cards/" + qrToken + "/reports/upload-url")
            .build());
  }

  @PostMapping("/{qrToken}/reports/occupied-slot")
  @Operation(summary = "Report that the assigned slot is occupied by another vehicle")
  public ResponseEntity<ApiResponse<OccupiedSlotReportResponse>> reportOccupiedSlot(
      @PathVariable String qrToken, @Valid @RequestBody OccupiedSlotReportRequest request) {
    return ResponseEntity.ok(
        ApiResponse.<OccupiedSlotReportResponse>builder()
            .code(ErrorCode.SUCCESS.getCode())
            .message(ErrorCode.SUCCESS.getDefaultMessage())
            .result(pwaCardSessionService.reportOccupiedSlot(qrToken, request))
            .timestamp(Instant.now())
            .path("/pwa/cards/" + qrToken + "/reports/occupied-slot")
            .build());
  }
}
