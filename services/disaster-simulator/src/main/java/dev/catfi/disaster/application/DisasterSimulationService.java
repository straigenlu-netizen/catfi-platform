package dev.catfi.disaster.application;

import dev.catfi.common.domain.DisasterEvent;
import java.time.Instant;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class DisasterSimulationService {
  private final Random random = new Random();
  private static final List<String> DISASTER_TYPES =
      List.of("EARTHQUAKE", "FLOOD", "TYPHOON", "TSUNAMI");

  public DisasterEvent createRandomDisaster() {
    String type = DISASTER_TYPES.get(random.nextInt(DISASTER_TYPES.size()));

    // Simulated coordinates around Japan
    double lat = 30.0 + (random.nextDouble() * 15.0);
    double lon = 130.0 + (random.nextDouble() * 10.0);
    double intensity = 5.0 + (random.nextDouble() * 4.0);

    return DisasterEvent.builder()
        .eventId(UUID.randomUUID())
        .type(type)
        .latitude(lat)
        .longitude(lon)
        .intensity(intensity)
        .timestamp(Instant.now())
        .description("Simulated " + type + " with intensity " + String.format("%.2f", intensity))
        .build();
  }
}
