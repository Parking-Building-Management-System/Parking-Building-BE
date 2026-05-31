package com.smartpark.swp391.infrastructure.payment.payos;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Service;

@Service
public class PayosSignatureService {

  private final ObjectMapper objectMapper;

  public PayosSignatureService(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  public String signCreatePaymentLink(
      long amount,
      String cancelUrl,
      String description,
      long orderCode,
      String returnUrl,
      String checksumKey) {
    String data =
        "amount="
            + amount
            + "&cancelUrl="
            + cancelUrl
            + "&description="
            + description
            + "&orderCode="
            + orderCode
            + "&returnUrl="
            + returnUrl;
    return hmacSha256(data, checksumKey);
  }

  public boolean isValidWebhookData(
      Map<String, Object> data, String currentSignature, String checksumKey) {
    if (data == null || currentSignature == null || currentSignature.isBlank()) {
      return false;
    }
    String expected = hmacSha256(toQueryString(data), checksumKey);
    return constantTimeEquals(expected, currentSignature);
  }

  private String toQueryString(Map<String, Object> data) {
    return data.entrySet().stream()
        .filter(entry -> entry.getValue() != null)
        .sorted(Comparator.comparing(Map.Entry::getKey))
        .map(entry -> entry.getKey() + "=" + normalizeValue(entry.getValue()))
        .reduce((left, right) -> left + "&" + right)
        .orElse("");
  }

  private String normalizeValue(Object value) {
    if (value == null || "null".equals(value) || "undefined".equals(value)) {
      return "";
    }
    if (value instanceof List<?> list) {
      try {
        return objectMapper.writeValueAsString(list);
      } catch (JsonProcessingException e) {
        throw new PayosProviderException("PAYOS_SIGNATURE_SERIALIZATION_FAILED", e);
      }
    }
    return String.valueOf(value);
  }

  private String hmacSha256(String data, String key) {
    try {
      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
      byte[] bytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
      StringBuilder hex = new StringBuilder(bytes.length * 2);
      for (byte b : bytes) {
        hex.append(String.format("%02x", b));
      }
      return hex.toString();
    } catch (Exception e) {
      throw new PayosProviderException("PAYOS_SIGNATURE_FAILED", e);
    }
  }

  private boolean constantTimeEquals(String left, String right) {
    byte[] leftBytes = left.getBytes(StandardCharsets.UTF_8);
    byte[] rightBytes = right.getBytes(StandardCharsets.UTF_8);
    if (leftBytes.length != rightBytes.length) {
      return false;
    }
    int result = 0;
    for (int i = 0; i < leftBytes.length; i++) {
      result |= leftBytes[i] ^ rightBytes[i];
    }
    return result == 0;
  }
}
