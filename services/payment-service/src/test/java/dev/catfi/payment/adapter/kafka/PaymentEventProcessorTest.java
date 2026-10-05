package dev.catfi.payment.adapter.kafka;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import dev.catfi.common.domain.*;
import dev.catfi.payment.adapter.persistence.*;
import dev.catfi.payment.application.PaymentService;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.kafka.core.KafkaTemplate;

class PaymentEventProcessorTest {

  @Test
  void shouldPayOnlyPayoutPartAndPersistBeforePublishing() {
    var repository = mock(PaymentRepository.class);
    KafkaTemplate<String, Payment> kafka = mock(KafkaTemplate.class);
    var processor = new PaymentEventProcessor(new PaymentService(), repository, kafka);
    var id = UUID.randomUUID();
    var property = UUID.randomUUID();
    var result =
        CompensationResult.builder()
            .resultId(id)
            .propertyId(property)
            .totalCompensation(new BigDecimal("100"))
            .amountToPay(new BigDecimal("30"))
            .amountToInvest(new BigDecimal("70"))
            .build();
    processor.handleCompensationCalculated(result);
    var entity = ArgumentCaptor.forClass(PaymentEntity.class);
    verify(repository).save(entity.capture());
    assertThat(entity.getValue().getAmount()).isEqualByComparingTo("30");
    var event = ArgumentCaptor.forClass(Payment.class);
    verify(kafka).send(eq("catfi.payment.completed"), eq(property.toString()), event.capture());
    assertThat(event.getValue().getCompensationResultId()).isEqualTo(id);
    assertThat(event.getValue().getAmount()).isEqualByComparingTo("30");
  }

  @Test
  void shouldNotPublishWhenPersistenceFails() {
    var repository = mock(PaymentRepository.class);
    KafkaTemplate<String, Payment> kafka = mock(KafkaTemplate.class);
    when(repository.save(any())).thenThrow(new IllegalStateException("Database unavailable"));
    var processor = new PaymentEventProcessor(new PaymentService(), repository, kafka);
    var result =
        CompensationResult.builder()
            .resultId(UUID.randomUUID())
            .propertyId(UUID.randomUUID())
            .amountToPay(BigDecimal.TEN)
            .build();
    assertThatThrownBy(() -> processor.handleCompensationCalculated(result))
        .isInstanceOf(IllegalStateException.class);
    verifyNoInteractions(kafka);
  }
}
