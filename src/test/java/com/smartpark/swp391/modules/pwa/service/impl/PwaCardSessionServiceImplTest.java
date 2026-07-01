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
import com.smartpark.swp391.modules.parking.enumType.SlotStatus;
import com.smartpark.swp391.modules.parking.repository.RfidCardRepository;
import com.smartpark.swp391.modules.parking.repository.SlotRepository;
import com.smartpark.swp391.modules.payment.dto.ExistingPaymentIntentResponse;
import com.smartpark.swp391.modules.payment.enumType.PaymentIntentStatus;
import com.smartpark.swp391.modules.payment.service.PwaPaymentService;
import com.smartpark.swp391.modules.penalty.entity.PenaltyCase;
import com.smartpark.swp391.modules.penalty.entity.PenaltyRule;
import com.smartpark.swp391.modules.penalty.enumType.PenaltyCaseStatus;
import com.smartpark.swp391.modules.penalty.enumType.PenaltyType;
import com.smartpark.swp391.modules.penalty.repository.PenaltyCaseRepository;
import com.smartpark.swp391.modules.penalty.service.PenaltyRuleLookupService;
import com.smartpark.swp391.modules.pricing.dto.PricingQuoteResponse;
import com.smartpark.swp391.modules.pricing.service.PricingQuoteService;
import com.smartpark.swp391.modules.pwa.dto.report.OccupiedSlotReportRequest;
import com.smartpark.swp391.modules.vehicle.entity.VehicleType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class PwaCardSessionServiceImplTest {

  @Mock RfidCardRepository rfidCardRepository;
  @Mock ParkingSessionRepository parkingSessionRepository;
  @Mock SlotRepository slotRepository;
  @Mock StorageService storageService;
  @Mock PricingQuoteService pricingQuoteService;
  @Mock PwaPaymentService pwaPaymentService;
  @Mock PenaltyRuleLookupService penaltyRuleLookupService;
  @Mock PenaltyCaseRepository penaltyCaseRepository;

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

  @Test
  void occupiedSlotReportMatchedOffenderCreatesAppliedPenaltyAndReassignsVictim() {
    TestData data = testData();
    Slot replacementSlot = replacementSlot(data);
    ParkingSession offender =
        ParkingSession.builder()
            .tenant(data.session.getTenant())
            .parking(data.session.getParking())
            .zone(data.session.getZone())
            .slot(data.session.getSlot())
            .rfidCard(data.card)
            .vehicleType(data.session.getVehicleType())
            .licensePlate("51A-99999")
            .checkInAt(LocalDateTime.parse("2026-05-31T08:30:00"))
            .status(ParkingSessionStatus.ACTIVE)
            .build();
    offender.setId(UUID.randomUUID());
    PenaltyRule rule = occupiedSlotRule(data);
    when(rfidCardRepository.findByQrToken("qr")).thenReturn(Optional.of(data.card));
    when(parkingSessionRepository.findActiveByRfidCardId(
            data.card.getId(), ParkingSessionStatus.ACTIVE, PageRequest.of(0, 1)))
        .thenReturn(List.of(data.session));
    when(penaltyRuleLookupService.requireActiveRule(
            data.session.getTenant().getId(),
            data.session.getParking().getId(),
            PenaltyType.OCCUPIED_ASSIGNED_SLOT))
        .thenReturn(rule);
    when(parkingSessionRepository.findActiveDetailsByTenantIdAndParkingId(
            data.session.getTenant().getId(),
            data.session.getParking().getId(),
            ParkingSessionStatus.ACTIVE))
        .thenReturn(List.of(data.session, offender));
    when(slotRepository.findFirstAvailableForReassignmentByVehicleType(
            data.session.getTenant().getId(),
            data.session.getParking().getId(),
            data.session.getVehicleType().getId(),
            data.session.getSlot().getId(),
            SlotStatus.AVAILABLE,
            PageRequest.of(0, 1)))
        .thenReturn(List.of(replacementSlot));
    when(penaltyCaseRepository.save(any(PenaltyCase.class)))
        .thenAnswer(
            invocation -> {
              PenaltyCase penaltyCase = invocation.getArgument(0);
              penaltyCase.setId(UUID.randomUUID());
              return penaltyCase;
            });

    var response =
        service()
            .reportOccupiedSlot(
                "qr",
                new OccupiedSlotReportRequest(
                    "51a 99999", "tenants/t/parking-sessions/report.jpg", "A-01 occupied"));

    ArgumentCaptor<PenaltyCase> penaltyCaptor = ArgumentCaptor.forClass(PenaltyCase.class);
    assertThat(response.offenderMatched()).isTrue();
    assertThat(response.oldSlotCode()).isEqualTo("A-01");
    assertThat(response.newSlotCode()).isEqualTo("B-05");
    assertThat(data.session.getSlot()).isEqualTo(replacementSlot);
    assertThat(data.slot.getStatus()).isEqualTo(SlotStatus.OCCUPIED);
    assertThat(replacementSlot.getStatus()).isEqualTo(SlotStatus.OCCUPIED);
    org.mockito.Mockito.verify(penaltyCaseRepository).save(penaltyCaptor.capture());
    assertThat(penaltyCaptor.getValue().getStatus()).isEqualTo(PenaltyCaseStatus.APPLIED);
    assertThat(penaltyCaptor.getValue().getTargetSession()).isEqualTo(offender);
    assertThat(penaltyCaptor.getValue().getVictimSession()).isEqualTo(data.session);
  }

  private PwaCardSessionServiceImpl service() {
    return new PwaCardSessionServiceImpl(
        rfidCardRepository,
        parkingSessionRepository,
        slotRepository,
        storageService,
        pricingQuoteService,
        pwaPaymentService,
        penaltyRuleLookupService,
        penaltyCaseRepository);
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
    return new TestData(card, session, slot);
  }

  private Slot replacementSlot(TestData data) {
    Slot slot =
        Slot.builder()
            .tenant(data.session.getTenant())
            .parking(data.session.getParking())
            .zone(data.session.getZone())
            .floor(data.session.getSlot().getFloor())
            .code("B-05")
            .slotNumber("05")
            .status(SlotStatus.AVAILABLE)
            .build();
    slot.setId(UUID.randomUUID());
    return slot;
  }

  private PenaltyRule occupiedSlotRule(TestData data) {
    PenaltyRule rule =
        PenaltyRule.builder()
            .tenant(data.session.getTenant())
            .parking(data.session.getParking())
            .code("OCCUPIED_ASSIGNED_SLOT")
            .name("Occupied slot")
            .type(PenaltyType.OCCUPIED_ASSIGNED_SLOT)
            .amount(new BigDecimal("50000"))
            .currency("VND")
            .build();
    rule.setId(UUID.randomUUID());
    return rule;
  }

  private record TestData(RfidCard card, ParkingSession session, Slot slot) {}
}
