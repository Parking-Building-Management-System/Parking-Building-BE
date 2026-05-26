package com.smartpark.swp391.modules.manager.dto.rfid;

import lombok.Builder;

@Builder
public record RfidCardGenerateResponse(int requestedCount, int createdCount, int existingCount) {}
