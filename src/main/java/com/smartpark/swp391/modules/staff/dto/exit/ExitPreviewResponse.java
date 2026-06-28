package com.smartpark.swp391.modules.staff.dto.exit;

import com.smartpark.swp391.modules.operation.enumType.SessionPaymentStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
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
    String currency,
    String message) {}
