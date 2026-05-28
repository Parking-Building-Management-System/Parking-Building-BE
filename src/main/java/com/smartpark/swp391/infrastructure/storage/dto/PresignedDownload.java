package com.smartpark.swp391.infrastructure.storage.dto;

import lombok.Builder;

@Builder
public record PresignedDownload(String downloadUrl, long expiresInSeconds) {}
