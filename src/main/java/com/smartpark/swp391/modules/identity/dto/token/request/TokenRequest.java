package com.smartpark.swp391.modules.identity.dto.token.request;

import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TokenRequest {
  private UUID userId;
  private UUID tenantId;
  private UUID sessionId;
  private String subject;
  private UUID refreshJti;
}
