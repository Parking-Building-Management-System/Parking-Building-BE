package com.smartpark.swp391.modules.manager.dto.firesafety;

import com.smartpark.swp391.modules.firesafety.enumType.FireInspectionResult;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Builder;

@Builder
public record FireInspectionLogResponse(
    UUID id,
    UUID fireExtinguisherId,
    String fireExtinguisherCode,
    UUID parkingId,
    String parkingName,
    UUID floorId,
    String floorName,
    UUID zoneId,
    String zoneName,
    UUID inspectedBy,
    String inspectedByName,
    FireInspectionResult result,
    Boolean pressureOk,
    Boolean sealOk,
    Boolean locationOk,
    Boolean expiryOk,
    String photoUrl,
    String photoObjectKey,
    String photoDisplayUrl,
    Long photoUrlExpiresInSeconds,
    String note,
    LocalDateTime inspectedAt,
    LocalDateTime nextInspectionAt) {}
