package com.smartpark.swp391.modules.settlement.dto;

import com.smartpark.swp391.modules.settlement.enumType.StaffCashTransactionSource;
import com.smartpark.swp391.modules.settlement.enumType.StaffCashTransactionType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Builder;

@Builder
public record StaffCashTransactionResponse(
    UUID id,
    StaffCashTransactionType type,
    StaffCashTransactionSource source,
    BigDecimal amount,
    LocalDateTime occurredAt,
    UUID parkingSessionId,
    UUID penaltyCaseId,
    String note) {}
