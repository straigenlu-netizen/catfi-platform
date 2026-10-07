package dev.catfi.notification;
import dev.catfi.common.domain.*;
import java.time.Instant; import java.util.UUID; import java.util.concurrent.ConcurrentHashMap;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
@Component
public class ClaimLifecycleTracker {
  private final KafkaTemplate<String,Object> kafka;
  private final ConcurrentHashMap<UUID,ClaimEvent> active=new ConcurrentHashMap<>();
  public ClaimLifecycleTracker(KafkaTemplate<String,Object> k){kafka=k;}
  @KafkaListener(topics="catfi.claim.submitted",groupId="claim-lifecycle")
  public void submitted(ClaimEvent e){active.put(e.getPropertyId(),e);}
  @KafkaListener(topics="catfi.damage.assessed",groupId="claim-lifecycle")
  public void damage(DamageAssessment e){advance(e.getPropertyId(),ClaimStatus.DAMAGE_ASSESSED);}
  @KafkaListener(topics="catfi.compensation.calculated",groupId="claim-lifecycle")
  public void compensation(CompensationResult e){advance(e.getPropertyId(),ClaimStatus.COMPENSATION_CALCULATED);}
  @KafkaListener(topics="catfi.payment.completed",groupId="claim-lifecycle")
  public void payment(Payment e){advance(e.getPropertyId(),ClaimStatus.PAYMENT_COMPLETED);}
  @KafkaListener(topics="catfi.investment.allocated",groupId="claim-lifecycle")
  public void investment(InvestmentDecision e){advance(e.getPropertyId(),ClaimStatus.INVESTMENT_ALLOCATED);}
  private void advance(UUID propertyId,ClaimStatus status){
    var claim=active.get(propertyId); if(claim==null)return;
    var next=ClaimEvent.builder().claimId(claim.getClaimId()).propertyId(propertyId)
      .clientId(claim.getClientId()).status(status).occurredAt(Instant.now()).build();
    active.put(propertyId,next);kafka.send("catfi.claim.status",claim.getClaimId().toString(),next);
  }
}
