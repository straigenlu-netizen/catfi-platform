package dev.catfi.compensation.domain;

import dev.catfi.common.domain.CompensationTerms;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

public final class CompensationCalculator {
  public BigDecimal calculate(CompensationTerms terms) {
    Objects.requireNonNull(terms, "terms");
    BigDecimal coveredDamage = terms.getDamage().multiply(terms.getCoverageRate());
    BigDecimal afterDeductible = coveredDamage.subtract(terms.getDeductible()).max(BigDecimal.ZERO);
    BigDecimal remainingLimit = terms.getPolicyLimit().subtract(terms.getAlreadyPaid());
    return afterDeductible.min(remainingLimit).setScale(2, RoundingMode.HALF_EVEN);
  }
}
