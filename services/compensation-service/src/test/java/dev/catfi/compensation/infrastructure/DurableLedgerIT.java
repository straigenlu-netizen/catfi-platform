package dev.catfi.compensation.infrastructure;

import static org.assertj.core.api.Assertions.*;
import dev.catfi.common.domain.DamageAssessment;
import dev.catfi.compensation.adapter.kafka.CompensationEventProcessor;
import dev.catfi.compensation.adapter.persistence.CompensationRepository;
import dev.catfi.compensation.application.*;
import dev.catfi.compensation.domain.CompensationCalculator;
import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=validate")
@org.springframework.transaction.annotation.Transactional(
    propagation = org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class DurableLedgerIT {
  @Container @ServiceConnection
  static final PostgreSQLContainer<?> DB = new PostgreSQLContainer<>("postgres:17.6-alpine");
  @Autowired JdbcTemplate jdbc;
  @Autowired CompensationRepository repo;
  @Autowired PlatformTransactionManager txManager;

  private DurableCompensationLedger ledger() {
    return new DurableCompensationLedger(jdbc, repo,
        new CompensationApplicationService(new CompensationCalculator()),
        new com.fasterxml.jackson.databind.ObjectMapper().findAndRegisterModules());
  }
  private CompensationEventProcessor.TrustedPolicy policy(UUID property, String limit) {
    return new CompensationEventProcessor.TrustedPolicy(property, BigDecimal.ONE, BigDecimal.ZERO,
        new BigDecimal(limit), BigDecimal.ZERO, true);
  }
  private DamageAssessment event(UUID property, UUID assessment, String damage) {
    return DamageAssessment.builder().propertyId(property).assessmentId(assessment)
        .damageAmount(new BigDecimal(damage)).build();
  }
  private <T> T commit(java.util.function.Supplier<T> action) {
    return new TransactionTemplate(txManager).execute(s -> action.get());
  }

  @Test
  void replayAfterRestartPreservesBudgetAndSingleOutboxRow() {
    UUID property = UUID.randomUUID(), assessment = UUID.randomUUID();
    var event = event(property, assessment, "70.00");
    var first = commit(() -> ledger().record(event, policy(property, "100.00")));
    var replay = commit(() -> ledger().record(event, null));
    assertThat(replay.getResultId()).isEqualTo(first.getResultId());
    assertThat(jdbc.queryForObject("SELECT count(*) FROM compensation_outbox WHERE id=?",
        Integer.class, first.getResultId())).isEqualTo(1);
    var next = commit(() -> ledger().record(event(property, UUID.randomUUID(), "70.00"),
        policy(property, "100.00")));
    assertThat(next.getTotalCompensation()).isEqualByComparingTo("30.00");
    assertThat(jdbc.queryForObject("SELECT sum(amount) FROM compensation_results WHERE property_id=?",
        BigDecimal.class, property)).isEqualByComparingTo("100.00");
  }

  @Test
  void concurrentAssessmentsCannotOverspend() throws Exception {
    UUID property = UUID.randomUUID();
    var executor = Executors.newFixedThreadPool(2);
    var gate = new CountDownLatch(1);
    try {
      var calls = java.util.stream.IntStream.range(0, 2).mapToObj(i ->
          executor.submit(() -> {
            gate.await();
            return commit(() -> ledger().record(event(property, UUID.randomUUID(), "80.00"),
                policy(property, "100.00"))).getTotalCompensation();
          })).toList();
      gate.countDown();
      var a = calls.get(0).get(20, TimeUnit.SECONDS);
      var b = calls.get(1).get(20, TimeUnit.SECONDS);
      assertThat(a.add(b)).isEqualByComparingTo("100.00");
      assertThat(java.util.List.of(a, b)).contains(new BigDecimal("80.00"), new BigDecimal("20.00"));
    } finally {
      executor.shutdownNow();
    }
  }

  @Test
  void outboxInsertFailureRollsBackCompensationAndBudget() {
    UUID property = UUID.randomUUID(), assessment = UUID.randomUUID();
    jdbc.execute("CREATE OR REPLACE FUNCTION catfi_reject_outbox() RETURNS trigger LANGUAGE plpgsql "
        + "AS $$ BEGIN RAISE EXCEPTION 'injected outbox failure'; END $$");
    jdbc.execute("CREATE TRIGGER catfi_reject_outbox BEFORE INSERT ON compensation_outbox "
        + "FOR EACH ROW EXECUTE FUNCTION catfi_reject_outbox()");
    try {
      assertThatThrownBy(() -> commit(() -> ledger().record(event(property, assessment, "50.00"),
          policy(property, "100.00")))).isInstanceOf(Exception.class);
      assertThat(repo.findByDamageAssessmentId(assessment)).isEmpty();
    } finally {
      jdbc.execute("DROP TRIGGER catfi_reject_outbox ON compensation_outbox");
      jdbc.execute("DROP FUNCTION catfi_reject_outbox()");
    }
    assertThat(commit(() -> ledger().record(event(property, assessment, "50.00"),
        policy(property, "100.00"))).getTotalCompensation()).isEqualByComparingTo("50.00");
  }

  @Test
  void conflictingAssessmentReplayFailsClosed() {
    UUID property = UUID.randomUUID(), id = UUID.randomUUID();
    commit(() -> ledger().record(event(property, id, "10.00"), policy(property, "100.00")));
    assertThatThrownBy(() -> commit(() -> ledger().record(event(property, id, "20.00"),
        policy(property, "100.00")))).isInstanceOf(IllegalStateException.class);
  }
}
