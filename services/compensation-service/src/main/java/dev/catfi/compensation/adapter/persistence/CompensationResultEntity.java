package dev.catfi.compensation.adapter.persistence;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.*;

@Entity
@Table(name = "compensation_results")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompensationResultEntity {
  @Id private UUID id;

  private UUID propertyId;
  private UUID damageAssessmentId;

  @Column(precision = 18, scale = 2, nullable = false)
  private BigDecimal amount;

  @Column(precision = 18, scale = 2, nullable = false)
  private BigDecimal damageAmount;

  @Column(precision = 7, scale = 6, nullable = false)
  private BigDecimal coverageRate;

  @Column(precision = 18, scale = 2, nullable = false)
  private BigDecimal deductible;

  private Instant calculatedAt;
}
