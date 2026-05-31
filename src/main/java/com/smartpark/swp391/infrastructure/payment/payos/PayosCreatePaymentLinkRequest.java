package com.smartpark.swp391.infrastructure.payment.payos;

import java.util.List;

public record PayosCreatePaymentLinkRequest(
    long orderCode,
    long amount,
    String description,
    List<PayosItemRequest> items,
    String returnUrl,
    String cancelUrl,
    Long expiredAt) {}
