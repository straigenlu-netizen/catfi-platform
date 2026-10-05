package dev.catfi.common.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class DamageAssessment {
  UUID assessmentId;
  UUID propertyId;
  UUID disasterEventId;
  BigDecimal damageAmount;
  String calculationBasis; // Evidence from sensors
  Instant assessedAt;
}
