package dev.catfi.sensor.application;

import static org.assertj.core.api.Assertions.*;

import java.util.*;
import org.junit.jupiter.api.Test;

class SensorSimulationServiceTest {

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.CsvSource({
    "EARTHQUAKE,ACCELEROMETER,g,0.5,2.5",
    "FLOOD,WATER_LEVEL,m,0.1,3.1",
    "TSUNAMI,WATER_LEVEL,m,0.1,3.1",
    "TYPHOON,WIND_SPEED,m/s,20,80"
  })
  void shouldGenerateReadingForDisaster(
      String disaster, String sensor, String unit, double min, double max) {
    var property = UUID.randomUUID();
    var result = new SensorSimulationService().generateReadingsForProperty(property, disaster);
    assertThat(result).hasSize(1);
    var reading = result.get(0);
    assertThat(reading.getPropertyId()).isEqualTo(property);
    assertThat(reading.getSensorType()).isEqualTo(sensor);
    assertThat(reading.getUnit()).isEqualTo(unit);
    assertThat(reading.getValue()).isBetween(min, max);
    assertThat(reading.getReadingId()).isNotNull();
  }

  @Test
  void shouldNotGenerateReadingForUnknownDisaster() {
    assertThat(
            new SensorSimulationService().generateReadingsForProperty(UUID.randomUUID(), "UNKNOWN"))
        .isEmpty();
  }
}
