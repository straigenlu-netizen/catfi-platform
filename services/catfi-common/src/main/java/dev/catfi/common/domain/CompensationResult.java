package dev.catfi.common.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class CompensationResult {
  UUID resultId;
  UUID propertyId;
  UUID damageAssessmentId;
  BigDecimal totalCompensation;
  BigDecimal amountToPay;
  BigDecimal amountToInvest;
  PayoutScenario scenario;
  BigDecimal coverageRate;
  BigDecimal deductible;
  Instant calculatedAt;
}
