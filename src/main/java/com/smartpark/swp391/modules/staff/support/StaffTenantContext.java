package com.smartpark.swp391.modules.staff.support;

import com.smartpark.swp391.common.exception.ApiException;
import com.smartpark.swp391.common.exception.ErrorCode;
import com.smartpark.swp391.infrastructure.tenant.TenantContext;
import java.util.UUID;
import java.util.function.Supplier;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component
public class StaffTenantContext {

  public <T> T call(Jwt jwt, Supplier<T> supplier) {
    TenantContext.setTenantId(extractTenantId(jwt));
    try {
      return supplier.get();
    } finally {
      TenantContext.clear();
    }
  }

  private UUID extractTenantId(Jwt jwt) {
    if (jwt == null || jwt.getClaimAsString("tenant_id") == null) {
      throw new ApiException(ErrorCode.UNAUTHENTICATED);
    }
    try {
      return UUID.fromString(jwt.getClaimAsString("tenant_id"));
    } catch (IllegalArgumentException e) {
      throw new ApiException(ErrorCode.UNAUTHENTICATED);
    }
  }
}
