package com.smartpark.swp391.modules.payment.dto;

import com.smartpark.swp391.modules.payment.enumType.PaymentIntentStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Builder;

@Builder
public record PaymentIntentStatusResponse(
    Long orderCode,
    PaymentIntentStatus status,
    BigDecimal amount,
    String currency,
    LocalDateTime paidAt,
    LocalDateTime exitDeadline,
    UUID sessionId,
    String plateNumber,
    String cardCode,
    String checkoutUrl,
    String qrCode) {}
