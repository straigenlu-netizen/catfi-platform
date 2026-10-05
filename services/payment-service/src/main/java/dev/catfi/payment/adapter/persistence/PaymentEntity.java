package dev.catfi.payment.adapter.persistence;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.*;

@Entity
@Table(name = "payments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentEntity {
  @Id private UUID id;
  private UUID compensationResultId;
  private UUID propertyId;
  private BigDecimal amount;

  @Enumerated(EnumType.STRING)
  private dev.catfi.common.domain.PaymentStatus status;

  private Instant initiatedAt;
  private Instant completedAt;
}
