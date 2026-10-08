package dev.catfi.notification;

import static org.mockito.Mockito.*;
import dev.catfi.common.domain.DamageAssessment;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.ContainerTestUtils;

@SpringBootTest(properties={"spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}", "spring.kafka.consumer.auto-offset-reset=earliest"})
@EmbeddedKafka(partitions=1,topics="catfi.damage.assessed")
class NotificationKafkaIT {
  @Autowired KafkaTemplate<String,Object> kafka;
  @Autowired KafkaListenerEndpointRegistry listeners;
  @MockBean NotificationService notifications;

  @BeforeEach
  void awaitDamageConsumerAssignment() {
    // Offset reset alone does not guarantee an assigned consumer before publish.
    var matching = listeners.getListenerContainers().stream()
        .filter(container -> {
          String[] topics = container.getContainerProperties().getTopics();
          return topics != null && Arrays.asList(topics).contains("catfi.damage.assessed");
        }).toList();
    org.assertj.core.api.Assertions.assertThat(matching).isNotEmpty();
    matching.forEach(container -> ContainerTestUtils.waitForAssignment(container, 1));
  }

  @Test void consumesDamageAssessmentFromKafka(){
    UUID property=UUID.randomUUID(),assessment=UUID.randomUUID();
    kafka.send("catfi.damage.assessed",property.toString(),DamageAssessment.builder()
        .assessmentId(assessment).propertyId(property).damageAmount(BigDecimal.TEN)
        .assessedAt(Instant.now()).build()).join();
    verify(notifications,timeout(10000)).notify(property,NotificationStage.CLAIM_IN_PROGRESS,assessment.toString());
  }
}
