package dev.catfi.notification;
import static org.mockito.Mockito.*;
import dev.catfi.common.domain.*;
import java.math.BigDecimal; import java.time.Instant; import java.util.UUID;
import org.junit.jupiter.api.Test; import org.springframework.kafka.core.KafkaTemplate;
class ClaimLifecycleTrackerTest {
  @Test void advancesSubmittedClaimWhenDamageIsAssessed(){
    KafkaTemplate<String,Object> kafka=mock(KafkaTemplate.class);var tracker=new ClaimLifecycleTracker(kafka);
    var property=UUID.randomUUID();var claim=UUID.randomUUID();
    tracker.submitted(ClaimEvent.builder().claimId(claim).propertyId(property).clientId(UUID.randomUUID())
      .status(ClaimStatus.SUBMITTED).occurredAt(Instant.now()).build());
    tracker.damage(DamageAssessment.builder().assessmentId(UUID.randomUUID()).propertyId(property)
      .damageAmount(BigDecimal.TEN).assessedAt(Instant.now()).build());
    verify(kafka).send(eq("catfi.claim.status"),eq(claim.toString()),
      argThat(v->v instanceof ClaimEvent e&&e.getStatus()==ClaimStatus.DAMAGE_ASSESSED));
  }
}
