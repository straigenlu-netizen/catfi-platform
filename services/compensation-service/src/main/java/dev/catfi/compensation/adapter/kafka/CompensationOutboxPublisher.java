package dev.catfi.compensation.adapter.kafka;

import dev.catfi.common.domain.CompensationResult;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class CompensationOutboxPublisher {
  private final JdbcTemplate jdbc;
  private final KafkaTemplate<String, CompensationResult> kafka;
  private final com.fasterxml.jackson.databind.ObjectMapper json;

  @Scheduled(fixedDelayString = "${catfi.outbox.poll-ms:5000}")
  @Transactional
  public void publishPending() {
    // The row lock is held through Kafka acknowledgement; crash after ack but before
    // commit may republish, so downstream consumers MUST remain idempotent.
    var rows = jdbc.query("SELECT id, property_id, payload::text FROM compensation_outbox "
        + "WHERE published_at IS NULL ORDER BY created_at, id LIMIT 50 FOR UPDATE SKIP LOCKED",
        (rs, n) -> new OutboxRow((UUID) rs.getObject(1), (UUID) rs.getObject(2), rs.getString(3)));
    for (var row : rows) {
      try {
        CompensationResult result = json.readValue(row.payload(), CompensationResult.class);
        kafka.send("catfi.compensation.calculated", row.propertyId().toString(), result)
            .get(10, TimeUnit.SECONDS);
        jdbc.update("UPDATE compensation_outbox SET published_at = now() WHERE id = ?", row.id());
      } catch (Exception ex) {
        // Roll back all changes, leaving the row pending for the next scheduled run.
        throw new IllegalStateException("Outbox publish failed for " + row.id(), ex);
      }
    }
  }

  private record OutboxRow(UUID id, UUID propertyId, String payload) {}
}
