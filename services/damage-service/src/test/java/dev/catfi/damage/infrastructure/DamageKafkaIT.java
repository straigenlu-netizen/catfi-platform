package dev.catfi.damage.infrastructure;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import dev.catfi.common.domain.*;
import dev.catfi.damage.adapter.kafka.DamageEventProcessor;
import dev.catfi.damage.adapter.persistence.DamageRepository;
import dev.catfi.damage.application.DamageCalculationService;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;

@org.springframework.boot.test.context.SpringBootTest(
    classes = DamageKafkaIT.TestApplication.class,
    properties = {
      "spring.kafka.consumer.auto-offset-reset=earliest",
      "spring.kafka.consumer.key-deserializer=org.apache.kafka.common.serialization.StringDeserializer",
      "spring.kafka.consumer.value-deserializer=org.springframework.kafka.support.serializer.JsonDeserializer",
      "spring.kafka.consumer.properties.spring.json.trusted.packages=dev.catfi.common.domain",
      "spring.kafka.producer.key-serializer=org.apache.kafka.common.serialization.StringSerializer",
      "spring.kafka.producer.value-serializer=org.springframework.kafka.support.serializer.JsonSerializer"
    })
@org.springframework.kafka.test.context.EmbeddedKafka(
    partitions = 1,
    topics = {"catfi.sensors", "catfi.damage.assessed"},
    bootstrapServersProperty = "spring.kafka.bootstrap-servers")
@org.springframework.test.annotation.DirtiesContext
class DamageKafkaIT {

  @org.springframework.context.annotation.Configuration
  @org.springframework.boot.autoconfigure.EnableAutoConfiguration(
      exclude = {
        org.springframework.boot.mongodb.autoconfigure.MongoAutoConfiguration.class,
        org.springframework.boot.data.mongodb.autoconfigure.MongoDataAutoConfiguration.class
      })
  @org.springframework.context.annotation.Import({
    DamageEventProcessor.class,
    DamageCalculationService.class
  })
  static class TestApplication {}

  @org.springframework.beans.factory.annotation.Autowired
  private KafkaTemplate<String, SensorReading> kafka;

  @org.springframework.beans.factory.annotation.Autowired
  private org.springframework.kafka.test.EmbeddedKafkaBroker broker;

  @org.springframework.test.context.bean.override.mockito.MockitoBean
  private DamageRepository repository;

  @Test
  void shouldConsumeReadingAndPublishMatchingAssessment() throws Exception {
    Map<String, Object> properties =
        org.springframework.kafka.test.utils.KafkaTestUtils.consumerProps(
            "damage-result-test", "false", broker);
    var deserializer =
        new org.springframework.kafka.support.serializer.JsonDeserializer<>(DamageAssessment.class);
    deserializer.addTrustedPackages("dev.catfi.common.domain");
    try (var consumer =
        new org.apache.kafka.clients.consumer.KafkaConsumer<String, DamageAssessment>(
            properties,
            new org.apache.kafka.common.serialization.StringDeserializer(),
            deserializer)) {
      broker.consumeFromAnEmbeddedTopic(consumer, "catfi.damage.assessed");
      var property = UUID.randomUUID();
      var disaster = UUID.randomUUID();
      kafka
          .send(
              "catfi.sensors",
              property.toString(),
              SensorReading.builder()
                  .readingId(UUID.randomUUID())
                  .propertyId(property)
                      .disasterEventId(disaster)
                  .sensorType("WATER_LEVEL")
                  .value(2)
                  .build())
          .get(10, java.util.concurrent.TimeUnit.SECONDS);
      var record =
          org.springframework.kafka.test.utils.KafkaTestUtils.getSingleRecord(
              consumer, "catfi.damage.assessed", java.time.Duration.ofSeconds(15));
      assertThat(record.key()).isEqualTo(property.toString());
      assertThat(record.value().getPropertyId()).isEqualTo(property);
      assertThat(record.value().getDisasterEventId()).isEqualTo(disaster);
      assertThat(record.value().getDamageAmount()).isEqualByComparingTo("30000");
      verify(repository).save(record.value());
    }
  }
}
