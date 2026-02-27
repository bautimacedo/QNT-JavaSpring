package com.gestion.qnt.model.business;

import com.gestion.qnt.model.dto.DroneAvailabilityDTO;
import com.gestion.qnt.model.entity.Battery;
import com.gestion.qnt.model.entity.Drone;
import com.gestion.qnt.model.enums.DroneStatus;
import com.gestion.qnt.model.persistence.DroneRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DroneBusinessTest {

    @Mock
    private DroneRepository droneRepository;

    @InjectMocks
    private DroneBusiness droneBusiness;

    @Test
    void findAvailableDrones_returnsOnlyReadyDronesWithBatteries() {
        Drone drone = Drone.builder()
                .id(1L)
                .name("Dron-1")
                .model("DJI Mavic")
                .serialNumber("SN001")
                .flightHoursTotal(100)
                .status(DroneStatus.ready)
                .batteries(List.of(
                        Battery.builder()
                                .id(1L)
                                .serial("BAT001")
                                .cycleCount(50)
                                .healthPercentage(BigDecimal.valueOf(95))
                                .build()))
                .build();
        when(droneRepository.findByStatus(DroneStatus.ready)).thenReturn(List.of(drone));

        List<DroneAvailabilityDTO> result = droneBusiness.findAvailableDrones();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Dron-1");
        assertThat(result.get(0).getAvailable()).isTrue();
        assertThat(result.get(0).getBatteries()).hasSize(1);
        assertThat(result.get(0).getBatteries().get(0).getSuitableForFlight()).isTrue();
    }
}
