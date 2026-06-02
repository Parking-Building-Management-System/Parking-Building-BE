package com.smartpark.swp391.modules.staff.dto.firesafety;

import com.smartpark.swp391.modules.firesafety.enumType.FireInspectionResult;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.UUID;

public record StaffFireInspectionRequest(
    @NotNull UUID fireExtinguisherId,
    @NotNull FireInspectionResult result,
    Boolean pressureOk,
    Boolean sealOk,
    Boolean locationOk,
    Boolean expiryOk,
    String photoUrl,
    String note,
    LocalDateTime nextInspectionAt) {}
