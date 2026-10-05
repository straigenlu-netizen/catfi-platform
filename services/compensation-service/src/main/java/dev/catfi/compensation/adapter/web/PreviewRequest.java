package dev.catfi.compensation.adapter.web;

import dev.catfi.common.domain.CompensationTerms;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record PreviewRequest(
    @NotNull @DecimalMin("0") @Digits(integer = 16, fraction = 2) BigDecimal damage,
    @NotNull @DecimalMin("0") @DecimalMax("1") @Digits(integer = 1, fraction = 6)
        BigDecimal coverageRate,
    @NotNull @DecimalMin("0") @Digits(integer = 16, fraction = 2) BigDecimal deductible,
    @NotNull @DecimalMin("0") @Digits(integer = 16, fraction = 2) BigDecimal policyLimit,
    @NotNull @DecimalMin("0") @Digits(integer = 16, fraction = 2) BigDecimal alreadyPaid) {
  CompensationTerms toTerms() {
    return new CompensationTerms(damage, coverageRate, deductible, policyLimit, alreadyPaid);
  }
}
