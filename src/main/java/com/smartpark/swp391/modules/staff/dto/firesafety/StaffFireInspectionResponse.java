package com.smartpark.swp391.modules.staff.dto.firesafety;

import com.smartpark.swp391.modules.firesafety.enumType.FireExtinguisherStatus;
import com.smartpark.swp391.modules.firesafety.enumType.FireInspectionResult;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Builder;

@Builder
public record StaffFireInspectionResponse(
    UUID inspectionId,
    UUID fireExtinguisherId,
    String code,
    FireInspectionResult result,
    FireExtinguisherStatus status,
    LocalDateTime inspectedAt,
    LocalDateTime nextInspectionAt) {}
