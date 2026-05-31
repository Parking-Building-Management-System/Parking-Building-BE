package com.smartpark.swp391.modules.manager.service.impl;

import com.smartpark.swp391.common.exception.ApiException;
import com.smartpark.swp391.common.exception.ErrorCode;
import com.smartpark.swp391.common.response.PageResponse;
import com.smartpark.swp391.infrastructure.tenant.TenantContext;
import com.smartpark.swp391.modules.identity.entity.Tenant;
import com.smartpark.swp391.modules.identity.repository.TenantRepository;
import com.smartpark.swp391.modules.manager.dto.pricing.ManagerPricingRulePreviewRequest;
import com.smartpark.swp391.modules.manager.dto.pricing.ManagerPricingRuleRequest;
import com.smartpark.swp391.modules.manager.dto.pricing.ManagerPricingRuleResponse;
import com.smartpark.swp391.modules.manager.dto.pricing.ManagerPricingRuleStatusRequest;
import com.smartpark.swp391.modules.manager.service.ManagerPricingRuleService;
import com.smartpark.swp391.modules.parking.entity.Parking;
import com.smartpark.swp391.modules.parking.repository.ParkingRepository;
import com.smartpark.swp391.modules.pricing.dto.PricingQuoteResponse;
import com.smartpark.swp391.modules.pricing.entity.PricingRule;
import com.smartpark.swp391.modules.pricing.enumType.PricingRuleStatus;
import com.smartpark.swp391.modules.pricing.repository.PricingRuleRepository;
import com.smartpark.swp391.modules.pricing.service.PricingQuoteService;
import com.smartpark.swp391.modules.vehicle.entity.VehicleType;
import com.smartpark.swp391.modules.vehicle.repository.VehicleTypeRepository;
import jakarta.persistence.criteria.Predicate;
import java.time.LocalDateTime;
import java.time.ZoneId;
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
public class ManagerPricingRuleServiceImpl implements ManagerPricingRuleService {

  PricingRuleRepository pricingRuleRepository;
  ParkingRepository parkingRepository;
  VehicleTypeRepository vehicleTypeRepository;
  TenantRepository tenantRepository;
  PricingQuoteService pricingQuoteService;
  ZoneId zoneId = ZoneId.systemDefault();

  @Override
  @Transactional(readOnly = true)
  public PageResponse<ManagerPricingRuleResponse> getRules(
      UUID parkingId, UUID vehicleTypeId, PricingRuleStatus status, int page, int size) {
    if (parkingId != null) {
      getParkingOrThrow(parkingId);
    }
    if (vehicleTypeId != null) {
      getVehicleTypeOrThrow(vehicleTypeId);
    }

    var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
    var result =
        pricingRuleRepository.findAll(specification(parkingId, vehicleTypeId, status), pageable);
    return new PageResponse<>(
        result.getContent().stream().map(this::toResponse).toList(),
        result.getNumber(),
        result.getSize(),
        result.getTotalElements(),
        result.getTotalPages());
  }

  @Override
  @Transactional
  public ManagerPricingRuleResponse createRule(ManagerPricingRuleRequest request) {
    PricingRule rule =
        PricingRule.builder()
            .tenant(currentTenantReference())
            .parking(resolveParking(request.parkingId()))
            .vehicleType(getVehicleTypeOrThrow(request.vehicleTypeId()))
            .build();
    applyRequest(rule, request);
    assertNoActiveDuplicate(rule, null);
    return toResponse(pricingRuleRepository.save(rule));
  }

  @Override
  @Transactional(readOnly = true)
  public ManagerPricingRuleResponse getRule(UUID id) {
    return toResponse(getRuleOrThrow(id));
  }

  @Override
  @Transactional
  public ManagerPricingRuleResponse updateRule(UUID id, ManagerPricingRuleRequest request) {
    PricingRule rule = getRuleOrThrow(id);
    rule.setParking(resolveParking(request.parkingId()));
    rule.setVehicleType(getVehicleTypeOrThrow(request.vehicleTypeId()));
    applyRequest(rule, request);
    assertNoActiveDuplicate(rule, id);
    return toResponse(pricingRuleRepository.save(rule));
  }

  @Override
  @Transactional
  public ManagerPricingRuleResponse updateStatus(UUID id, ManagerPricingRuleStatusRequest request) {
    PricingRule rule = getRuleOrThrow(id);
    rule.setStatus(request.status());
    assertNoActiveDuplicate(rule, id);
    return toResponse(pricingRuleRepository.save(rule));
  }

  @Override
  @Transactional
  public void deleteRule(UUID id) {
    PricingRule rule = getRuleOrThrow(id);
    rule.setDeleted(true);
    rule.setStatus(PricingRuleStatus.INACTIVE);
    pricingRuleRepository.save(rule);
  }

  @Override
  @Transactional(readOnly = true)
  public PricingQuoteResponse preview(UUID id, ManagerPricingRulePreviewRequest request) {
    PricingRule rule = getRuleOrThrow(id);
    if (request.vehicleTypeId() != null
        && !rule.getVehicleType().getId().equals(request.vehicleTypeId())) {
      throw new ApiException(ErrorCode.INVALID_INPUT, "vehicleTypeId does not match pricing rule");
    }
    LocalDateTime checkInAt = toLocal(request.checkInAt());
    LocalDateTime checkOutAt = toLocal(request.checkOutAt());
    if (checkOutAt.isBefore(checkInAt)) {
      throw new ApiException(ErrorCode.INVALID_INPUT, "checkOutAt must be after checkInAt");
    }
    return pricingQuoteService.preview(rule, checkInAt, checkOutAt);
  }

