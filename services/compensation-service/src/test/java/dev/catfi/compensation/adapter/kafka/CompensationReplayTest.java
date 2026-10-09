package dev.catfi.compensation.adapter.kafka;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import dev.catfi.common.domain.*;
import dev.catfi.compensation.adapter.persistence.*;
import dev.catfi.compensation.application.CompensationApplicationService;
import dev.catfi.compensation.domain.CompensationCalculator;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.client.RestTemplate;

class CompensationReplayTest {
  @Test
  void replayUsesSameResultAndDoesNotReservePolicyTwice() {
    var repo = mock(CompensationRepository.class);
    KafkaTemplate<String, CompensationResult> kafka = mock(KafkaTemplate.class);
    var rest = mock(RestTemplate.class);
    UUID property = UUID.randomUUID(), assessment = UUID.randomUUID(), result = UUID.randomUUID();
    when(repo.findByDamageAssessmentId(assessment)).thenReturn(Optional.of(
        CompensationResultEntity.builder().id(result).propertyId(property)
            .damageAssessmentId(assessment).amount(new BigDecimal("13.00"))
            .damageAmount(new BigDecimal("20.00")).coverageRate(BigDecimal.ONE)
            .deductible(BigDecimal.ZERO).calculatedAt(Instant.now()).build()));
    var processor = new CompensationEventProcessor(
        new CompensationApplicationService(new CompensationCalculator()), repo, kafka, rest);
    processor.handleDamageAssessed(DamageAssessment.builder()
        .assessmentId(assessment).propertyId(property)
        .damageAmount(new BigDecimal("20.00")).build());
    var capture = ArgumentCaptor.forClass(CompensationResult.class);
    verify(kafka).send(eq("catfi.compensation.calculated"), eq(property.toString()), capture.capture());
    assertThat(capture.getValue().getResultId()).isEqualTo(result);
    assertThat(capture.getValue().getAmountToPay()).isEqualByComparingTo("6.50");
    verifyNoInteractions(rest);
    verify(repo, never()).saveAndFlush(any());
  }
}
