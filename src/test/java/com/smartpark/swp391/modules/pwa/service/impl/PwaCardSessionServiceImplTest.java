package com.smartpark.swp391.modules.pwa.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.smartpark.swp391.infrastructure.storage.service.StorageService;
import com.smartpark.swp391.modules.identity.entity.Tenant;
import com.smartpark.swp391.modules.operation.entity.ParkingSession;
import com.smartpark.swp391.modules.operation.enumType.ParkingSessionStatus;
import com.smartpark.swp391.modules.operation.enumType.SessionPaymentStatus;
import com.smartpark.swp391.modules.operation.repository.ParkingSessionRepository;
import com.smartpark.swp391.modules.parking.entity.Floor;
import com.smartpark.swp391.modules.parking.entity.Parking;
import com.smartpark.swp391.modules.parking.entity.RfidCard;
import com.smartpark.swp391.modules.parking.entity.Slot;
import com.smartpark.swp391.modules.parking.entity.Zone;
import com.smartpark.swp391.modules.parking.enumType.RfidCardStatus;
import com.smartpark.swp391.modules.parking.repository.RfidCardRepository;
import com.smartpark.swp391.modules.payment.dto.ExistingPaymentIntentResponse;
import com.smartpark.swp391.modules.payment.enumType.PaymentIntentStatus;
import com.smartpark.swp391.modules.payment.service.PwaPaymentService;
import com.smartpark.swp391.modules.pricing.dto.PricingQuoteResponse;
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
class PwaCardSessionServiceImplTest {

  @Mock RfidCardRepository rfidCardRepository;
  @Mock ParkingSessionRepository parkingSessionRepository;
  @Mock StorageService storageService;
  @Mock PricingQuoteService pricingQuoteService;
  @Mock PwaPaymentService pwaPaymentService;

  @Test
  void checkoutQuoteReflectsPendingPaymentIntent() {
    TestData data = testData();
    when(rfidCardRepository.findByQrToken("qr")).thenReturn(Optional.of(data.card));
    when(parkingSessionRepository.findActiveByRfidCardId(
            data.card.getId(), ParkingSessionStatus.ACTIVE, PageRequest.of(0, 1)))
        .thenReturn(List.of(data.session));
    when(pricingQuoteService.quote(any(), any(), any(), any(), any())).thenReturn(quote());
    when(pwaPaymentService.paymentProviderAvailable()).thenReturn(true);
    when(pwaPaymentService.findReusablePendingIntent(data.session.getId(), new BigDecimal("30000")))
        .thenReturn(
            Optional.of(
                ExistingPaymentIntentResponse.builder()
                    .orderCode(202605310001L)
                    .status(PaymentIntentStatus.PENDING)
                    .checkoutUrl("https://pay.payos.vn/x")
                    .expiresAt(LocalDateTime.parse("2026-05-31T10:15:00"))
                    .build()));

    var response = service().getCheckoutQuote("qr");

    assertThat(response.nextAction()).isEqualTo("CONTINUE_PAYMENT");
    assertThat(response.existingPaymentIntent().orderCode()).isEqualTo(202605310001L);
    assertThat(response.paymentAvailable()).isTrue();
  }

  @Test
  void checkoutQuoteReflectsPaidSession() {
    TestData data = testData();
    data.session.setPaymentStatus(SessionPaymentStatus.PAID);
    data.session.setPaidAt(LocalDateTime.parse("2026-05-31T10:30:00"));
    data.session.setExitDeadline(LocalDateTime.parse("2026-05-31T10:45:00"));
    when(rfidCardRepository.findByQrToken("qr")).thenReturn(Optional.of(data.card));
    when(parkingSessionRepository.findActiveByRfidCardId(
            data.card.getId(), ParkingSessionStatus.ACTIVE, PageRequest.of(0, 1)))
        .thenReturn(List.of(data.session));
    when(pricingQuoteService.quote(any(), any(), any(), any(), any())).thenReturn(quote());

    var response = service().getCheckoutQuote("qr");

    assertThat(response.paymentStatus()).isEqualTo(SessionPaymentStatus.PAID);
    assertThat(response.nextAction()).isEqualTo("EXIT_WITHIN_GRACE_PERIOD");
    assertThat(response.exitDeadline()).isEqualTo(LocalDateTime.parse("2026-05-31T10:45:00"));
  }

  @Test
  void checkoutQuoteReflectsProviderDisabled() {
    TestData data = testData();
    when(rfidCardRepository.findByQrToken("qr")).thenReturn(Optional.of(data.card));
    when(parkingSessionRepository.findActiveByRfidCardId(
            data.card.getId(), ParkingSessionStatus.ACTIVE, PageRequest.of(0, 1)))
        .thenReturn(List.of(data.session));
    when(pricingQuoteService.quote(any(), any(), any(), any(), any())).thenReturn(quote());
    when(pwaPaymentService.paymentProviderAvailable()).thenReturn(false);
    when(pwaPaymentService.findReusablePendingIntent(data.session.getId(), new BigDecimal("30000")))
        .thenReturn(Optional.empty());

    var response = service().getCheckoutQuote("qr");

    assertThat(response.paymentAvailable()).isFalse();
    assertThat(response.nextAction()).isEqualTo("PAYMENT_PROVIDER_DISABLED");
  }

  private PwaCardSessionServiceImpl service() {
    return new PwaCardSessionServiceImpl(
        rfidCardRepository,
        parkingSessionRepository,
        storageService,
        pricingQuoteService,
        pwaPaymentService);
  }

  private PricingQuoteResponse quote() {
    return PricingQuoteResponse.builder()
        .pricingRuleId(UUID.randomUUID())
        .pricingRuleName("Rule")
        .checkInAt(LocalDateTime.parse("2026-05-31T08:00:00"))
        .quotedAt(LocalDateTime.parse("2026-05-31T10:00:00"))
        .durationMinutes(120)
        .chargeableMinutes(120)
        .amount(new BigDecimal("30000"))
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
    Floor floor = Floor.builder().tenant(tenant).parking(parking).name("B1").code("B1").build();
    floor.setId(UUID.randomUUID());
    Zone zone =
        Zone.builder().tenant(tenant).parking(parking).floor(floor).name("A").code("A").build();
    zone.setId(UUID.randomUUID());
    Slot slot =
        Slot.builder()
            .tenant(tenant)
            .parking(parking)
            .zone(zone)
            .floor(floor)
            .code("A-01")
            .slotNumber("01")
            .build();
    slot.setId(UUID.randomUUID());
    ParkingSession session =
        ParkingSession.builder()
            .tenant(tenant)
            .parking(parking)
            .zone(zone)
            .slot(slot)
            .rfidCard(card)
            .vehicleType(vehicleType)
            .licensePlate("51A-12345")
            .checkInAt(LocalDateTime.parse("2026-05-31T08:00:00"))
            .status(ParkingSessionStatus.ACTIVE)
            .build();
    session.setId(UUID.randomUUID());
    return new TestData(card, session);
  }

  private record TestData(RfidCard card, ParkingSession session) {}
}
