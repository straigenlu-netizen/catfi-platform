package dev.catfi.disaster.adapter.kafka;

import dev.catfi.common.domain.DisasterEvent;
import dev.catfi.disaster.application.DisasterSimulationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(
    name = "simulation.scheduled.enabled",
    havingValue = "true")
@RequiredArgsConstructor
public class DisasterEventPublisher {
  private final KafkaTemplate<String, DisasterEvent> kafkaTemplate;
  private final DisasterSimulationService simulationService;

  private static final String TOPIC = "catfi.disasters";

  @Scheduled(fixedRate = 60000)
  public void publishScheduledDisaster() {
    DisasterEvent event = simulationService.createRandomDisaster();
    log.info("Publishing disaster event: {} to topic {}", event.getEventId(), TOPIC);
    kafkaTemplate.send(TOPIC, event.getEventId().toString(), event);
  }
}
