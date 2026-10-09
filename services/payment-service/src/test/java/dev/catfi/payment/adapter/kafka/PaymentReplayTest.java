package dev.catfi.payment.adapter.kafka;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import dev.catfi.common.domain.*;
import dev.catfi.payment.adapter.persistence.*;
import dev.catfi.payment.application.PaymentService;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;

class PaymentReplayTest {
  @Test
  void duplicateCompensationReusesOriginalPaymentAndNeverCreatesAnotherPayout() {
    var repository = mock(PaymentRepository.class);
    KafkaTemplate<String, Payment> kafka = mock(KafkaTemplate.class);
    var compensationId = UUID.randomUUID();
    var propertyId = UUID.randomUUID();
    var payoutId = UUID.randomUUID();
    var existing = PaymentEntity.builder().id(payoutId).compensationResultId(compensationId)
        .propertyId(propertyId).amount(new BigDecimal("10.00"))
        .status(PaymentStatus.COMPLETED).initiatedAt(Instant.now())
        .completedAt(Instant.now()).eventPublished(true).build();
    when(repository.findByCompensationResultId(compensationId)).thenReturn(Optional.of(existing));
    var processor = new PaymentEventProcessor(new PaymentService(), repository, kafka);
    processor.handleCompensationCalculated(CompensationResult.builder()
        .resultId(compensationId).propertyId(propertyId).amountToPay(BigDecimal.TEN).build());
    verify(repository, never()).saveAndFlush(any());
    verifyNoInteractions(kafka);
  }

  @Test
  void unpublishedExistingPaymentIsRepublishedWithTheOriginalId() {
    var repository = mock(PaymentRepository.class);
    KafkaTemplate<String, Payment> kafka = mock(KafkaTemplate.class);
    when(kafka.send(anyString(), anyString(), any(Payment.class)))
        .thenReturn(CompletableFuture.completedFuture(null));
    var entity = PaymentEntity.builder().id(UUID.randomUUID()).propertyId(UUID.randomUUID())
        .compensationResultId(UUID.randomUUID()).amount(BigDecimal.TEN)
        .status(PaymentStatus.COMPLETED).initiatedAt(Instant.now()).build();
    when(repository.findTop100ByEventPublishedFalseOrderByInitiatedAtAsc())
        .thenReturn(java.util.List.of(entity));
    new PaymentEventProcessor(new PaymentService(), repository, kafka).retryUnpublishedPayments();
    org.mockito.ArgumentCaptor<Payment> captor = org.mockito.ArgumentCaptor.forClass(Payment.class);
    verify(kafka).send(eq("catfi.payment.completed"),eq(entity.getPropertyId().toString()),captor.capture());
    assertThat(captor.getValue().getPaymentId()).isEqualTo(entity.getId());
    assertThat(entity.isEventPublished()).isTrue();
    verify(repository).save(entity);
  }
}
