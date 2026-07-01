package com.smartpark.swp391.modules.settlement.service;

import com.smartpark.swp391.modules.settlement.dto.ManagerShiftSettlementListItemResponse;
import com.smartpark.swp391.modules.settlement.dto.StaffCashShiftResponse;
import com.smartpark.swp391.modules.settlement.dto.StaffCashTransactionResponse;
import com.smartpark.swp391.modules.settlement.entity.StaffCashShift;
import com.smartpark.swp391.modules.settlement.entity.StaffCashTransaction;
import org.springframework.stereotype.Component;

@Component
public class StaffCashSettlementMapper {

  public StaffCashShiftResponse toShiftResponse(StaffCashShift shift) {
    return StaffCashShiftResponse.builder()
        .id(shift.getId())
        .staffId(shift.getStaff().getId())
        .staffName(shift.getStaff().getFullName())
        .staffUsername(shift.getStaff().getUsername())
        .parkingId(shift.getParking().getId())
        .parkingName(shift.getParking().getName())
        .kioskId(shift.getKiosk().getId())
        .kioskName(shift.getKiosk().getName())
        .openedAt(shift.getOpenedAt())
        .closedAt(shift.getClosedAt())
        .status(shift.getStatus())
        .expectedCashAmount(shift.getExpectedCashAmount())
        .countedCashAmount(shift.getCountedCashAmount())
        .varianceAmount(shift.getVarianceAmount())
        .onlineAmount(shift.getOnlineAmount())
        .cashParkingAmount(shift.getCashParkingAmount())
        .surchargeCashAmount(shift.getSurchargeCashAmount())
        .penaltyCashAmount(shift.getPenaltyCashAmount())
        .lostCardCashAmount(shift.getLostCardCashAmount())
        .transactionCount(shift.getTransactionCount())
        .note(shift.getNote())
        .createdAt(shift.getCreatedAt())
        .updatedAt(shift.getUpdatedAt())
        .build();
  }

  public ManagerShiftSettlementListItemResponse toManagerListItem(StaffCashShift shift) {
    return ManagerShiftSettlementListItemResponse.builder()
        .id(shift.getId())
        .staffId(shift.getStaff().getId())
        .staffName(shift.getStaff().getFullName())
        .staffUsername(shift.getStaff().getUsername())
        .parkingId(shift.getParking().getId())
        .parkingName(shift.getParking().getName())
        .kioskId(shift.getKiosk().getId())
        .kioskName(shift.getKiosk().getName())
        .openedAt(shift.getOpenedAt())
        .closedAt(shift.getClosedAt())
        .status(shift.getStatus())
        .expectedCashAmount(shift.getExpectedCashAmount())
        .countedCashAmount(shift.getCountedCashAmount())
        .varianceAmount(shift.getVarianceAmount())
        .onlineAmount(shift.getOnlineAmount())
        .transactionCount(shift.getTransactionCount())
        .build();
  }

  public StaffCashTransactionResponse toTransactionResponse(StaffCashTransaction transaction) {
    return StaffCashTransactionResponse.builder()
        .id(transaction.getId())
        .type(transaction.getType())
        .source(transaction.getSource())
        .amount(transaction.getAmount())
        .occurredAt(transaction.getOccurredAt())
        .parkingSessionId(
            transaction.getParkingSession() == null
                ? null
                : transaction.getParkingSession().getId())
        .penaltyCaseId(
            transaction.getPenaltyCase() == null ? null : transaction.getPenaltyCase().getId())
        .note(transaction.getNote())
        .build();
  }
}
