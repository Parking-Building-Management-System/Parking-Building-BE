package com.smartpark.swp391.modules.pwa.controller;

import com.smartpark.swp391.common.exception.ErrorCode;
import com.smartpark.swp391.common.response.ApiResponse;
import com.smartpark.swp391.modules.pwa.dto.CardActiveSessionResponse;
import com.smartpark.swp391.modules.pwa.dto.CardCheckoutQuoteResponse;
import com.smartpark.swp391.modules.pwa.service.PwaCardSessionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/pwa/cards")
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@Tag(
    name = "PWA Driver",
    description = "Public RFID-card QR APIs for driver active-session map and checkout quote")
public class PwaCardSessionController {

  PwaCardSessionService pwaCardSessionService;

  @GetMapping("/{qrToken}/active-session")
  @Operation(
      summary = "Get active session guide",
      description =
          "Actor: Driver/PWA. Resolves a QR token printed on the RFID card to the current ACTIVE"
              + " parking session, floor map image, and assigned slot pin. Read-only and public by"
              + " design; the QR token is the access handle.")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Active session guide loaded"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid or inactive card token"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "No active parking session for this card")
  })
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
  @Operation(
      summary = "Get checkout quote",
      description =
          "Actor: Driver/PWA. Resolves the active card session and calculates duration, pricing"
              + " breakdown, amount due, payment status, and next action. Read-only.")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Checkout quote loaded"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid or inactive card token"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "No active session or pricing rule configured")
  })
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
}
