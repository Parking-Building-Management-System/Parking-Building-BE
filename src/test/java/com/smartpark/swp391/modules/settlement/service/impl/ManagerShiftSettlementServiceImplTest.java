package com.smartpark.swp391.modules.settlement.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.smartpark.swp391.infrastructure.tenant.TenantContext;
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
import com.smartpark.swp391.modules.settlement.service.StaffCashSettlementMapper;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class ManagerShiftSettlementServiceImplTest {

  @Mock StaffCashShiftRepository staffCashShiftRepository;
  @Mock StaffCashTransactionRepository staffCashTransactionRepository;

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
  void listSettlementsReturnsClosedShift() {
    StaffCashShift shift = closedShift();
    when(staffCashShiftRepository.findAll(
            org.mockito.ArgumentMatchers.<Specification<StaffCashShift>>any(),
            org.mockito.ArgumentMatchers.any(Pageable.class)))
        .thenReturn(new PageImpl<>(List.of(shift)));

    var response =
        service()
            .getSettlements(
                data.parking.getId(),
                data.staff.getId(),
                StaffCashShiftStatus.CLOSED,
                null,
                null,
                0,
                20);

    assertThat(response.content()).hasSize(1);
    assertThat(response.content().getFirst().id()).isEqualTo(shift.getId());
    assertThat(response.content().getFirst().varianceAmount()).isEqualByComparingTo("0");
  }

  @Test
  void detailReturnsTransactionsAndTotals() {
    StaffCashShift shift = closedShift();
    StaffCashTransaction transaction =
        StaffCashTransaction.builder()
            .tenant(data.tenant)
            .shift(shift)
            .parking(data.parking)
            .kiosk(data.kiosk)
            .staff(data.staff)
            .type(StaffCashTransactionType.PARKING_CASH)
            .source(StaffCashTransactionSource.NORMAL_EXIT)
            .amount(new BigDecimal("30000"))
            .occurredAt(LocalDateTime.now())
            .build();
    transaction.setId(UUID.randomUUID());
    when(staffCashShiftRepository.findByTenantIdAndId(data.tenant.getId(), shift.getId()))
        .thenReturn(Optional.of(shift));
    when(staffCashTransactionRepository.findByTenantIdAndShiftIdOrderByOccurredAtDesc(
            data.tenant.getId(), shift.getId()))
        .thenReturn(List.of(transaction));

    var response = service().getSettlement(shift.getId());

    assertThat(response.shift().expectedCashAmount()).isEqualByComparingTo("30000");
    assertThat(response.transactions()).hasSize(1);
    assertThat(response.transactions().getFirst().type())
        .isEqualTo(StaffCashTransactionType.PARKING_CASH);
  }

  private ManagerShiftSettlementServiceImpl service() {
    return new ManagerShiftSettlementServiceImpl(
        staffCashShiftRepository, staffCashTransactionRepository, new StaffCashSettlementMapper());
  }

  private StaffCashShift closedShift() {
    StaffCashShift shift =
        StaffCashShift.builder()
            .tenant(data.tenant)
            .staff(data.staff)
            .parking(data.parking)
            .kiosk(data.kiosk)
            .openedAt(LocalDateTime.now().minusHours(8))
            .closedAt(LocalDateTime.now())
            .status(StaffCashShiftStatus.CLOSED)
            .expectedCashAmount(new BigDecimal("30000"))
            .countedCashAmount(new BigDecimal("30000"))
            .varianceAmount(BigDecimal.ZERO)
            .onlineAmount(new BigDecimal("70000"))
            .cashParkingAmount(new BigDecimal("30000"))
            .surchargeCashAmount(BigDecimal.ZERO)
            .penaltyCashAmount(BigDecimal.ZERO)
            .lostCardCashAmount(BigDecimal.ZERO)
            .transactionCount(1)
            .build();
    shift.setId(UUID.randomUUID());
    return shift;
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
    return new TestData(tenant, staff, parking, kiosk);
  }

  private record TestData(Tenant tenant, User staff, Parking parking, Kiosk kiosk) {}
}
