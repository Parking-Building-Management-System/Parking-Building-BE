package com.smartpark.swp391.modules.settlement.dto;

import com.smartpark.swp391.modules.settlement.enumType.StaffCashShiftStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Builder;

@Builder
public record ManagerShiftSettlementListItemResponse(
    UUID id,
    UUID staffId,
    String staffName,
    String staffUsername,
    UUID parkingId,
    String parkingName,
    UUID kioskId,
    String kioskName,
    LocalDateTime openedAt,
    LocalDateTime closedAt,
    StaffCashShiftStatus status,
    BigDecimal expectedCashAmount,
    BigDecimal countedCashAmount,
    BigDecimal varianceAmount,
    BigDecimal onlineAmount,
    Integer transactionCount) {}
