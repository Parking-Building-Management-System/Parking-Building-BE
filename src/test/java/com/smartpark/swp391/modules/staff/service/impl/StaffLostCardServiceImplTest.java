package com.smartpark.swp391.modules.staff.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.smartpark.swp391.infrastructure.storage.service.StorageService;
import com.smartpark.swp391.infrastructure.tenant.TenantContext;
import com.smartpark.swp391.modules.identity.entity.Tenant;
import com.smartpark.swp391.modules.operation.entity.ParkingSession;
import com.smartpark.swp391.modules.operation.enumType.KioskType;
import com.smartpark.swp391.modules.operation.enumType.ParkingSessionStatus;
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
import com.smartpark.swp391.modules.penalty.dto.PenaltyCaseResponse;
import com.smartpark.swp391.modules.penalty.entity.PenaltyCase;
import com.smartpark.swp391.modules.penalty.entity.PenaltyRule;
import com.smartpark.swp391.modules.penalty.enumType.PenaltyCaseStatus;
import com.smartpark.swp391.modules.penalty.enumType.PenaltyRuleStatus;
import com.smartpark.swp391.modules.penalty.enumType.PenaltyType;
import com.smartpark.swp391.modules.penalty.repository.PenaltyCaseRepository;
import com.smartpark.swp391.modules.penalty.service.PenaltyCaseResponseMapper;
import com.smartpark.swp391.modules.penalty.service.PenaltyRuleLookupService;
import com.smartpark.swp391.modules.pricing.dto.PricingQuoteResponse;
import com.smartpark.swp391.modules.pricing.entity.PricingRule;
import com.smartpark.swp391.modules.pricing.repository.PricingRuleRepository;
import com.smartpark.swp391.modules.pricing.service.PricingQuoteService;
import com.smartpark.swp391.modules.staff.dto.StaffWorkContextResponse;
import com.smartpark.swp391.modules.staff.dto.lostcard.StaffLostCardCaseRequest;
import com.smartpark.swp391.modules.staff.dto.lostcard.StaffLostCardCompleteExitRequest;
import com.smartpark.swp391.modules.staff.service.StaffWorkContextService;
import com.smartpark.swp391.modules.vehicle.entity.VehicleType;
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

@ExtendWith(MockitoExtension.class)
class StaffLostCardServiceImplTest {

  @Mock StaffWorkContextService staffWorkContextService;
  @Mock ParkingSessionRepository parkingSessionRepository;
  @Mock PricingQuoteService pricingQuoteService;
  @Mock PricingRuleRepository pricingRuleRepository;
  @Mock PenaltyRuleLookupService penaltyRuleLookupService;
  @Mock PenaltyCaseRepository penaltyCaseRepository;
  @Mock PenaltyCaseResponseMapper penaltyCaseResponseMapper;
  @Mock SlotRepository slotRepository;
  @Mock RfidCardRepository rfidCardRepository;
  @Mock StorageService storageService;

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
  void previewByPlateReturnsSessionDueAndLostCardPenalty() {
    PenaltyCase existingPenalty = occupiedSlotPenalty(data);
    stubExitContext();
    when(parkingSessionRepository.findActiveDetailsByTenantIdAndParkingId(
            data.tenant.getId(), data.parking.getId(), ParkingSessionStatus.ACTIVE))
        .thenReturn(List.of(data.session));
    when(penaltyRuleLookupService.requireActiveRule(
            data.tenant.getId(), data.parking.getId(), PenaltyType.LOST_CARD))
        .thenReturn(data.lostCardRule);
    stubQuote(List.of(existingPenalty));
    when(penaltyCaseResponseMapper.toResponse(existingPenalty))
        .thenReturn(
            PenaltyCaseResponse.builder()
                .id(existingPenalty.getId())
                .type(PenaltyType.OCCUPIED_ASSIGNED_SLOT)
                .amount(new BigDecimal("50000"))
                .currency("VND")
                .status(PenaltyCaseStatus.APPLIED)
                .build());

    var response = service().previewByPlate("51a 12345");

    assertThat(response.sessionId()).isEqualTo(data.session.getId());
    assertThat(response.parkingAmountDue()).isEqualByComparingTo("30000");
    assertThat(response.existingPenaltyAmount()).isEqualByComparingTo("50000");
    assertThat(response.lostCardPenaltyAmount()).isEqualByComparingTo("100000");
    assertThat(response.totalDueIfLostCard()).isEqualByComparingTo("180000");
    assertThat(response.currentRfidCardCode()).isEqualTo("***0004");
    assertThat(response.activePenaltyCases()).hasSize(1);
  }

