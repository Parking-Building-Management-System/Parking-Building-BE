package com.smartpark.swp391.modules.payment.service;

import java.util.Map;

public interface PayosWebhookService {
  void process(Map<String, Object> payload, String rawPayload);
}
