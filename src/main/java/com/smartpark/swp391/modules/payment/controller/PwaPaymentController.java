package com.smartpark.swp391.modules.payment.controller;

import com.smartpark.swp391.common.exception.ErrorCode;
import com.smartpark.swp391.common.response.ApiResponse;
import com.smartpark.swp391.modules.payment.dto.PaymentIntentResponse;
import com.smartpark.swp391.modules.payment.dto.PaymentIntentStatusResponse;
import com.smartpark.swp391.modules.payment.service.PwaPaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/pwa")
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@Tag(name = "PWA Payments", description = "Public PWA payment intent APIs")
public class PwaPaymentController {

  PwaPaymentService pwaPaymentService;

  @PostMapping("/cards/{qrToken}/payment-intents")
  @Operation(summary = "Create PayOS payment intent for an active RFID card session")
  public ResponseEntity<ApiResponse<PaymentIntentResponse>> createPaymentIntent(
      @PathVariable String qrToken) {
    return ResponseEntity.ok(
        ApiResponse.<PaymentIntentResponse>builder()
            .code(ErrorCode.SUCCESS.getCode())
            .message(ErrorCode.SUCCESS.getDefaultMessage())
            .result(pwaPaymentService.createPaymentIntent(qrToken))
            .timestamp(Instant.now())
            .path("/pwa/cards/" + qrToken + "/payment-intents")
            .build());
  }

  @GetMapping("/payment-intents/{orderCode}")
  @Operation(summary = "Get public payment intent status")
  public ResponseEntity<ApiResponse<PaymentIntentStatusResponse>> getPaymentIntentStatus(
      @PathVariable Long orderCode) {
    return ResponseEntity.ok(
        ApiResponse.<PaymentIntentStatusResponse>builder()
            .code(ErrorCode.SUCCESS.getCode())
            .message(ErrorCode.SUCCESS.getDefaultMessage())
            .result(pwaPaymentService.getPaymentIntentStatus(orderCode))
            .timestamp(Instant.now())
            .path("/pwa/payment-intents/" + orderCode)
            .build());
  }
}
