package dev.catfi.sensor.adapter.kafka;

import dev.catfi.common.domain.*;
import dev.catfi.sensor.application.SensorSimulationService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@RequiredArgsConstructor
public class SensorEventProcessor {
  private final SensorSimulationService simulationService;
  private final KafkaTemplate<String, SensorReading> kafkaTemplate;
  private final RestTemplate restTemplate;

  @Value("${services.policy.url:http://localhost:8084}")
  private String policyUrl;

  @KafkaListener(topics = "catfi.disasters", groupId = "sensor-simulator-group")
  public void handleDisasterEvent(DisasterEvent event) {
    RegisteredProperty[] properties =
        restTemplate.getForObject(
            policyUrl + "/api/v1/policies/properties", RegisteredProperty[].class);
    if (properties == null)
      throw new IllegalStateException("Property service returned no response");
    for (RegisteredProperty property : properties) {
      for (SensorReading reading :
          simulationService.generateReadingsForProperty(
              property.propertyId(), event.getType(), event.getEventId()))
        kafkaTemplate.send("catfi.sensors", reading.getPropertyId().toString(), reading);
    }
  }

  @com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
  public record RegisteredProperty(UUID propertyId) {}
}
