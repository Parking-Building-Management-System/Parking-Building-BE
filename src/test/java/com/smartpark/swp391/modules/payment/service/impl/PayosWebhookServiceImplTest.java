package com.smartpark.swp391.modules.payment.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartpark.swp391.common.exception.ApiException;
import com.smartpark.swp391.infrastructure.payment.payos.PayosProperties;
import com.smartpark.swp391.infrastructure.payment.payos.PayosSignatureService;
import com.smartpark.swp391.modules.identity.entity.Tenant;
import com.smartpark.swp391.modules.operation.entity.ParkingSession;
import com.smartpark.swp391.modules.operation.enumType.SessionPaymentStatus;
import com.smartpark.swp391.modules.payment.entity.PaymentIntent;
import com.smartpark.swp391.modules.payment.entity.PaymentWebhookLog;
import com.smartpark.swp391.modules.payment.enumType.PaymentIntentStatus;
import com.smartpark.swp391.modules.payment.repository.PaymentIntentRepository;
import com.smartpark.swp391.modules.payment.repository.PaymentWebhookLogRepository;
import com.smartpark.swp391.modules.pricing.entity.PricingRule;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PayosWebhookServiceImplTest {

  @Mock PaymentWebhookLogRepository webhookLogRepository;
  @Mock PaymentIntentRepository paymentIntentRepository;

  @Test
  void invalidSignatureIsRejected() {
    when(webhookLogRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    assertThatThrownBy(() -> service().process(payload("bad-signature", 30000), "{}"))
        .isInstanceOf(ApiException.class)
        .hasMessage("PAYOS_INVALID_SIGNATURE");
  }

  @Test
  void amountMismatchIsRejected() {
    PaymentIntent intent = intent(new BigDecimal("30000"));
    when(webhookLogRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    when(paymentIntentRepository.findByOrderCodeAndDeletedFalse(202605310001L))
        .thenReturn(Optional.of(intent));

    assertThatThrownBy(() -> service().process(validPayload(25000), "{}"))
        .isInstanceOf(ApiException.class)
        .hasMessage("PAYOS_AMOUNT_MISMATCH");
  }

  @Test
  void successfulWebhookMarksIntentAndSessionPaid() {
    PaymentIntent intent = intent(new BigDecimal("30000"));
    when(webhookLogRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    when(paymentIntentRepository.findByOrderCodeAndDeletedFalse(202605310001L))
        .thenReturn(Optional.of(intent));

    service().process(validPayload(30000), "{}");

    assertThat(intent.getStatus()).isEqualTo(PaymentIntentStatus.PAID);
    assertThat(intent.getPaidAt()).isEqualTo(LocalDateTime.parse("2026-05-31T10:30:00"));
    assertThat(intent.getParkingSession().getPaymentStatus()).isEqualTo(SessionPaymentStatus.PAID);
    assertThat(intent.getParkingSession().getExitDeadline())
        .isEqualTo(LocalDateTime.parse("2026-05-31T10:45:00"));
  }

  @Test
  void duplicatePaidWebhookIsIdempotent() {
    PaymentIntent intent = intent(new BigDecimal("30000"));
    intent.setStatus(PaymentIntentStatus.PAID);
    when(webhookLogRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    when(paymentIntentRepository.findByOrderCodeAndDeletedFalse(202605310001L))
        .thenReturn(Optional.of(intent));

    service().process(validPayload(30000), "{}");

    assertThat(intent.getStatus()).isEqualTo(PaymentIntentStatus.PAID);
  }

  @Test
  void unknownOrderCodeIsLoggedAndIgnored() {
    when(webhookLogRepository.save(any()))
        .thenAnswer(
            invocation -> {
              PaymentWebhookLog log = invocation.getArgument(0);
              log.setId(UUID.randomUUID());
              return log;
            });
    when(paymentIntentRepository.findByOrderCodeAndDeletedFalse(202605310001L))
        .thenReturn(Optional.empty());

    service().process(validPayload(30000), "{}");

    assertThat(true).isTrue();
  }

  private PayosWebhookServiceImpl service() {
    return new PayosWebhookServiceImpl(
        new PayosSignatureService(new ObjectMapper()),
        new PayosProperties(true, "client", "api", "secret", "", "return", "cancel"),
        webhookLogRepository,
        paymentIntentRepository);
  }

  private Map<String, Object> validPayload(long amount) {
    Map<String, Object> data = data(amount);
    String signature =
        new PayosSignatureService(new ObjectMapper()).isValidWebhookData(data, "", "secret")
            ? ""
            : sign(data);
    return payload(signature, amount);
  }

  private Map<String, Object> payload(String signature, long amount) {
    Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("code", "00");
    payload.put("success", true);
    payload.put("data", data(amount));
    payload.put("signature", signature);
    return payload;
  }

  private Map<String, Object> data(long amount) {
    Map<String, Object> data = new LinkedHashMap<>();
    data.put("amount", amount);
    data.put("orderCode", 202605310001L);
    data.put("transactionDateTime", "2026-05-31T10:30:00");
    return data;
  }

  private String sign(Map<String, Object> data) {
    return TestPayosSigner.sign(data, "secret");
  }

  private PaymentIntent intent(BigDecimal amount) {
    Tenant tenant = Tenant.builder().name("Tenant").slug("tenant").emailContact("a@b.com").build();
    tenant.setId(UUID.randomUUID());
    ParkingSession session = ParkingSession.builder().tenant(tenant).licensePlate("51A").build();
    session.setId(UUID.randomUUID());
    PricingRule rule =
        PricingRule.builder()
            .tenant(tenant)
            .name("Rule")
            .graceMinutesAfterPayment(15)
            .firstBlockMinutes(60)
            .firstBlockPrice(new BigDecimal("20000"))
            .nextBlockMinutes(60)
            .nextBlockPrice(new BigDecimal("10000"))
            .build();
    return PaymentIntent.builder()
        .tenant(tenant)
        .parkingSession(session)
        .orderCode(202605310001L)
        .amount(amount)
        .status(PaymentIntentStatus.PENDING)
        .pricingRule(rule)
        .build();
  }
}
