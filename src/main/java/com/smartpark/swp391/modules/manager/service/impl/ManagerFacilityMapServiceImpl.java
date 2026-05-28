package com.smartpark.swp391.modules.manager.service.impl;

import com.smartpark.swp391.common.exception.ApiException;
import com.smartpark.swp391.common.exception.ErrorCode;
import com.smartpark.swp391.infrastructure.cached.redis.service.ManagerFacilityCacheService;
import com.smartpark.swp391.infrastructure.tenant.TenantContext;
import com.smartpark.swp391.modules.manager.dto.map.FloorMapDetailResponse;
import com.smartpark.swp391.modules.manager.dto.map.FloorMapRequest;
import com.smartpark.swp391.modules.manager.dto.map.FloorMapResponse;
import com.smartpark.swp391.modules.manager.dto.map.SlotCoordinateBulkRequest;
import com.smartpark.swp391.modules.manager.dto.map.SlotCoordinateBulkResponse;
import com.smartpark.swp391.modules.manager.dto.map.SlotCoordinateRequest;
import com.smartpark.swp391.modules.manager.dto.map.SlotCoordinateResponse;
import com.smartpark.swp391.modules.manager.dto.map.SlotMapItemResponse;
import com.smartpark.swp391.modules.manager.service.ManagerFacilityMapService;
import com.smartpark.swp391.modules.parking.entity.Floor;
import com.smartpark.swp391.modules.parking.entity.Parking;
import com.smartpark.swp391.modules.parking.entity.Slot;
import com.smartpark.swp391.modules.parking.entity.Zone;
import com.smartpark.swp391.modules.parking.repository.FloorRepository;
import com.smartpark.swp391.modules.parking.repository.SlotRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class ManagerFacilityMapServiceImpl implements ManagerFacilityMapService {

  FloorRepository floorRepository;
  SlotRepository slotRepository;
  ManagerFacilityCacheService managerFacilityCacheService;

  @Override
  @Transactional
  public FloorMapResponse updateFloorMap(UUID floorId, FloorMapRequest request) {
    Floor floor = getFloorOrThrow(floorId);
    String mapImageUrl = normalizeMapImageUrl(request.mapImageUrl());
    floor.setMapImageUrl(mapImageUrl);
    Floor saved = floorRepository.save(floor);
    evictTopology(saved.getParking().getId());
    return toFloorMapResponse(saved);
  }

  @Override
  @Transactional(readOnly = true)
  public FloorMapDetailResponse getFloorMap(UUID floorId) {
    Floor floor = getFloorOrThrow(floorId);
    List<SlotMapItemResponse> slots =
        slotRepository
            .findAllByFloorIdAndTenantIdAndIsDeletedFalseOrderByCodeAsc(floorId, currentTenantId())
            .stream()
            .map(this::toSlotMapItemResponse)
            .toList();

    Parking parking = floor.getParking();
    return FloorMapDetailResponse.builder()
        .floorId(floor.getId())
        .floorName(floor.getName())
        .parkingId(parking.getId())
        .parkingName(parking.getName())
        .mapImageUrl(floor.getMapImageUrl())
        .coordinateMode("PERCENT")
        .slots(slots)
        .build();
  }

  @Override
  @Transactional
  public SlotCoordinateResponse updateSlotCoordinate(UUID slotId, SlotCoordinateRequest request) {
    Slot slot = getSlotOrThrow(slotId);
    setCoordinates(slot, request.xCoordinate(), request.yCoordinate());
    Slot saved = slotRepository.save(slot);
    evictTopology(saved.getParking().getId());
    return toSlotCoordinateResponse(saved);
  }

  @Override
  @Transactional
  public SlotCoordinateBulkResponse updateSlotCoordinates(SlotCoordinateBulkRequest request) {
    List<UUID> slotIds = request.items().stream().map(item -> item.slotId()).distinct().toList();
    if (slotIds.size() != request.items().size()) {
      throw new ApiException(ErrorCode.INVALID_INPUT, "Duplicate slotId in coordinate request");
    }

    List<Slot> slots = slotRepository.findAllById(slotIds);
    if (slots.size() != slotIds.size()
        || slots.stream().anyMatch(slot -> !currentTenantId().equals(slot.getTenant().getId()))) {
      throw new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "One or more slots were not found");
    }

    var coordinatesBySlotId =
        request.items().stream().collect(Collectors.toMap(item -> item.slotId(), item -> item));
    Set<UUID> parkingIds =
        slots.stream().map(slot -> slot.getParking().getId()).collect(Collectors.toSet());
    slots.forEach(
        slot -> {
          var item = coordinatesBySlotId.get(slot.getId());
          setCoordinates(slot, item.xCoordinate(), item.yCoordinate());
        });

    slotRepository.saveAll(slots);
    parkingIds.forEach(this::evictTopology);
    return SlotCoordinateBulkResponse.builder().updatedCount(slots.size()).build();
  }

  private Floor getFloorOrThrow(UUID id) {
    return floorRepository
        .findByIdAndTenantIdAndDeletedFalse(id, currentTenantId())
        .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Floor not found"));
  }

  private Slot getSlotOrThrow(UUID id) {
    return slotRepository
        .findByIdAndTenantIdAndIsDeletedFalse(id, currentTenantId())
        .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Slot not found"));
  }

  private void setCoordinates(Slot slot, BigDecimal xCoordinate, BigDecimal yCoordinate) {
    requirePercent(xCoordinate, "xCoordinate");
    requirePercent(yCoordinate, "yCoordinate");
    slot.setXCoordinate(xCoordinate);
    slot.setYCoordinate(yCoordinate);
  }

  private void requirePercent(BigDecimal value, String field) {
    if (value == null
        || value.compareTo(BigDecimal.ZERO) < 0
        || value.compareTo(BigDecimal.valueOf(100)) > 0) {
      throw new ApiException(ErrorCode.INVALID_INPUT, field + " must be between 0 and 100");
    }
  }

  private String normalizeMapImageUrl(String value) {
    String normalized = value == null ? "" : value.trim();
    if (normalized.isBlank()) {
      throw new ApiException(ErrorCode.INVALID_INPUT, "mapImageUrl must not be blank");
    }
    String lower = normalized.toLowerCase(Locale.ROOT);
    String tenantPrefix = "tenants/" + currentTenantId() + "/";
    if (lower.startsWith("http://")
        || lower.startsWith("https://")
        || normalized.startsWith(tenantPrefix)) {
      return normalized;
    }
    throw new ApiException(
        ErrorCode.INVALID_INPUT, "mapImageUrl must be http(s) URL or current tenant object key");
  }

  private FloorMapResponse toFloorMapResponse(Floor floor) {
    Parking parking = floor.getParking();
    return FloorMapResponse.builder()
        .floorId(floor.getId())
        .floorName(floor.getName())
        .parkingId(parking.getId())
        .parkingName(parking.getName())
        .mapImageUrl(floor.getMapImageUrl())
        .build();
  }

  private SlotMapItemResponse toSlotMapItemResponse(Slot slot) {
    Zone zone = slot.getZone();
    return SlotMapItemResponse.builder()
        .slotId(slot.getId())
        .slotCode(slot.getCode())
        .zoneId(zone.getId())
        .zoneName(zone.getName())
        .status(slot.getStatus())
        .xCoordinate(slot.getXCoordinate())
        .yCoordinate(slot.getYCoordinate())
        .hasCoordinate(slot.getXCoordinate() != null && slot.getYCoordinate() != null)
        .build();
  }

  private SlotCoordinateResponse toSlotCoordinateResponse(Slot slot) {
    return SlotCoordinateResponse.builder()
        .slotId(slot.getId())
        .xCoordinate(slot.getXCoordinate())
        .yCoordinate(slot.getYCoordinate())
        .hasCoordinate(slot.getXCoordinate() != null && slot.getYCoordinate() != null)
        .build();
  }

  private UUID currentTenantId() {
    return TenantContext.getTenantId()
        .orElseThrow(() -> new ApiException(ErrorCode.UNAUTHENTICATED));
  }

  private void evictTopology(UUID parkingId) {
    managerFacilityCacheService.evictTopology(currentTenantId(), parkingId);
  }
}
