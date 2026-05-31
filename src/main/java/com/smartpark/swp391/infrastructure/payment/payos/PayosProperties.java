package com.smartpark.swp391.infrastructure.payment.payos;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.payos")
public record PayosProperties(
    boolean enabled,
    String clientId,
    String apiKey,
    String checksumKey,
    String webhookUrl,
    String returnUrl,
    String cancelUrl) {

  public boolean configured() {
    return hasText(clientId)
        && hasText(apiKey)
        && hasText(checksumKey)
        && hasText(webhookUrl)
        && hasText(returnUrl)
        && hasText(cancelUrl);
  }

  public boolean webhookVerificationConfigured() {
    return hasText(checksumKey);
  }

  public void requireReadyForPaymentCreation() {
    if (!enabled) {
      throw new PayosProviderException("PAYMENT_PROVIDER_DISABLED");
    }
    if (!configured()) {
      throw new PayosProviderException("PAYOS_NOT_CONFIGURED");
    }
  }

  private boolean hasText(String value) {
    return value != null && !value.isBlank();
  }
}
