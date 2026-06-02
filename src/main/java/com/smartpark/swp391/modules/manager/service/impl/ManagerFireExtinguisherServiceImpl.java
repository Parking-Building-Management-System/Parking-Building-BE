package com.smartpark.swp391.modules.manager.service.impl;

import com.smartpark.swp391.common.exception.ApiException;
import com.smartpark.swp391.common.exception.ErrorCode;
import com.smartpark.swp391.common.response.PageResponse;
import com.smartpark.swp391.infrastructure.tenant.TenantContext;
import com.smartpark.swp391.modules.firesafety.entity.FireExtinguisher;
import com.smartpark.swp391.modules.firesafety.entity.FireExtinguisherInspection;
import com.smartpark.swp391.modules.firesafety.enumType.FireExtinguisherStatus;
import com.smartpark.swp391.modules.firesafety.enumType.FireExtinguisherType;
import com.smartpark.swp391.modules.firesafety.enumType.FireInspectionResult;
import com.smartpark.swp391.modules.firesafety.repository.FireExtinguisherInspectionRepository;
import com.smartpark.swp391.modules.firesafety.repository.FireExtinguisherRepository;
import com.smartpark.swp391.modules.identity.entity.Tenant;
import com.smartpark.swp391.modules.identity.entity.User;
import com.smartpark.swp391.modules.identity.repository.TenantRepository;
import com.smartpark.swp391.modules.manager.dto.firesafety.FireExtinguisherCoordinateRequest;
import com.smartpark.swp391.modules.manager.dto.firesafety.FireExtinguisherRequest;
import com.smartpark.swp391.modules.manager.dto.firesafety.FireExtinguisherResponse;
import com.smartpark.swp391.modules.manager.dto.firesafety.FireExtinguisherStatusRequest;
import com.smartpark.swp391.modules.manager.dto.firesafety.FireExtinguisherSummaryResponse;
import com.smartpark.swp391.modules.manager.dto.firesafety.FireInspectionLogResponse;
import com.smartpark.swp391.modules.manager.dto.firesafety.FireSafetyMapItemResponse;
import com.smartpark.swp391.modules.manager.dto.firesafety.FireSafetyMapResponse;
import com.smartpark.swp391.modules.manager.service.ManagerFireExtinguisherService;
import com.smartpark.swp391.modules.parking.entity.Floor;
import com.smartpark.swp391.modules.parking.entity.Parking;
import com.smartpark.swp391.modules.parking.entity.Zone;
import com.smartpark.swp391.modules.parking.repository.FloorRepository;
import com.smartpark.swp391.modules.parking.repository.ParkingRepository;
import com.smartpark.swp391.modules.parking.repository.ZoneRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumMap;
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
public class ManagerFireExtinguisherServiceImpl implements ManagerFireExtinguisherService {

  FireExtinguisherRepository fireExtinguisherRepository;
  FireExtinguisherInspectionRepository inspectionRepository;
  ParkingRepository parkingRepository;
  FloorRepository floorRepository;
  ZoneRepository zoneRepository;
  TenantRepository tenantRepository;

  @Override
  @Transactional(readOnly = true)
  public PageResponse<FireExtinguisherResponse> getExtinguishers(
      UUID parkingId,
      UUID floorId,
      UUID zoneId,
      FireExtinguisherStatus status,
      FireExtinguisherType type,
      String search,
      Integer expiringWithinDays,
      int page,
      int size) {
    LocalDate expiringUntil =
        expiringWithinDays == null ? null : LocalDate.now().plusDays(expiringWithinDays);
    var result =
        fireExtinguisherRepository.search(
            currentTenantId(),
            parkingId,
            floorId,
            zoneId,
            status,
            type,
            trimToNull(search),
            expiringUntil,
            PageRequest.of(page, size, Sort.by("code").ascending()));
    return new PageResponse<>(
        result.getContent().stream().map(this::toResponse).toList(),
        result.getNumber(),
        result.getSize(),
        result.getTotalElements(),
        result.getTotalPages());
  }

