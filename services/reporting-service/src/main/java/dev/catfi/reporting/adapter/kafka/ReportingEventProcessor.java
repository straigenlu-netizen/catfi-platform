package dev.catfi.reporting.adapter.kafka;

import dev.catfi.common.domain.*;
import dev.catfi.reporting.application.ReportingService;
import java.math.BigDecimal;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ReportingEventProcessor {
  private final ReportingService reportingService;
  private final Map<UUID, DamageAssessment> damageByProperty = new HashMap<>();
  private final Map<UUID, CompensationResult> compensationByAssessment = new HashMap<>();
  private final Map<UUID, Payment> paymentByCompensation = new HashMap<>();
  private final Map<UUID, InvestmentDecision> investmentByCompensation = new HashMap<>();

  @KafkaListener(topics = "catfi.damage.assessed", groupId = "reporting-group")
  public synchronized void handleDamage(DamageAssessment assessment) {
    damageByProperty.put(assessment.getPropertyId(), assessment);
    update(assessment.getPropertyId());
  }

  @KafkaListener(topics = "catfi.compensation.calculated", groupId = "reporting-group")
  public synchronized void handleCompensation(CompensationResult result) {
    compensationByAssessment.put(result.getDamageAssessmentId(), result);
    update(result.getPropertyId());
  }

  @KafkaListener(topics = "catfi.payment.completed", groupId = "reporting-group")
  public synchronized void handlePayment(Payment payment) {
    paymentByCompensation.put(payment.getCompensationResultId(), payment);
    update(payment.getPropertyId());
  }

  @KafkaListener(topics = "catfi.investment.allocated", groupId = "reporting-group")
  public synchronized void handleInvestment(InvestmentDecision decision) {
    investmentByCompensation.put(decision.getCompensationResultId(), decision);
    update(decision.getPropertyId());
  }

  private void update(UUID property) {
    DamageAssessment damage = damageByProperty.get(property);
    if (damage == null) return;
    CompensationResult compensation = compensationByAssessment.get(damage.getAssessmentId());
    Payment payment =
        compensation == null ? null : paymentByCompensation.get(compensation.getResultId());
    InvestmentDecision investment =
        compensation == null ? null : investmentByCompensation.get(compensation.getResultId());
    boolean paid =
        compensation != null && (compensation.getAmountToPay().signum() == 0 || payment != null);
    boolean invested =
        compensation != null
            && (compensation.getAmountToInvest().signum() == 0 || investment != null);
    reportingService.updateReport(
        property,
        damage.getDamageAmount(),
        compensation == null ? BigDecimal.ZERO : compensation.getTotalCompensation(),
        payment == null ? BigDecimal.ZERO : payment.getAmount(),
        investment == null ? Map.of() : investment.getAllocation(),
        paid && invested ? "REPORTED" : "IMPACTED");
  }
}