  @Test
  void createCasePersistsEvidenceAndAppliedLostCardPenalty() {
    stubExitContext();
    when(parkingSessionRepository.findDetailByTenantIdAndId(
            data.tenant.getId(), data.session.getId()))
        .thenReturn(Optional.of(data.session));
    when(penaltyRuleLookupService.requireActiveRule(
            data.tenant.getId(), data.parking.getId(), PenaltyType.LOST_CARD))
        .thenReturn(data.lostCardRule);
    when(penaltyCaseRepository.findByTargetSessionAndStatuses(
            data.tenant.getId(), data.session.getId(), List.of(PenaltyCaseStatus.APPLIED)))
        .thenReturn(List.of());
    when(penaltyCaseRepository.save(any(PenaltyCase.class))).thenAnswer(this::savePenaltyCase);
    when(penaltyCaseResponseMapper.toResponse(any(PenaltyCase.class)))
        .thenAnswer(
            invocation -> {
              PenaltyCase penaltyCase = invocation.getArgument(0);
              return PenaltyCaseResponse.builder()
                  .id(penaltyCase.getId())
                  .type(penaltyCase.getType())
                  .amount(penaltyCase.getAmount())
                  .currency(penaltyCase.getCurrency())
                  .status(penaltyCase.getStatus())
                  .build();
            });

    var response =
        service()
            .createCase(
                new StaffLostCardCaseRequest(
                    data.session.getId(),
                    " tenants/t/id.jpg ",
                    "tenants/t/vehicle.jpg",
                    "tenants/t/plate.jpg",
                    " Driver reported lost card "));

    ArgumentCaptor<PenaltyCase> captor = ArgumentCaptor.forClass(PenaltyCase.class);
    verify(penaltyCaseRepository).save(captor.capture());
    PenaltyCase saved = captor.getValue();
    assertThat(saved.getType()).isEqualTo(PenaltyType.LOST_CARD);
    assertThat(saved.getStatus()).isEqualTo(PenaltyCaseStatus.APPLIED);
    assertThat(saved.getTargetSession()).isEqualTo(data.session);
    assertThat(saved.getTargetLicensePlate()).isEqualTo("51A-12345");
    assertThat(saved.getIdentityImageUrl()).isEqualTo("tenants/t/id.jpg");
    assertThat(saved.getVehicleImageUrl()).isEqualTo("tenants/t/vehicle.jpg");
    assertThat(saved.getLicensePlateImageUrl()).isEqualTo("tenants/t/plate.jpg");
    assertThat(response.penaltyCase().amount()).isEqualByComparingTo("100000");
  }

  @Test
  void completeExitCollectsLostCardPenaltyCompletesSessionAndMarksCardLost() {
    PenaltyCase lostCardCase = lostCardCase(data);
    stubExitContext();
    when(parkingSessionRepository.findDetailByTenantIdAndId(
            data.tenant.getId(), data.session.getId()))
        .thenReturn(Optional.of(data.session));
    when(penaltyCaseRepository.findDetailByTenantIdAndId(
            data.tenant.getId(), lostCardCase.getId()))
        .thenReturn(Optional.of(lostCardCase));
    stubQuote(List.of(lostCardCase));

    var response =
        service()
            .completeExit(
                new StaffLostCardCompleteExitRequest(
                    data.session.getId(), lostCardCase.getId(), new BigDecimal("130000"), null));

    assertThat(response.status()).isEqualTo(ParkingSessionStatus.COMPLETED);
    assertThat(response.penaltyAmountDue()).isEqualByComparingTo("100000");
    assertThat(response.totalAmountDue()).isEqualByComparingTo("130000");
    assertThat(data.session.getStatus()).isEqualTo(ParkingSessionStatus.COMPLETED);
    assertThat(data.slot.getStatus()).isEqualTo(SlotStatus.AVAILABLE);
    assertThat(data.card.getStatus()).isEqualTo(RfidCardStatus.LOST);
    assertThat(lostCardCase.getStatus()).isEqualTo(PenaltyCaseStatus.COLLECTED);
    assertThat(lostCardCase.getCollectedAt()).isNotNull();
    verify(penaltyCaseRepository).saveAll(List.of(lostCardCase));
    verify(slotRepository).save(data.slot);
    verify(rfidCardRepository).save(data.card);
    verify(parkingSessionRepository).save(data.session);
  }

