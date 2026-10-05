package dev.catfi.compensation.application;

import dev.catfi.common.domain.CompensationTerms;
import dev.catfi.compensation.domain.CompensationCalculator;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public final class PreviewCompensation {
  private final CompensationCalculator calculator;

  public BigDecimal execute(CompensationTerms terms) {
    return calculator.calculate(terms);
  }
}
