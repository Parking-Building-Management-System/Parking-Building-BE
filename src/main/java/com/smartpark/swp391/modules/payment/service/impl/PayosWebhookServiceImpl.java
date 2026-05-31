package com.smartpark.swp391.modules.payment.service.impl;

import com.smartpark.swp391.common.exception.ApiException;
import com.smartpark.swp391.common.exception.ErrorCode;
import com.smartpark.swp391.infrastructure.payment.payos.PayosProperties;
import com.smartpark.swp391.infrastructure.payment.payos.PayosSignatureService;
import com.smartpark.swp391.modules.operation.entity.ParkingSession;
import com.smartpark.swp391.modules.operation.enumType.SessionPaymentStatus;
import com.smartpark.swp391.modules.payment.entity.PaymentIntent;
import com.smartpark.swp391.modules.payment.entity.PaymentWebhookLog;
import com.smartpark.swp391.modules.payment.enumType.PaymentIntentStatus;
import com.smartpark.swp391.modules.payment.enumType.PaymentProvider;
import com.smartpark.swp391.modules.payment.enumType.PaymentWebhookLogStatus;
import com.smartpark.swp391.modules.payment.repository.PaymentIntentRepository;
import com.smartpark.swp391.modules.payment.repository.PaymentWebhookLogRepository;
import com.smartpark.swp391.modules.payment.service.PayosWebhookService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Map;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class PayosWebhookServiceImpl implements PayosWebhookService {

  PayosSignatureService signatureService;
  PayosProperties payosProperties;
  PaymentWebhookLogRepository webhookLogRepository;
  PaymentIntentRepository paymentIntentRepository;

  @Override
  @Transactional
  @SuppressWarnings("unchecked")
  public void process(Map<String, Object> payload, String rawPayload) {
    Map<String, Object> data =
        payload.get("data") instanceof Map<?, ?> map ? (Map<String, Object>) map : Map.of();
    String signature = stringValue(payload.get("signature"));
    Long orderCode = longValue(data.get("orderCode"));
    String eventCode = stringValue(payload.get("code"));

    PaymentWebhookLog log =
        webhookLogRepository.save(
            PaymentWebhookLog.builder()
                .provider(PaymentProvider.PAYOS)
                .eventCode(eventCode)
                .orderCode(orderCode)
                .signature(signature)
                .payloadJson(rawPayload)
                .status(PaymentWebhookLogStatus.RECEIVED)
                .receivedAt(LocalDateTime.now())
                .build());

    if (!payosProperties.webhookVerificationConfigured()
        || !signatureService.isValidWebhookData(data, signature, payosProperties.checksumKey())) {
      fail(log, "PAYOS_INVALID_SIGNATURE");
      throw new ApiException(ErrorCode.INVALID_INPUT, "PAYOS_INVALID_SIGNATURE");
    }

    if (orderCode == null) {
      fail(log, "PAYOS_ORDER_CODE_MISSING");
      throw new ApiException(ErrorCode.INVALID_INPUT, "PAYOS_ORDER_CODE_MISSING");
    }

    PaymentIntent intent =
        paymentIntentRepository.findByOrderCodeAndDeletedFalse(orderCode).orElse(null);
    if (intent == null) {
      ignore(log, "PAYMENT_INTENT_NOT_FOUND");
      return;
    }

    if (intent.getStatus() == PaymentIntentStatus.PAID) {
      processed(log);
      return;
    }

    BigDecimal paidAmount = decimalValue(data.get("amount"));
    if (paidAmount == null || intent.getAmount().compareTo(paidAmount) != 0) {
      fail(log, "PAYOS_AMOUNT_MISMATCH");
      throw new ApiException(ErrorCode.INVALID_INPUT, "PAYOS_AMOUNT_MISMATCH");
    }

    if (isPaid(payload, data)) {
      LocalDateTime paidAt = paidAt(data);
      intent.setStatus(PaymentIntentStatus.PAID);
      intent.setPaidAt(paidAt);
      markSessionPaid(intent, paidAt);
      processed(log);
      return;
    }

    ignore(log, "PAYOS_WEBHOOK_NOT_PAID");
  }

  private boolean isPaid(Map<String, Object> payload, Map<String, Object> data) {
    Object success = payload.get("success");
    String code = stringValue(payload.get("code"));
    String dataCode = stringValue(data.get("code"));
    return Boolean.TRUE.equals(success) || "00".equals(code) || "00".equals(dataCode);
  }

  private void markSessionPaid(PaymentIntent intent, LocalDateTime paidAt) {
    ParkingSession session = intent.getParkingSession();
    int graceMinutes =
        intent.getPricingRule() == null
            ? 15
            : intent.getPricingRule().getGraceMinutesAfterPayment();
    session.setPaymentStatus(SessionPaymentStatus.PAID);
    session.setPaymentMethod(PaymentProvider.PAYOS.name());
    session.setPaymentReference(String.valueOf(intent.getOrderCode()));
    session.setPaidAt(paidAt);
    session.setExitDeadline(paidAt.plusMinutes(graceMinutes));
    session.setTotalAmount(intent.getAmount());
  }

  private LocalDateTime paidAt(Map<String, Object> data) {
    String value = stringValue(data.get("transactionDateTime"));
    if (value == null || value.isBlank()) {
      return LocalDateTime.now();
    }
    for (DateTimeFormatter formatter :
        new DateTimeFormatter[] {
          DateTimeFormatter.ISO_LOCAL_DATE_TIME, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        }) {
      try {
        return LocalDateTime.parse(value, formatter);
      } catch (DateTimeParseException ignored) {
        // Try the next known PayOS timestamp format.
      }
    }
    return LocalDateTime.now();
  }

  private void processed(PaymentWebhookLog log) {
    log.setStatus(PaymentWebhookLogStatus.PROCESSED);
    log.setProcessedAt(LocalDateTime.now());
  }

  private void ignore(PaymentWebhookLog log, String reason) {
    log.setStatus(PaymentWebhookLogStatus.IGNORED);
    log.setErrorMessage(reason);
    log.setProcessedAt(LocalDateTime.now());
  }

  private void fail(PaymentWebhookLog log, String reason) {
    log.setStatus(PaymentWebhookLogStatus.FAILED);
    log.setErrorMessage(reason);
    log.setProcessedAt(LocalDateTime.now());
  }

  private String stringValue(Object value) {
    return value == null ? null : String.valueOf(value);
  }

  private Long longValue(Object value) {
    if (value instanceof Number number) {
      return number.longValue();
    }
    try {
      return value == null ? null : Long.parseLong(String.valueOf(value));
    } catch (NumberFormatException e) {
      return null;
    }
  }

  private BigDecimal decimalValue(Object value) {
    if (value == null) {
      return null;
    }
    if (value instanceof BigDecimal decimal) {
      return decimal;
    }
    if (value instanceof Number number) {
      return BigDecimal.valueOf(number.longValue());
    }
    try {
      return new BigDecimal(String.valueOf(value));
    } catch (NumberFormatException e) {
      return null;
    }
  }
}
