package com.smartpark.swp391.modules.settlement.service.impl;

import com.smartpark.swp391.common.exception.ApiException;
import com.smartpark.swp391.common.exception.ErrorCode;
import com.smartpark.swp391.modules.identity.entity.Tenant;
import com.smartpark.swp391.modules.identity.entity.User;
import com.smartpark.swp391.modules.operation.entity.Kiosk;
import com.smartpark.swp391.modules.parking.entity.Parking;
import com.smartpark.swp391.modules.settlement.entity.StaffCashShift;
import com.smartpark.swp391.modules.settlement.entity.StaffCashTransaction;
import com.smartpark.swp391.modules.settlement.enumType.StaffCashShiftStatus;
import com.smartpark.swp391.modules.settlement.repository.StaffCashShiftRepository;
import com.smartpark.swp391.modules.settlement.repository.StaffCashTransactionRepository;
import com.smartpark.swp391.modules.settlement.service.StaffCashLedgerEntry;
import com.smartpark.swp391.modules.settlement.service.StaffCashLedgerService;
import com.smartpark.swp391.modules.staff.dto.StaffResolvedContext;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class StaffCashLedgerServiceImpl implements StaffCashLedgerService {

  StaffCashShiftRepository staffCashShiftRepository;
  StaffCashTransactionRepository staffCashTransactionRepository;
  EntityManager entityManager;

  @Override
  @Transactional
  public void recordCashTransactions(
      StaffResolvedContext context, List<StaffCashLedgerEntry> entries) {
    List<StaffCashLedgerEntry> billableEntries =
        entries.stream().filter(this::isPositiveCashEntry).toList();
    if (billableEntries.isEmpty()) {
      return;
    }

    StaffCashShift shift = getOrAutoOpenShift(context);
    if (shift.getStatus() != StaffCashShiftStatus.OPEN) {
      throw new ApiException(ErrorCode.INVALID_INPUT, "CLOSED_SHIFT_CANNOT_ACCEPT_CASH");
    }
    requireShiftMatchesContext(shift, context);

    LocalDateTime now = LocalDateTime.now();
    List<StaffCashTransaction> transactions =
        billableEntries.stream().map(entry -> toTransaction(context, shift, entry, now)).toList();
    staffCashTransactionRepository.saveAll(transactions);
  }

  private boolean isPositiveCashEntry(StaffCashLedgerEntry entry) {
    if (entry == null || entry.amount() == null) {
      return false;
    }
    if (entry.amount().signum() < 0) {
      throw new ApiException(ErrorCode.INVALID_INPUT, "CASH_TRANSACTION_AMOUNT_MUST_BE_POSITIVE");
    }
    if (entry.type() == null || entry.source() == null) {
      throw new ApiException(ErrorCode.INVALID_INPUT, "CASH_TRANSACTION_CLASSIFICATION_REQUIRED");
    }
    return entry.amount().signum() > 0;
  }

  private StaffCashShift getOrAutoOpenShift(StaffResolvedContext context) {
    return staffCashShiftRepository
        .findOpenForStaffForUpdate(context.tenantId(), context.staffId())
        .orElseGet(() -> staffCashShiftRepository.save(newAutoOpenedShift(context)));
  }

  private StaffCashShift newAutoOpenedShift(StaffResolvedContext context) {
    return StaffCashShift.builder()
        .tenant(entityManager.getReference(Tenant.class, context.tenantId()))
        .staff(entityManager.getReference(User.class, context.staffId()))
        .parking(entityManager.getReference(Parking.class, context.parkingId()))
        .kiosk(entityManager.getReference(Kiosk.class, context.kioskId()))
        .openedAt(LocalDateTime.now())
        .status(StaffCashShiftStatus.OPEN)
        .note("Auto-opened by cash collection")
        .expectedCashAmount(BigDecimal.ZERO)
        .onlineAmount(BigDecimal.ZERO)
        .cashParkingAmount(BigDecimal.ZERO)
        .surchargeCashAmount(BigDecimal.ZERO)
        .penaltyCashAmount(BigDecimal.ZERO)
        .lostCardCashAmount(BigDecimal.ZERO)
        .transactionCount(0)
        .build();
  }

  private void requireShiftMatchesContext(StaffCashShift shift, StaffResolvedContext context) {
    if (!shift.getParking().getId().equals(context.parkingId())
        || !shift.getKiosk().getId().equals(context.kioskId())) {
      throw new ApiException(ErrorCode.FORBIDDEN_ACTION, "OPEN_SHIFT_IS_FOR_DIFFERENT_KIOSK");
    }
  }

  private StaffCashTransaction toTransaction(
      StaffResolvedContext context,
      StaffCashShift shift,
      StaffCashLedgerEntry entry,
      LocalDateTime occurredAt) {
    return StaffCashTransaction.builder()
        .tenant(entityManager.getReference(Tenant.class, context.tenantId()))
        .shift(shift)
        .parking(shift.getParking())
        .kiosk(shift.getKiosk())
        .staff(shift.getStaff())
        .parkingSession(entry.parkingSession())
        .penaltyCase(entry.penaltyCase())
        .type(entry.type())
        .amount(entry.amount())
        .occurredAt(occurredAt)
        .source(entry.source())
        .note(trimToNull(entry.note()))
        .build();
  }

  private String trimToNull(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }
}
