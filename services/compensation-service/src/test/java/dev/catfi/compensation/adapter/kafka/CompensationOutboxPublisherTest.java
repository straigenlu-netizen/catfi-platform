package dev.catfi.compensation.adapter.kafka;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import dev.catfi.common.domain.CompensationResult;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;

class CompensationOutboxPublisherTest {
  @Test
  void emptyOutboxDoesNotPublish() {
    var jdbc = mock(JdbcTemplate.class);
    KafkaTemplate<String, CompensationResult> kafka = mock(KafkaTemplate.class);
    when(jdbc.query(anyString(), any(org.springframework.jdbc.core.RowMapper.class)))
        .thenReturn(List.of());
    new CompensationOutboxPublisher(jdbc, kafka, new com.fasterxml.jackson.databind.ObjectMapper())
        .publishPending();
    verifyNoInteractions(kafka);
  }
}
