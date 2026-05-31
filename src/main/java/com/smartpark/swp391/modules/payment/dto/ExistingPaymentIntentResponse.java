package com.smartpark.swp391.modules.payment.dto;

import com.smartpark.swp391.modules.payment.enumType.PaymentIntentStatus;
import java.time.LocalDateTime;
import lombok.Builder;

@Builder
public record ExistingPaymentIntentResponse(
    Long orderCode, PaymentIntentStatus status, String checkoutUrl, LocalDateTime expiresAt) {}
