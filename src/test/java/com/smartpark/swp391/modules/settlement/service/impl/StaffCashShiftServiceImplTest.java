package com.smartpark.swp391.modules.settlement.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.smartpark.swp391.common.exception.ApiException;
import com.smartpark.swp391.modules.identity.entity.Tenant;
import com.smartpark.swp391.modules.identity.entity.User;
import com.smartpark.swp391.modules.operation.entity.Kiosk;
import com.smartpark.swp391.modules.operation.enumType.KioskType;
import com.smartpark.swp391.modules.parking.entity.Parking;
import com.smartpark.swp391.modules.payment.enumType.PaymentIntentStatus;
import com.smartpark.swp391.modules.payment.enumType.PaymentProvider;
import com.smartpark.swp391.modules.payment.repository.PaymentIntentRepository;
import com.smartpark.swp391.modules.settlement.dto.StaffCashShiftCloseRequest;
import com.smartpark.swp391.modules.settlement.entity.StaffCashShift;
import com.smartpark.swp391.modules.settlement.entity.StaffCashTransaction;
import com.smartpark.swp391.modules.settlement.enumType.StaffCashShiftStatus;
import com.smartpark.swp391.modules.settlement.enumType.StaffCashTransactionSource;
import com.smartpark.swp391.modules.settlement.enumType.StaffCashTransactionType;
import com.smartpark.swp391.modules.settlement.repository.StaffCashShiftRepository;
import com.smartpark.swp391.modules.settlement.repository.StaffCashTransactionRepository;
import com.smartpark.swp391.modules.settlement.service.StaffCashSettlementMapper;
import com.smartpark.swp391.modules.staff.dto.StaffResolvedContext;
import com.smartpark.swp391.modules.staff.service.StaffWorkContextService;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class StaffCashShiftServiceImplTest {

  @Mock StaffWorkContextService staffWorkContextService;
  @Mock StaffCashShiftRepository staffCashShiftRepository;
  @Mock StaffCashTransactionRepository staffCashTransactionRepository;
  @Mock PaymentIntentRepository paymentIntentRepository;
  @Mock EntityManager entityManager;

  TestData data;

  @BeforeEach
  void setUp() {
    data = testData();
  }

  @Test
  void startCreatesOpenShift() {
    when(staffWorkContextService.requireCurrentResolvedContext()).thenReturn(data.context);
    when(staffCashShiftRepository.findFirstByTenantIdAndStaffIdAndStatusOrderByOpenedAtDesc(
            data.tenant.getId(), data.staff.getId(), StaffCashShiftStatus.OPEN))
        .thenReturn(Optional.empty());
    stubReferences();
    when(staffCashShiftRepository.save(any(StaffCashShift.class)))
        .thenAnswer(
            invocation -> {
              StaffCashShift shift = invocation.getArgument(0);
              shift.setId(UUID.randomUUID());
              return shift;
            });

    var response = service().startShift();

    assertThat(response.status()).isEqualTo(StaffCashShiftStatus.OPEN);
    assertThat(response.staffId()).isEqualTo(data.staff.getId());
    assertThat(response.parkingId()).isEqualTo(data.parking.getId());
    verify(staffCashShiftRepository).save(any(StaffCashShift.class));
  }

  @Test
  void startReturnsExistingOpenShift() {
    StaffCashShift openShift = openShift();
    when(staffWorkContextService.requireCurrentResolvedContext()).thenReturn(data.context);
    when(staffCashShiftRepository.findFirstByTenantIdAndStaffIdAndStatusOrderByOpenedAtDesc(
            data.tenant.getId(), data.staff.getId(), StaffCashShiftStatus.OPEN))
        .thenReturn(Optional.of(openShift));

    var response = service().startShift();

    assertThat(response.id()).isEqualTo(openShift.getId());
    assertThat(response.status()).isEqualTo(StaffCashShiftStatus.OPEN);
  }

  @Test
  void previewCalculatesExpectedCashFromTransactionsAndOnlineSeparately() {
    StaffCashShift openShift = openShift();
    when(staffWorkContextService.requireCurrentResolvedContext()).thenReturn(data.context);
    when(staffCashShiftRepository.findFirstByTenantIdAndStaffIdAndStatusOrderByOpenedAtDesc(
            data.tenant.getId(), data.staff.getId(), StaffCashShiftStatus.OPEN))
        .thenReturn(Optional.of(openShift));
    when(staffCashTransactionRepository.findByTenantIdAndShiftIdOrderByOccurredAtDesc(
            data.tenant.getId(), openShift.getId()))
        .thenReturn(
            List.of(
                transaction(openShift, StaffCashTransactionType.PARKING_CASH, "30000"),
                transaction(openShift, StaffCashTransactionType.PENALTY_CASH, "50000")));
    when(staffCashTransactionRepository.findByTenantIdAndShiftIdOrderByOccurredAtDesc(
            data.tenant.getId(), openShift.getId(), PageRequest.of(0, 20)))
        .thenReturn(List.of());
    when(paymentIntentRepository.sumAmountByParkingAndPaidAtRange(
            eq(data.tenant.getId()),
            eq(data.parking.getId()),
            eq(PaymentIntentStatus.PAID),
            eq(PaymentProvider.PAYOS),
            eq(openShift.getOpenedAt()),
            any(LocalDateTime.class)))
        .thenReturn(new BigDecimal("70000"));

    var response = service().getCurrentSettlementPreview();

    assertThat(response.expectedCashAmount()).isEqualByComparingTo("80000");
    assertThat(response.cashParkingAmount()).isEqualByComparingTo("30000");
    assertThat(response.penaltyCashAmount()).isEqualByComparingTo("50000");
    assertThat(response.onlineAmount()).isEqualByComparingTo("70000");
    assertThat(response.transactionCount()).isEqualTo(2);
  }

  @Test
  void closeStoresCountedCashAndVarianceSnapshot() {
    StaffCashShift openShift = openShift();
    when(staffWorkContextService.requireCurrentResolvedContext()).thenReturn(data.context);
    when(staffCashShiftRepository.findOpenForStaffForUpdate(
            data.tenant.getId(), data.staff.getId()))
        .thenReturn(Optional.of(openShift));
    when(staffCashTransactionRepository.findByTenantIdAndShiftIdOrderByOccurredAtDesc(
            data.tenant.getId(), openShift.getId()))
        .thenReturn(
            List.of(
                transaction(openShift, StaffCashTransactionType.PARKING_CASH, "30000"),
                transaction(openShift, StaffCashTransactionType.LOST_CARD_FINE, "100000")));
    when(paymentIntentRepository.sumAmountByParkingAndPaidAtRange(
            eq(data.tenant.getId()),
            eq(data.parking.getId()),
            eq(PaymentIntentStatus.PAID),
            eq(PaymentProvider.PAYOS),
            eq(openShift.getOpenedAt()),
            any(LocalDateTime.class)))
        .thenReturn(BigDecimal.ZERO);
    when(staffCashShiftRepository.save(openShift)).thenReturn(openShift);

    var response =
        service()
            .closeCurrentShift(new StaffCashShiftCloseRequest(new BigDecimal("120000"), "End"));

    assertThat(response.status()).isEqualTo(StaffCashShiftStatus.CLOSED);
    assertThat(response.expectedCashAmount()).isEqualByComparingTo("130000");
    assertThat(response.countedCashAmount()).isEqualByComparingTo("120000");
    assertThat(response.varianceAmount()).isEqualByComparingTo("-10000");
    assertThat(response.lostCardCashAmount()).isEqualByComparingTo("100000");
    assertThat(response.transactionCount()).isEqualTo(2);
  }

  @Test
  void closeWithoutOpenShiftFails() {
    when(staffWorkContextService.requireCurrentResolvedContext()).thenReturn(data.context);
    when(staffCashShiftRepository.findOpenForStaffForUpdate(
            data.tenant.getId(), data.staff.getId()))
        .thenReturn(Optional.empty());

    assertThatThrownBy(
            () ->
                service()
                    .closeCurrentShift(new StaffCashShiftCloseRequest(new BigDecimal("0"), null)))
        .isInstanceOf(ApiException.class)
        .hasMessage("OPEN_SHIFT_NOT_FOUND");
  }

  private StaffCashShiftServiceImpl service() {
    return new StaffCashShiftServiceImpl(
        staffWorkContextService,
        staffCashShiftRepository,
        staffCashTransactionRepository,
        paymentIntentRepository,
        new StaffCashSettlementMapper(),
        entityManager);
  }

  private void stubReferences() {
    when(entityManager.getReference(Tenant.class, data.tenant.getId())).thenReturn(data.tenant);
    when(entityManager.getReference(User.class, data.staff.getId())).thenReturn(data.staff);
    when(entityManager.getReference(Parking.class, data.parking.getId())).thenReturn(data.parking);
    when(entityManager.getReference(Kiosk.class, data.kiosk.getId())).thenReturn(data.kiosk);
  }

  private StaffCashShift openShift() {
    StaffCashShift shift =
        StaffCashShift.builder()
            .tenant(data.tenant)
            .staff(data.staff)
            .parking(data.parking)
            .kiosk(data.kiosk)
            .openedAt(LocalDateTime.now().minusHours(2))
            .status(StaffCashShiftStatus.OPEN)
            .expectedCashAmount(BigDecimal.ZERO)
            .onlineAmount(BigDecimal.ZERO)
            .cashParkingAmount(BigDecimal.ZERO)
            .surchargeCashAmount(BigDecimal.ZERO)
            .penaltyCashAmount(BigDecimal.ZERO)
            .lostCardCashAmount(BigDecimal.ZERO)
            .transactionCount(0)
            .build();
    shift.setId(UUID.randomUUID());
    return shift;
  }

  private StaffCashTransaction transaction(
      StaffCashShift shift, StaffCashTransactionType type, String amount) {
    StaffCashTransaction transaction =
        StaffCashTransaction.builder()
            .tenant(data.tenant)
            .shift(shift)
            .parking(data.parking)
            .kiosk(data.kiosk)
            .staff(data.staff)
            .type(type)
            .source(StaffCashTransactionSource.NORMAL_EXIT)
            .amount(new BigDecimal(amount))
            .occurredAt(LocalDateTime.now())
            .build();
    transaction.setId(UUID.randomUUID());
    return transaction;
  }

  private TestData testData() {
    Tenant tenant = Tenant.builder().name("Tenant").slug("tenant").emailContact("a@b.com").build();
    tenant.setId(UUID.randomUUID());
    User staff =
        User.builder()
            .tenant(tenant)
            .username("staff")
            .password("secret")
            .fullName("Staff User")
            .build();
    staff.setId(UUID.randomUUID());
    Parking parking = Parking.builder().tenant(tenant).name("Parking").code("P").build();
    parking.setId(UUID.randomUUID());
    Kiosk kiosk =
        Kiosk.builder()
            .tenant(tenant)
            .parking(parking)
            .code("K1")
            .name("Exit Kiosk")
            .type(KioskType.EXIT)
            .build();
    kiosk.setId(UUID.randomUUID());
    StaffResolvedContext context =
        StaffResolvedContext.builder()
            .tenantId(tenant.getId())
            .staffId(staff.getId())
            .parkingId(parking.getId())
            .parkingName(parking.getName())
            .kioskId(kiosk.getId())
            .kioskName(kiosk.getName())
            .kioskType(KioskType.EXIT)
            .build();
    return new TestData(tenant, staff, parking, kiosk, context);
  }

  private record TestData(
      Tenant tenant, User staff, Parking parking, Kiosk kiosk, StaffResolvedContext context) {}
}
