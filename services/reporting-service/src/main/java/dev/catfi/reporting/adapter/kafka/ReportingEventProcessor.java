package dev.catfi.reporting.adapter.kafka;

import dev.catfi.common.domain.*;
import dev.catfi.reporting.application.ReportingService;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ReportingEventProcessor {
  private final ReportingService reportingService;
  @KafkaListener(topics="catfi.damage.assessed",groupId="reporting-group")
  public void handleDamage(DamageAssessment d) { reportingService.accept(d.getPropertyId(),"damage",d.getAssessmentId().toString(),d); }
  @KafkaListener(topics="catfi.compensation.calculated",groupId="reporting-group")
  public void handleCompensation(CompensationResult c) { reportingService.accept(c.getPropertyId(),"compensation",c.getResultId().toString(),c); }
  @KafkaListener(topics="catfi.payment.completed",groupId="reporting-group")
  public void handlePayment(Payment p) { reportingService.accept(p.getPropertyId(),"payment",p.getCompensationResultId().toString(),p); }
  @KafkaListener(topics="catfi.investment.allocated",groupId="reporting-group")
  public void handleInvestment(InvestmentDecision i) { reportingService.accept(i.getPropertyId(),"investment",i.getCompensationResultId().toString(),i); }
}
