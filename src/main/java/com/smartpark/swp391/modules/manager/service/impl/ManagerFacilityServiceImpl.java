package com.smartpark.swp391.modules.manager.service.impl;

import com.smartpark.swp391.common.exception.ApiException;
import com.smartpark.swp391.common.exception.ErrorCode;
import com.smartpark.swp391.infrastructure.cached.redis.service.ManagerFacilityCacheService;
import com.smartpark.swp391.infrastructure.tenant.TenantContext;
import com.smartpark.swp391.modules.identity.entity.Tenant;
import com.smartpark.swp391.modules.identity.repository.TenantRepository;
import com.smartpark.swp391.modules.manager.dto.facility.FloorRequest;
import com.smartpark.swp391.modules.manager.dto.facility.FloorResponse;
import com.smartpark.swp391.modules.manager.dto.facility.ParkingRequest;
import com.smartpark.swp391.modules.manager.dto.facility.ParkingResponse;
import com.smartpark.swp391.modules.manager.dto.facility.ParkingStatusRequest;
import com.smartpark.swp391.modules.manager.dto.facility.ParkingStatusResponse;
import com.smartpark.swp391.modules.manager.dto.facility.ZoneRequest;
import com.smartpark.swp391.modules.manager.dto.facility.ZoneResponse;
import com.smartpark.swp391.modules.manager.dto.topology.FloorTopologyResponse;
import com.smartpark.swp391.modules.manager.dto.topology.ParkingTopologyResponse;
import com.smartpark.swp391.modules.manager.dto.topology.ZoneTopologyResponse;
import com.smartpark.swp391.modules.manager.service.ManagerFacilityService;
import com.smartpark.swp391.modules.parking.entity.Floor;
import com.smartpark.swp391.modules.parking.entity.Parking;
import com.smartpark.swp391.modules.parking.entity.Zone;
import com.smartpark.swp391.modules.parking.enumType.ParkingStatus;
import com.smartpark.swp391.modules.parking.enumType.ZoneStatus;
import com.smartpark.swp391.modules.parking.repository.FloorRepository;
import com.smartpark.swp391.modules.parking.repository.ParkingRepository;
import com.smartpark.swp391.modules.parking.repository.SlotRepository;
import com.smartpark.swp391.modules.parking.repository.ZoneRepository;
import com.smartpark.swp391.modules.vehicle.entity.VehicleType;
import com.smartpark.swp391.modules.vehicle.repository.VehicleTypeRepository;
import java.util.Collections;
import java.util.List;
import java.util.Map;
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
public class ManagerFacilityServiceImpl implements ManagerFacilityService {

  ParkingRepository parkingRepository;
  FloorRepository floorRepository;
  ZoneRepository zoneRepository;
  SlotRepository slotRepository;
  VehicleTypeRepository vehicleTypeRepository;
  TenantRepository tenantRepository;
  ManagerFacilityCacheService managerFacilityCacheService;

  @Override
  @Transactional(readOnly = true)
  public List<ParkingResponse> getParkings() {
    return parkingRepository
        .findAllByTenantIdAndIsDeletedFalseOrderByNameAsc(currentTenantId())
        .stream()
        .map(this::toParkingResponse)
        .toList();
  }

  @Override
  @Transactional(readOnly = true)
  public ParkingResponse getParking(UUID id) {
    return toParkingResponse(getParkingOrThrow(id));
  }

  @Override
  @Transactional
  public ParkingResponse createParking(ParkingRequest request) {
    String code = normalizeCode(request.code());
    UUID tenantId = currentTenantId();
    if (parkingRepository.existsByTenantIdAndCodeIgnoreCaseAndIsDeletedFalse(tenantId, code)) {
      throw new ApiException(ErrorCode.DUPLICATE_RESOURCE, "Parking code already exists");
    }

    Parking parking =
        Parking.builder()
            .tenant(currentTenantReference())
            .code(code)
            .name(request.name().trim())
            .address(trimToNull(request.address()))
            .totalCapacity(0)
            .status(request.status() == null ? ParkingStatus.ACTIVE : request.status())
            .isDeleted(false)
            .build();

    return toParkingResponse(parkingRepository.save(parking));
  }

