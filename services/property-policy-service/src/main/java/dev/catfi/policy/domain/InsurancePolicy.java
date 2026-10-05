package dev.catfi.policy.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class InsurancePolicy {
  UUID policyId;
  UUID propertyId;
  UUID clientId;
  BigDecimal coverageRate; // e.g., 0.90 for 90%
  BigDecimal deductible; // e.g., 5000 EUR
  BigDecimal policyLimit; // Max total payout
  BigDecimal alreadyPaid; // Aggregate paid so far
  LocalDate startDate;
  LocalDate endDate;
  boolean active;
}
