package com.smartpark.swp391.modules.manager.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.smartpark.swp391.common.exception.ApiException;
import com.smartpark.swp391.infrastructure.tenant.TenantContext;
import com.smartpark.swp391.modules.identity.entity.Tenant;
import com.smartpark.swp391.modules.identity.repository.TenantRepository;
import com.smartpark.swp391.modules.manager.dto.penalty.ManagerPenaltyRuleRequest;
import com.smartpark.swp391.modules.manager.dto.penalty.ManagerPenaltyRuleStatusRequest;
import com.smartpark.swp391.modules.parking.entity.Parking;
import com.smartpark.swp391.modules.parking.repository.ParkingRepository;
import com.smartpark.swp391.modules.penalty.entity.PenaltyRule;
import com.smartpark.swp391.modules.penalty.enumType.PenaltyRuleStatus;
import com.smartpark.swp391.modules.penalty.enumType.PenaltyType;
import com.smartpark.swp391.modules.penalty.repository.PenaltyRuleRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class ManagerPenaltyRuleServiceImplTest {

  @Mock PenaltyRuleRepository penaltyRuleRepository;
  @Mock ParkingRepository parkingRepository;
  @Mock TenantRepository tenantRepository;

  Tenant tenant;
  Parking parking;

  @BeforeEach
  void setUp() {
    tenant = Tenant.builder().name("Tenant").slug("tenant").emailContact("a@b.com").build();
    tenant.setId(UUID.randomUUID());
    parking = Parking.builder().tenant(tenant).code("P1").name("Parking 1").build();
    parking.setId(UUID.randomUUID());
    TenantContext.setTenantId(tenant.getId());
  }

  @AfterEach
  void tearDown() {
    TenantContext.clear();
  }

  @Test
  void createRuleNormalizesDefaultsAndPersistsTenantScopedRule() {
    when(tenantRepository.getReferenceById(tenant.getId())).thenReturn(tenant);
    when(penaltyRuleRepository.existsActiveScope(
            tenant.getId(), null, PenaltyType.LOST_CARD, PenaltyRuleStatus.ACTIVE, null))
        .thenReturn(false);
    when(penaltyRuleRepository.save(any(PenaltyRule.class))).thenAnswer(this::saveRule);

    var response =
        service()
            .createRule(
                new ManagerPenaltyRuleRequest(
                    null,
                    " Lost RFID card ",
                    null,
                    PenaltyType.LOST_CARD,
                    new BigDecimal("100000"),
                    null,
                    null,
                    " Required at exit ",
                    null));

    ArgumentCaptor<PenaltyRule> captor = ArgumentCaptor.forClass(PenaltyRule.class);
    verify(penaltyRuleRepository).save(captor.capture());
    PenaltyRule saved = captor.getValue();
    assertThat(saved.getTenant()).isEqualTo(tenant);
    assertThat(saved.getCode()).isEqualTo("LOST_CARD");
    assertThat(saved.getName()).isEqualTo("Lost RFID card");
    assertThat(saved.getCurrency()).isEqualTo("VND");
    assertThat(saved.isRequiresPhoto()).isTrue();
    assertThat(saved.getStatus()).isEqualTo(PenaltyRuleStatus.ACTIVE);
    assertThat(response.id()).isEqualTo(saved.getId());
    assertThat(response.amount()).isEqualByComparingTo("100000");
  }

  @Test
  void createRuleRejectsDuplicateActiveScope() {
    when(tenantRepository.getReferenceById(tenant.getId())).thenReturn(tenant);
    when(penaltyRuleRepository.existsActiveScope(
            tenant.getId(),
            null,
            PenaltyType.OCCUPIED_ASSIGNED_SLOT,
            PenaltyRuleStatus.ACTIVE,
            null))
        .thenReturn(true);

    assertThatThrownBy(
            () ->
                service()
                    .createRule(
                        new ManagerPenaltyRuleRequest(
                            null,
                            "Occupied assigned slot",
                            null,
                            PenaltyType.OCCUPIED_ASSIGNED_SLOT,
                            new BigDecimal("50000"),
                            "VND",
                            true,
                            null,
                            PenaltyRuleStatus.ACTIVE)))
        .isInstanceOf(ApiException.class)
        .hasMessage("Active penalty rule already exists for this parking scope and type");
  }

  @Test
  void getRulesReturnsPagedDtos() {
    PenaltyRule rule = rule(PenaltyType.OCCUPIED_ASSIGNED_SLOT, new BigDecimal("50000"));
    when(penaltyRuleRepository.findAll(
            ArgumentMatchers.<Specification<PenaltyRule>>any(), any(Pageable.class)))
        .thenReturn(new PageImpl<>(List.of(rule)));

    var response = service().getRules(null, null, null, 0, 10);

    assertThat(response.content()).hasSize(1);
    assertThat(response.content().getFirst().type()).isEqualTo(PenaltyType.OCCUPIED_ASSIGNED_SLOT);
    assertThat(response.totalElements()).isEqualTo(1);
  }

  @Test
  void updateStatusDisablesRuleWithoutDuplicateCheck() {
    PenaltyRule rule = rule(PenaltyType.LOST_CARD, new BigDecimal("100000"));
    when(penaltyRuleRepository.findDetailByIdAndTenantId(rule.getId(), tenant.getId()))
        .thenReturn(Optional.of(rule));
    when(penaltyRuleRepository.save(rule)).thenAnswer(this::saveRule);

    var response =
        service().updateStatus(rule.getId(), new ManagerPenaltyRuleStatusRequest(PenaltyRuleStatus.INACTIVE));

    assertThat(response.status()).isEqualTo(PenaltyRuleStatus.INACTIVE);
    verify(penaltyRuleRepository).save(rule);
  }

  @Test
  void deleteRuleSoftDeletesAndInactivatesRule() {
    PenaltyRule rule = rule(PenaltyType.LOST_CARD, new BigDecimal("100000"));
    when(penaltyRuleRepository.findDetailByIdAndTenantId(rule.getId(), tenant.getId()))
        .thenReturn(Optional.of(rule));

    service().deleteRule(rule.getId());

    assertThat(rule.isDeleted()).isTrue();
    assertThat(rule.getStatus()).isEqualTo(PenaltyRuleStatus.INACTIVE);
    verify(penaltyRuleRepository).save(rule);
  }

  @Test
  void createParkingScopedRuleValidatesParkingScope() {
    when(tenantRepository.getReferenceById(tenant.getId())).thenReturn(tenant);
    when(parkingRepository.findByIdAndTenantIdAndIsDeletedFalse(parking.getId(), tenant.getId()))
        .thenReturn(Optional.of(parking));
    when(penaltyRuleRepository.existsActiveScope(
            tenant.getId(),
            parking.getId(),
            PenaltyType.OCCUPIED_ASSIGNED_SLOT,
            PenaltyRuleStatus.ACTIVE,
            null))
        .thenReturn(false);
    when(penaltyRuleRepository.save(any(PenaltyRule.class))).thenAnswer(this::saveRule);

    var response =
        service()
            .createRule(
                new ManagerPenaltyRuleRequest(
                    "slot_violation",
                    "Slot occupied",
                    parking.getId(),
                    PenaltyType.OCCUPIED_ASSIGNED_SLOT,
                    new BigDecimal("50000"),
                    "vnd",
                    true,
                    null,
                    PenaltyRuleStatus.ACTIVE));

    assertThat(response.parkingId()).isEqualTo(parking.getId());
    assertThat(response.code()).isEqualTo("SLOT_VIOLATION");
    verify(parkingRepository).findByIdAndTenantIdAndIsDeletedFalse(parking.getId(), tenant.getId());
    verify(penaltyRuleRepository)
        .existsActiveScope(
            eq(tenant.getId()),
            eq(parking.getId()),
            eq(PenaltyType.OCCUPIED_ASSIGNED_SLOT),
            eq(PenaltyRuleStatus.ACTIVE),
            eq(null));
  }

  private ManagerPenaltyRuleServiceImpl service() {
    return new ManagerPenaltyRuleServiceImpl(
        penaltyRuleRepository, parkingRepository, tenantRepository);
  }

  private PenaltyRule rule(PenaltyType type, BigDecimal amount) {
    PenaltyRule rule =
        PenaltyRule.builder()
            .tenant(tenant)
            .parking(null)
            .code(type.name())
            .name(type.name())
            .type(type)
            .amount(amount)
            .currency("VND")
            .requiresPhoto(true)
            .status(PenaltyRuleStatus.ACTIVE)
            .build();
    rule.setId(UUID.randomUUID());
    return rule;
  }

  private PenaltyRule saveRule(org.mockito.invocation.InvocationOnMock invocation) {
    PenaltyRule rule = invocation.getArgument(0);
    if (rule.getId() == null) {
      rule.setId(UUID.randomUUID());
    }
    return rule;
  }
}
