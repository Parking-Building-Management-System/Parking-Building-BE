package com.smartpark.swp391.modules.staff.dto.lostcard;

import com.smartpark.swp391.modules.penalty.dto.PenaltyCaseResponse;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.Builder;

@Builder
public record StaffLostCardPreviewResponse(
    UUID sessionId,
    String plateNumber,
    UUID vehicleTypeId,
    String vehicleType,
    UUID parkingId,
    String parkingName,
    UUID zoneId,
    String zoneName,
    UUID slotId,
    String slotCode,
    LocalDateTime checkInAt,
    String entryImageUrl,
    String licensePlateImageUrl,
    BigDecimal parkingAmountDue,
    BigDecimal surchargeAmountDue,
    BigDecimal existingPenaltyAmount,
    BigDecimal lostCardPenaltyAmount,
    BigDecimal totalDueIfLostCard,
    String currency,
    String currentRfidCardCode,
    List<PenaltyCaseResponse> activePenaltyCases) {}
