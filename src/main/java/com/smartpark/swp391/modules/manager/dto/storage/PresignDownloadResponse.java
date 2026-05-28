package com.smartpark.swp391.modules.manager.dto.storage;

import lombok.Builder;

@Builder
public record PresignDownloadResponse(String downloadUrl, long expiresInSeconds) {}