  private void stubExitContext() {
    when(staffWorkContextService.requireCurrentContext())
        .thenReturn(
            StaffWorkContextResponse.builder()
                .kioskId(UUID.randomUUID())
                .kioskName("Exit")
                .kioskType(KioskType.EXIT)
                .parkingId(data.parking.getId())
                .parkingName(data.parking.getName())
                .build());
  }

  private void stubQuote(List<PenaltyCase> penaltyCases) {
    when(pricingQuoteService.quote(
            eq(data.tenant.getId()),
            eq(data.parking.getId()),
            eq(data.vehicleType.getId()),
            eq(data.session.getCheckInAt()),
            any(LocalDateTime.class)))
        .thenReturn(
            PricingQuoteResponse.builder()
                .pricingRuleId(data.pricingRule.getId())
                .pricingRuleName(data.pricingRule.getName())
                .checkInAt(data.session.getCheckInAt())
                .quotedAt(LocalDateTime.now())
                .durationMinutes(120)
                .chargeableMinutes(120)
                .amount(new BigDecimal("30000"))
                .currency("VND")
                .pricingBreakdown(List.of())
                .build());
    when(pricingRuleRepository.findById(data.pricingRule.getId()))
        .thenReturn(Optional.of(data.pricingRule));
    when(penaltyCaseRepository.findByTargetSessionAndStatuses(
            data.tenant.getId(), data.session.getId(), List.of(PenaltyCaseStatus.APPLIED)))
        .thenReturn(penaltyCases);
  }

  private StaffLostCardServiceImpl service() {
    return new StaffLostCardServiceImpl(
        staffWorkContextService,
        parkingSessionRepository,
        pricingQuoteService,
        pricingRuleRepository,
        penaltyRuleLookupService,
        penaltyCaseRepository,
        penaltyCaseResponseMapper,
        slotRepository,
        rfidCardRepository,
        storageService);
  }

  private PenaltyCase savePenaltyCase(org.mockito.invocation.InvocationOnMock invocation) {
    PenaltyCase penaltyCase = invocation.getArgument(0);
    if (penaltyCase.getId() == null) {
      penaltyCase.setId(UUID.randomUUID());
    }
    return penaltyCase;
  }

  private PenaltyCase occupiedSlotPenalty(TestData data) {
    PenaltyCase penaltyCase =
        PenaltyCase.builder()
            .tenant(data.tenant)
            .parking(data.parking)
            .type(PenaltyType.OCCUPIED_ASSIGNED_SLOT)
            .amount(new BigDecimal("50000"))
            .currency("VND")
            .status(PenaltyCaseStatus.APPLIED)
            .targetSession(data.session)
            .targetLicensePlate(data.session.getLicensePlate())
            .build();
    penaltyCase.setId(UUID.randomUUID());
    return penaltyCase;
  }

  private PenaltyCase lostCardCase(TestData data) {
    PenaltyCase penaltyCase =
        PenaltyCase.builder()
            .tenant(data.tenant)
            .parking(data.parking)
            .rule(data.lostCardRule)
            .type(PenaltyType.LOST_CARD)
            .amount(new BigDecimal("100000"))
            .currency("VND")
            .status(PenaltyCaseStatus.APPLIED)
            .targetSession(data.session)
            .targetLicensePlate(data.session.getLicensePlate())
            .build();
    penaltyCase.setId(UUID.randomUUID());
    return penaltyCase;
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
    PricingRule pricingRule =
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
    pricingRule.setId(UUID.randomUUID());
    PenaltyRule lostCardRule =
        PenaltyRule.builder()
            .tenant(tenant)
            .code("LOST_CARD")
            .name("Lost card")
            .type(PenaltyType.LOST_CARD)
            .amount(new BigDecimal("100000"))
            .currency("VND")
            .requiresPhoto(true)
            .status(PenaltyRuleStatus.ACTIVE)
            .build();
    lostCardRule.setId(UUID.randomUUID());
    return new TestData(
        tenant, parking, vehicleType, card, floor, zone, slot, session, pricingRule, lostCardRule);
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
      PricingRule pricingRule,
      PenaltyRule lostCardRule) {}
}
