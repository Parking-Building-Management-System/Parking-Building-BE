package com.smartpark.swp391.modules.payment.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartpark.swp391.common.exception.ApiException;
import com.smartpark.swp391.common.exception.ErrorCode;
import com.smartpark.swp391.modules.payment.service.PayosWebhookService;
import io.swagger.v3.oas.annotations.Operation;
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
@Tag(name = "Payment Webhooks", description = "Payment provider webhook callbacks")
public class PayosWebhookController {

  PayosWebhookService payosWebhookService;
  ObjectMapper objectMapper;

  @PostMapping("/payos")
  @Operation(summary = "Handle PayOS payment webhook")
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
