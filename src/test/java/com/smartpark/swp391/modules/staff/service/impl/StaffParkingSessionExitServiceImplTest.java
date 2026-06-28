package com.smartpark.swp391.modules.staff.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.smartpark.swp391.common.exception.ApiException;
import com.smartpark.swp391.infrastructure.tenant.TenantContext;
import com.smartpark.swp391.modules.identity.entity.Tenant;
import com.smartpark.swp391.modules.identity.repository.TenantRepository;
import com.smartpark.swp391.modules.operation.entity.ParkingSession;
import com.smartpark.swp391.modules.operation.enumType.KioskType;
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
import com.smartpark.swp391.modules.parking.repository.ParkingRepository;
import com.smartpark.swp391.modules.parking.repository.RfidCardRepository;
import com.smartpark.swp391.modules.parking.repository.SlotRepository;
import com.smartpark.swp391.modules.pricing.dto.PricingQuoteResponse;
import com.smartpark.swp391.modules.pricing.entity.PricingRule;
import com.smartpark.swp391.modules.pricing.repository.PricingRuleRepository;
import com.smartpark.swp391.modules.pricing.service.PricingQuoteService;
import com.smartpark.swp391.modules.staff.dto.ParkingSessionCheckInRequest;
import com.smartpark.swp391.modules.staff.dto.StaffWorkContextResponse;
import com.smartpark.swp391.modules.staff.dto.exit.CompleteExitRequest;
import com.smartpark.swp391.modules.staff.dto.exit.ExitDecision;
import com.smartpark.swp391.modules.staff.dto.exit.ExitPaymentMode;
import com.smartpark.swp391.modules.staff.dto.exit.ExitPreviewRequest;
import com.smartpark.swp391.modules.staff.service.StaffWorkContextService;
import com.smartpark.swp391.modules.vehicle.entity.VehicleType;
import com.smartpark.swp391.modules.vehicle.repository.VehicleTypeRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class StaffParkingSessionExitServiceImplTest {

  @Mock ParkingSessionRepository parkingSessionRepository;
  @Mock ParkingRepository parkingRepository;
  @Mock RfidCardRepository rfidCardRepository;
  @Mock SlotRepository slotRepository;
  @Mock VehicleTypeRepository vehicleTypeRepository;
  @Mock TenantRepository tenantRepository;
  @Mock StaffWorkContextService staffWorkContextService;
  @Mock PricingQuoteService pricingQuoteService;
  @Mock PricingRuleRepository pricingRuleRepository;

  TestData data;

  @BeforeEach
  void setUp() {
    data = testData();
    TenantContext.setTenantId(data.tenant.getId());
  }

  @AfterEach
  void tearDown() {
    TenantContext.clear();
  }

  @Test
  void checkInWithVehicleTypeAssignsMatchingZoneSlot() {
    VehicleType motorbike = VehicleType.builder().name("Motorcycle").code("MOTORBIKE").build();
    motorbike.setId(UUID.randomUUID());
    Zone motorbikeZone =
        Zone.builder()
            .tenant(data.tenant)
            .parking(data.parking)
            .floor(data.floor)
            .name("Motorcycles")
            .code("B")
            .vehicleType(motorbike)
            .build();
    motorbikeZone.setId(UUID.randomUUID());
    Slot motorbikeSlot =
        Slot.builder()
            .tenant(data.tenant)
            .parking(data.parking)
            .zone(motorbikeZone)
            .floor(data.floor)
            .code("B-01")
            .slotNumber("B1")
            .status(SlotStatus.AVAILABLE)
            .build();
    motorbikeSlot.setId(UUID.randomUUID());
    stubCheckInBase(data.card);
    when(vehicleTypeRepository.findByIdAndDeletedFalse(motorbike.getId()))
        .thenReturn(Optional.of(motorbike));
    when(slotRepository.findFirstAvailableForCheckInByVehicleType(
            data.tenant.getId(),
            data.parking.getId(),
            motorbike.getId(),
            SlotStatus.AVAILABLE,
            PageRequest.of(0, 1)))
        .thenReturn(List.of(motorbikeSlot));
    when(parkingSessionRepository.save(any(ParkingSession.class)))
        .thenAnswer(
            invocation -> {
              ParkingSession session = invocation.getArgument(0);
              session.setId(UUID.randomUUID());
              return session;
            });

    var response =
        service()
            .checkIn(
                new ParkingSessionCheckInRequest(
                    "demo-bike-001",
                    data.card.getCode(),
                    data.parking.getId(),
                    motorbike.getId(),
                    null));

    ArgumentCaptor<ParkingSession> sessionCaptor = ArgumentCaptor.forClass(ParkingSession.class);
    verify(parkingSessionRepository).save(sessionCaptor.capture());
    ParkingSession savedSession = sessionCaptor.getValue();
    assertThat(savedSession.getStatus()).isEqualTo(ParkingSessionStatus.ACTIVE);
    assertThat(savedSession.getVehicleType().getId()).isEqualTo(motorbike.getId());
    assertThat(savedSession.getSlot().getZone().getVehicleType().getId()).isEqualTo(motorbike.getId());
    assertThat(motorbikeSlot.getStatus()).isEqualTo(SlotStatus.OCCUPIED);
    assertThat(response.assignedSlotId()).isEqualTo(motorbikeSlot.getId());
    assertThat(response.vehicleTypeId()).isEqualTo(motorbike.getId());
    assertThat(response.vehicleTypeCode()).isEqualTo(motorbike.getCode());
    assertThat(response.vehicleTypeName()).isEqualTo(motorbike.getName());
  }

  @Test
  void checkInWithVehicleTypeFailsWhenNoMatchingSlotAvailable() {
    stubCheckInBase(data.card);
    when(vehicleTypeRepository.findByIdAndDeletedFalse(data.vehicleType.getId()))
        .thenReturn(Optional.of(data.vehicleType));
    when(slotRepository.findFirstAvailableForCheckInByVehicleType(
            data.tenant.getId(),
            data.parking.getId(),
            data.vehicleType.getId(),
            SlotStatus.AVAILABLE,
            PageRequest.of(0, 1)))
        .thenReturn(List.of());

    assertThatThrownBy(
            () ->
                service()
                    .checkIn(
                        new ParkingSessionCheckInRequest(
                            "demo-car-001",
                            data.card.getCode(),
                            data.parking.getId(),
                            data.vehicleType.getId(),
                            null)))
        .isInstanceOf(ApiException.class)
        .hasMessage("No available slot for selected vehicle type");
    verify(parkingSessionRepository, never()).save(any(ParkingSession.class));
  }

  @Test
  void checkInWithoutVehicleTypeFallsBackToSlotZoneVehicleType() {
    data.slot.setStatus(SlotStatus.AVAILABLE);
    stubCheckInBase(data.card);
    when(slotRepository.findFirstAvailableForCheckIn(
            data.tenant.getId(),
            data.parking.getId(),
            SlotStatus.AVAILABLE,
            PageRequest.of(0, 1)))
        .thenReturn(List.of(data.slot));
    when(parkingSessionRepository.save(any(ParkingSession.class)))
        .thenAnswer(
            invocation -> {
              ParkingSession session = invocation.getArgument(0);
              session.setId(UUID.randomUUID());
              return session;
            });

    service()
        .checkIn(
            new ParkingSessionCheckInRequest(
                "demo-car-002", data.card.getCode(), data.parking.getId(), null, null));

    ArgumentCaptor<ParkingSession> sessionCaptor = ArgumentCaptor.forClass(ParkingSession.class);
    verify(parkingSessionRepository).save(sessionCaptor.capture());
    ParkingSession savedSession = sessionCaptor.getValue();
    assertThat(savedSession.getVehicleType().getId()).isEqualTo(data.vehicleType.getId());
    assertThat(data.slot.getStatus()).isEqualTo(SlotStatus.OCCUPIED);
  }

  @Test
  void checkInRejectsRfidCardAlreadyInUseBeforeSlotLookup() {
    when(staffWorkContextService.requireCurrentContext())
        .thenReturn(workContext(data.parking.getId()));
    when(tenantRepository.getReferenceById(data.tenant.getId())).thenReturn(data.tenant);
    when(parkingRepository.findByIdAndTenantIdAndIsDeletedFalse(
            data.parking.getId(), data.tenant.getId()))
        .thenReturn(Optional.of(data.parking));
    when(rfidCardRepository.findByTenantIdAndCodeIgnoreCase(
            data.tenant.getId(), data.card.getCode()))
        .thenReturn(Optional.of(data.card));
    when(parkingSessionRepository.existsByTenantIdAndRfidCardIdAndStatus(
            data.tenant.getId(), data.card.getId(), ParkingSessionStatus.ACTIVE))
        .thenReturn(true);

    assertThatThrownBy(
            () ->
                service()
                    .checkIn(
                        new ParkingSessionCheckInRequest(
                            "demo-car-003",
                            data.card.getCode(),
                            data.parking.getId(),
                            data.vehicleType.getId(),
                            null)))
        .isInstanceOf(ApiException.class)
        .hasMessage("RFID card is already in use");
    verify(slotRepository, never())
        .findFirstAvailableForCheckInByVehicleType(any(), any(), any(), any(), any());
    verify(slotRepository, never()).findFirstAvailableForCheckIn(any(), any(), any(), any());
  }

  @Test
  void checkInParkingOutsideWorkContextRejected() {
    UUID otherParkingId = UUID.randomUUID();
    when(staffWorkContextService.requireCurrentContext())
        .thenReturn(workContext(data.parking.getId()));

    assertThatThrownBy(
            () ->
                service()
                    .checkIn(
                        new ParkingSessionCheckInRequest(
                            "demo-car-004",
                            data.card.getCode(),
                            otherParkingId,
                            data.vehicleType.getId(),
                            null)))
        .isInstanceOf(ApiException.class)
        .hasMessage("CHECK_IN_PARKING_NOT_IN_KIOSK_CONTEXT");
    verify(parkingRepository, never()).findByIdAndTenantIdAndIsDeletedFalse(any(), any());
  }

  @Test
  void exitPreviewPaidWithinGraceAllowsExit() {
    data.session.setPaymentStatus(SessionPaymentStatus.PAID);
    data.session.setPaidAt(LocalDateTime.now().minusMinutes(3));
    data.session.setExitDeadline(LocalDateTime.now().plusMinutes(10));
    data.session.setTotalAmount(new BigDecimal("30000"));
    stubPreview(data);

    var response = service().previewExit(new ExitPreviewRequest(data.card.getCode()));

    assertThat(response.exitDecision()).isEqualTo(ExitDecision.ALLOW_EXIT);
    assertThat(response.amountDue()).isEqualByComparingTo("0");
    assertThat(response.surchargeAmount()).isEqualByComparingTo("0");
    assertThat(response.totalAmount()).isEqualByComparingTo("30000");
  }

  @Test
  void exitPreviewPaidExpiredRequiresSurcharge() {
    data.session.setPaymentStatus(SessionPaymentStatus.PAID);
    data.session.setPaidAt(LocalDateTime.now().minusMinutes(30));
    data.session.setExitDeadline(LocalDateTime.now().minusMinutes(1));
    data.session.setTotalAmount(new BigDecimal("30000"));
    stubPreview(data);

    var response = service().previewExit(new ExitPreviewRequest(data.card.getCode()));

    assertThat(response.exitDecision()).isEqualTo(ExitDecision.GRACE_EXPIRED_SURCHARGE);
    assertThat(response.surchargeAmount()).isEqualByComparingTo("10000");
    assertThat(response.totalAmount()).isEqualByComparingTo("40000");
  }

  @Test
  void exitPreviewUnpaidCollectsCash() {
    stubPreview(data);

    var response = service().previewExit(new ExitPreviewRequest(data.card.getCode()));

    assertThat(response.exitDecision()).isEqualTo(ExitDecision.COLLECT_CASH);
    assertThat(response.amountDue()).isEqualByComparingTo("30000");
    assertThat(response.totalAmount()).isEqualByComparingTo("30000");
  }

  @Test
  void completeOnlinePaidWithinGraceCompletesSessionAndReleasesSlot() {
    data.session.setPaymentStatus(SessionPaymentStatus.PAID);
    data.session.setPaidAt(LocalDateTime.now().minusMinutes(3));
    data.session.setExitDeadline(LocalDateTime.now().plusMinutes(10));
    data.session.setTotalAmount(new BigDecimal("30000"));
    stubComplete(data);

    var response =
        service()
            .completeExit(
                new CompleteExitRequest(
                    data.session.getId(),
                    data.card.getCode(),
                    ExitPaymentMode.ONLINE,
                    BigDecimal.ZERO,
                    null));

    assertThat(response.status()).isEqualTo(ParkingSessionStatus.COMPLETED);
    assertThat(response.slotStatus()).isEqualTo(SlotStatus.AVAILABLE);
    assertThat(response.cardStatus()).isEqualTo(RfidCardStatus.ACTIVE);
    assertThat(data.session.getCheckOutAt()).isNotNull();
    assertThat(data.session.getTotalAmount()).isEqualByComparingTo("30000");
  }

  @Test
  void completeCashUnpaidCompletesSession() {
    stubComplete(data);

    var response =
        service()
            .completeExit(
                new CompleteExitRequest(
                    data.session.getId(),
                    data.card.getCode(),
                    ExitPaymentMode.CASH,
                    new BigDecimal("30000"),
                    null));

    assertThat(response.status()).isEqualTo(ParkingSessionStatus.COMPLETED);
    assertThat(data.session.getPaymentStatus()).isEqualTo(SessionPaymentStatus.CASH_COLLECTED);
    assertThat(data.session.getTotalAmount()).isEqualByComparingTo("30000");
    assertThat(data.slot.getStatus()).isEqualTo(SlotStatus.AVAILABLE);
  }

  @Test
  void completeSurchargeExpiredCompletesSession() {
    data.session.setPaymentStatus(SessionPaymentStatus.PAID);
    data.session.setPaidAt(LocalDateTime.now().minusMinutes(30));
    data.session.setExitDeadline(LocalDateTime.now().minusMinutes(1));
    data.session.setTotalAmount(new BigDecimal("30000"));
    stubComplete(data);

    var response =
        service()
            .completeExit(
                new CompleteExitRequest(
                    data.session.getId(),
                    data.card.getCode(),
                    ExitPaymentMode.SURCHARGE_CASH,
                    new BigDecimal("10000"),
                    null));

    assertThat(response.status()).isEqualTo(ParkingSessionStatus.COMPLETED);
    assertThat(data.session.getPaymentStatus()).isEqualTo(SessionPaymentStatus.SURCHARGE_COLLECTED);
    assertThat(data.session.getTotalAmount()).isEqualByComparingTo("40000");
  }

  @Test
  void wrongKioskParkingRejected() {
    UUID otherParkingId = UUID.randomUUID();
    when(staffWorkContextService.requireCurrentContext()).thenReturn(workContext(otherParkingId));
    when(rfidCardRepository.findByTenantIdAndCodeIgnoreCase(
            data.tenant.getId(), data.card.getCode()))
        .thenReturn(Optional.of(data.card));
    when(parkingSessionRepository.findActiveDetailByTenantAndRfidCardId(
            data.tenant.getId(),
            data.card.getId(),
            ParkingSessionStatus.ACTIVE,
            PageRequest.of(0, 1)))
        .thenReturn(List.of(data.session));

    assertThatThrownBy(() -> service().previewExit(new ExitPreviewRequest(data.card.getCode())))
        .isInstanceOf(ApiException.class)
        .hasMessage("SESSION_NOT_IN_KIOSK_PARKING");
  }

  @Test
  void alreadyCompletedRejected() {
    data.session.setStatus(ParkingSessionStatus.COMPLETED);
    when(staffWorkContextService.requireCurrentContext())
        .thenReturn(workContext(data.parking.getId()));
    when(parkingSessionRepository.findDetailByTenantIdAndId(
            data.tenant.getId(), data.session.getId()))
        .thenReturn(Optional.of(data.session));

    assertThatThrownBy(
            () ->
                service()
                    .completeExit(
                        new CompleteExitRequest(
                            data.session.getId(),
                            data.card.getCode(),
                            ExitPaymentMode.ONLINE,
                            BigDecimal.ZERO,
                            null)))
        .isInstanceOf(ApiException.class)
        .hasMessage("SESSION_ALREADY_COMPLETED");
  }

  @Test
  void entryKioskRejectedForExit() {
    when(staffWorkContextService.requireCurrentContext())
        .thenReturn(
            StaffWorkContextResponse.builder()
                .kioskId(UUID.randomUUID())
                .kioskName("Entry")
                .kioskType(KioskType.ENTRY)
                .parkingId(data.parking.getId())
                .parkingName(data.parking.getName())
                .build());

    assertThatThrownBy(() -> service().previewExit(new ExitPreviewRequest(data.card.getCode())))
        .isInstanceOf(ApiException.class)
        .hasMessage("EXIT_KIOSK_REQUIRED");
  }

  private void stubPreview(TestData data) {
    when(staffWorkContextService.requireCurrentContext())
        .thenReturn(workContext(data.parking.getId()));
    when(rfidCardRepository.findByTenantIdAndCodeIgnoreCase(
            data.tenant.getId(), data.card.getCode()))
        .thenReturn(Optional.of(data.card));
    when(parkingSessionRepository.findActiveDetailByTenantAndRfidCardId(
            data.tenant.getId(),
            data.card.getId(),
            ParkingSessionStatus.ACTIVE,
            PageRequest.of(0, 1)))
        .thenReturn(List.of(data.session));
    stubQuote(data);
  }

  private void stubComplete(TestData data) {
    when(staffWorkContextService.requireCurrentContext())
        .thenReturn(workContext(data.parking.getId()));
    when(parkingSessionRepository.findDetailByTenantIdAndId(
            data.tenant.getId(), data.session.getId()))
        .thenReturn(Optional.of(data.session));
    stubQuote(data);
  }

  private void stubQuote(TestData data) {
    when(pricingQuoteService.quote(
            eq(data.tenant.getId()),
            eq(data.parking.getId()),
            eq(data.vehicleType.getId()),
            eq(data.session.getCheckInAt()),
            any(LocalDateTime.class)))
        .thenReturn(quote(data.rule.getId()));
    when(pricingRuleRepository.findById(data.rule.getId())).thenReturn(Optional.of(data.rule));
  }

  private void stubCheckInBase(RfidCard card) {
    when(staffWorkContextService.requireCurrentContext())
        .thenReturn(workContext(data.parking.getId()));
    when(tenantRepository.getReferenceById(data.tenant.getId())).thenReturn(data.tenant);
    when(parkingRepository.findByIdAndTenantIdAndIsDeletedFalse(
            data.parking.getId(), data.tenant.getId()))
        .thenReturn(Optional.of(data.parking));
    when(rfidCardRepository.findByTenantIdAndCodeIgnoreCase(data.tenant.getId(), card.getCode()))
        .thenReturn(Optional.of(card));
    when(parkingSessionRepository.existsByTenantIdAndRfidCardIdAndStatus(
            data.tenant.getId(), card.getId(), ParkingSessionStatus.ACTIVE))
        .thenReturn(false);
  }

  private StaffParkingSessionServiceImpl service() {
    return new StaffParkingSessionServiceImpl(
        parkingSessionRepository,
        parkingRepository,
        rfidCardRepository,
        slotRepository,
        vehicleTypeRepository,
        tenantRepository,
        staffWorkContextService,
        pricingQuoteService,
        pricingRuleRepository);
  }

  private StaffWorkContextResponse workContext(UUID parkingId) {
    return StaffWorkContextResponse.builder()
        .kioskId(UUID.randomUUID())
        .kioskName("Exit")
        .kioskType(KioskType.EXIT)
        .parkingId(parkingId)
        .parkingName("Parking")
        .build();
  }

  private PricingQuoteResponse quote(UUID ruleId) {
    return PricingQuoteResponse.builder()
        .pricingRuleId(ruleId)
        .pricingRuleName("Rule")
        .checkInAt(data.session.getCheckInAt())
        .quotedAt(LocalDateTime.now())
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
        Zone.builder()
            .tenant(tenant)
            .parking(parking)
            .floor(floor)
            .name("A")
            .code("A")
            .vehicleType(vehicleType)
            .build();
    zone.setId(UUID.randomUUID());
    Slot slot =
        Slot.builder()
            .tenant(tenant)
            .parking(parking)
            .zone(zone)
            .floor(floor)
            .code("A-01")
            .slotNumber("01")
            .status(SlotStatus.OCCUPIED)
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
            .checkInAt(LocalDateTime.now().minusHours(2))
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
    return new TestData(tenant, parking, vehicleType, card, floor, zone, slot, session, rule);
  }

  private record TestData(
      Tenant tenant,
      Parking parking,
      VehicleType vehicleType,
      RfidCard card,
      Floor floor,
      Zone zone,
      Slot slot,
      ParkingSession session,
      PricingRule rule) {}
}
