package dev.catfi.common.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class Payment {
  UUID paymentId;
  UUID compensationResultId;
  UUID propertyId;
  BigDecimal amount;
  PaymentStatus status;
  Instant initiatedAt;
  Instant completedAt;
}
