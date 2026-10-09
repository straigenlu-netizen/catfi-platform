package dev.catfi.portfolio.adapter.kafka;

import static org.assertj.core.api.Assertions.assertThat;

import dev.catfi.common.domain.InvestmentDecision;
import dev.catfi.portfolio.adapter.persistence.PortfolioRepository;
import dev.catfi.portfolio.adapter.persistence.ProcessedInvestmentRepository;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(properties = {
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.kafka.consumer.auto-offset-reset=earliest",
    "spring.kafka.consumer.properties.spring.json.trusted.packages=dev.catfi.common.domain",
    "spring.kafka.producer.key-serializer=org.apache.kafka.common.serialization.StringSerializer",
    "spring.kafka.producer.value-serializer=org.springframework.kafka.support.serializer.JsonSerializer"
})
@EmbeddedKafka(partitions = 1, topics = "catfi.investment.allocated",
    bootstrapServersProperty = "spring.kafka.bootstrap-servers")
@Testcontainers
@DirtiesContext
class PortfolioKafkaIT {
  @Container @ServiceConnection
  static final PostgreSQLContainer<?> DB = new PostgreSQLContainer<>("postgres:17.6-alpine");

  @Autowired KafkaTemplate<String, InvestmentDecision> kafka;
  @Autowired PortfolioRepository positions;
  @Autowired ProcessedInvestmentRepository receipts;

  @Test
  void duplicateKafkaDeliveryCreatesOnlyOneDurablePosition() throws Exception {
    UUID property = UUID.randomUUID();
    InvestmentDecision event = InvestmentDecision.builder()
        .decisionId(UUID.randomUUID()).compensationResultId(UUID.randomUUID())
        .propertyId(property).totalInvested(new BigDecimal("2000"))
        .allocation(Map.of("GOLD", new BigDecimal("2000"))).build();
    kafka.send("catfi.investment.allocated", property.toString(), event).get();
    kafka.send("catfi.investment.allocated", property.toString(), event).get();
    Awaitility.await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
      assertThat(receipts.count()).isEqualTo(1);
      assertThat(positions.count()).isEqualTo(1);
      assertThat(positions.findByPropertyIdAndAssetSymbol(property, "GOLD").getQuantity())
          .isEqualByComparingTo("1.00000000");
    });
  }
}
