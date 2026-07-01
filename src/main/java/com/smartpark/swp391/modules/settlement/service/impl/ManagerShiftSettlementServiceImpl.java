package com.smartpark.swp391.modules.settlement.service.impl;

import com.smartpark.swp391.common.exception.ApiException;
import com.smartpark.swp391.common.exception.ErrorCode;
import com.smartpark.swp391.common.response.PageResponse;
import com.smartpark.swp391.infrastructure.tenant.TenantContext;
import com.smartpark.swp391.modules.settlement.dto.ManagerShiftSettlementDetailResponse;
import com.smartpark.swp391.modules.settlement.dto.ManagerShiftSettlementListItemResponse;
import com.smartpark.swp391.modules.settlement.entity.StaffCashShift;
import com.smartpark.swp391.modules.settlement.enumType.StaffCashShiftStatus;
import com.smartpark.swp391.modules.settlement.repository.StaffCashShiftRepository;
import com.smartpark.swp391.modules.settlement.repository.StaffCashTransactionRepository;
import com.smartpark.swp391.modules.settlement.service.ManagerShiftSettlementService;
import com.smartpark.swp391.modules.settlement.service.StaffCashSettlementMapper;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class ManagerShiftSettlementServiceImpl implements ManagerShiftSettlementService {

  StaffCashShiftRepository staffCashShiftRepository;
  StaffCashTransactionRepository staffCashTransactionRepository;
  StaffCashSettlementMapper mapper;

  @Override
  @Transactional(readOnly = true)
  public PageResponse<ManagerShiftSettlementListItemResponse> getSettlements(
      UUID parkingId,
      UUID staffId,
      StaffCashShiftStatus status,
      LocalDateTime from,
      LocalDateTime to,
      int page,
      int size) {
    var pageable = PageRequest.of(page, size, Sort.by("openedAt").descending());
    var resultPage =
        staffCashShiftRepository.findAll(
            specification(parkingId, staffId, status, from, to), pageable);
    return new PageResponse<>(
        resultPage.getContent().stream().map(mapper::toManagerListItem).toList(),
        resultPage.getNumber(),
        resultPage.getSize(),
        resultPage.getTotalElements(),
        resultPage.getTotalPages());
  }

  @Override
  @Transactional(readOnly = true)
  public ManagerShiftSettlementDetailResponse getSettlement(UUID id) {
    UUID tenantId = currentTenantId();
    StaffCashShift shift =
        staffCashShiftRepository
            .findByTenantIdAndId(tenantId, id)
            .orElseThrow(
                () -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "SHIFT_SETTLEMENT_NOT_FOUND"));
    return ManagerShiftSettlementDetailResponse.builder()
        .shift(mapper.toShiftResponse(shift))
        .transactions(
            staffCashTransactionRepository
                .findByTenantIdAndShiftIdOrderByOccurredAtDesc(tenantId, shift.getId())
                .stream()
                .map(mapper::toTransactionResponse)
                .toList())
        .build();
  }

  private Specification<StaffCashShift> specification(
      UUID parkingId,
      UUID staffId,
      StaffCashShiftStatus status,
      LocalDateTime from,
      LocalDateTime to) {
    return (root, query, cb) -> {
      var predicates = new ArrayList<Predicate>();
      predicates.add(cb.equal(root.get("tenant").get("id"), currentTenantId()));
      if (parkingId != null) {
        predicates.add(cb.equal(root.get("parking").get("id"), parkingId));
      }
      if (staffId != null) {
        predicates.add(cb.equal(root.get("staff").get("id"), staffId));
      }
      if (status != null) {
        predicates.add(cb.equal(root.get("status"), status));
      }
      Expression<LocalDateTime> lifecycleTime =
          cb.coalesce(root.get("closedAt"), root.get("openedAt"));
      if (from != null) {
        predicates.add(cb.greaterThanOrEqualTo(lifecycleTime, from));
      }
      if (to != null) {
        predicates.add(cb.lessThanOrEqualTo(lifecycleTime, to));
      }
      return cb.and(predicates.toArray(Predicate[]::new));
    };
  }

  private UUID currentTenantId() {
    return TenantContext.getTenantId()
        .orElseThrow(() -> new ApiException(ErrorCode.UNAUTHENTICATED));
  }
}
