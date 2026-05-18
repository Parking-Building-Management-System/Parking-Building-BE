package com.smartpark.swp391.modules.identity.dto.token.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TokenPair {
  private String accessToken;
  private String refreshToken;
  private long accessTtl;
  private long refreshTtl;
}
