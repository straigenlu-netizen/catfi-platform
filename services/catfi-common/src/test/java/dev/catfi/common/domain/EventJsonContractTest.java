package dev.catfi.common.domain;

import static org.assertj.core.api.Assertions.*;

import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.Test;

class EventJsonContractTest {

  private final com.fasterxml.jackson.databind.ObjectMapper mapper =
      new com.fasterxml.jackson.databind.ObjectMapper().findAndRegisterModules();

  @Test
  void shouldRoundTripSharedTransportContracts() throws Exception {
    var id = UUID.randomUUID();
    var time = java.time.Instant.parse("2026-10-05T10:00:00Z");
    List<Object> events =
        List.of(
            DisasterEvent.builder().eventId(id).type("FLOOD").timestamp(time).build(),
            SensorReading.builder()
                .readingId(id)
                .propertyId(id)
                .sensorType("WATER_LEVEL")
                .value(2)
                .timestamp(time)
                .build(),
            DamageAssessment.builder()
                .assessmentId(id)
                .propertyId(id)
                .damageAmount(new BigDecimal("30000.00"))
                .assessedAt(time)
                .build(),
            CompensationResult.builder()
                .resultId(id)
                .propertyId(id)
                .totalCompensation(BigDecimal.TEN)
                .amountToPay(BigDecimal.ONE)
                .amountToInvest(new BigDecimal("9"))
                .scenario(PayoutScenario.HYBRID)
                .calculatedAt(time)
                .build(),
            Payment.builder()
                .paymentId(id)
                .propertyId(id)
                .amount(BigDecimal.ONE)
                .status(PaymentStatus.COMPLETED)
                .completedAt(time)
                .build(),
            InvestmentDecision.builder()
                .decisionId(id)
                .propertyId(id)
                .allocation(Map.of("GOLD", BigDecimal.ONE))
                .totalInvested(BigDecimal.ONE)
                .decidedAt(time)
                .build(),
            Position.builder()
                .propertyId(id)
                .assetSymbol("GOLD")
                .quantity(BigDecimal.ONE)
                .lastUpdated(time)
                .build(),
            PropertyFullReport.builder()
                .propertyId(id)
                .currentPortfolio(Map.of("GOLD", BigDecimal.ONE))
                .reportGeneratedAt(time)
                .build(),
            ClientProfile.builder().clientId(id).riskProfile(RiskProfile.MODERATE).build(),
            new CompensationTerms(
                BigDecimal.TEN, BigDecimal.ONE, BigDecimal.ZERO, BigDecimal.TEN, BigDecimal.ZERO));
    for (Object event : events)
      assertThat(mapper.readValue(mapper.writeValueAsBytes(event), event.getClass()))
          .isEqualTo(event);
  }
}
