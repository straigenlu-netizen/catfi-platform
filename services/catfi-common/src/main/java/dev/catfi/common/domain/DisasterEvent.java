package dev.catfi.common.domain;

import java.time.Instant;
import java.util.UUID;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class DisasterEvent {
  UUID eventId;
  String type; // e.g., EARTHQUAKE, FLOOD, TYPHOON
  double latitude;
  double longitude;
  double intensity;
  Instant timestamp;
  String description;
}
