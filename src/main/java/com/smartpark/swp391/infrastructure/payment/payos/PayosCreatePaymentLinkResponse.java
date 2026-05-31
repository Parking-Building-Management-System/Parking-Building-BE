package com.smartpark.swp391.infrastructure.payment.payos;

public record PayosCreatePaymentLinkResponse(
    String paymentLinkId, String checkoutUrl, String qrCode, String rawResponse) {}
