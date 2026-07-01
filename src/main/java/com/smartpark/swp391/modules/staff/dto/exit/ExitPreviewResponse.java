package com.smartpark.swp391.modules.staff.dto.exit;

import com.smartpark.swp391.modules.operation.enumType.SessionPaymentStatus;
import com.smartpark.swp391.modules.penalty.dto.PenaltyCaseResponse;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.Builder;

@Builder
public record ExitPreviewResponse(
    UUID sessionId,
    String plateNumber,
    String cardCode,
    String parkingName,
    String floorName,
    String zoneName,
    String slotCode,
    String entryImageUrl,
    String licensePlateImageUrl,
    LocalDateTime checkInAt,
    LocalDateTime paidAt,
    LocalDateTime exitDeadline,
    SessionPaymentStatus paymentStatus,
    ExitDecision exitDecision,
    BigDecimal amountDue,
    BigDecimal surchargeAmount,
    BigDecimal totalAmount,
    BigDecimal parkingAmountDue,
    BigDecimal surchargeAmountDue,
    BigDecimal penaltyAmountDue,
    BigDecimal totalAmountDue,
    List<PenaltyCaseResponse> penaltyCases,
    boolean hasUnpaidPenalties,
    String currency,
    String message) {}