  @Override
  @Transactional
  public ParkingResponse updateParking(UUID id, ParkingRequest request) {
    Parking parking = getParkingOrThrow(id);
    String code = normalizeCode(request.code());
    UUID tenantId = currentTenantId();
    if (parkingRepository.existsByTenantIdAndCodeIgnoreCaseAndIdNotAndIsDeletedFalse(
        tenantId, code, id)) {
      throw new ApiException(ErrorCode.DUPLICATE_RESOURCE, "Parking code already exists");
    }

    parking.setCode(code);
    parking.setName(request.name().trim());
    parking.setAddress(trimToNull(request.address()));
    if (request.status() != null) {
      parking.setStatus(request.status());
    }

    Parking saved = parkingRepository.save(parking);
    evictTopology(id);
    return toParkingResponse(saved);
  }

  @Override
  @Transactional
  public ParkingStatusResponse updateParkingStatus(UUID id, ParkingStatusRequest request) {
    Parking parking = getParkingOrThrow(id);
    ParkingStatus nextStatus =
        request == null
            ? toggleStatus(parking.getStatus())
            : request.status();

    parking.setStatus(nextStatus);
    parkingRepository.save(parking);
    evictTopology(parking.getId());

    return ParkingStatusResponse.builder().id(parking.getId()).status(nextStatus).build();
  }

  @Override
  @Transactional(readOnly = true)
  public ParkingTopologyResponse getTopology(UUID parkingId) {
    UUID tenantId = currentTenantId();
    return managerFacilityCacheService
        .getTopology(tenantId, parkingId)
        .orElseGet(
            () -> {
              ParkingTopologyResponse topology = buildTopology(parkingId);
              managerFacilityCacheService.saveTopology(tenantId, parkingId, topology);
              return topology;
            });
  }

  @Override
  @Transactional(readOnly = true)
  public List<FloorResponse> getFloors(UUID parkingId) {
    getParkingOrThrow(parkingId);
    return floorRepository
        .findAllByParkingIdAndDeletedFalseOrderByDisplayOrderAscNameAsc(parkingId)
        .stream()
        .map(this::toFloorResponse)
        .toList();
  }

  @Override
  @Transactional(readOnly = true)
  public FloorResponse getFloor(UUID id) {
    return toFloorResponse(getFloorOrThrow(id));
  }

  @Override
  @Transactional
  public FloorResponse createFloor(UUID parkingId, FloorRequest request) {
    Parking parking = getParkingOrThrow(parkingId);
    String code = normalizeCode(request.code());

    if (floorRepository.existsByParkingIdAndCodeIgnoreCaseAndDeletedFalse(parkingId, code)) {
      throw new ApiException(ErrorCode.DUPLICATE_RESOURCE, "Floor code already exists");
    }

    Floor floor =
        Floor.builder()
            .tenant(currentTenantReference())
            .parking(parking)
            .code(code)
            .name(request.name().trim())
            .displayOrder(request.displayOrder())
            .active(request.active() == null || request.active())
            .deleted(false)
            .build();

    Floor saved = floorRepository.save(floor);
    evictTopology(parkingId);
    return toFloorResponse(saved);
  }

  @Override
  @Transactional
  public FloorResponse updateFloor(UUID id, FloorRequest request) {
    Floor floor = getFloorOrThrow(id);
    UUID parkingId = floor.getParking().getId();
    String code = normalizeCode(request.code());

    if (floorRepository.existsByParkingIdAndCodeIgnoreCaseAndIdNotAndDeletedFalse(
        parkingId, code, id)) {
      throw new ApiException(ErrorCode.DUPLICATE_RESOURCE, "Floor code already exists");
    }

    floor.setCode(code);
    floor.setName(request.name().trim());
    floor.setDisplayOrder(request.displayOrder());
    if (request.active() != null) {
      floor.setActive(request.active());
    }

    Floor saved = floorRepository.save(floor);
    evictTopology(parkingId);
    return toFloorResponse(saved);
  }

  @Override
  @Transactional
  public void deleteFloor(UUID id) {
    Floor floor = getFloorOrThrow(id);
    long zoneCount = zoneRepository.countByFloorIdAndIsDeletedFalse(id);
    if (zoneCount > 0) {
      throw new ApiException(ErrorCode.INVALID_INPUT, "Cannot delete a floor that still has zones");
    }

    floor.setDeleted(true);
    floorRepository.save(floor);
    evictTopology(floor.getParking().getId());
  }

