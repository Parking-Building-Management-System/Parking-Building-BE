package com.smartpark.swp391.modules.pwa.service.impl;

import com.smartpark.swp391.common.exception.ApiException;
import com.smartpark.swp391.common.exception.ErrorCode;
import com.smartpark.swp391.infrastructure.storage.dto.PresignedDownload;
import com.smartpark.swp391.infrastructure.storage.service.StorageService;
import com.smartpark.swp391.modules.operation.entity.ParkingSession;
import com.smartpark.swp391.modules.operation.enumType.ParkingSessionStatus;
import com.smartpark.swp391.modules.operation.repository.ParkingSessionRepository;
import com.smartpark.swp391.modules.parking.entity.Floor;
import com.smartpark.swp391.modules.parking.entity.Parking;
import com.smartpark.swp391.modules.parking.entity.RfidCard;
import com.smartpark.swp391.modules.parking.entity.Slot;
import com.smartpark.swp391.modules.parking.entity.Zone;
import com.smartpark.swp391.modules.parking.enumType.RfidCardStatus;
import com.smartpark.swp391.modules.parking.repository.RfidCardRepository;
import com.smartpark.swp391.modules.pwa.dto.CardActiveSessionResponse;
import com.smartpark.swp391.modules.pwa.service.PwaCardSessionService;
import java.util.Locale;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class PwaCardSessionServiceImpl implements PwaCardSessionService {

  RfidCardRepository rfidCardRepository;
  ParkingSessionRepository parkingSessionRepository;
  StorageService storageService;

  @Override
  @Transactional(readOnly = true)
  public CardActiveSessionResponse getActiveSession(String qrToken) {
    RfidCard card =
        rfidCardRepository
            .findByQrToken(normalizeToken(qrToken))
            .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "CARD_QR_NOT_FOUND"));

    if (card.getStatus() != RfidCardStatus.ACTIVE) {
      throw new ApiException(ErrorCode.FORBIDDEN_ACTION, "CARD_NOT_ACTIVE");
    }

    ParkingSession session =
        parkingSessionRepository
            .findActiveByRfidCardId(card.getId(), ParkingSessionStatus.ACTIVE, PageRequest.of(0, 1))
            .stream()
            .findFirst()
            .orElseThrow(
                () -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "NO_ACTIVE_SESSION_FOR_CARD"));

    return toResponse(session);
  }

  private CardActiveSessionResponse toResponse(ParkingSession session) {
    Parking parking = session.getParking();
    Zone zone = session.getZone();
    Slot slot = session.getSlot();
    Floor floor = slot.getFloor();
    String guideText = buildGuideText(floor, zone, slot);
    String mapImageUrl = mapImageUrl(floor);
    MapDisplay mapDisplay = resolveMapDisplay(session, mapImageUrl);

    return CardActiveSessionResponse.builder()
        .sessionId(session.getId())
        .plateNumber(session.getLicensePlate())
        .licensePlate(session.getLicensePlate())
        .cardCode(session.getRfidCard().getCode())
        .checkInAt(session.getCheckInAt())
        .parkingId(parking.getId())
        .parkingName(parking.getName())
        .floorId(floor == null ? null : floor.getId())
        .floorName(floor == null ? null : floor.getName())
        .zoneId(zone.getId())
        .zoneName(zone.getName())
        .slotId(slot.getId())
        .slotCode(slot.getCode())
        .xCoordinate(slot.getXCoordinate())
        .yCoordinate(slot.getYCoordinate())
        .coordinateMode("PERCENT")
        .mapImageUrl(mapImageUrl)
        .mapDisplayUrl(mapDisplay.url())
        .mapUrlExpiresInSeconds(mapDisplay.expiresInSeconds())
        .status(session.getStatus())
        .guideText(guideText)
        .build();
  }

  private String mapImageUrl(Floor floor) {
    if (floor == null || floor.getMapImageUrl() == null || floor.getMapImageUrl().isBlank()) {
      return null;
    }
    return floor.getMapImageUrl();
  }

  private MapDisplay resolveMapDisplay(ParkingSession session, String mapImageUrl) {
    if (mapImageUrl == null) {
      return new MapDisplay(null, null);
    }

    String normalizedMapImageUrl = mapImageUrl.trim();
    if (isHttpUrl(normalizedMapImageUrl)) {
      return new MapDisplay(normalizedMapImageUrl, null);
    }

    PresignedDownload download =
        storageService.createPresignedDownload(session.getTenant().getId(), normalizedMapImageUrl);
    return new MapDisplay(download.downloadUrl(), download.expiresInSeconds());
  }

  private boolean isHttpUrl(String value) {
    String lower = value.toLowerCase(Locale.ROOT);
    return lower.startsWith("http://") || lower.startsWith("https://");
  }

  private String buildGuideText(Floor floor, Zone zone, Slot slot) {
    if (floor == null) {
      return "Xe cua ban o khu " + zone.getName() + ", slot " + slot.getCode() + ".";
    }
    return "Xe cua ban o tang "
        + floor.getName()
        + ", khu "
        + zone.getName()
        + ", slot "
        + slot.getCode()
        + ".";
  }

  private String normalizeToken(String qrToken) {
    if (qrToken == null || qrToken.isBlank()) {
      throw new ApiException(ErrorCode.INVALID_INPUT, "qrToken must not be blank");
    }
    return qrToken.trim();
  }

  private record MapDisplay(String url, Long expiresInSeconds) {}
}
