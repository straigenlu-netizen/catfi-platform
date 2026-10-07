package dev.catfi.notification;

import java.time.Instant;
import java.util.UUID;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ClaimEvent {
  UUID claimId;
  UUID propertyId;
  UUID clientId;
  ClaimStatus status;
  Instant occurredAt;
}
