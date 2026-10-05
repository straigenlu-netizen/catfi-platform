package dev.catfi.compensation.domain;

import static org.assertj.core.api.Assertions.*;

import dev.catfi.common.domain.CompensationTerms;
import java.math.BigDecimal;
import java.util.*;

class CompensationCalculatorTest {

  private final CompensationCalculator calculator = new CompensationCalculator();

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.CsvSource({
    "20000,0.9,5000,100000,0,13000",
    "4000,1,5000,100000,0,0",
    "5000,1,0,10000,8000,2000",
    "5000,1,0,10000,10000,0",
    "100,0,0,1000,0,0",
    "1.01,0.5,0,1000,0,0.50",
    "1.03,0.5,0,1000,0,0.52"
  })
  void shouldApplyCoverageDeductibleLimitAndHalfEvenRounding(
      String damage, String rate, String deductible, String limit, String paid, String expected) {
    var terms =
        new CompensationTerms(
            new BigDecimal(damage),
            new BigDecimal(rate),
            new BigDecimal(deductible),
            new BigDecimal(limit),
            new BigDecimal(paid));
    assertThat(calculator.calculate(terms)).isEqualByComparingTo(expected).hasScaleOf(2);
  }
}
