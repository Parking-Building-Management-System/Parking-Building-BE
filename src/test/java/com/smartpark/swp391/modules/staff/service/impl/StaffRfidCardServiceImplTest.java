package com.smartpark.swp391.modules.staff.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.smartpark.swp391.infrastructure.tenant.TenantContext;
import com.smartpark.swp391.modules.identity.entity.Tenant;
import com.smartpark.swp391.modules.parking.entity.RfidCard;
import com.smartpark.swp391.modules.parking.enumType.RfidCardStatus;
import com.smartpark.swp391.modules.parking.repository.RfidCardRepository;
import com.smartpark.swp391.modules.staff.dto.AvailableRfidCardResponse;
import com.smartpark.swp391.modules.staff.dto.StaffWorkContextResponse;
import com.smartpark.swp391.modules.staff.service.StaffWorkContextService;
import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class StaffRfidCardServiceImplTest {

  @Mock RfidCardRepository rfidCardRepository;
  @Mock StaffWorkContextService staffWorkContextService;

  private final UUID tenantId = UUID.randomUUID();
  private StaffRfidCardServiceImpl service;

  @BeforeEach
  void setUp() {
    TenantContext.setTenantId(tenantId);
    when(staffWorkContextService.requireCurrentContext())
        .thenReturn(StaffWorkContextResponse.builder().parkingId(UUID.randomUUID()).build());
    service = new StaffRfidCardServiceImpl(rfidCardRepository, staffWorkContextService);
  }

  @AfterEach
  void clearTenantContext() {
    TenantContext.clear();
  }

  @Test
  void blankAndWhitespaceSearchUseNormalAvailableResults() {
    when(rfidCardRepository.findAvailableForStaffParking(
            eq(tenantId), eq(RfidCardStatus.ACTIVE), any()))
        .thenReturn(List.of());

    service.getAvailableCards("", 50);
    service.getAvailableCards("   ", 50);

    verify(rfidCardRepository, org.mockito.Mockito.times(2))
        .findAvailableForStaffParking(eq(tenantId), eq(RfidCardStatus.ACTIVE), any());
    verify(rfidCardRepository, never()).searchAvailableForStaffParking(any(), any(), any(), any());
  }

  @Test
  void searchIsTrimmedAndLimitIsCappedAtSafeMaximum() {
    when(rfidCardRepository.searchAvailableForStaffParking(
            eq(tenantId), eq(RfidCardStatus.ACTIVE), eq("VINCOM-0008"), any()))
        .thenReturn(List.of());
    ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);

    service.getAvailableCards("  VINCOM-0008  ", 500);

    verify(rfidCardRepository)
        .searchAvailableForStaffParking(
            eq(tenantId), eq(RfidCardStatus.ACTIVE), eq("VINCOM-0008"), pageableCaptor.capture());
    assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(100);
  }

  @Test
  void defaultLimitIsFiftyAndResponseContainsOnlyStaffVisibleFields() {
    Tenant tenant = Tenant.builder().id(tenantId).build();
    RfidCard card =
        RfidCard.builder()
            .id(UUID.randomUUID())
            .tenant(tenant)
            .code("VINCOM-0008")
            .uid("PRIVATE-UID")
            .qrToken("PRIVATE-QR-TOKEN")
            .status(RfidCardStatus.ACTIVE)
            .build();
    when(rfidCardRepository.findAvailableForStaffParking(
            eq(tenantId), eq(RfidCardStatus.ACTIVE), any()))
        .thenReturn(List.of(card));
    ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);

    var responses = service.getAvailableCards(null, null);

    verify(rfidCardRepository)
        .findAvailableForStaffParking(
            eq(tenantId), eq(RfidCardStatus.ACTIVE), pageableCaptor.capture());
    assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(50);
    assertThat(responses)
        .containsExactly(
            AvailableRfidCardResponse.builder()
                .id(card.getId())
                .code(card.getCode())
                .label(card.getCode())
                .status(RfidCardStatus.ACTIVE)
                .build());
    assertThat(
            Arrays.stream(AvailableRfidCardResponse.class.getRecordComponents())
                .map(RecordComponent::getName))
        .containsExactly("id", "code", "label", "status")
        .doesNotContain("uid", "qrToken");
  }
}
