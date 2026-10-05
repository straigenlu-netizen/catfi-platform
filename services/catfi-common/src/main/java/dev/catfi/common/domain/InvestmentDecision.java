package dev.catfi.common.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class InvestmentDecision {
  UUID decisionId;
  UUID compensationResultId;
  UUID propertyId;
  Map<String, BigDecimal> allocation; // Asset symbol -> Amount in EUR
  BigDecimal totalInvested;
  String strategyVersion;
  String riskProfile;
  Instant decidedAt;
}
