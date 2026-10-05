package dev.catfi.disaster;

import static org.assertj.core.api.Assertions.assertThat;

import dev.catfi.common.domain.DisasterEvent;
import dev.catfi.disaster.application.DisasterSimulationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DisasterSimulationServiceTest {

  @InjectMocks private DisasterSimulationService simulationService;

  @Test
  void shouldGenerateDisasterEventWithValidData() {
    DisasterEvent event = simulationService.createRandomDisaster();

    assertThat(event).isNotNull();
    assertThat(event.getEventId()).isNotNull();
    assertThat(event.getType()).isIn("EARTHQUAKE", "FLOOD", "TYPHOON", "TSUNAMI");
    assertThat(event.getLatitude()).isBetween(30.0, 45.0);
    assertThat(event.getLongitude()).isBetween(130.0, 140.0);
    assertThat(event.getIntensity()).isBetween(5.0, 9.0);
  }
}
