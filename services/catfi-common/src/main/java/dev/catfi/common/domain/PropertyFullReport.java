package dev.catfi.common.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class PropertyFullReport {
  UUID propertyId;
  BigDecimal totalDamage;
  BigDecimal totalCompensation;
  BigDecimal actualPayout;
  Map<String, BigDecimal> currentPortfolio;
  Instant reportGeneratedAt;
  String status; // e.g., "PROCESSED", "PENDING_PAYMENT"
}
