package dev.catfi.common.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class CompensationTerms {
  BigDecimal damage;
  BigDecimal coverageRate;
  BigDecimal deductible;
  BigDecimal policyLimit;
  BigDecimal alreadyPaid;

  @java.beans.ConstructorProperties({
    "damage",
    "coverageRate",
    "deductible",
    "policyLimit",
    "alreadyPaid"
  })
  public CompensationTerms(
      BigDecimal damage,
      BigDecimal coverageRate,
      BigDecimal deductible,
      BigDecimal policyLimit,
      BigDecimal alreadyPaid) {
    this.damage = money(damage, "damage");
    this.coverageRate = Objects.requireNonNull(coverageRate, "coverageRate");
    if (this.coverageRate.signum() < 0 || this.coverageRate.compareTo(BigDecimal.ONE) > 0) {
      throw new IllegalArgumentException("coverageRate must be between 0 and 1");
    }
    this.deductible = money(deductible, "deductible");
    this.policyLimit = money(policyLimit, "policyLimit");
    this.alreadyPaid = money(alreadyPaid, "alreadyPaid");
    if (this.alreadyPaid.compareTo(this.policyLimit) > 0) {
      throw new IllegalArgumentException("alreadyPaid exceeds policyLimit");
    }
  }

  private static BigDecimal money(BigDecimal value, String name) {
    Objects.requireNonNull(value, name);
    if (value.signum() < 0) {
      throw new IllegalArgumentException(name + " must be non-negative and have at most 18 digits");
    }
    try {
      BigDecimal normalized = value.setScale(2, RoundingMode.UNNECESSARY);
      if (normalized.precision() > 18) {
        throw new IllegalArgumentException(name + " exceeds the supported monetary range");
      }
      return normalized;
    } catch (ArithmeticException exception) {
      throw new IllegalArgumentException(name + " must have at most two decimal places", exception);
    }
  }
}
