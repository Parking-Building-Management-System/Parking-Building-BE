package com.smartpark.swp391.modules.penalty.service;

import com.smartpark.swp391.modules.operation.entity.ParkingSession;
import com.smartpark.swp391.modules.parking.entity.Slot;
import com.smartpark.swp391.modules.penalty.dto.PenaltyCaseResponse;
import com.smartpark.swp391.modules.penalty.entity.PenaltyCase;
import org.springframework.stereotype.Component;

@Component
public class PenaltyCaseResponseMapper {

  public PenaltyCaseResponse toResponse(PenaltyCase penaltyCase) {
    Slot reportedSlot = penaltyCase.getReportedSlot();
    Slot reassignedSlot = penaltyCase.getReassignedSlot();
    ParkingSession targetSession = penaltyCase.getTargetSession();
    ParkingSession victimSession = penaltyCase.getVictimSession();
    ParkingSession offenderSession = penaltyCase.getOffenderSession();
    return PenaltyCaseResponse.builder()
        .id(penaltyCase.getId())
        .type(penaltyCase.getType())
        .name(
            penaltyCase.getRule() == null
                ? penaltyCase.getType().name()
                : penaltyCase.getRule().getName())
        .amount(penaltyCase.getAmount())
        .currency(penaltyCase.getCurrency())
        .status(penaltyCase.getStatus())
        .targetSessionId(targetSession == null ? null : targetSession.getId())
        .victimSessionId(victimSession == null ? null : victimSession.getId())
        .offenderSessionId(offenderSession == null ? null : offenderSession.getId())
        .targetLicensePlate(penaltyCase.getTargetLicensePlate())
        .offenderLicensePlate(penaltyCase.getOffenderLicensePlate())
        .reportedSlotId(reportedSlot == null ? null : reportedSlot.getId())
        .reportedSlotCode(reportedSlot == null ? null : reportedSlot.getCode())
        .reassignedSlotId(reassignedSlot == null ? null : reassignedSlot.getId())
        .reassignedSlotCode(reassignedSlot == null ? null : reassignedSlot.getCode())
        .evidenceImageUrl(penaltyCase.getEvidenceImageUrl())
        .identityImageUrl(penaltyCase.getIdentityImageUrl())
        .vehicleImageUrl(penaltyCase.getVehicleImageUrl())
        .licensePlateImageUrl(penaltyCase.getLicensePlateImageUrl())
        .note(penaltyCase.getNote())
        .createdAt(penaltyCase.getCreatedAt())
        .collectedAt(penaltyCase.getCollectedAt())
        .build();
  }
}
