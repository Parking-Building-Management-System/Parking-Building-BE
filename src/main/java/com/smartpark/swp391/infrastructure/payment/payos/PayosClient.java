package com.smartpark.swp391.infrastructure.payment.payos;

public interface PayosClient {
  PayosCreatePaymentLinkResponse createPaymentLink(PayosCreatePaymentLinkRequest request);
}
