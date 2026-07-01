package com.smartpark.swp391.modules.staff.dto.lostcard;

import com.smartpark.swp391.modules.penalty.dto.PenaltyCaseResponse;
import lombok.Builder;

@Builder
public record StaffLostCardCaseResponse(PenaltyCaseResponse penaltyCase, String message) {}
