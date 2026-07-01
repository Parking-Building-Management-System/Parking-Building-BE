package com.smartpark.swp391.modules.staff.dto.lostcard;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record StaffLostCardCaseRequest(
    @NotNull UUID sessionId,
    @NotBlank @Size(max = 1000) String identityImageUrl,
    @NotBlank @Size(max = 1000) String vehicleImageUrl,
    @NotBlank @Size(max = 1000) String licensePlateImageUrl,
    @Size(max = 1000) String note) {}