  @Override
  @Transactional
  public FireExtinguisherResponse createExtinguisher(FireExtinguisherRequest request) {
    UUID tenantId = currentTenantId();
    String code = normalizeCode(request.code());
    if (fireExtinguisherRepository.existsByTenantIdAndCodeIgnoreCaseAndDeletedFalse(
        tenantId, code)) {
      throw new ApiException(ErrorCode.DUPLICATE_RESOURCE, "Fire extinguisher code already exists");
    }

    ValidatedLocation location =
        validateLocation(request.parkingId(), request.floorId(), request.zoneId());
    FireExtinguisher extinguisher =
        FireExtinguisher.builder()
            .tenant(currentTenantReference())
            .parking(location.parking())
            .floor(location.floor())
            .zone(location.zone())
            .code(code)
            .type(request.type())
            .locationDescription(trimToNull(request.locationDescription()))
            .xCoordinate(request.xCoordinate())
            .yCoordinate(request.yCoordinate())
            .manufactureDate(request.manufactureDate())
            .expiryDate(request.expiryDate())
            .nextInspectionAt(request.nextInspectionAt())
            .status(request.status() == null ? FireExtinguisherStatus.ACTIVE : request.status())
            .note(trimToNull(request.note()))
            .deleted(false)
            .build();
    return toResponse(fireExtinguisherRepository.save(extinguisher));
  }

  @Override
  @Transactional(readOnly = true)
  public FireExtinguisherResponse getExtinguisher(UUID id) {
    return toResponse(getExtinguisherOrThrow(id));
  }

  @Override
  @Transactional
  public FireExtinguisherResponse updateExtinguisher(UUID id, FireExtinguisherRequest request) {
    FireExtinguisher extinguisher = getExtinguisherOrThrow(id);
    String code = normalizeCode(request.code());
    if (fireExtinguisherRepository.existsByTenantIdAndCodeIgnoreCaseAndIdNotAndDeletedFalse(
        currentTenantId(), code, id)) {
      throw new ApiException(ErrorCode.DUPLICATE_RESOURCE, "Fire extinguisher code already exists");
    }
    ValidatedLocation location =
        validateLocation(request.parkingId(), request.floorId(), request.zoneId());

    extinguisher.setParking(location.parking());
    extinguisher.setFloor(location.floor());
    extinguisher.setZone(location.zone());
    extinguisher.setCode(code);
    extinguisher.setType(request.type());
    extinguisher.setLocationDescription(trimToNull(request.locationDescription()));
    extinguisher.setXCoordinate(request.xCoordinate());
    extinguisher.setYCoordinate(request.yCoordinate());
    extinguisher.setManufactureDate(request.manufactureDate());
    extinguisher.setExpiryDate(request.expiryDate());
    extinguisher.setNextInspectionAt(request.nextInspectionAt());
    if (request.status() != null) {
      extinguisher.setStatus(request.status());
    }
    extinguisher.setNote(trimToNull(request.note()));
    return toResponse(fireExtinguisherRepository.save(extinguisher));
  }

  @Override
  @Transactional
  public FireExtinguisherResponse updateStatus(UUID id, FireExtinguisherStatusRequest request) {
    FireExtinguisher extinguisher = getExtinguisherOrThrow(id);
    extinguisher.setStatus(request.status());
    if (request.note() != null) {
      extinguisher.setNote(trimToNull(request.note()));
    }
    return toResponse(fireExtinguisherRepository.save(extinguisher));
  }

  @Override
  @Transactional
  public FireExtinguisherResponse updateCoordinate(
      UUID id, FireExtinguisherCoordinateRequest request) {
    FireExtinguisher extinguisher = getExtinguisherOrThrow(id);
    requirePercent(request.xCoordinate(), "xCoordinate");
    requirePercent(request.yCoordinate(), "yCoordinate");
    extinguisher.setXCoordinate(request.xCoordinate());
    extinguisher.setYCoordinate(request.yCoordinate());
    return toResponse(fireExtinguisherRepository.save(extinguisher));
  }

  @Override
  @Transactional
  public void deleteExtinguisher(UUID id) {
    FireExtinguisher extinguisher = getExtinguisherOrThrow(id);
    extinguisher.setDeleted(true);
    fireExtinguisherRepository.save(extinguisher);
  }

