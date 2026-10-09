package dev.catfi.compensation.adapter.kafka;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import dev.catfi.common.domain.*;
import dev.catfi.compensation.adapter.persistence.*;
import dev.catfi.compensation.application.*;
import dev.catfi.compensation.domain.CompensationCalculator;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

class CompensationReplayTest {
  @Test
  void replayReturnsPersistedResultWithoutSpendingOrWritingOutbox() {
    var repo = mock(CompensationRepository.class);
    var jdbc = mock(JdbcTemplate.class);
    var json = new com.fasterxml.jackson.databind.ObjectMapper();
    UUID property = UUID.randomUUID(), assessment = UUID.randomUUID(), result = UUID.randomUUID();
    when(repo.findByDamageAssessmentId(assessment)).thenReturn(Optional.of(
        CompensationResultEntity.builder().id(result).propertyId(property)
            .damageAssessmentId(assessment).amount(new BigDecimal("13.00"))
            .damageAmount(new BigDecimal("20.00")).coverageRate(BigDecimal.ONE)
            .deductible(BigDecimal.ZERO).calculatedAt(Instant.now()).build()));
    var ledger = new DurableCompensationLedger(jdbc, repo,
        new CompensationApplicationService(new CompensationCalculator()), json);
    var event = DamageAssessment.builder().assessmentId(assessment).propertyId(property)
        .damageAmount(new BigDecimal("20.00")).build();
    var saved = ledger.record(event, null);
    assertThat(saved.getResultId()).isEqualTo(result);
    assertThat(saved.getAmountToPay()).isEqualByComparingTo("6.50");
    verify(repo, never()).saveAndFlush(any());
    verify(jdbc, never()).update(anyString(), any(Object[].class));
  }
}
