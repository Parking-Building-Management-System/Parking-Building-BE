package com.smartpark.swp391.modules.manager.service.impl;

import com.smartpark.swp391.common.exception.ApiException;
import com.smartpark.swp391.common.exception.ErrorCode;
import com.smartpark.swp391.common.response.PageResponse;
import com.smartpark.swp391.infrastructure.tenant.TenantContext;
import com.smartpark.swp391.modules.identity.entity.Tenant;
import com.smartpark.swp391.modules.identity.repository.TenantRepository;
import com.smartpark.swp391.modules.manager.dto.penalty.ManagerPenaltyRuleRequest;
import com.smartpark.swp391.modules.manager.dto.penalty.ManagerPenaltyRuleResponse;
import com.smartpark.swp391.modules.manager.dto.penalty.ManagerPenaltyRuleStatusRequest;
import com.smartpark.swp391.modules.manager.service.ManagerPenaltyRuleService;
import com.smartpark.swp391.modules.parking.entity.Parking;
import com.smartpark.swp391.modules.parking.repository.ParkingRepository;
import com.smartpark.swp391.modules.penalty.entity.PenaltyRule;
import com.smartpark.swp391.modules.penalty.enumType.PenaltyRuleStatus;
import com.smartpark.swp391.modules.penalty.enumType.PenaltyType;
import com.smartpark.swp391.modules.penalty.repository.PenaltyRuleRepository;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.Locale;
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
public class ManagerPenaltyRuleServiceImpl implements ManagerPenaltyRuleService {

  PenaltyRuleRepository penaltyRuleRepository;
  ParkingRepository parkingRepository;
  TenantRepository tenantRepository;

  @Override
  @Transactional(readOnly = true)
  public PageResponse<ManagerPenaltyRuleResponse> getRules(
      UUID parkingId, PenaltyType type, PenaltyRuleStatus status, int page, int size) {
    if (parkingId != null) {
      getParkingOrThrow(parkingId);
    }

    var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
    var result = penaltyRuleRepository.findAll(specification(parkingId, type, status), pageable);
    return new PageResponse<>(
        result.getContent().stream().map(this::toResponse).toList(),
        result.getNumber(),
        result.getSize(),
        result.getTotalElements(),
        result.getTotalPages());
  }

  @Override
  @Transactional
  public ManagerPenaltyRuleResponse createRule(ManagerPenaltyRuleRequest request) {
    PenaltyRule rule =
        PenaltyRule.builder()
            .tenant(currentTenantReference())
            .parking(resolveParking(request.parkingId()))
            .type(request.type())
            .build();
    applyRequest(rule, request);
    assertNoActiveDuplicate(rule, null);
    return toResponse(penaltyRuleRepository.save(rule));
  }

  @Override
  @Transactional(readOnly = true)
  public ManagerPenaltyRuleResponse getRule(UUID id) {
    return toResponse(getRuleOrThrow(id));
  }

  @Override
  @Transactional
  public ManagerPenaltyRuleResponse updateRule(UUID id, ManagerPenaltyRuleRequest request) {
    PenaltyRule rule = getRuleOrThrow(id);
    rule.setParking(resolveParking(request.parkingId()));
    rule.setType(request.type());
    applyRequest(rule, request);
    assertNoActiveDuplicate(rule, id);
    return toResponse(penaltyRuleRepository.save(rule));
  }

  @Override
  @Transactional
  public ManagerPenaltyRuleResponse updateStatus(UUID id, ManagerPenaltyRuleStatusRequest request) {
    PenaltyRule rule = getRuleOrThrow(id);
    rule.setStatus(request.status());
    assertNoActiveDuplicate(rule, id);
    return toResponse(penaltyRuleRepository.save(rule));
  }

  @Override
  @Transactional
  public void deleteRule(UUID id) {
    PenaltyRule rule = getRuleOrThrow(id);
    rule.setDeleted(true);
    rule.setStatus(PenaltyRuleStatus.INACTIVE);
    penaltyRuleRepository.save(rule);
  }

  private Specification<PenaltyRule> specification(
      UUID parkingId, PenaltyType type, PenaltyRuleStatus status) {
    return (root, query, criteriaBuilder) -> {
      var predicates = new ArrayList<Predicate>();
      predicates.add(criteriaBuilder.equal(root.get("tenant").get("id"), currentTenantId()));
      predicates.add(criteriaBuilder.isFalse(root.get("deleted")));
      if (parkingId != null) {
        predicates.add(criteriaBuilder.equal(root.get("parking").get("id"), parkingId));
      }
      if (type != null) {
        predicates.add(criteriaBuilder.equal(root.get("type"), type));
      }
      if (status != null) {
        predicates.add(criteriaBuilder.equal(root.get("status"), status));
      }
      return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
    };
  }

  private void applyRequest(PenaltyRule rule, ManagerPenaltyRuleRequest request) {
    rule.setCode(normalizeCode(request.code(), request.type()));
    rule.setName(request.name().trim());
    rule.setAmount(request.amount());
    rule.setCurrency(normalizeCurrency(request.currency()));
    rule.setRequiresPhoto(request.requiresPhoto() == null || request.requiresPhoto());
    rule.setDescription(normalizeOptional(request.description()));
    rule.setStatus(request.status() == null ? PenaltyRuleStatus.ACTIVE : request.status());
  }

  private void assertNoActiveDuplicate(PenaltyRule rule, UUID excludedId) {
    if (rule.getStatus() != PenaltyRuleStatus.ACTIVE || rule.isDeleted()) {
      return;
    }
    UUID parkingId = rule.getParking() == null ? null : rule.getParking().getId();
    if (penaltyRuleRepository.existsActiveScope(
        currentTenantId(), parkingId, rule.getType(), PenaltyRuleStatus.ACTIVE, excludedId)) {
      throw new ApiException(
          ErrorCode.DUPLICATE_RESOURCE,
          "Active penalty rule already exists for this parking scope and type");
    }
  }

  private PenaltyRule getRuleOrThrow(UUID id) {
    return penaltyRuleRepository
        .findDetailByIdAndTenantId(id, currentTenantId())
        .orElseThrow(
            () -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Penalty rule not found"));
  }

  private Parking resolveParking(UUID parkingId) {
    return parkingId == null ? null : getParkingOrThrow(parkingId);
  }

  private Parking getParkingOrThrow(UUID parkingId) {
    return parkingRepository
        .findByIdAndTenantIdAndIsDeletedFalse(parkingId, currentTenantId())
        .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Parking not found"));
  }

  private String normalizeCode(String code, PenaltyType type) {
    if (code == null || code.isBlank()) {
      return type.name();
    }
    return code.trim().toUpperCase(Locale.ROOT);
  }

  private String normalizeCurrency(String currency) {
    if (currency == null || currency.isBlank()) {
      return "VND";
    }
    return currency.trim().toUpperCase(Locale.ROOT);
  }

  private String normalizeOptional(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }

  private ManagerPenaltyRuleResponse toResponse(PenaltyRule rule) {
    Parking parking = rule.getParking();
    return ManagerPenaltyRuleResponse.builder()
        .id(rule.getId())
        .code(rule.getCode())
        .name(rule.getName())
        .parkingId(parking == null ? null : parking.getId())
        .parkingName(parking == null ? null : parking.getName())
        .type(rule.getType())
        .amount(rule.getAmount())
        .currency(rule.getCurrency())
        .requiresPhoto(rule.isRequiresPhoto())
        .description(rule.getDescription())
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
