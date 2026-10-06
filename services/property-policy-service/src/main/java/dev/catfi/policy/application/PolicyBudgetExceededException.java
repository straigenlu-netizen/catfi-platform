package dev.catfi.policy.application;

import java.math.BigDecimal;
import java.util.UUID;

public class PolicyBudgetExceededException extends RuntimeException {
  public PolicyBudgetExceededException(
      UUID propertyId, BigDecimal policyLimit, BigDecimal alreadyPaid, BigDecimal requested) {
    super(
        "Policy budget exceeded for property %s: limit=%s, alreadyPaid=%s, requested=%s"
            .formatted(propertyId, policyLimit, alreadyPaid, requested));
  }
}
