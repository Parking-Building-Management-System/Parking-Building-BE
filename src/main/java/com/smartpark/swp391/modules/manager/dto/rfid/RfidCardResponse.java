package com.smartpark.swp391.modules.manager.dto.rfid;

import com.smartpark.swp391.modules.parking.enumType.RfidCardStatus;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Builder;

@Builder
public record RfidCardResponse(
    UUID id,
    String code,
    String uid,
    String qrToken,
    UUID assignedUserId,
    RfidCardStatus status,
    LocalDateTime activatedAt,
    LocalDateTime expiredAt) {}
