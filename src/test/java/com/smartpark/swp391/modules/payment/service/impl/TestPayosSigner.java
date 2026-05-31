package com.smartpark.swp391.modules.payment.service.impl;

import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

final class TestPayosSigner {

  private TestPayosSigner() {}

  static String sign(Map<String, Object> data, String key) {
    String query =
        data.entrySet().stream()
            .filter(entry -> entry.getValue() != null)
            .sorted(Comparator.comparing(Map.Entry::getKey))
            .map(entry -> entry.getKey() + "=" + entry.getValue())
            .reduce((left, right) -> left + "&" + right)
            .orElse("");
    try {
      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
      byte[] bytes = mac.doFinal(query.getBytes(StandardCharsets.UTF_8));
      StringBuilder hex = new StringBuilder(bytes.length * 2);
      for (byte b : bytes) {
        hex.append(String.format("%02x", b));
      }
      return hex.toString();
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }
}
