package dev.catfi.common.domain;

import java.time.Instant;
import java.util.UUID;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class SensorReading {
  UUID readingId;
  UUID propertyId;
  UUID disasterEventId;
  String sensorType; // e.g., WATER_LEVEL, ACCELEROMETER, SMOKE
  double value;
  String unit;
  Instant timestamp;
}
