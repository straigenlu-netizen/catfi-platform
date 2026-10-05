package dev.catfi.disaster.adapter.web;

import dev.catfi.common.domain.DisasterEvent;
import dev.catfi.disaster.application.DisasterSimulationService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/simulations")
@RequiredArgsConstructor
public class SimulationController {
  private final DisasterSimulationService simulationService;
  private final org.springframework.kafka.core.KafkaTemplate<String, Object> kafkaTemplate;

  @PostMapping("/disaster")
  public ResponseEntity<DisasterEvent> triggerDisaster(
      @jakarta.validation.Valid @RequestBody SimulationRequest request) {
    log.info("Manual disaster trigger received: {}", request);

    DisasterEvent event =
        DisasterEvent.builder()
            .eventId(UUID.randomUUID())
            .type(request.type())
            .latitude(request.latitude())
            .longitude(request.longitude())
            .intensity(request.intensity())
            .timestamp(java.time.Instant.now())
            .description("Manual trigger: " + request.type())
            .build();

    try {
      kafkaTemplate
          .send("catfi.disasters", event.getEventId().toString(), event)
          .get(10, java.util.concurrent.TimeUnit.SECONDS);
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new org.springframework.web.server.ResponseStatusException(
          org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE,
          "Disaster publication interrupted");
    } catch (java.util.concurrent.ExecutionException
        | java.util.concurrent.TimeoutException exception) {
      throw new org.springframework.web.server.ResponseStatusException(
          org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE, "Kafka publication unavailable");
    }
    log.info("Manual disaster event published to Kafka: {}", event.getEventId());

    return ResponseEntity.ok(event);
  }

  @GetMapping("/random")
  public ResponseEntity<DisasterEvent> triggerRandomDisaster() {
    log.info("Random disaster trigger received");
    DisasterEvent event = simulationService.createRandomDisaster();
    try {
      kafkaTemplate
          .send("catfi.disasters", event.getEventId().toString(), event)
          .get(10, java.util.concurrent.TimeUnit.SECONDS);
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new org.springframework.web.server.ResponseStatusException(
          org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE,
          "Disaster publication interrupted");
    } catch (java.util.concurrent.ExecutionException
        | java.util.concurrent.TimeoutException exception) {
      throw new org.springframework.web.server.ResponseStatusException(
          org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE, "Kafka publication unavailable");
    }
    return ResponseEntity.ok(event);
  }

  public record SimulationRequest(
      @jakarta.validation.constraints.NotBlank
          @jakarta.validation.constraints.Pattern(regexp = "EARTHQUAKE|FLOOD|TYPHOON|TSUNAMI")
          String type,
      @jakarta.validation.constraints.NotNull
          @jakarta.validation.constraints.DecimalMin("-90")
          @jakarta.validation.constraints.DecimalMax("90")
          Double latitude,
      @jakarta.validation.constraints.NotNull
          @jakarta.validation.constraints.DecimalMin("-180")
          @jakarta.validation.constraints.DecimalMax("180")
          Double longitude,
      @jakarta.validation.constraints.NotNull
          @jakarta.validation.constraints.DecimalMin(value = "0", inclusive = false)
          @jakarta.validation.constraints.DecimalMax("10")
          Double intensity) {}
}