  @Override
  @Transactional(readOnly = true)
  public List<ZoneResponse> getZones(UUID floorId) {
    getFloorOrThrow(floorId);
    return zoneRepository.findAllByFloorIdAndIsDeletedFalseOrderByNameAsc(floorId).stream()
        .map(this::toZoneResponse)
        .toList();
  }

  @Override
  @Transactional(readOnly = true)
  public ZoneResponse getZone(UUID id) {
    return toZoneResponse(getZoneOrThrow(id));
  }

  @Override
  @Transactional
  public ZoneResponse createZone(UUID floorId, ZoneRequest request) {
    Floor floor = getFloorOrThrow(floorId);
    Parking parking = floor.getParking();
    String code = normalizeCode(request.code());

    if (zoneRepository.existsByParkingIdAndCodeIgnoreCaseAndIsDeletedFalse(parking.getId(), code)) {
      throw new ApiException(ErrorCode.DUPLICATE_RESOURCE, "Zone code already exists");
    }

    VehicleType vehicleType = getVehicleTypeOrThrow(request.vehicleTypeCode());
    Zone zone =
        Zone.builder()
            .tenant(currentTenantReference())
            .parking(parking)
            .floor(floor)
            .vehicleType(vehicleType)
            .code(code)
            .name(request.name().trim())
            .floorName(floor.getName())
            .capacity(request.capacity())
            .status(request.status() == null ? ZoneStatus.ACTIVE : request.status())
            .isDeleted(false)
            .build();

    Zone saved = zoneRepository.save(zone);
    evictTopology(parking.getId());
    return toZoneResponse(saved);
  }

  @Override
  @Transactional
  public ZoneResponse updateZone(UUID id, ZoneRequest request) {
    Zone zone = getZoneOrThrow(id);
    String code = normalizeCode(request.code());
    UUID parkingId = zone.getParking().getId();

    if (zoneRepository.existsByParkingIdAndCodeIgnoreCaseAndIdNotAndIsDeletedFalse(
        parkingId, code, id)) {
      throw new ApiException(ErrorCode.DUPLICATE_RESOURCE, "Zone code already exists");
    }

    VehicleType vehicleType = getVehicleTypeOrThrow(request.vehicleTypeCode());
    zone.setCode(code);
    zone.setName(request.name().trim());
    zone.setVehicleType(vehicleType);
    zone.setCapacity(request.capacity());
    if (request.status() != null) {
      zone.setStatus(request.status());
    }
    if (zone.getFloor() != null) {
      zone.setFloorName(zone.getFloor().getName());
    }

    Zone saved = zoneRepository.save(zone);
    evictTopology(parkingId);
    return toZoneResponse(saved);
  }

  @Override
  @Transactional
  public void deleteZone(UUID id) {
    Zone zone = getZoneOrThrow(id);
    long slotCount = slotRepository.countByZoneIdAndIsDeletedFalse(id);
    if (slotCount > 0) {
      throw new ApiException(ErrorCode.INVALID_INPUT, "Cannot delete a zone that still has slots");
    }

    zone.setDeleted(true);
    zoneRepository.save(zone);
    evictTopology(zone.getParking().getId());
  }

  private ParkingTopologyResponse buildTopology(UUID parkingId) {
    Parking parking = getParkingOrThrow(parkingId);
    List<Floor> floors =
        floorRepository.findAllByParkingIdAndDeletedFalseOrderByDisplayOrderAscNameAsc(parkingId);
    List<Zone> zones = zoneRepository.findAllByParkingIdAndIsDeletedFalseOrderByNameAsc(parkingId);
    Map<UUID, List<Zone>> zonesByFloorId =
        zones.stream()
            .filter(zone -> zone.getFloor() != null)
            .collect(Collectors.groupingBy(zone -> zone.getFloor().getId()));

    List<FloorTopologyResponse> floorResponses =
        floors.stream().map(floor -> toFloorTopologyResponse(floor, zonesByFloorId)).toList();

    return ParkingTopologyResponse.builder()
        .id(parking.getId())
        .code(parking.getCode())
        .name(parking.getName())
        .status(parking.getStatus())
        .totalCapacity((int) slotRepository.countByParkingIdAndIsDeletedFalse(parking.getId()))
        .floors(floorResponses)
        .build();
  }

