package com.smartpark.swp391.modules.payment.dto;

import com.smartpark.swp391.modules.payment.enumType.PaymentIntentStatus;
import com.smartpark.swp391.modules.payment.enumType.PaymentProvider;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Builder;

@Builder
public record PaymentIntentResponse(
    UUID paymentIntentId,
    Long orderCode,
    BigDecimal amount,
    String currency,
    PaymentIntentStatus status,
    PaymentProvider provider,
    String checkoutUrl,
    String qrCode,
    LocalDateTime expiresAt,
    String description,
    LocalDateTime paidAt,
    LocalDateTime exitDeadline) {}
