package com.smartpark.swp391.modules.manager.dto.pricing;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartpark.swp391.modules.pricing.enumType.PricingRuleStatus;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;

@JsonTest
class ManagerPricingRuleJsonTest {

  @Autowired ObjectMapper objectMapper;

  @Test
  void legacyDailyCapPropertyIsIgnoredByConfiguredJacksonMapper() throws Exception {
    assertThat(objectMapper.isEnabled(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)).isFalse();

    ManagerPricingRuleRequest request =
        objectMapper.readValue(
            """
            {
              "name": "Legacy client request",
              "parkingId": null,
              "vehicleTypeId": "00000000-0000-0000-0000-000000000001",
              "freeMinutes": 0,
              "firstBlockMinutes": 60,
              "firstBlockPrice": 5000,
              "nextBlockMinutes": 60,
              "nextBlockPrice": 5000,
              "dailyCapPrice": 50000,
              "graceMinutesAfterPayment": 15,
              "status": "ACTIVE"
            }
            """,
            ManagerPricingRuleRequest.class);

    assertThat(request.name()).isEqualTo("Legacy client request");
    assertThat(request.nextBlockPrice()).isEqualByComparingTo("5000");
  }

  @Test
  void managerPricingResponseDoesNotExposeDailyCapProperty() throws Exception {
    String json =
        objectMapper.writeValueAsString(
            ManagerPricingRuleResponse.builder()
                .id(UUID.randomUUID())
                .name("Rule")
                .vehicleTypeId(UUID.randomUUID())
                .vehicleTypeCode("CAR")
                .vehicleTypeName("Car")
                .freeMinutes(0)
                .firstBlockMinutes(60)
                .firstBlockPrice(new BigDecimal("5000"))
                .nextBlockMinutes(60)
                .nextBlockPrice(new BigDecimal("5000"))
                .graceMinutesAfterPayment(15)
                .status(PricingRuleStatus.ACTIVE)
                .build());

    assertThat(json).doesNotContain("dailyCapPrice");
  }
}