  private FloorTopologyResponse toFloorTopologyResponse(
      Floor floor, Map<UUID, List<Zone>> zonesByFloorId) {
    List<ZoneTopologyResponse> zoneResponses =
        zonesByFloorId.getOrDefault(floor.getId(), Collections.emptyList()).stream()
            .map(this::toZoneTopologyResponse)
            .toList();

    return FloorTopologyResponse.builder()
        .id(floor.getId())
        .code(floor.getCode())
        .name(floor.getName())
        .displayOrder(floor.getDisplayOrder())
        .zones(zoneResponses)
        .build();
  }

  private ZoneTopologyResponse toZoneTopologyResponse(Zone zone) {
    VehicleType vehicleType = zone.getVehicleType();
    return ZoneTopologyResponse.builder()
        .id(zone.getId())
        .code(zone.getCode())
        .name(zone.getName())
        .vehicleTypeCode(vehicleType == null ? null : vehicleType.getCode())
        .vehicleTypeName(vehicleType == null ? null : vehicleType.getName())
        .capacity(zone.getCapacity())
        .slotCount(slotRepository.countByZoneIdAndIsDeletedFalse(zone.getId()))
        .status(zone.getStatus())
        .build();
  }

  private ParkingResponse toParkingResponse(Parking parking) {
    return ParkingResponse.builder()
        .id(parking.getId())
        .code(parking.getCode())
        .name(parking.getName())
        .address(parking.getAddress())
        .totalCapacity((int) slotRepository.countByParkingIdAndIsDeletedFalse(parking.getId()))
        .status(parking.getStatus())
        .build();
  }

  private FloorResponse toFloorResponse(Floor floor) {
    return FloorResponse.builder()
        .id(floor.getId())
        .parkingId(floor.getParking().getId())
        .code(floor.getCode())
        .name(floor.getName())
        .displayOrder(floor.getDisplayOrder())
        .active(floor.isActive())
        .build();
  }

  private ZoneResponse toZoneResponse(Zone zone) {
    VehicleType vehicleType = zone.getVehicleType();
    return ZoneResponse.builder()
        .id(zone.getId())
        .parkingId(zone.getParking().getId())
        .floorId(zone.getFloor() == null ? null : zone.getFloor().getId())
        .code(zone.getCode())
        .name(zone.getName())
        .vehicleTypeCode(vehicleType == null ? null : vehicleType.getCode())
        .vehicleTypeName(vehicleType == null ? null : vehicleType.getName())
        .capacity(zone.getCapacity())
        .status(zone.getStatus())
        .build();
  }

  private Parking getParkingOrThrow(UUID id) {
    return parkingRepository
        .findByIdAndTenantIdAndIsDeletedFalse(id, currentTenantId())
        .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Parking not found"));
  }

  private Floor getFloorOrThrow(UUID id) {
    return floorRepository
        .findByIdAndTenantIdAndDeletedFalse(id, currentTenantId())
        .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Floor not found"));
  }

  private Zone getZoneOrThrow(UUID id) {
    return zoneRepository
        .findByIdAndTenantIdAndIsDeletedFalse(id, currentTenantId())
        .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Zone not found"));
  }

  private VehicleType getVehicleTypeOrThrow(String code) {
    VehicleType vehicleType =
        vehicleTypeRepository
            .findByCodeIgnoreCaseAndDeletedFalse(normalizeCode(code))
            .orElseThrow(
                () -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Vehicle type not found"));

    if (!vehicleType.isActive()) {
      throw new ApiException(ErrorCode.INVALID_INPUT, "Vehicle type is inactive");
    }
    return vehicleType;
  }

  private Tenant currentTenantReference() {
    return tenantRepository.getReferenceById(currentTenantId());
  }

  private UUID currentTenantId() {
    return TenantContext.getTenantId()
        .orElseThrow(() -> new ApiException(ErrorCode.UNAUTHENTICATED));
  }

  private void evictTopology(UUID parkingId) {
    managerFacilityCacheService.evictTopology(currentTenantId(), parkingId);
  }

  private String normalizeCode(String code) {
    if (code == null || code.isBlank()) {
      throw new ApiException(ErrorCode.INVALID_INPUT, "Code must not be blank");
    }
    return code.trim();
  }

  private String trimToNull(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return value.trim();
  }

  private ParkingStatus toggleStatus(ParkingStatus currentStatus) {
    return ParkingStatus.ACTIVE.equals(currentStatus)
        ? ParkingStatus.MAINTENANCE
        : ParkingStatus.ACTIVE;
  }
}
