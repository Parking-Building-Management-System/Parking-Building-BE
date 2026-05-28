package com.smartpark.swp391.modules.admin.dto.security;

import jakarta.validation.constraints.Size;

public record SecurityActionRequest(@Size(max = 1000) String reason) {}
