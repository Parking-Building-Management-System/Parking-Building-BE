package com.smartpark.swp391.modules.payment.service;

import com.smartpark.swp391.modules.payment.dto.ExistingPaymentIntentResponse;
import com.smartpark.swp391.modules.payment.dto.PaymentIntentResponse;
import com.smartpark.swp391.modules.payment.dto.PaymentIntentStatusResponse;
import com.smartpark.swp391.modules.payment.entity.PaymentIntent;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

public interface PwaPaymentService {
  PaymentIntentResponse createPaymentIntent(String qrToken);

  PaymentIntentStatusResponse getPaymentIntentStatus(Long orderCode);

  Optional<ExistingPaymentIntentResponse> findReusablePendingIntent(
      UUID sessionId, BigDecimal amount);

  boolean paymentProviderAvailable();

  PaymentIntentResponse toResponse(PaymentIntent intent);
}
