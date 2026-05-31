package com.smartpark.swp391.infrastructure.payment.payos;

public class PayosProviderException extends RuntimeException {
  public PayosProviderException(String message) {
    super(message);
  }

  public PayosProviderException(String message, Throwable cause) {
    super(message, cause);
  }
}
