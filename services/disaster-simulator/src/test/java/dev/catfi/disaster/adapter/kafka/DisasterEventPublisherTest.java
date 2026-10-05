package dev.catfi.disaster.adapter.kafka;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import dev.catfi.common.domain.DisasterEvent;
import dev.catfi.disaster.application.DisasterSimulationService;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;

class DisasterEventPublisherTest {

  @Test
  void shouldPublishGeneratedDisasterWithEventIdAsKey() {
    var service = mock(DisasterSimulationService.class);
    KafkaTemplate<String, DisasterEvent> kafka = mock(KafkaTemplate.class);
    var event = DisasterEvent.builder().eventId(UUID.randomUUID()).type("FLOOD").build();
    when(service.createRandomDisaster()).thenReturn(event);
    new DisasterEventPublisher(kafka, service).publishScheduledDisaster();
    verify(kafka).send("catfi.disasters", event.getEventId().toString(), event);
  }
}
