package dev.catfi.investment.adapter.kafka;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import dev.catfi.common.domain.*;
import dev.catfi.investment.application.InvestmentService;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.client.RestTemplate;

class InvestmentEventProcessorTest {

  @Test
  void shouldInvestOnlyInvestmentPartAndReferenceCompensation() {
    KafkaTemplate<String, InvestmentDecision> kafka = mock(KafkaTemplate.class);
    var rest = mock(RestTemplate.class);
    when(rest.getForObject(anyString(), eq(ClientProfile.class)))
        .thenReturn(ClientProfile.builder().riskProfile(RiskProfile.CONSERVATIVE).build());
    when(rest.getForObject(anyString(), eq(InvestmentEventProcessor.PropertyOwner.class)))
        .thenReturn(new InvestmentEventProcessor.PropertyOwner(UUID.randomUUID()));
    var id = UUID.randomUUID();
    var property = UUID.randomUUID();
    var result =
        CompensationResult.builder()
            .resultId(id)
            .propertyId(property)
            .amountToPay(new BigDecimal("30"))
            .amountToInvest(new BigDecimal("70"))
            .build();
    new InvestmentEventProcessor(new InvestmentService(), kafka, rest)
        .handleCompensationCalculated(result);
    var event = ArgumentCaptor.forClass(InvestmentDecision.class);
    verify(kafka).send(eq("catfi.investment.allocated"), eq(property.toString()), event.capture());
    assertThat(event.getValue().getTotalInvested()).isEqualByComparingTo("70");
    assertThat(event.getValue().getCompensationResultId()).isEqualTo(id);
  }

  @Test
  void shouldSkipFullPayoutCompensation() {
    KafkaTemplate<String, InvestmentDecision> kafka = mock(KafkaTemplate.class);
    var rest = mock(RestTemplate.class);
    new InvestmentEventProcessor(new InvestmentService(), kafka, rest)
        .handleCompensationCalculated(
            CompensationResult.builder().amountToInvest(BigDecimal.ZERO).build());
    verifyNoInteractions(kafka, rest);
  }
}
