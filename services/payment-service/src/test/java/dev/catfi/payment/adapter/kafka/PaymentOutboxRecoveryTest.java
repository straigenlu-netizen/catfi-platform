package dev.catfi.payment.adapter.kafka;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import dev.catfi.common.domain.Payment;
import dev.catfi.common.domain.PaymentStatus;
import dev.catfi.payment.adapter.persistence.PaymentEntity;
import dev.catfi.payment.adapter.persistence.PaymentRepository;
import dev.catfi.payment.application.PaymentService;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;

class PaymentOutboxRecoveryTest {
  @Test
  void failedBrokerAcknowledgementKeepsPaymentPendingForNextPoll() {
    var repo = mock(PaymentRepository.class);
    KafkaTemplate<String, Payment> kafka = mock(KafkaTemplate.class);
    var pending = PaymentEntity.builder().id(UUID.randomUUID()).propertyId(UUID.randomUUID())
        .compensationResultId(UUID.randomUUID()).amount(BigDecimal.TEN)
        .status(PaymentStatus.COMPLETED).initiatedAt(Instant.now()).build();
    when(repo.findTop100ByEventPublishedFalseOrderByInitiatedAtAsc())
        .thenReturn(List.of(pending));
    when(kafka.send(anyString(), anyString(), any(Payment.class)))
        .thenReturn(CompletableFuture.failedFuture(new IllegalStateException("Kafka offline")));
    var processor = new PaymentEventProcessor(new PaymentService(), repo, kafka);
    processor.retryUnpublishedPayments();
    assertThat(pending.isEventPublished()).isFalse();
    verify(repo, never()).save(any());
    verify(kafka).send(eq("catfi.payment.completed"), anyString(), any(Payment.class));
  }
}
