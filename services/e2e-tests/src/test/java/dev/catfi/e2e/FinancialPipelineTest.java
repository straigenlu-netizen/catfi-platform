package dev.catfi.e2e;

import static org.assertj.core.api.Assertions.*;

import dev.catfi.common.domain.*;
import dev.catfi.compensation.application.CompensationApplicationService;
import dev.catfi.compensation.domain.CompensationCalculator;
import dev.catfi.damage.application.DamageCalculationService;
import dev.catfi.investment.application.InvestmentService;
import dev.catfi.payment.application.PaymentService;
import dev.catfi.portfolio.application.PortfolioService;
import dev.catfi.reporting.application.ReportingService;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.Test;

class FinancialPipelineTest {

  @Test
  void shouldPreserveMoneyAndPropertyAcrossTheApplicationPipeline() {
    var property = UUID.randomUUID();
    var disaster = UUID.randomUUID();
    var damage =
        new DamageCalculationService().calculateDamage(property, disaster, "ACCELEROMETER", 2);
    var terms =
        new CompensationTerms(
            damage.getDamageAmount(),
            new BigDecimal("0.9"),
            new BigDecimal("5000"),
            new BigDecimal("100000"),
            BigDecimal.ZERO);
    var compensation =
        new CompensationApplicationService(new CompensationCalculator())
            .calculateCompensation(
                property,
                damage.getAssessmentId(),
                damage.getDamageAmount(),
                terms,
                PayoutScenario.HYBRID);
    var payment =
        new PaymentService()
            .initiatePayment(compensation.getResultId(), property, compensation.getAmountToPay());
    var investment =
        new InvestmentService()
            .allocateFunds(
                compensation.getResultId(), property, compensation.getAmountToInvest(), "MODERATE");
    assertThat(payment.getAmount().add(investment.getTotalInvested()))
        .isEqualByComparingTo("13000");
    assertThat(
            investment.getAllocation().values().stream().reduce(BigDecimal.ZERO, BigDecimal::add))
        .isEqualByComparingTo("6500");
    var position =
        new PortfolioService()
            .updatePosition(
                property, "GOLD", investment.getAllocation().get("GOLD"), new BigDecimal("2000"));
    assertThat(position.getQuantity()).isEqualByComparingTo("0.975");
    var reporting = new ReportingService();
    reporting.updateReport(
        property,
        damage.getDamageAmount(),
        compensation.getTotalCompensation(),
        payment.getAmount(),
        investment.getAllocation());
    var report = reporting.getReport(property);
    assertThat(report.getPropertyId()).isEqualTo(property);
    assertThat(report.getTotalDamage()).isEqualByComparingTo("20000");
    assertThat(report.getActualPayout()).isEqualByComparingTo("6500");
  }
}