  @Override
  @Transactional(readOnly = true)
  public FireExtinguisherSummaryResponse getSummary() {
    UUID tenantId = currentTenantId();
    EnumMap<FireExtinguisherStatus, Long> counts = new EnumMap<>(FireExtinguisherStatus.class);
    fireExtinguisherRepository
        .countByStatus(tenantId)
        .forEach(row -> counts.put((FireExtinguisherStatus) row[0], (Long) row[1]));
    LocalDate today = LocalDate.now();
    return FireExtinguisherSummaryResponse.builder()
        .total(fireExtinguisherRepository.countByTenantIdAndDeletedFalse(tenantId))
        .active(counts.getOrDefault(FireExtinguisherStatus.ACTIVE, 0L))
        .expired(counts.getOrDefault(FireExtinguisherStatus.EXPIRED, 0L))
        .missing(counts.getOrDefault(FireExtinguisherStatus.MISSING, 0L))
        .damaged(counts.getOrDefault(FireExtinguisherStatus.DAMAGED, 0L))
        .maintenance(counts.getOrDefault(FireExtinguisherStatus.MAINTENANCE, 0L))
        .dueInspection(
            fireExtinguisherRepository
                .countByTenantIdAndDeletedFalseAndNextInspectionAtLessThanEqual(
                    tenantId, LocalDateTime.now()))
        .expiringSoon(
            fireExtinguisherRepository.countByTenantIdAndDeletedFalseAndExpiryDateBetween(
                tenantId, today, today.plusDays(30)))
        .build();
  }

  @Override
  @Transactional(readOnly = true)
  public FireSafetyMapResponse getFireSafetyMap(UUID floorId) {
    Floor floor = getFloorOrThrow(floorId);
    Parking parking = floor.getParking();
    return FireSafetyMapResponse.builder()
        .parkingId(parking.getId())
        .parkingName(parking.getName())
        .floorId(floor.getId())
        .floorName(floor.getName())
        .floorCode(floor.getCode())
        .mapImageUrl(floor.getMapImageUrl())
        .coordinateMode("PERCENT")
        .extinguishers(
            fireExtinguisherRepository
                .findMapItemsByTenantIdAndFloorId(currentTenantId(), floorId)
                .stream()
                .map(this::toMapItem)
                .toList())
        .build();
  }

  @Override
  @Transactional(readOnly = true)
  public PageResponse<FireInspectionLogResponse> getInspectionLogs(
      UUID extinguisherId,
      UUID parkingId,
      UUID floorId,
      FireInspectionResult result,
      LocalDateTime from,
      LocalDateTime to,
      int page,
      int size) {
    var logs =
        inspectionRepository.searchLogs(
            currentTenantId(),
            extinguisherId,
            parkingId,
            floorId,
            result,
            from,
            to,
            PageRequest.of(page, size, Sort.by("inspectedAt").descending()));
    return new PageResponse<>(
        logs.getContent().stream().map(this::toInspectionLog).toList(),
        logs.getNumber(),
        logs.getSize(),
        logs.getTotalElements(),
        logs.getTotalPages());
  }

  private ValidatedLocation validateLocation(UUID parkingId, UUID floorId, UUID zoneId) {
    Parking parking =
        parkingRepository
            .findByIdAndTenantIdAndIsDeletedFalse(parkingId, currentTenantId())
            .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Parking not found"));
    Floor floor = getFloorOrThrow(floorId);
    if (!floor.getParking().getId().equals(parking.getId())) {
      throw new ApiException(ErrorCode.INVALID_INPUT, "Floor does not belong to parking");
    }
    Zone zone = null;
    if (zoneId != null) {
      zone =
          zoneRepository
              .findByIdAndTenantIdAndIsDeletedFalse(zoneId, currentTenantId())
              .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Zone not found"));
      if (zone.getFloor() == null || !zone.getFloor().getId().equals(floor.getId())) {
        throw new ApiException(ErrorCode.INVALID_INPUT, "Zone does not belong to floor");
      }
    }
    return new ValidatedLocation(parking, floor, zone);
  }

  private Floor getFloorOrThrow(UUID floorId) {
    return floorRepository
        .findByIdAndTenantIdAndDeletedFalse(floorId, currentTenantId())
        .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Floor not found"));
  }

