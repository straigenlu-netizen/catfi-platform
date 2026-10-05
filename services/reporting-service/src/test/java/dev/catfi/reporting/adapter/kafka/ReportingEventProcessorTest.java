package dev.catfi.reporting.adapter.kafka;

import static org.assertj.core.api.Assertions.*;

import dev.catfi.common.domain.*;
import dev.catfi.reporting.application.ReportingService;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.Test;

class ReportingEventProcessorTest {

  private final ReportingService service = new ReportingService();
  private final ReportingEventProcessor processor = new ReportingEventProcessor(service);
  private final UUID property = UUID.randomUUID();
  private final UUID assessment = UUID.randomUUID();
  private final UUID compensation = UUID.randomUUID();

  private void completeOutOfOrder() {
    processor.handlePayment(
        Payment.builder()
            .propertyId(property)
            .compensationResultId(compensation)
            .amount(new BigDecimal("5"))
            .build());
    processor.handleInvestment(
        InvestmentDecision.builder()
            .propertyId(property)
            .compensationResultId(compensation)
            .allocation(Map.of("GOLD", new BigDecimal("5")))
            .build());
    processor.handleCompensation(
        CompensationResult.builder()
            .resultId(compensation)
            .damageAssessmentId(assessment)
            .propertyId(property)
            .totalCompensation(BigDecimal.TEN)
            .amountToPay(new BigDecimal("5"))
            .amountToInvest(new BigDecimal("5"))
            .build());
    processor.handleDamage(
        DamageAssessment.builder()
            .assessmentId(assessment)
            .propertyId(property)
            .damageAmount(new BigDecimal("20"))
            .build());
  }

  @Test
  void shouldAggregateCorrelatedEventsInAnyOrder() {
    completeOutOfOrder();
    var report = service.getReport(property);
    assertThat(report.getTotalDamage()).isEqualByComparingTo("20");
    assertThat(report.getTotalCompensation()).isEqualByComparingTo("10");
    assertThat(report.getActualPayout()).isEqualByComparingTo("5");
    assertThat(report.getStatus()).isEqualTo("REPORTED");
  }

  @Test
  void shouldNotReuseOldPaymentsForANewAssessment() {
    completeOutOfOrder();
    processor.handleDamage(
        DamageAssessment.builder()
            .assessmentId(UUID.randomUUID())
            .propertyId(property)
            .damageAmount(new BigDecimal("99"))
            .build());
    var report = service.getReport(property);
    assertThat(report.getStatus()).isEqualTo("IMPACTED");
    assertThat(report.getTotalCompensation()).isEqualByComparingTo("0");
    assertThat(report.getCurrentPortfolio()).isEmpty();
  }
}
