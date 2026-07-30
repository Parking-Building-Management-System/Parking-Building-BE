package com.smartpark.swp391.modules.identity.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.smartpark.swp391.common.security.annotation.RateLimit;
import com.smartpark.swp391.modules.identity.dto.authentication.request.StaffPasswordResetCreateRequest;
import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;

class StaffPasswordResetRequestControllerTest {

  @Test
  void publicRequestEndpointRetainsPerEmailRateLimit() throws Exception {
    Method method =
        StaffPasswordResetRequestController.class.getMethod(
            "createRequest", StaffPasswordResetCreateRequest.class);
    RateLimit rateLimit = method.getAnnotation(RateLimit.class);

    assertThat(rateLimit).isNotNull();
    assertThat(rateLimit.limit()).isEqualTo(5);
    assertThat(rateLimit.duration()).isEqualTo(60);
    assertThat(rateLimit.type()).isEqualTo(RateLimit.Type.REQUEST_FIELD);
    assertThat(rateLimit.fieldName()).contains("normalizedEmail");
  }

  @Test
  void emailNormalizationTrimsAndLowercases() {
    var request = new StaffPasswordResetCreateRequest("  Staff@Example.COM ");

    assertThat(request.normalizedEmail()).isEqualTo("staff@example.com");
  }
}
