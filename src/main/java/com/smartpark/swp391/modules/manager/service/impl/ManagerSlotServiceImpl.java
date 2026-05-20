package com.smartpark.swp391.modules.manager.service.impl;

import com.smartpark.swp391.common.exception.ApiException;
import com.smartpark.swp391.common.exception.ErrorCode;
import com.smartpark.swp391.common.response.PageResponse;
import com.smartpark.swp391.infrastructure.cached.redis.service.ManagerFacilityCacheService;
import com.smartpark.swp391.infrastructure.tenant.TenantContext;
import com.smartpark.swp391.modules.identity.entity.Tenant;
import com.smartpark.swp391.modules.identity.repository.TenantRepository;
import com.smartpark.swp391.modules.manager.dto.slot.SlotBulkStatusRequest;
import com.smartpark.swp391.modules.manager.dto.slot.SlotBulkStatusResponse;
import com.smartpark.swp391.modules.manager.dto.slot.SlotImportResponse;
import com.smartpark.swp391.modules.manager.dto.slot.SlotResponse;
import com.smartpark.swp391.modules.manager.service.ManagerSlotService;
import com.smartpark.swp391.modules.parking.entity.Floor;
import com.smartpark.swp391.modules.parking.entity.Parking;
import com.smartpark.swp391.modules.parking.entity.Slot;
import com.smartpark.swp391.modules.parking.entity.Zone;
import com.smartpark.swp391.modules.parking.enumType.SlotStatus;
import com.smartpark.swp391.modules.parking.repository.FloorRepository;
import com.smartpark.swp391.modules.parking.repository.ParkingRepository;
import com.smartpark.swp391.modules.parking.repository.SlotRepository;
import com.smartpark.swp391.modules.parking.repository.ZoneRepository;
import jakarta.persistence.criteria.Predicate;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class ManagerSlotServiceImpl implements ManagerSlotService {

  private static final Set<SlotStatus> BULK_ALLOWED_STATUSES =
      Set.of(SlotStatus.AVAILABLE, SlotStatus.MAINTENANCE, SlotStatus.LOCKED);

  SlotRepository slotRepository;
  ParkingRepository parkingRepository;
  FloorRepository floorRepository;
  ZoneRepository zoneRepository;
  TenantRepository tenantRepository;
  ManagerFacilityCacheService managerFacilityCacheService;

  @Override
  @Transactional(readOnly = true)
  public PageResponse<SlotResponse> getSlots(
      UUID zoneId, SlotStatus status, String slotCode, boolean exact, int page, int size) {
    Specification<Slot> spec = buildSlotSpecification(zoneId, status, slotCode, exact);
    var pageable = PageRequest.of(page, size, Sort.by("code").ascending());
    var result = slotRepository.findAll(spec, pageable);

    return new PageResponse<>(
        result.getContent().stream().map(this::toSlotResponse).toList(),
        result.getNumber(),
        result.getSize(),
        result.getTotalElements(),
        result.getTotalPages());
  }

  @Override
  @Transactional
  public SlotBulkStatusResponse bulkUpdateStatus(SlotBulkStatusRequest request) {
    if (!BULK_ALLOWED_STATUSES.contains(request.newStatus())) {
      throw new ApiException(
          ErrorCode.INVALID_INPUT, "Bulk status must be AVAILABLE, MAINTENANCE, or LOCKED");
    }

    List<UUID> slotIds = request.slotIds().stream().distinct().toList();
    List<Slot> slots = slotRepository.findAllById(slotIds);
    if (slots.size() != slotIds.size()) {
      throw new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "One or more slots were not found");
    }

    Set<UUID> parkingIds = new HashSet<>();
    slots.forEach(slot -> parkingIds.add(slot.getParking().getId()));

    int updatedCount =
        slotRepository.bulkUpdateStatus(currentTenantId(), slotIds, request.newStatus());
    parkingIds.forEach(this::evictTopology);

    return SlotBulkStatusResponse.builder()
        .updatedCount(updatedCount)
        .newStatus(request.newStatus())
        .build();
  }

  @Override
  @Transactional
  public SlotImportResponse importSlots(MultipartFile file) {
    if (file == null || file.isEmpty()) {
      throw new ApiException(ErrorCode.INVALID_INPUT, "Import file must not be empty");
    }

    try (var inputStream = file.getInputStream();
        Workbook workbook = WorkbookFactory.create(inputStream)) {
      Sheet sheet = workbook.getNumberOfSheets() == 0 ? null : workbook.getSheetAt(0);
      if (sheet == null) {
        return SlotImportResponse.builder().insertedCount(0).build();
      }

      DataFormatter formatter = new DataFormatter();
      Map<String, Integer> headers = readHeaders(sheet.getRow(0), formatter);
      requireHeaders(headers, "parkingCode", "floorCode", "zoneCode", "slotCode");

      Tenant tenant = currentTenantReference();
      ImportLookup lookup = new ImportLookup();
      List<Slot> slots = new ArrayList<>();
      Set<String> fileDuplicateGuard = new LinkedHashSet<>();
      Set<UUID> parkingIds = new HashSet<>();

      for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
        Row row = sheet.getRow(rowIndex);
        if (isBlankRow(row, formatter)) {
          continue;
        }

        Slot slot = parseSlotRow(row, headers, formatter, tenant, lookup, fileDuplicateGuard);
        slots.add(slot);
        parkingIds.add(slot.getParking().getId());
      }

      slotRepository.saveAll(slots);
      parkingIds.forEach(this::evictTopology);
      return SlotImportResponse.builder().insertedCount(slots.size()).build();
    } catch (IOException e) {
      throw new ApiException(ErrorCode.INVALID_INPUT, "Cannot read import file");
    }
  }

  @Override
  @Transactional(readOnly = true)
  public byte[] exportSlots() {
    List<Slot> slots = slotRepository.findAllForExport();

    try (Workbook workbook = new XSSFWorkbook();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
      Sheet sheet = workbook.createSheet("slots");
      String[] headers = {
        "Parking Code",
        "Parking Name",
        "Floor Code",
        "Floor Name",
        "Zone Code",
        "Zone Name",
        "Slot Code",
        "Slot Number",
        "Status"
      };

      Row headerRow = sheet.createRow(0);
      for (int i = 0; i < headers.length; i++) {
        headerRow.createCell(i).setCellValue(headers[i]);
      }

      for (int i = 0; i < slots.size(); i++) {
        writeSlotRow(sheet.createRow(i + 1), slots.get(i));
      }

      for (int i = 0; i < headers.length; i++) {
        sheet.autoSizeColumn(i);
      }

      workbook.write(outputStream);
      return outputStream.toByteArray();
    } catch (IOException e) {
      throw new ApiException(ErrorCode.UNEXPECTED_ERROR, "Cannot export slots");
    }
  }

  private Specification<Slot> buildSlotSpecification(
      UUID zoneId, SlotStatus status, String slotCode, boolean exact) {
    return (root, query, criteriaBuilder) -> {
      List<Predicate> predicates = new ArrayList<>();
      predicates.add(criteriaBuilder.isFalse(root.get("isDeleted")));

      if (zoneId != null) {
        predicates.add(criteriaBuilder.equal(root.get("zone").get("id"), zoneId));
      }

      if (status != null) {
        predicates.add(criteriaBuilder.equal(root.get("status"), status));
      }

      if (slotCode != null && !slotCode.isBlank()) {
        String normalized = slotCode.trim().toLowerCase(Locale.ROOT);
        if (exact) {
          predicates.add(
              criteriaBuilder.equal(criteriaBuilder.lower(root.get("code")), normalized));
        } else {
          predicates.add(
              criteriaBuilder.like(
                  criteriaBuilder.lower(root.get("code")), "%" + normalized + "%"));
        }
      }

      return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
    };
  }

  private Slot parseSlotRow(
      Row row,
      Map<String, Integer> headers,
      DataFormatter formatter,
      Tenant tenant,
      ImportLookup lookup,
      Set<String> fileDuplicateGuard) {
    int rowNumber = row.getRowNum() + 1;
    String parkingCode = requiredCell(row, headers, formatter, "parkingCode", rowNumber);
    String floorCode = requiredCell(row, headers, formatter, "floorCode", rowNumber);
    String zoneCode = requiredCell(row, headers, formatter, "zoneCode", rowNumber);
    String slotCode = requiredCell(row, headers, formatter, "slotCode", rowNumber);
    String slotNumber = optionalCell(row, headers, formatter, "slotNumber");
    String statusValue = optionalCell(row, headers, formatter, "status");

    Parking parking = lookup.parking(parkingCode, rowNumber);
    Floor floor = lookup.floor(parking, floorCode, rowNumber);
    Zone zone = lookup.zone(floor, zoneCode, rowNumber);
    if (!parking.getId().equals(zone.getParking().getId())) {
      throw new ApiException(ErrorCode.INVALID_INPUT, "Row " + rowNumber + ": zone mismatch");
    }

    String normalizedSlotCode = slotCode.trim();
    String duplicateKey = zone.getId() + ":" + normalizedSlotCode.toLowerCase(Locale.ROOT);
    if (!fileDuplicateGuard.add(duplicateKey)
        || slotRepository
            .findByZoneIdAndCodeIgnoreCaseAndIsDeletedFalse(zone.getId(), normalizedSlotCode)
            .isPresent()) {
      throw new ApiException(
          ErrorCode.DUPLICATE_RESOURCE,
          "Row " + rowNumber + ": slot code already exists in this zone");
    }

    return Slot.builder()
        .tenant(tenant)
        .parking(parking)
        .floor(floor)
        .zone(zone)
        .code(normalizedSlotCode)
        .slotNumber(
            slotNumber == null || slotNumber.isBlank() ? normalizedSlotCode : slotNumber.trim())
        .status(parseStatus(statusValue, rowNumber))
        .isDeleted(false)
        .build();
  }

  private SlotStatus parseStatus(String value, int rowNumber) {
    if (value == null || value.isBlank()) {
      return SlotStatus.AVAILABLE;
    }

    try {
      return SlotStatus.valueOf(value.trim().toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException e) {
      throw new ApiException(ErrorCode.INVALID_INPUT, "Row " + rowNumber + ": invalid slot status");
    }
  }

  private Map<String, Integer> readHeaders(Row row, DataFormatter formatter) {
    if (row == null) {
      throw new ApiException(ErrorCode.INVALID_INPUT, "Import file must include a header row");
    }

    Map<String, Integer> headers = new HashMap<>();
    for (Cell cell : row) {
      String value = formatter.formatCellValue(cell);
      if (value != null && !value.isBlank()) {
        headers.put(canonicalHeader(value), cell.getColumnIndex());
      }
    }
    return headers;
  }

  private void requireHeaders(Map<String, Integer> headers, String... names) {
    for (String name : names) {
      if (!headers.containsKey(canonicalHeader(name))) {
        throw new ApiException(ErrorCode.INVALID_INPUT, "Missing import header: " + name);
      }
    }
  }

  private String requiredCell(
      Row row, Map<String, Integer> headers, DataFormatter formatter, String name, int rowNumber) {
    String value = optionalCell(row, headers, formatter, name);
    if (value == null || value.isBlank()) {
      throw new ApiException(
          ErrorCode.INVALID_INPUT, "Row " + rowNumber + ": " + name + " must not be blank");
    }
    return value.trim();
  }

  private String optionalCell(
      Row row, Map<String, Integer> headers, DataFormatter formatter, String name) {
    Integer index = headers.get(canonicalHeader(name));
    if (index == null || row == null) {
      return null;
    }
    return formatter.formatCellValue(row.getCell(index));
  }

  private boolean isBlankRow(Row row, DataFormatter formatter) {
    if (row == null) {
      return true;
    }

    for (Cell cell : row) {
      if (!formatter.formatCellValue(cell).isBlank()) {
        return false;
      }
    }
    return true;
  }

  private String canonicalHeader(String value) {
    return value.replaceAll("[^A-Za-z0-9]", "").toLowerCase(Locale.ROOT);
  }

  private void writeSlotRow(Row row, Slot slot) {
    Floor floor = slot.getFloor();
    Zone zone = slot.getZone();
    Parking parking = slot.getParking();

    row.createCell(0).setCellValue(parking.getCode());
    row.createCell(1).setCellValue(parking.getName());
    row.createCell(2).setCellValue(floor == null ? "" : floor.getCode());
    row.createCell(3).setCellValue(floor == null ? "" : floor.getName());
    row.createCell(4).setCellValue(zone.getCode());
    row.createCell(5).setCellValue(zone.getName());
    row.createCell(6).setCellValue(slot.getCode());
    row.createCell(7).setCellValue(slot.getSlotNumber());
    row.createCell(8).setCellValue(slot.getStatus().name());
  }

  private SlotResponse toSlotResponse(Slot slot) {
    Floor floor = slot.getFloor();
    return SlotResponse.builder()
        .id(slot.getId())
        .parkingId(slot.getParking().getId())
        .parkingName(slot.getParking().getName())
        .floorId(floor == null ? null : floor.getId())
        .floorName(floor == null ? null : floor.getName())
        .zoneId(slot.getZone().getId())
        .zoneName(slot.getZone().getName())
        .code(slot.getCode())
        .slotNumber(slot.getSlotNumber())
        .status(slot.getStatus())
        .build();
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

  private final class ImportLookup {
    private final Map<String, Parking> parkings = new HashMap<>();
    private final Map<String, Floor> floors = new HashMap<>();
    private final Map<String, Zone> zones = new HashMap<>();

    private Parking parking(String code, int rowNumber) {
      String key = code.trim().toLowerCase(Locale.ROOT);
      return parkings.computeIfAbsent(
          key,
          ignored ->
              parkingRepository
                  .findByCodeIgnoreCaseAndIsDeletedFalse(code.trim())
                  .orElseThrow(
                      () ->
                          new ApiException(
                              ErrorCode.RESOURCE_NOT_FOUND,
                              "Row " + rowNumber + ": parking not found")));
    }

    private Floor floor(Parking parking, String code, int rowNumber) {
      String key = parking.getId() + ":" + code.trim().toLowerCase(Locale.ROOT);
      return floors.computeIfAbsent(
          key,
          ignored ->
              floorRepository
                  .findByParkingIdAndCodeIgnoreCaseAndDeletedFalse(parking.getId(), code.trim())
                  .orElseThrow(
                      () ->
                          new ApiException(
                              ErrorCode.RESOURCE_NOT_FOUND,
                              "Row " + rowNumber + ": floor not found")));
    }

    private Zone zone(Floor floor, String code, int rowNumber) {
      String key = floor.getId() + ":" + code.trim().toLowerCase(Locale.ROOT);
      return zones.computeIfAbsent(
          key,
          ignored ->
              zoneRepository
                  .findByFloorIdAndCodeIgnoreCaseAndIsDeletedFalse(floor.getId(), code.trim())
                  .orElseThrow(
                      () ->
                          new ApiException(
                              ErrorCode.RESOURCE_NOT_FOUND,
                              "Row " + rowNumber + ": zone not found")));
    }
  }
}