  private FireExtinguisher getExtinguisherOrThrow(UUID id) {
    return fireExtinguisherRepository
        .findByIdAndTenantIdAndDeletedFalse(id, currentTenantId())
        .orElseThrow(
            () -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Fire extinguisher not found"));
  }

  private FireExtinguisherResponse toResponse(FireExtinguisher extinguisher) {
    Parking parking = extinguisher.getParking();
    Floor floor = extinguisher.getFloor();
    Zone zone = extinguisher.getZone();
    return FireExtinguisherResponse.builder()
        .id(extinguisher.getId())
        .parkingId(parking.getId())
        .parkingName(parking.getName())
        .floorId(floor.getId())
        .floorName(floor.getName())
        .floorCode(floor.getCode())
        .zoneId(zone == null ? null : zone.getId())
        .zoneName(zone == null ? null : zone.getName())
        .code(extinguisher.getCode())
        .type(extinguisher.getType())
        .locationDescription(extinguisher.getLocationDescription())
        .xCoordinate(extinguisher.getXCoordinate())
        .yCoordinate(extinguisher.getYCoordinate())
        .hasCoordinate(hasCoordinate(extinguisher))
        .manufactureDate(extinguisher.getManufactureDate())
        .expiryDate(extinguisher.getExpiryDate())
        .lastInspectedAt(extinguisher.getLastInspectedAt())
        .nextInspectionAt(extinguisher.getNextInspectionAt())
        .status(extinguisher.getStatus())
        .note(extinguisher.getNote())
        .createdAt(extinguisher.getCreatedAt())
        .updatedAt(extinguisher.getUpdatedAt())
        .build();
  }

  private FireSafetyMapItemResponse toMapItem(FireExtinguisher extinguisher) {
    Zone zone = extinguisher.getZone();
    return FireSafetyMapItemResponse.builder()
        .id(extinguisher.getId())
        .code(extinguisher.getCode())
        .type(extinguisher.getType())
        .status(extinguisher.getStatus())
        .zoneId(zone == null ? null : zone.getId())
        .zoneName(zone == null ? null : zone.getName())
        .locationDescription(extinguisher.getLocationDescription())
        .xCoordinate(extinguisher.getXCoordinate())
        .yCoordinate(extinguisher.getYCoordinate())
        .expiryDate(extinguisher.getExpiryDate())
        .nextInspectionAt(extinguisher.getNextInspectionAt())
        .hasCoordinate(hasCoordinate(extinguisher))
        .build();
  }

  private FireInspectionLogResponse toInspectionLog(FireExtinguisherInspection inspection) {
    FireExtinguisher extinguisher = inspection.getFireExtinguisher();
    Parking parking = extinguisher.getParking();
    Floor floor = extinguisher.getFloor();
    Zone zone = extinguisher.getZone();
    User user = inspection.getInspectedBy();
    return FireInspectionLogResponse.builder()
        .id(inspection.getId())
        .fireExtinguisherId(extinguisher.getId())
        .fireExtinguisherCode(extinguisher.getCode())
        .parkingId(parking.getId())
        .parkingName(parking.getName())
        .floorId(floor.getId())
        .floorName(floor.getName())
        .zoneId(zone == null ? null : zone.getId())
        .zoneName(zone == null ? null : zone.getName())
        .inspectedBy(user == null ? null : user.getId())
        .inspectedByName(user == null ? null : user.getFullName())
        .result(inspection.getResult())
        .pressureOk(inspection.getPressureOk())
        .sealOk(inspection.getSealOk())
        .locationOk(inspection.getLocationOk())
        .expiryOk(inspection.getExpiryOk())
        .photoUrl(inspection.getPhotoUrl())
        .note(inspection.getNote())
        .inspectedAt(inspection.getInspectedAt())
        .nextInspectionAt(inspection.getNextInspectionAt())
        .build();
  }

  private boolean hasCoordinate(FireExtinguisher extinguisher) {
    return extinguisher.getXCoordinate() != null && extinguisher.getYCoordinate() != null;
  }

  private void requirePercent(BigDecimal value, String field) {
    if (value == null
        || value.compareTo(BigDecimal.ZERO) < 0
        || value.compareTo(BigDecimal.valueOf(100)) > 0) {
      throw new ApiException(ErrorCode.INVALID_INPUT, field + " must be between 0 and 100");
    }
  }

  private String normalizeCode(String code) {
    if (code == null || code.isBlank()) {
      throw new ApiException(ErrorCode.INVALID_INPUT, "code must not be blank");
    }
    return code.trim().toUpperCase();
  }

  private String trimToNull(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }

  private Tenant currentTenantReference() {
    return tenantRepository.getReferenceById(currentTenantId());
  }

  private UUID currentTenantId() {
    return TenantContext.getTenantId()
        .orElseThrow(() -> new ApiException(ErrorCode.UNAUTHENTICATED));
  }

  private record ValidatedLocation(Parking parking, Floor floor, Zone zone) {}
}
