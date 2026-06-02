package com.smartpark.swp391.modules.staff.service;

import com.smartpark.swp391.modules.staff.dto.AvailableRfidCardResponse;
import java.util.List;

public interface StaffRfidCardService {

  List<AvailableRfidCardResponse> getAvailableCards(String search, Integer limit);
}
