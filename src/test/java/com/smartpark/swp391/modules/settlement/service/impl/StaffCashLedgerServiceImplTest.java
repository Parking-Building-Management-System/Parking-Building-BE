package com.smartpark.swp391.modules.settlement.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.smartpark.swp391.common.exception.ApiException;
import com.smartpark.swp391.modules.identity.entity.Tenant;
import com.smartpark.swp391.modules.identity.entity.User;
import com.smartpark.swp391.modules.operation.entity.Kiosk;
import com.smartpark.swp391.modules.operation.enumType.KioskType;
import com.smartpark.swp391.modules.parking.entity.Parking;
import com.smartpark.swp391.modules.settlement.entity.StaffCashShift;
import com.smartpark.swp391.modules.settlement.entity.StaffCashTransaction;
import com.smartpark.swp391.modules.settlement.enumType.StaffCashShiftStatus;
import com.smartpark.swp391.modules.settlement.enumType.StaffCashTransactionSource;
import com.smartpark.swp391.modules.settlement.enumType.StaffCashTransactionType;
import com.smartpark.swp391.modules.settlement.repository.StaffCashShiftRepository;
import com.smartpark.swp391.modules.settlement.repository.StaffCashTransactionRepository;
import com.smartpark.swp391.modules.settlement.service.StaffCashLedgerEntry;
import com.smartpark.swp391.modules.staff.dto.StaffResolvedContext;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StaffCashLedgerServiceImplTest {

  @Mock StaffCashShiftRepository staffCashShiftRepository;
  @Mock StaffCashTransactionRepository staffCashTransactionRepository;
  @Mock EntityManager entityManager;

  TestData data;

  @BeforeEach
  void setUp() {
    data = testData();
  }

  @Test
  void noOpenShiftAutoOpensAndWritesParkingCash() {
    when(staffCashShiftRepository.findOpenForStaffForUpdate(
            data.tenant.getId(), data.staff.getId()))
        .thenReturn(Optional.empty());
    stubReferences();
    when(staffCashShiftRepository.save(any(StaffCashShift.class)))
        .thenAnswer(
            invocation -> {
              StaffCashShift shift = invocation.getArgument(0);
              shift.setId(UUID.randomUUID());
              return shift;
            });

    service()
        .recordCashTransactions(
            data.context,
            List.of(
                new StaffCashLedgerEntry(
                    StaffCashTransactionType.PARKING_CASH,
                    new BigDecimal("30000"),
                    null,
                    null,
                    StaffCashTransactionSource.NORMAL_EXIT,
                    null)));

    ArgumentCaptor<List<StaffCashTransaction>> transactionsCaptor = transactionsCaptor();
    verify(staffCashTransactionRepository).saveAll(transactionsCaptor.capture());
    assertThat(transactionsCaptor.getValue()).hasSize(1);
    assertThat(transactionsCaptor.getValue().getFirst().getType())
        .isEqualTo(StaffCashTransactionType.PARKING_CASH);
    assertThat(transactionsCaptor.getValue().getFirst().getAmount()).isEqualByComparingTo("30000");
  }

  @Test
  void zeroEntriesDoNotOpenShiftOrWriteTransactions() {
    service()
        .recordCashTransactions(
            data.context,
            List.of(
                new StaffCashLedgerEntry(
                    StaffCashTransactionType.PARKING_CASH,
                    BigDecimal.ZERO,
                    null,
                    null,
                    StaffCashTransactionSource.NORMAL_EXIT,
                    null)));

    verify(staffCashShiftRepository, never()).findOpenForStaffForUpdate(any(), any());
    verify(staffCashTransactionRepository, never()).saveAll(any());
  }

  @Test
  void closedShiftCannotAcceptTransactions() {
    StaffCashShift closedShift = shift(StaffCashShiftStatus.CLOSED);
    when(staffCashShiftRepository.findOpenForStaffForUpdate(
            data.tenant.getId(), data.staff.getId()))
        .thenReturn(Optional.of(closedShift));

    assertThatThrownBy(
            () ->
                service()
                    .recordCashTransactions(
                        data.context,
                        List.of(
                            new StaffCashLedgerEntry(
                                StaffCashTransactionType.PARKING_CASH,
                                new BigDecimal("30000"),
                                null,
                                null,
                                StaffCashTransactionSource.NORMAL_EXIT,
                                null))))
        .isInstanceOf(ApiException.class)
        .hasMessage("CLOSED_SHIFT_CANNOT_ACCEPT_CASH");
  }

  private StaffCashLedgerServiceImpl service() {
    return new StaffCashLedgerServiceImpl(
        staffCashShiftRepository, staffCashTransactionRepository, entityManager);
  }

  private void stubReferences() {
    when(entityManager.getReference(Tenant.class, data.tenant.getId())).thenReturn(data.tenant);
    when(entityManager.getReference(User.class, data.staff.getId())).thenReturn(data.staff);
    when(entityManager.getReference(Parking.class, data.parking.getId())).thenReturn(data.parking);
    when(entityManager.getReference(Kiosk.class, data.kiosk.getId())).thenReturn(data.kiosk);
  }

  private StaffCashShift shift(StaffCashShiftStatus status) {
    StaffCashShift shift =
        StaffCashShift.builder()
            .tenant(data.tenant)
            .staff(data.staff)
            .parking(data.parking)
            .kiosk(data.kiosk)
            .openedAt(LocalDateTime.now())
            .status(status)
            .build();
    shift.setId(UUID.randomUUID());
    return shift;
  }

  @SuppressWarnings("unchecked")
  private ArgumentCaptor<List<StaffCashTransaction>> transactionsCaptor() {
    return ArgumentCaptor.forClass(List.class);
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
