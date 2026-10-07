package dev.catfi.notification;
import dev.catfi.common.domain.*;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
@Component
public class NotificationEventConsumer {
  private final NotificationService notifications; public NotificationEventConsumer(NotificationService n){notifications=n;}
  @KafkaListener(topics="catfi.claim.submitted",groupId="notification-service") public void claim(ClaimEvent e){notifications.notify(e.getPropertyId(),NotificationStage.CLAIM_SUBMITTED,e.getClaimId().toString());}
  @KafkaListener(topics="catfi.damage.assessed",groupId="notification-service") public void damage(DamageAssessment e){notifications.notify(e.getPropertyId(),NotificationStage.CLAIM_IN_PROGRESS,e.getAssessmentId().toString());}
  @KafkaListener(topics="catfi.compensation.calculated",groupId="notification-service") public void compensation(CompensationResult e){notifications.notify(e.getPropertyId(),NotificationStage.COMPENSATION_CALCULATED,e.getResultId().toString());}
  @KafkaListener(topics="catfi.payment.completed",groupId="notification-service") public void payment(Payment e){notifications.notify(e.getPropertyId(),NotificationStage.PAYMENT_COMPLETED,e.getPaymentId().toString());}
  @KafkaListener(topics="catfi.investment.allocated",groupId="notification-service") public void investment(InvestmentDecision e){notifications.notify(e.getPropertyId(),NotificationStage.INVESTMENT_ALLOCATED,e.getDecisionId().toString());}
}
