package com.smartpark.swp391.modules.manager.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.smartpark.swp391.modules.vehicle.entity.VehicleType;
import com.smartpark.swp391.modules.vehicle.repository.VehicleTypeRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ManagerMasterDataServiceImplTest {

  @Mock VehicleTypeRepository vehicleTypeRepository;

  @Test
  void getActiveVehicleTypesUsesActiveNonDeletedRepositoryQuery() {
    VehicleType car = VehicleType.builder().name("Car").code("CAR").active(true).build();
    car.setId(UUID.randomUUID());
    when(vehicleTypeRepository.findAllByActiveTrueAndDeletedFalseOrderByNameAsc())
        .thenReturn(List.of(car));

    var result = new ManagerMasterDataServiceImpl(vehicleTypeRepository).getActiveVehicleTypes();

    assertThat(result).hasSize(1);
    assertThat(result.getFirst().code()).isEqualTo("CAR");
    assertThat(result.getFirst().active()).isTrue();
    verify(vehicleTypeRepository).findAllByActiveTrueAndDeletedFalseOrderByNameAsc();
  }
}
