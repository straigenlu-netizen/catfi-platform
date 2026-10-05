package dev.catfi.damage.adapter.kafka;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import dev.catfi.common.domain.*;
import dev.catfi.damage.adapter.persistence.DamageRepository;
import dev.catfi.damage.application.DamageCalculationService;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.kafka.core.KafkaTemplate;

class DamageEventProcessorTest {

  @Test
  void shouldPersistAndPublishAssessmentForTheReading() {
    var repository = mock(DamageRepository.class);
    KafkaTemplate<String, DamageAssessment> kafka = mock(KafkaTemplate.class);
    var property = UUID.randomUUID();
    var reading =
        SensorReading.builder().propertyId(property).sensorType("WATER_LEVEL").value(2).build();
    new DamageEventProcessor(new DamageCalculationService(), repository, kafka)
        .handleSensorReading(reading);
    var result = ArgumentCaptor.forClass(DamageAssessment.class);
    verify(repository).save(result.capture());
    assertThat(result.getValue().getPropertyId()).isEqualTo(property);
    assertThat(result.getValue().getDamageAmount()).isEqualByComparingTo("30000");
    verify(kafka).send("catfi.damage.assessed", property.toString(), result.getValue());
  }

  @Test
  void shouldNotPublishWhenPersistenceFails() {
    var repository = mock(DamageRepository.class);
    KafkaTemplate<String, DamageAssessment> kafka = mock(KafkaTemplate.class);
    doThrow(new IllegalStateException("Database unavailable")).when(repository).save(any());
    var reading =
        SensorReading.builder()
            .propertyId(UUID.randomUUID())
            .sensorType("WATER_LEVEL")
            .value(2)
            .build();
    assertThatThrownBy(
            () ->
                new DamageEventProcessor(new DamageCalculationService(), repository, kafka)
                    .handleSensorReading(reading))
        .isInstanceOf(IllegalStateException.class);
    verifyNoInteractions(kafka);
  }
}