  private Specification<PricingRule> specification(
      UUID parkingId, UUID vehicleTypeId, PricingRuleStatus status) {
    return (root, query, criteriaBuilder) -> {
      var predicates = new ArrayList<Predicate>();
      predicates.add(criteriaBuilder.equal(root.get("tenant").get("id"), currentTenantId()));
      predicates.add(criteriaBuilder.isFalse(root.get("deleted")));
      if (parkingId != null) {
        predicates.add(criteriaBuilder.equal(root.get("parking").get("id"), parkingId));
      }
      if (vehicleTypeId != null) {
        predicates.add(criteriaBuilder.equal(root.get("vehicleType").get("id"), vehicleTypeId));
      }
      if (status != null) {
        predicates.add(criteriaBuilder.equal(root.get("status"), status));
      }
      return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
    };
  }

  private void applyRequest(PricingRule rule, ManagerPricingRuleRequest request) {
    rule.setName(request.name().trim());
    rule.setFreeMinutes(request.freeMinutes());
    rule.setFirstBlockMinutes(request.firstBlockMinutes());
    rule.setFirstBlockPrice(request.firstBlockPrice());
    rule.setNextBlockMinutes(request.nextBlockMinutes());
    rule.setNextBlockPrice(request.nextBlockPrice());
    rule.setDailyCapPrice(request.dailyCapPrice());
    rule.setGraceMinutesAfterPayment(request.graceMinutesAfterPayment());
    rule.setStatus(request.status() == null ? PricingRuleStatus.ACTIVE : request.status());
  }

  private void assertNoActiveDuplicate(PricingRule rule, UUID excludedId) {
    if (rule.getStatus() != PricingRuleStatus.ACTIVE || rule.isDeleted()) {
      return;
    }
    UUID parkingId = rule.getParking() == null ? null : rule.getParking().getId();
    UUID vehicleTypeId = rule.getVehicleType().getId();
    if (pricingRuleRepository.existsActiveScope(
        currentTenantId(), parkingId, vehicleTypeId, excludedId)) {
      throw new ApiException(
          ErrorCode.DUPLICATE_RESOURCE,
          "Active pricing rule already exists for this parking scope and vehicle type");
    }
  }

  private PricingRule getRuleOrThrow(UUID id) {
    return pricingRuleRepository
        .findDetailByIdAndTenantId(id, currentTenantId())
        .orElseThrow(
            () -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Pricing rule not found"));
  }

  private Parking resolveParking(UUID parkingId) {
    return parkingId == null ? null : getParkingOrThrow(parkingId);
  }

  private Parking getParkingOrThrow(UUID parkingId) {
    return parkingRepository
        .findByIdAndTenantIdAndIsDeletedFalse(parkingId, currentTenantId())
        .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Parking not found"));
  }

  private VehicleType getVehicleTypeOrThrow(UUID vehicleTypeId) {
    VehicleType vehicleType =
        vehicleTypeRepository
            .findById(vehicleTypeId)
            .orElseThrow(
                () -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Vehicle type not found"));
    if (!vehicleType.isActive()) {
      throw new ApiException(ErrorCode.INVALID_INPUT, "Vehicle type is inactive");
    }
    return vehicleType;
  }

  private LocalDateTime toLocal(java.time.OffsetDateTime value) {
    return value.atZoneSameInstant(zoneId).toLocalDateTime();
  }

  private ManagerPricingRuleResponse toResponse(PricingRule rule) {
    Parking parking = rule.getParking();
    VehicleType vehicleType = rule.getVehicleType();
    return ManagerPricingRuleResponse.builder()
        .id(rule.getId())
        .name(rule.getName())
        .parkingId(parking == null ? null : parking.getId())
        .parkingName(parking == null ? null : parking.getName())
        .vehicleTypeId(vehicleType.getId())
        .vehicleTypeCode(vehicleType.getCode())
        .vehicleTypeName(vehicleType.getName())
        .freeMinutes(rule.getFreeMinutes())
        .firstBlockMinutes(rule.getFirstBlockMinutes())
        .firstBlockPrice(rule.getFirstBlockPrice())
        .nextBlockMinutes(rule.getNextBlockMinutes())
        .nextBlockPrice(rule.getNextBlockPrice())
        .dailyCapPrice(rule.getDailyCapPrice())
        .graceMinutesAfterPayment(rule.getGraceMinutesAfterPayment())
        .status(rule.getStatus())
        .createdAt(rule.getCreatedAt())
        .updatedAt(rule.getUpdatedAt())
        .build();
  }

  private Tenant currentTenantReference() {
    return tenantRepository.getReferenceById(currentTenantId());
  }

  private UUID currentTenantId() {
    return TenantContext.getTenantId()
        .orElseThrow(() -> new ApiException(ErrorCode.UNAUTHENTICATED));
  }
}
