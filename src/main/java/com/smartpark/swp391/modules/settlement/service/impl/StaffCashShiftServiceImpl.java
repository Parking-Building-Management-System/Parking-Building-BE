package com.smartpark.swp391.modules.settlement.service.impl;

import com.smartpark.swp391.common.exception.ApiException;
import com.smartpark.swp391.common.exception.ErrorCode;
import com.smartpark.swp391.modules.identity.entity.Tenant;
import com.smartpark.swp391.modules.identity.entity.User;
import com.smartpark.swp391.modules.operation.entity.Kiosk;
import com.smartpark.swp391.modules.parking.entity.Parking;
import com.smartpark.swp391.modules.payment.enumType.PaymentIntentStatus;
import com.smartpark.swp391.modules.payment.enumType.PaymentProvider;
import com.smartpark.swp391.modules.payment.repository.PaymentIntentRepository;
import com.smartpark.swp391.modules.settlement.dto.StaffCashSettlementPreviewResponse;
import com.smartpark.swp391.modules.settlement.dto.StaffCashShiftCloseRequest;
import com.smartpark.swp391.modules.settlement.dto.StaffCashShiftResponse;
import com.smartpark.swp391.modules.settlement.dto.StaffCurrentCashShiftResponse;
import com.smartpark.swp391.modules.settlement.entity.StaffCashShift;
import com.smartpark.swp391.modules.settlement.entity.StaffCashTransaction;
import com.smartpark.swp391.modules.settlement.enumType.StaffCashShiftStatus;
import com.smartpark.swp391.modules.settlement.enumType.StaffCashTransactionType;
import com.smartpark.swp391.modules.settlement.repository.StaffCashShiftRepository;
import com.smartpark.swp391.modules.settlement.repository.StaffCashTransactionRepository;
import com.smartpark.swp391.modules.settlement.service.StaffCashSettlementMapper;
import com.smartpark.swp391.modules.settlement.service.StaffCashShiftService;
import com.smartpark.swp391.modules.staff.dto.StaffResolvedContext;
import com.smartpark.swp391.modules.staff.service.StaffWorkContextService;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class StaffCashShiftServiceImpl implements StaffCashShiftService {

  StaffWorkContextService staffWorkContextService;
  StaffCashShiftRepository staffCashShiftRepository;
  StaffCashTransactionRepository staffCashTransactionRepository;
  PaymentIntentRepository paymentIntentRepository;
  StaffCashSettlementMapper mapper;
  EntityManager entityManager;

  @Override
  @Transactional
  public StaffCashShiftResponse startShift() {
    StaffResolvedContext context = staffWorkContextService.requireCurrentResolvedContext();
    return staffCashShiftRepository
        .findFirstByTenantIdAndStaffIdAndStatusOrderByOpenedAtDesc(
            context.tenantId(), context.staffId(), StaffCashShiftStatus.OPEN)
        .map(mapper::toShiftResponse)
        .orElseGet(() -> mapper.toShiftResponse(createOpenShift(context, null)));
  }

  @Override
  @Transactional(readOnly = true)
  public StaffCurrentCashShiftResponse getCurrentShift() {
    StaffResolvedContext context = staffWorkContextService.requireCurrentResolvedContext();
    return staffCashShiftRepository
        .findFirstByTenantIdAndStaffIdAndStatusOrderByOpenedAtDesc(
            context.tenantId(), context.staffId(), StaffCashShiftStatus.OPEN)
        .map(
            shift ->
                StaffCurrentCashShiftResponse.builder()
                    .hasOpenShift(true)
                    .shift(mapper.toShiftResponse(shift))
                    .build())
        .orElseGet(
            () -> StaffCurrentCashShiftResponse.builder().hasOpenShift(false).shift(null).build());
  }

  @Override
  @Transactional(readOnly = true)
  public StaffCashSettlementPreviewResponse getCurrentSettlementPreview() {
    StaffResolvedContext context = staffWorkContextService.requireCurrentResolvedContext();
    StaffCashShift shift = requireOpenShift(context);
    LocalDateTime now = LocalDateTime.now();
    SettlementTotals totals = calculateTotals(context.tenantId(), shift, now);

    return StaffCashSettlementPreviewResponse.builder()
        .shift(mapper.toShiftResponse(shift))
        .expectedCashAmount(totals.expectedCashAmount())
        .onlineAmount(totals.onlineAmount())
        .cashParkingAmount(totals.cashParkingAmount())
        .surchargeCashAmount(totals.surchargeCashAmount())
        .penaltyCashAmount(totals.penaltyCashAmount())
        .lostCardCashAmount(totals.lostCardCashAmount())
        .transactionCount(totals.transactionCount())
        .recentTransactions(
            staffCashTransactionRepository
                .findByTenantIdAndShiftIdOrderByOccurredAtDesc(
                    context.tenantId(), shift.getId(), PageRequest.of(0, 20))
                .stream()
                .map(mapper::toTransactionResponse)
                .toList())
        .build();
  }

  @Override
  @Transactional
  public StaffCashShiftResponse closeCurrentShift(StaffCashShiftCloseRequest request) {
    StaffResolvedContext context = staffWorkContextService.requireCurrentResolvedContext();
    StaffCashShift shift =
        staffCashShiftRepository
            .findOpenForStaffForUpdate(context.tenantId(), context.staffId())
            .orElseThrow(
                () -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "OPEN_SHIFT_NOT_FOUND"));
    if (shift.getStatus() != StaffCashShiftStatus.OPEN) {
      throw new ApiException(ErrorCode.INVALID_INPUT, "SHIFT_ALREADY_CLOSED");
    }
    LocalDateTime closedAt = LocalDateTime.now();
    SettlementTotals totals = calculateTotals(context.tenantId(), shift, closedAt);
    BigDecimal countedCashAmount = request.countedCashAmount();

    shift.setExpectedCashAmount(totals.expectedCashAmount());
    shift.setCountedCashAmount(countedCashAmount);
    shift.setVarianceAmount(countedCashAmount.subtract(totals.expectedCashAmount()));
    shift.setOnlineAmount(totals.onlineAmount());
    shift.setCashParkingAmount(totals.cashParkingAmount());
    shift.setSurchargeCashAmount(totals.surchargeCashAmount());
    shift.setPenaltyCashAmount(totals.penaltyCashAmount());
    shift.setLostCardCashAmount(totals.lostCardCashAmount());
    shift.setTransactionCount(totals.transactionCount());
    shift.setClosedAt(closedAt);
    shift.setStatus(StaffCashShiftStatus.CLOSED);
    shift.setNote(trimToNull(request.note()));

    return mapper.toShiftResponse(staffCashShiftRepository.save(shift));
  }

  private StaffCashShift createOpenShift(StaffResolvedContext context, String note) {
    try {
      return staffCashShiftRepository.save(
          StaffCashShift.builder()
              .tenant(entityManager.getReference(Tenant.class, context.tenantId()))
              .staff(entityManager.getReference(User.class, context.staffId()))
              .parking(entityManager.getReference(Parking.class, context.parkingId()))
              .kiosk(entityManager.getReference(Kiosk.class, context.kioskId()))
              .openedAt(LocalDateTime.now())
              .status(StaffCashShiftStatus.OPEN)
              .expectedCashAmount(BigDecimal.ZERO)
              .onlineAmount(BigDecimal.ZERO)
              .cashParkingAmount(BigDecimal.ZERO)
              .surchargeCashAmount(BigDecimal.ZERO)
              .penaltyCashAmount(BigDecimal.ZERO)
              .lostCardCashAmount(BigDecimal.ZERO)
              .transactionCount(0)
              .note(trimToNull(note))
              .build());
    } catch (DataIntegrityViolationException e) {
      return staffCashShiftRepository
          .findFirstByTenantIdAndStaffIdAndStatusOrderByOpenedAtDesc(
              context.tenantId(), context.staffId(), StaffCashShiftStatus.OPEN)
          .orElseThrow(() -> e);
    }
  }

  private StaffCashShift requireOpenShift(StaffResolvedContext context) {
    return staffCashShiftRepository
        .findFirstByTenantIdAndStaffIdAndStatusOrderByOpenedAtDesc(
            context.tenantId(), context.staffId(), StaffCashShiftStatus.OPEN)
        .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "OPEN_SHIFT_NOT_FOUND"));
  }

  private SettlementTotals calculateTotals(UUID tenantId, StaffCashShift shift, LocalDateTime to) {
    var transactions =
        staffCashTransactionRepository.findByTenantIdAndShiftIdOrderByOccurredAtDesc(
            tenantId, shift.getId());
    BigDecimal cashParkingAmount = sum(transactions, StaffCashTransactionType.PARKING_CASH);
    BigDecimal surchargeCashAmount = sum(transactions, StaffCashTransactionType.SURCHARGE_CASH);
    BigDecimal penaltyCashAmount = sum(transactions, StaffCashTransactionType.PENALTY_CASH);
    BigDecimal lostCardCashAmount = sum(transactions, StaffCashTransactionType.LOST_CARD_FINE);
    BigDecimal expectedCashAmount =
        transactions.stream()
            .map(StaffCashTransaction::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal onlineAmount =
        paymentIntentRepository.sumAmountByParkingAndPaidAtRange(
            tenantId,
            shift.getParking().getId(),
            PaymentIntentStatus.PAID,
            PaymentProvider.PAYOS,
            shift.getOpenedAt(),
            to);
    if (onlineAmount == null) {
      onlineAmount = BigDecimal.ZERO;
    }
    return new SettlementTotals(
        expectedCashAmount,
        onlineAmount,
        cashParkingAmount,
        surchargeCashAmount,
        penaltyCashAmount,
        lostCardCashAmount,
        transactions.size());
  }

  private BigDecimal sum(
      java.util.List<StaffCashTransaction> transactions, StaffCashTransactionType type) {
    return transactions.stream()
        .filter(transaction -> transaction.getType() == type)
        .map(StaffCashTransaction::getAmount)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  private String trimToNull(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }

  private record SettlementTotals(
      BigDecimal expectedCashAmount,
      BigDecimal onlineAmount,
      BigDecimal cashParkingAmount,
      BigDecimal surchargeCashAmount,
      BigDecimal penaltyCashAmount,
      BigDecimal lostCardCashAmount,
      int transactionCount) {}
}
