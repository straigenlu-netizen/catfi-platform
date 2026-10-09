package dev.catfi.compensation.adapter.kafka;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import dev.catfi.common.domain.*;
import dev.catfi.compensation.adapter.persistence.*;
import dev.catfi.compensation.application.CompensationApplicationService;
import dev.catfi.compensation.domain.CompensationCalculator;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.client.RestTemplate;

class CompensationEventProcessorTest {

  @Test
  void shouldUseTrustedPolicyAndPublishCalculatedCompensation() {
    var repository = mock(CompensationRepository.class);
    var rest = mock(RestTemplate.class);
    KafkaTemplate<String, CompensationResult> kafka = mock(KafkaTemplate.class);
    var property = UUID.randomUUID();
    var damage = UUID.randomUUID();
    when(rest.getForObject(anyString(), eq(CompensationEventProcessor.TrustedPolicy.class)))
        .thenReturn(
            new CompensationEventProcessor.TrustedPolicy(
                property,
                new BigDecimal("0.9"),
                new BigDecimal("5000"),
                new BigDecimal("100000"),
                BigDecimal.ZERO,
                true));
    var processor =
        new CompensationEventProcessor(
            new CompensationApplicationService(new CompensationCalculator()),
            repository,
            kafka,
            rest);
    processor.handleDamageAssessed(
        DamageAssessment.builder()
            .assessmentId(damage)
            .propertyId(property)
            .damageAmount(new BigDecimal("20000"))
            .build());
    var entity = ArgumentCaptor.forClass(CompensationResultEntity.class);
    verify(repository).saveAndFlush(entity.capture());
    assertThat(entity.getValue().getAmount()).isEqualByComparingTo("13000");
    assertThat(entity.getValue().getDamageAssessmentId()).isEqualTo(damage);
    verify(kafka)
        .send(
            eq("catfi.compensation.calculated"),
            eq(property.toString()),
            any(CompensationResult.class));
  }

  @Test
  void shouldNotInventPolicyTermsWhenPolicyServiceFails() {
    var repository = mock(CompensationRepository.class);
    var rest = mock(RestTemplate.class);
    KafkaTemplate<String, CompensationResult> kafka = mock(KafkaTemplate.class);
    when(rest.getForObject(anyString(), eq(CompensationEventProcessor.TrustedPolicy.class)))
        .thenThrow(new IllegalStateException("Policy unavailable"));
    var processor =
        new CompensationEventProcessor(
            new CompensationApplicationService(new CompensationCalculator()),
            repository,
            kafka,
            rest);
    assertThatThrownBy(
            () ->
                processor.handleDamageAssessed(
                    DamageAssessment.builder()
                        .assessmentId(UUID.randomUUID())
                        .propertyId(UUID.randomUUID())
                        .damageAmount(BigDecimal.TEN)
                        .build()))
        .isInstanceOf(IllegalStateException.class);
    verify(repository).findByDamageAssessmentId(any(UUID.class));
    verifyNoMoreInteractions(repository);
    verifyNoInteractions(kafka);
  }
}
