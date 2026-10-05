package dev.catfi.damage.adapter.kafka;

import dev.catfi.common.domain.DamageAssessment;
import dev.catfi.common.domain.SensorReading;
import dev.catfi.damage.adapter.persistence.DamageRepository;
import dev.catfi.damage.application.DamageCalculationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DamageEventProcessor {
  private final DamageCalculationService calculationService;
  private final DamageRepository repository;
  private final KafkaTemplate<String, DamageAssessment> kafkaTemplate;

  private static final String INPUT_TOPIC = "catfi.sensors";
  private static final String OUTPUT_TOPIC = "catfi.damage.assessed";

  @KafkaListener(topics = INPUT_TOPIC, groupId = "damage-service-group")
  public void handleSensorReading(SensorReading reading) {
    log.info(
        "Processing sensor reading for property {}: {} = {}",
        reading.getPropertyId(),
        reading.getSensorType(),
        reading.getValue());

    DamageAssessment assessment =
        calculationService.calculateDamage(
            reading.getPropertyId(),
            reading.getDisasterEventId(),
            reading.getSensorType(),
            reading.getValue());

    repository.save(assessment);

    log.info(
        "Damage assessed for property {}: {} EUR",
        assessment.getPropertyId(),
        assessment.getDamageAmount());
    kafkaTemplate.send(OUTPUT_TOPIC, assessment.getPropertyId().toString(), assessment);
  }
}
