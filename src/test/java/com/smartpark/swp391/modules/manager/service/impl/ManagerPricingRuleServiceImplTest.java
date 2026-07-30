package com.smartpark.swp391.modules.manager.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.smartpark.swp391.infrastructure.tenant.TenantContext;
import com.smartpark.swp391.modules.identity.entity.Tenant;
import com.smartpark.swp391.modules.identity.repository.TenantRepository;
import com.smartpark.swp391.modules.manager.dto.pricing.ManagerPricingRuleRequest;
import com.smartpark.swp391.modules.parking.repository.ParkingRepository;
import com.smartpark.swp391.modules.pricing.entity.PricingRule;
import com.smartpark.swp391.modules.pricing.enumType.PricingRuleStatus;
import com.smartpark.swp391.modules.pricing.repository.PricingRuleRepository;
import com.smartpark.swp391.modules.pricing.service.PricingQuoteService;
import com.smartpark.swp391.modules.vehicle.entity.VehicleType;
import com.smartpark.swp391.modules.vehicle.repository.VehicleTypeRepository;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ManagerPricingRuleServiceImplTest {

  @Mock PricingRuleRepository pricingRuleRepository;
  @Mock ParkingRepository parkingRepository;
  @Mock VehicleTypeRepository vehicleTypeRepository;
  @Mock TenantRepository tenantRepository;
  @Mock PricingQuoteService pricingQuoteService;

  private ManagerPricingRuleServiceImpl service;
  private UUID tenantId;
  private VehicleType vehicleType;
  private Tenant tenant;

  @BeforeEach
  void setUp() {
    tenantId = UUID.randomUUID();
    TenantContext.setTenantId(tenantId);
    tenant =
        Tenant.builder().id(tenantId).name("Tenant").slug("tenant").emailContact("a@b.com").build();
    vehicleType =
        VehicleType.builder()
            .id(UUID.randomUUID())
            .code("CAR")
            .name("Car")
            .active(true)
            .deleted(false)
            .build();
    service =
        new ManagerPricingRuleServiceImpl(
            pricingRuleRepository,
            parkingRepository,
            vehicleTypeRepository,
            tenantRepository,
            pricingQuoteService);
  }

  @AfterEach
  void tearDown() {
    TenantContext.clear();
  }

  @Test
  void createRuleWithoutDailyCapSucceeds() {
    when(tenantRepository.getReferenceById(tenantId)).thenReturn(tenant);
    when(vehicleTypeRepository.findById(vehicleType.getId())).thenReturn(Optional.of(vehicleType));
    when(pricingRuleRepository.save(any(PricingRule.class)))
        .thenAnswer(
            invocation -> {
              PricingRule rule = invocation.getArgument(0);
              rule.setId(UUID.randomUUID());
              return rule;
            });

    var response = service.createRule(request("Created rule"));

    assertThat(response.name()).isEqualTo("Created rule");
    assertThat(response.firstBlockPrice()).isEqualByComparingTo("5000");
    assertThat(response.nextBlockPrice()).isEqualByComparingTo("5000");
    assertThat(response.graceMinutesAfterPayment()).isEqualTo(15);

    ArgumentCaptor<PricingRule> ruleCaptor = ArgumentCaptor.forClass(PricingRule.class);
    org.mockito.Mockito.verify(pricingRuleRepository).save(ruleCaptor.capture());
    assertThat(ruleCaptor.getValue().getTenant()).isEqualTo(tenant);
  }

  @Test
  void updateRuleWithoutDailyCapSucceeds() {
    PricingRule existing =
        PricingRule.builder()
            .id(UUID.randomUUID())
            .tenant(tenant)
            .name("Old rule")
            .vehicleType(vehicleType)
            .freeMinutes(10)
            .firstBlockMinutes(120)
            .firstBlockPrice(new BigDecimal("20000"))
            .nextBlockMinutes(60)
            .nextBlockPrice(new BigDecimal("10000"))
            .graceMinutesAfterPayment(10)
            .status(PricingRuleStatus.ACTIVE)
            .build();
    when(pricingRuleRepository.findDetailByIdAndTenantId(existing.getId(), tenantId))
        .thenReturn(Optional.of(existing));
    when(vehicleTypeRepository.findById(vehicleType.getId())).thenReturn(Optional.of(vehicleType));
    when(pricingRuleRepository.save(existing)).thenReturn(existing);

    var response = service.updateRule(existing.getId(), request("Updated rule"));

    assertThat(response.name()).isEqualTo("Updated rule");
    assertThat(response.freeMinutes()).isZero();
    assertThat(response.firstBlockMinutes()).isEqualTo(60);
    assertThat(response.graceMinutesAfterPayment()).isEqualTo(15);
  }

  private ManagerPricingRuleRequest request(String name) {
    return new ManagerPricingRuleRequest(
        name,
        null,
        vehicleType.getId(),
        0,
        60,
        new BigDecimal("5000"),
        60,
        new BigDecimal("5000"),
        15,
        PricingRuleStatus.ACTIVE);
  }
}
