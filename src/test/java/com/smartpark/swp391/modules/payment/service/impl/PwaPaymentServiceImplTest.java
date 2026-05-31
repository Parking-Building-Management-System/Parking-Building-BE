package com.smartpark.swp391.modules.payment.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartpark.swp391.common.exception.ApiException;
import com.smartpark.swp391.infrastructure.payment.payos.PayosClient;
import com.smartpark.swp391.infrastructure.payment.payos.PayosCreatePaymentLinkResponse;
import com.smartpark.swp391.infrastructure.payment.payos.PayosProperties;
import com.smartpark.swp391.modules.identity.entity.Tenant;
import com.smartpark.swp391.modules.operation.entity.ParkingSession;
import com.smartpark.swp391.modules.operation.enumType.ParkingSessionStatus;
import com.smartpark.swp391.modules.operation.enumType.SessionPaymentStatus;
import com.smartpark.swp391.modules.operation.repository.ParkingSessionRepository;
import com.smartpark.swp391.modules.parking.entity.Parking;
import com.smartpark.swp391.modules.parking.entity.RfidCard;
import com.smartpark.swp391.modules.parking.enumType.RfidCardStatus;
import com.smartpark.swp391.modules.parking.repository.RfidCardRepository;
import com.smartpark.swp391.modules.payment.entity.PaymentIntent;
import com.smartpark.swp391.modules.payment.enumType.PaymentIntentStatus;
import com.smartpark.swp391.modules.payment.repository.PaymentIntentRepository;
import com.smartpark.swp391.modules.payment.service.OrderCodeGenerator;
import com.smartpark.swp391.modules.pricing.dto.PricingQuoteResponse;
import com.smartpark.swp391.modules.pricing.entity.PricingRule;
import com.smartpark.swp391.modules.pricing.repository.PricingRuleRepository;
import com.smartpark.swp391.modules.pricing.service.PricingQuoteService;
import com.smartpark.swp391.modules.vehicle.entity.VehicleType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class PwaPaymentServiceImplTest {

  @Mock RfidCardRepository rfidCardRepository;
  @Mock ParkingSessionRepository parkingSessionRepository;
  @Mock PricingQuoteService pricingQuoteService;
  @Mock PricingRuleRepository pricingRuleRepository;
  @Mock PaymentIntentRepository paymentIntentRepository;
  @Mock OrderCodeGenerator orderCodeGenerator;
  @Mock PayosClient payosClient;

  @Test
  void paymentIntentCreationReturnsCheckoutUrlAndOrderCode() {
    TestData data = testData();
    PricingQuoteResponse quote = quote(data.rule.getId(), "30000");
    when(rfidCardRepository.findByQrToken("qr")).thenReturn(Optional.of(data.card));
    when(parkingSessionRepository.findActiveByRfidCardId(
            data.card.getId(), ParkingSessionStatus.ACTIVE, PageRequest.of(0, 1)))
        .thenReturn(List.of(data.session));
    when(pricingQuoteService.quote(any(), any(), any(), any(), any())).thenReturn(quote);
    when(paymentIntentRepository.findBySessionAndStatus(
            data.session.getId(), PaymentIntentStatus.PENDING))
        .thenReturn(List.of());
    when(pricingRuleRepository.findById(data.rule.getId())).thenReturn(Optional.of(data.rule));
    when(orderCodeGenerator.nextOrderCode()).thenReturn(202605310001L);
    when(paymentIntentRepository.existsByOrderCode(202605310001L)).thenReturn(false);
    when(payosClient.createPaymentLink(any()))
        .thenReturn(
            new PayosCreatePaymentLinkResponse("link-id", "https://pay.payos.vn/x", "qr", "{}"));
    when(paymentIntentRepository.save(any()))
        .thenAnswer(
            invocation -> {
              PaymentIntent intent = invocation.getArgument(0);
              intent.setId(UUID.randomUUID());
              return intent;
            });

    var response = enabledService().createPaymentIntent("qr");

    assertThat(response.orderCode()).isEqualTo(202605310001L);
    assertThat(response.checkoutUrl()).isEqualTo("https://pay.payos.vn/x");
    assertThat(response.status()).isEqualTo(PaymentIntentStatus.PENDING);
  }

  @Test
  void existingPendingIntentIsReused() {
    TestData data = testData();
    PaymentIntent existing =
        PaymentIntent.builder()
            .tenant(data.tenant)
            .parkingSession(data.session)
            .rfidCard(data.card)
            .orderCode(202605310001L)
            .amount(new BigDecimal("30000"))
            .status(PaymentIntentStatus.PENDING)
            .checkoutUrl("https://pay.payos.vn/existing")
            .expiresAt(LocalDateTime.now().plusMinutes(10))
            .build();
    existing.setId(UUID.randomUUID());
    when(rfidCardRepository.findByQrToken("qr")).thenReturn(Optional.of(data.card));
    when(parkingSessionRepository.findActiveByRfidCardId(
            data.card.getId(), ParkingSessionStatus.ACTIVE, PageRequest.of(0, 1)))
        .thenReturn(List.of(data.session));
    when(pricingQuoteService.quote(any(), any(), any(), any(), any()))
        .thenReturn(quote(data.rule.getId(), "30000"));
    when(paymentIntentRepository.findBySessionAndStatus(
            data.session.getId(), PaymentIntentStatus.PENDING))
        .thenReturn(List.of(existing));
    when(paymentIntentRepository.findByOrderCodeAndDeletedFalse(202605310001L))
        .thenReturn(Optional.of(existing));

    var response = enabledService().createPaymentIntent("qr");

    assertThat(response.checkoutUrl()).isEqualTo("https://pay.payos.vn/existing");
    verify(paymentIntentRepository).findByOrderCodeAndDeletedFalse(202605310001L);
  }

  @Test
  void providerDisabledReturnsClearError() {
    TestData data = testData();
    when(rfidCardRepository.findByQrToken("qr")).thenReturn(Optional.of(data.card));
    when(parkingSessionRepository.findActiveByRfidCardId(
            data.card.getId(), ParkingSessionStatus.ACTIVE, PageRequest.of(0, 1)))
        .thenReturn(List.of(data.session));
    when(pricingQuoteService.quote(any(), any(), any(), any(), any()))
        .thenReturn(quote(data.rule.getId(), "30000"));
    when(paymentIntentRepository.findBySessionAndStatus(
            data.session.getId(), PaymentIntentStatus.PENDING))
        .thenReturn(List.of());
    when(pricingRuleRepository.findById(data.rule.getId())).thenReturn(Optional.of(data.rule));

    assertThatThrownBy(() -> disabledService().createPaymentIntent("qr"))
        .isInstanceOf(ApiException.class)
        .hasMessage("PAYMENT_PROVIDER_DISABLED");
  }

  @Test
  void zeroAmountIntentMarksSessionPaidWithoutPayos() {
    TestData data = testData();
    when(rfidCardRepository.findByQrToken("qr")).thenReturn(Optional.of(data.card));
    when(parkingSessionRepository.findActiveByRfidCardId(
            data.card.getId(), ParkingSessionStatus.ACTIVE, PageRequest.of(0, 1)))
        .thenReturn(List.of(data.session));
    when(pricingQuoteService.quote(any(), any(), any(), any(), any()))
        .thenReturn(quote(data.rule.getId(), "0"));
    when(paymentIntentRepository.findBySessionAndStatus(
            data.session.getId(), PaymentIntentStatus.PENDING))
        .thenReturn(List.of());
    when(pricingRuleRepository.findById(data.rule.getId())).thenReturn(Optional.of(data.rule));
    when(orderCodeGenerator.nextOrderCode()).thenReturn(202605310001L);
    when(paymentIntentRepository.existsByOrderCode(202605310001L)).thenReturn(false);
    when(paymentIntentRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    var response = disabledService().createPaymentIntent("qr");

    assertThat(response.status()).isEqualTo(PaymentIntentStatus.PAID);
    assertThat(data.session.getPaymentStatus()).isEqualTo(SessionPaymentStatus.PAID);
    assertThat(data.session.getExitDeadline()).isEqualTo(data.session.getPaidAt().plusMinutes(15));
  }

  private PwaPaymentServiceImpl enabledService() {
    return service(
        new PayosProperties(true, "client", "api", "checksum", "webhook", "return", "cancel"));
  }

  private PwaPaymentServiceImpl disabledService() {
    return service(new PayosProperties(false, "", "", "", "", "", ""));
  }

  private PwaPaymentServiceImpl service(PayosProperties properties) {
    return new PwaPaymentServiceImpl(
        rfidCardRepository,
        parkingSessionRepository,
        pricingQuoteService,
        pricingRuleRepository,
        paymentIntentRepository,
        orderCodeGenerator,
        payosClient,
        properties,
        new ObjectMapper().findAndRegisterModules());
  }

  private PricingQuoteResponse quote(UUID pricingRuleId, String amount) {
    return PricingQuoteResponse.builder()
        .pricingRuleId(pricingRuleId)
        .pricingRuleName("Rule")
        .checkInAt(LocalDateTime.parse("2026-05-31T08:00:00"))
        .quotedAt(LocalDateTime.parse("2026-05-31T10:00:00"))
        .durationMinutes(120)
        .chargeableMinutes(120)
        .amount(new BigDecimal(amount))
        .currency("VND")
        .pricingBreakdown(List.of())
        .build();
  }

  private TestData testData() {
    Tenant tenant = Tenant.builder().name("Tenant").slug("tenant").emailContact("a@b.com").build();
    tenant.setId(UUID.randomUUID());
    Parking parking = Parking.builder().tenant(tenant).name("Parking").code("P").build();
    parking.setId(UUID.randomUUID());
    VehicleType vehicleType = VehicleType.builder().name("Car").code("CAR").build();
    vehicleType.setId(UUID.randomUUID());
    RfidCard card =
        RfidCard.builder()
            .tenant(tenant)
            .code("BCONS-0004")
            .uid("uid")
            .qrToken("qr")
            .status(RfidCardStatus.ACTIVE)
            .build();
    card.setId(UUID.randomUUID());
    ParkingSession session =
        ParkingSession.builder()
            .tenant(tenant)
            .parking(parking)
            .rfidCard(card)
            .vehicleType(vehicleType)
            .licensePlate("51A-12345")
            .checkInAt(LocalDateTime.parse("2026-05-31T08:00:00"))
            .status(ParkingSessionStatus.ACTIVE)
            .build();
    session.setId(UUID.randomUUID());
    PricingRule rule =
        PricingRule.builder()
            .tenant(tenant)
            .name("Rule")
            .vehicleType(vehicleType)
            .firstBlockMinutes(60)
            .firstBlockPrice(new BigDecimal("20000"))
            .nextBlockMinutes(60)
            .nextBlockPrice(new BigDecimal("10000"))
            .graceMinutesAfterPayment(15)
            .build();
    rule.setId(UUID.randomUUID());
    return new TestData(tenant, card, session, rule);
  }

  private record TestData(Tenant tenant, RfidCard card, ParkingSession session, PricingRule rule) {}
}
