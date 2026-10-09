package dev.catfi.compensation.adapter.kafka;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import dev.catfi.common.domain.DamageAssessment;
import dev.catfi.compensation.application.DurableCompensationLedger;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestTemplate;

class CompensationEventProcessorTest {
  @Test
  void delegatesToTransactionalLedgerWithoutRemoteReservationOrDirectKafkaPublish() {
    var ledger = mock(DurableCompensationLedger.class);
    var rest = mock(RestTemplate.class);
    UUID property = UUID.randomUUID();
    var policy = new CompensationEventProcessor.TrustedPolicy(property, BigDecimal.ONE,
        BigDecimal.ZERO, new BigDecimal("100"), BigDecimal.ZERO, true);
    when(rest.getForObject(anyString(), eq(CompensationEventProcessor.TrustedPolicy.class)))
        .thenReturn(policy);
    var assessment = DamageAssessment.builder().assessmentId(UUID.randomUUID())
        .propertyId(property).damageAmount(BigDecimal.TEN).build();
    new CompensationEventProcessor(ledger, rest).handleDamageAssessed(assessment);
    verify(ledger).record(assessment, policy);
    verify(rest, never()).postForObject(anyString(), any(), any());
    verify(rest).getForObject(anyString(), eq(CompensationEventProcessor.TrustedPolicy.class));
    verifyNoMoreInteractions(rest);
  }

  @Test
  void policyFailureDoesNotAttemptLedgerWrite() {
    var ledger = mock(DurableCompensationLedger.class);
    var rest = mock(RestTemplate.class);
    when(rest.getForObject(anyString(), eq(CompensationEventProcessor.TrustedPolicy.class)))
        .thenThrow(new IllegalStateException("Unavailable"));
    var assessment = DamageAssessment.builder().assessmentId(UUID.randomUUID())
        .propertyId(UUID.randomUUID()).damageAmount(BigDecimal.TEN).build();
    assertThatThrownBy(() -> new CompensationEventProcessor(ledger, rest)
        .handleDamageAssessed(assessment)).isInstanceOf(IllegalStateException.class);
    verifyNoInteractions(ledger);
  }
}
