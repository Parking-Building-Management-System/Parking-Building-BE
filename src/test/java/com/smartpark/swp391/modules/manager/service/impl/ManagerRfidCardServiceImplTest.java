package com.smartpark.swp391.modules.manager.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.smartpark.swp391.infrastructure.tenant.TenantContext;
import com.smartpark.swp391.modules.identity.repository.TenantRepository;
import com.smartpark.swp391.modules.manager.dto.rfid.RfidCardResponse;
import com.smartpark.swp391.modules.parking.entity.RfidCard;
import com.smartpark.swp391.modules.parking.enumType.RfidCardStatus;
import com.smartpark.swp391.modules.parking.repository.RfidCardRepository;
import com.smartpark.swp391.modules.parking.repository.SlotRepository;
import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class ManagerRfidCardServiceImplTest {

  @Mock RfidCardRepository rfidCardRepository;
  @Mock SlotRepository slotRepository;
  @Mock TenantRepository tenantRepository;

  private final UUID tenantId = UUID.randomUUID();
  private ManagerRfidCardServiceImpl service;

  @BeforeEach
  void setUp() {
    TenantContext.setTenantId(tenantId);
    service = new ManagerRfidCardServiceImpl(rfidCardRepository, slotRepository, tenantRepository);
  }

  @AfterEach
  void clearTenantContext() {
    TenantContext.clear();
  }

  @Test
  void blankSearchIsOmittedAndUsesRequestedPageSize() {
    when(rfidCardRepository.findAllByTenantId(
            org.mockito.ArgumentMatchers.eq(tenantId), org.mockito.ArgumentMatchers.any()))
        .thenReturn(Page.empty());

    service.getCards("   ", null, 2, 25);

    verify(rfidCardRepository)
        .findAllByTenantId(
            org.mockito.ArgumentMatchers.eq(tenantId),
            argThat(pageable -> hasPage(pageable, 2, 25)));
    verify(rfidCardRepository, never())
        .searchByTenantId(
            org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.any());
  }

  @Test
  void searchIsTrimmedAndCombinedWithStatus() {
    when(rfidCardRepository.searchByTenantIdAndStatus(
            org.mockito.ArgumentMatchers.eq(tenantId),
            org.mockito.ArgumentMatchers.eq(RfidCardStatus.LOST),
            org.mockito.ArgumentMatchers.eq("BCONS-000"),
            org.mockito.ArgumentMatchers.any()))
        .thenReturn(Page.<RfidCard>empty());

    service.getCards("  BCONS-000  ", RfidCardStatus.LOST, 0, 50);

    verify(rfidCardRepository)
        .searchByTenantIdAndStatus(
            org.mockito.ArgumentMatchers.eq(tenantId),
            org.mockito.ArgumentMatchers.eq(RfidCardStatus.LOST),
            org.mockito.ArgumentMatchers.eq("BCONS-000"),
            argThat(pageable -> hasPage(pageable, 0, 50)));
  }

  @Test
  void managerResponseDoesNotExposeQrToken() {
    assertThat(
            Arrays.stream(RfidCardResponse.class.getRecordComponents())
                .map(RecordComponent::getName))
        .doesNotContain("qrToken");
  }

  private boolean hasPage(Pageable pageable, int page, int size) {
    return pageable.getPageNumber() == page
        && pageable.getPageSize() == size
        && pageable.getSort().getOrderFor("code") != null
        && pageable.getSort().getOrderFor("code").isAscending();
  }
}
