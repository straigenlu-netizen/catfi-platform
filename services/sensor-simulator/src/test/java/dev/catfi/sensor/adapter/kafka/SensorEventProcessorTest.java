package dev.catfi.sensor.adapter.kafka;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import dev.catfi.common.domain.*;
import dev.catfi.sensor.application.SensorSimulationService;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.kafka.core.KafkaTemplate;

class SensorEventProcessorTest {

  @Test
  void shouldPublishReadingsForRegisteredPropertiesAndPreserveDisasterId() {
    KafkaTemplate<String, SensorReading> kafka = mock(KafkaTemplate.class);
    var rest = mock(org.springframework.web.client.RestTemplate.class);
    var property = UUID.randomUUID();
    var disaster = UUID.randomUUID();
    when(rest.getForObject(anyString(), eq(SensorEventProcessor.RegisteredProperty[].class)))
        .thenReturn(
            new SensorEventProcessor.RegisteredProperty[] {
              new SensorEventProcessor.RegisteredProperty(property)
            });
    new SensorEventProcessor(new SensorSimulationService(), kafka, rest)
        .handleDisasterEvent(DisasterEvent.builder().eventId(disaster).type("FLOOD").build());
    var reading = ArgumentCaptor.forClass(SensorReading.class);
    verify(kafka).send(eq("catfi.sensors"), eq(property.toString()), reading.capture());
    assertThat(reading.getValue().getPropertyId()).isEqualTo(property);
    assertThat(reading.getValue().getDisasterEventId()).isEqualTo(disaster);
  }
}
