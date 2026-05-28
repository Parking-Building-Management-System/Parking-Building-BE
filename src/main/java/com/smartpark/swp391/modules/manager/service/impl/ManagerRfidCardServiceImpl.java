package com.smartpark.swp391.modules.manager.service.impl;

import com.smartpark.swp391.common.exception.ApiException;
import com.smartpark.swp391.common.exception.ErrorCode;
import com.smartpark.swp391.common.response.PageResponse;
import com.smartpark.swp391.infrastructure.tenant.TenantContext;
import com.smartpark.swp391.modules.identity.entity.Tenant;
import com.smartpark.swp391.modules.identity.repository.TenantRepository;
import com.smartpark.swp391.modules.manager.dto.rfid.RfidCardGenerateRequest;
import com.smartpark.swp391.modules.manager.dto.rfid.RfidCardGenerateResponse;
import com.smartpark.swp391.modules.manager.dto.rfid.RfidCardResponse;
import com.smartpark.swp391.modules.manager.dto.rfid.RfidCardStatusRequest;
import com.smartpark.swp391.modules.manager.service.ManagerRfidCardService;
import com.smartpark.swp391.modules.parking.entity.RfidCard;
import com.smartpark.swp391.modules.parking.enumType.RfidCardStatus;
import com.smartpark.swp391.modules.parking.repository.RfidCardRepository;
import com.smartpark.swp391.modules.parking.repository.SlotRepository;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class ManagerRfidCardServiceImpl implements ManagerRfidCardService {

  private static final SecureRandom QR_TOKEN_RANDOM = new SecureRandom();

  RfidCardRepository rfidCardRepository;
  SlotRepository slotRepository;
  TenantRepository tenantRepository;

  @Override
  @Transactional(readOnly = true)
  public PageResponse<RfidCardResponse> getCards(RfidCardStatus status, int page, int size) {
    var pageable = PageRequest.of(page, size, Sort.by("code").ascending());
    UUID tenantId = currentTenantId();
    var result =
        status == null
            ? rfidCardRepository.findAllByTenantId(tenantId, pageable)
            : rfidCardRepository.findAllByTenantIdAndStatus(tenantId, status, pageable);

    return new PageResponse<>(
        result.getContent().stream().map(this::toResponse).toList(),
        result.getNumber(),
        result.getSize(),
        result.getTotalElements(),
        result.getTotalPages());
  }

  @Override
  @Transactional
  public RfidCardGenerateResponse generateCards(RfidCardGenerateRequest request) {
    UUID tenantId = currentTenantId();
    Tenant tenant = currentTenantReference();
    int requestedCount = resolveCount(request);
    String prefix = resolvePrefix(request, tenant);
    List<RfidCard> newCards = new ArrayList<>();
    int existingCount = 0;

    for (int i = 1; i <= requestedCount; i++) {
      String code = prefix + "-" + String.format("%04d", i);
      if (rfidCardRepository.countByTenantIdAndCodeIgnoreCase(tenantId, code) > 0) {
        existingCount++;
        continue;
      }

      newCards.add(
          RfidCard.builder()
              .tenant(tenant)
              .code(code)
              .uid("UID-" + tenantId + "-" + code)
              .qrToken(generateUniqueQrToken())
              .assignedUser(null)
              .status(RfidCardStatus.ACTIVE)
              .activatedAt(LocalDateTime.now())
              .expiredAt(null)
              .build());
    }

    rfidCardRepository.saveAll(newCards);
    return RfidCardGenerateResponse.builder()
        .requestedCount(requestedCount)
        .createdCount(newCards.size())
        .existingCount(existingCount)
        .build();
  }

  @Override
  @Transactional
  public RfidCardResponse updateStatus(UUID id, RfidCardStatusRequest request) {
    RfidCard card =
        rfidCardRepository
            .findByIdAndTenantId(id, currentTenantId())
            .orElseThrow(
                () -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "RFID card not found"));

    card.setStatus(request.status());
    return toResponse(rfidCardRepository.save(card));
  }

  private int resolveCount(RfidCardGenerateRequest request) {
    if (request != null && request.count() != null) {
      return request.count();
    }

    long slots = slotRepository.countByTenantIdAndIsDeletedFalse(currentTenantId());
    return Math.max((int) Math.ceil(slots * 1.2), 50);
  }

  private String resolvePrefix(RfidCardGenerateRequest request, Tenant tenant) {
    String rawPrefix = request == null ? null : request.prefix();
    if (rawPrefix == null || rawPrefix.isBlank()) {
      rawPrefix = tenant.getSlug();
    }

    String prefix = rawPrefix.replaceAll("[^A-Za-z0-9]", "").toUpperCase(Locale.ROOT);
    return prefix.isBlank() ? "CARD" : prefix;
  }

  private RfidCardResponse toResponse(RfidCard card) {
    return RfidCardResponse.builder()
        .id(card.getId())
        .code(card.getCode())
        .uid(card.getUid())
        .qrToken(card.getQrToken())
        .assignedUserId(card.getAssignedUser() == null ? null : card.getAssignedUser().getId())
        .status(card.getStatus())
        .activatedAt(card.getActivatedAt())
        .expiredAt(card.getExpiredAt())
        .build();
  }

  private Tenant currentTenantReference() {
    return tenantRepository.getReferenceById(currentTenantId());
  }

  private UUID currentTenantId() {
    return TenantContext.getTenantId()
        .orElseThrow(() -> new ApiException(ErrorCode.UNAUTHENTICATED));
  }

  private String generateUniqueQrToken() {
    String token;
    do {
      token = generateQrToken();
    } while (rfidCardRepository.existsByQrToken(token));
    return token;
  }

  private String generateQrToken() {
    byte[] bytes = new byte[32];
    QR_TOKEN_RANDOM.nextBytes(bytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }
}
