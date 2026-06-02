package com.smartpark.swp391.modules.staff.service.impl;

import com.smartpark.swp391.common.exception.ApiException;
import com.smartpark.swp391.common.exception.ErrorCode;
import com.smartpark.swp391.infrastructure.tenant.TenantContext;
import com.smartpark.swp391.modules.parking.entity.RfidCard;
import com.smartpark.swp391.modules.parking.enumType.RfidCardStatus;
import com.smartpark.swp391.modules.parking.repository.RfidCardRepository;
import com.smartpark.swp391.modules.staff.dto.AvailableRfidCardResponse;
import com.smartpark.swp391.modules.staff.dto.StaffWorkContextResponse;
import com.smartpark.swp391.modules.staff.service.StaffRfidCardService;
import com.smartpark.swp391.modules.staff.service.StaffWorkContextService;
import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class StaffRfidCardServiceImpl implements StaffRfidCardService {

  RfidCardRepository rfidCardRepository;
  StaffWorkContextService staffWorkContextService;

  @Override
  @Transactional(readOnly = true)
  public List<AvailableRfidCardResponse> getAvailableCards(String search, Integer limit) {
    StaffWorkContextResponse workContext = staffWorkContextService.requireCurrentContext();
    int resolvedLimit = limit == null ? 50 : Math.max(1, Math.min(limit, 100));
    return rfidCardRepository
        .findAvailableForStaffParking(
            currentTenantId(),
            workContext.parkingId(),
            RfidCardStatus.ACTIVE,
            trimToNull(search),
            PageRequest.of(0, resolvedLimit))
        .stream()
        .map(this::toResponse)
        .toList();
  }

  private AvailableRfidCardResponse toResponse(RfidCard card) {
    return AvailableRfidCardResponse.builder()
        .id(card.getId())
        .code(card.getCode())
        .label(card.getCode())
        .status(card.getStatus())
        .build();
  }

  private String trimToNull(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }

  private UUID currentTenantId() {
    return TenantContext.getTenantId()
        .orElseThrow(() -> new ApiException(ErrorCode.UNAUTHENTICATED));
  }
}
