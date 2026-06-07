package com.smartpark.swp391.modules.payment.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartpark.swp391.common.exception.ApiException;
import com.smartpark.swp391.common.exception.ErrorCode;
import com.smartpark.swp391.modules.payment.service.PayosWebhookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/payments/webhooks")
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@Tag(name = "Payment Webhooks", description = "PayOS provider webhook callback APIs")
public class PayosWebhookController {

  PayosWebhookService payosWebhookService;
  ObjectMapper objectMapper;

  @PostMapping("/payos")
  @Operation(
      summary = "Process PayOS webhook",
      description =
          "Actor: PayOS. Verifies webhook signature, logs every payload, updates the matching"
              + " payment intent to PAID when amount and status are valid, then marks the parking"
              + " session paid and sets the exit deadline. No bearer token is used; trust is the"
              + " PayOS checksum signature.")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Webhook processed, ignored, or accepted as idempotent"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Malformed payload, invalid signature, missing order code, or amount mismatch")
  })
  public ResponseEntity<Map<String, Object>> handlePayosWebhook(
      @RequestBody Map<String, Object> payload) {
    payosWebhookService.process(payload, rawJson(payload));
    return ResponseEntity.ok(Map.of("success", true));
  }

  private String rawJson(Map<String, Object> payload) {
    try {
      return objectMapper.writeValueAsString(payload);
    } catch (JsonProcessingException e) {
      throw new ApiException(ErrorCode.MALFORMED_JSON, "PAYOS_WEBHOOK_PAYLOAD_INVALID");
    }
  }
}
