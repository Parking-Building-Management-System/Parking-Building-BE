package com.smartpark.swp391.modules.staff.dto.exit;

import jakarta.validation.constraints.NotBlank;

public record ExitPreviewRequest(@NotBlank String cardCode) {}
