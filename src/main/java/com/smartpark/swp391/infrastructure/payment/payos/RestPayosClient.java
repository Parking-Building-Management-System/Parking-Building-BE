package com.smartpark.swp391.infrastructure.payment.payos;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Component
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class RestPayosClient implements PayosClient {

  private static final String CREATE_PAYMENT_URL =
      "https://api-merchant.payos.vn/v2/payment-requests";

  PayosProperties properties;
  PayosSignatureService signatureService;
  RestTemplate restTemplate;
  ObjectMapper objectMapper;

  @Override
  public PayosCreatePaymentLinkResponse createPaymentLink(PayosCreatePaymentLinkRequest request) {
    properties.requireReadyForPaymentCreation();

    Map<String, Object> body = new LinkedHashMap<>();
    body.put("orderCode", request.orderCode());
    body.put("amount", request.amount());
    body.put("description", request.description());
    body.put("items", request.items());
    body.put("cancelUrl", request.cancelUrl());
    body.put("returnUrl", request.returnUrl());
    if (request.expiredAt() != null) {
      body.put("expiredAt", request.expiredAt());
    }
    body.put(
        "signature",
        signatureService.signCreatePaymentLink(
            request.amount(),
            request.cancelUrl(),
            request.description(),
            request.orderCode(),
            request.returnUrl(),
            properties.checksumKey()));

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.set("x-client-id", properties.clientId());
    headers.set("x-api-key", properties.apiKey());

    try {
      String raw =
          restTemplate.postForObject(
              CREATE_PAYMENT_URL, new HttpEntity<>(body, headers), String.class);
      JsonNode root = objectMapper.readTree(raw);
      if (!"00".equals(root.path("code").asText())) {
        throw new PayosProviderException("PAYOS_CREATE_PAYMENT_FAILED");
      }
      JsonNode data = root.path("data");
      return new PayosCreatePaymentLinkResponse(
          textOrNull(data, "paymentLinkId"),
          textOrNull(data, "checkoutUrl"),
          textOrNull(data, "qrCode"),
          raw);
    } catch (RestClientException | JsonProcessingException e) {
      throw new PayosProviderException("PAYOS_CREATE_PAYMENT_FAILED", e);
    }
  }

  private String textOrNull(JsonNode node, String field) {
    JsonNode value = node.path(field);
    return value.isMissingNode() || value.isNull() ? null : value.asText();
  }
}
