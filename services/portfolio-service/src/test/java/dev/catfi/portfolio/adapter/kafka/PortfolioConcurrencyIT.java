package dev.catfi.portfolio.adapter.kafka;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.catfi.common.domain.InvestmentDecision;
import dev.catfi.portfolio.adapter.persistence.PortfolioRepository;
import dev.catfi.portfolio.adapter.persistence.ProcessedInvestmentRepository;
import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/** Actual PostgreSQL transactions, parallel writers and embedded Kafka listener delivery. */
@SpringBootTest(properties = {
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.kafka.listener.auto-startup=false",
    "spring.kafka.consumer.auto-offset-reset=earliest"
})
@Testcontainers
@DirtiesContext
class PortfolioConcurrencyIT {
  @Container @ServiceConnection
  static final PostgreSQLContainer<?> DB = new PostgreSQLContainer<>("postgres:17.6-alpine");

  @Autowired PortfolioEventProcessor processor;
  @Autowired PortfolioRepository positions;
  @Autowired ProcessedInvestmentRepository receipts;

  @org.junit.jupiter.api.BeforeEach
  void resetDatabase() {
    receipts.deleteAll();
    positions.deleteAll();
  }

  private InvestmentDecision decision(UUID property, UUID id, String amount) {
    return InvestmentDecision.builder().decisionId(id)
        .compensationResultId(UUID.randomUUID()).propertyId(property)
        .totalInvested(new BigDecimal(amount))
        .allocation(Map.of("GOLD", new BigDecimal(amount))).build();
  }

  @Test
  void thousandConcurrentReplaysCommitOnePositionAndOneReceipt() throws Exception {
    var property = UUID.randomUUID();
    var input = decision(property, UUID.randomUUID(), "2000.00");
    try (var executor = Executors.newFixedThreadPool(32)) {
      var tasks = new java.util.ArrayList<Callable<Void>>();
      for (int i = 0; i < 1000; i++) tasks.add(() -> {
        processor.handleInvestmentAllocated(input);
        return null;
      });
      for (var result : executor.invokeAll(tasks)) result.get(120, TimeUnit.SECONDS);
    }
    assertThat(receipts.count()).isEqualTo(1);
    assertThat(positions.count()).isEqualTo(1);
    assertThat(positions.findByPropertyIdAndAssetSymbol(property, "GOLD").getQuantity())
        .isEqualByComparingTo("1.00000000");
  }

  @Test
  void concurrentDistinctInvestmentsAreSerializedWithoutLostUpdates() throws Exception {
    var property = UUID.randomUUID();
    var tasks = new java.util.ArrayList<Callable<Void>>();
    for (int i = 0; i < 100; i++) {
      var input = decision(property, UUID.randomUUID(), "2000.00");
      tasks.add(() -> { processor.handleInvestmentAllocated(input); return null; });
    }
    try (var executor = Executors.newFixedThreadPool(20)) {
      for (var result : executor.invokeAll(tasks)) result.get(120, TimeUnit.SECONDS);
    }
    assertThat(receipts.count()).isEqualTo(100);
    assertThat(positions.count()).isEqualTo(1);
    assertThat(positions.findByPropertyIdAndAssetSymbol(property, "GOLD").getQuantity())
        .isEqualByComparingTo("100.00000000");
  }

  @Test
  void rejectedConflictingReplayLeavesPositionsUntouched() {
    var property = UUID.randomUUID();
    var input = decision(property, UUID.randomUUID(), "2000.00");
    processor.handleInvestmentAllocated(input);
    var conflicting = InvestmentDecision.builder().decisionId(input.getDecisionId())
        .compensationResultId(input.getCompensationResultId()).propertyId(property)
        .totalInvested(new BigDecimal("4000"))
        .allocation(Map.of("GOLD", new BigDecimal("4000"))).build();
    assertThatThrownBy(() -> processor.handleInvestmentAllocated(conflicting))
        .isInstanceOf(IllegalStateException.class);
    assertThat(positions.findByPropertyIdAndAssetSymbol(property, "GOLD").getQuantity())
        .isEqualByComparingTo("1.00000000");
  }
}
