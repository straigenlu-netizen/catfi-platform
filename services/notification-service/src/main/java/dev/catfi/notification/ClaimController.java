package dev.catfi.notification;
import dev.catfi.common.domain.*;
import jakarta.validation.Valid; import jakarta.validation.constraints.NotNull;
import java.time.Instant; import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/v1/claims")
public class ClaimController {
  private final RecipientResolver recipients; private final KafkaTemplate<String,Object> kafka;
  public ClaimController(RecipientResolver r,KafkaTemplate<String,Object> k){recipients=r;kafka=k;}
  @PostMapping
  public ResponseEntity<ClaimEvent> submit(@Valid @RequestBody SubmitClaim request){
    var client=recipients.resolve(request.propertyId());
    var event=ClaimEvent.builder().claimId(UUID.randomUUID()).propertyId(request.propertyId())
      .clientId(client.getClientId()).status(ClaimStatus.SUBMITTED).occurredAt(Instant.now()).build();
    kafka.send("catfi.claim.submitted",event.getClaimId().toString(),event);
    return ResponseEntity.accepted().body(event);
  }
  public record SubmitClaim(@NotNull UUID propertyId){}
}
