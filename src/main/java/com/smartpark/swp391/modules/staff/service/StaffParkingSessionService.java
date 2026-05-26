package com.smartpark.swp391.modules.staff.service;

import com.smartpark.swp391.modules.staff.dto.ParkingSessionCheckInRequest;
import com.smartpark.swp391.modules.staff.dto.ParkingSessionCheckInResponse;

public interface StaffParkingSessionService {

  ParkingSessionCheckInResponse checkIn(ParkingSessionCheckInRequest request);
}
