package dev.catfi.sensor.application;

import dev.catfi.common.domain.SensorReading;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SensorSimulationService {
  private final Random random = new Random();

  public List<SensorReading> generateReadingsForProperty(UUID propertyId, String disasterType) {
    return generateReadingsForProperty(propertyId, disasterType, null);
  }

  public List<SensorReading> generateReadingsForProperty(
      UUID propertyId, String disasterType, UUID disasterEventId) {
    List<SensorReading> readings = new ArrayList<>();

    switch (disasterType) {
      case "EARTHQUAKE" ->
          readings.add(
              createReading(
                  propertyId,
                  disasterEventId,
                  "ACCELEROMETER",
                  0.5 + random.nextDouble() * 2.0,
                  "g"));
      case "FLOOD", "TSUNAMI" ->
          readings.add(
              createReading(
                  propertyId,
                  disasterEventId,
                  "WATER_LEVEL",
                  0.1 + random.nextDouble() * 3.0,
                  "m"));
      case "TYPHOON" ->
          readings.add(
              createReading(
                  propertyId,
                  disasterEventId,
                  "WIND_SPEED",
                  20.0 + random.nextDouble() * 60.0,
                  "m/s"));
    }

    return readings;
  }

  private SensorReading createReading(
      UUID propertyId, UUID disasterEventId, String type, double value, String unit) {
    return SensorReading.builder()
        .readingId(UUID.randomUUID())
        .propertyId(propertyId)
        .disasterEventId(disasterEventId)
        .sensorType(type)
        .value(value)
        .unit(unit)
        .timestamp(Instant.now())
        .build();
  }
}
