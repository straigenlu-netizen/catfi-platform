package dev.catfi.common.domain;

import static org.assertj.core.api.Assertions.*;

import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.Test;

class CompensationTermsTest {

  @Test
  void shouldRejectAlreadyPaidAboveLimit() {
    assertThatIllegalArgumentException()
        .isThrownBy(
            () ->
                new CompensationTerms(
                    BigDecimal.TEN,
                    BigDecimal.ONE,
                    BigDecimal.ZERO,
                    BigDecimal.TEN,
                    new BigDecimal("11")));
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(strings = {"-0.01", "1.001", "10000000000000000"})
  void shouldRejectInvalidMoney(String value) {
    assertThatIllegalArgumentException()
        .isThrownBy(
            () ->
                new CompensationTerms(
                    new BigDecimal(value),
                    BigDecimal.ONE,
                    BigDecimal.ZERO,
                    BigDecimal.TEN,
                    BigDecimal.ZERO));
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(strings = {"-0.1", "1.1"})
  void shouldRejectInvalidCoverage(String value) {
    assertThatIllegalArgumentException()
        .isThrownBy(
            () ->
                new CompensationTerms(
                    BigDecimal.TEN,
                    new BigDecimal(value),
                    BigDecimal.ZERO,
                    BigDecimal.TEN,
                    BigDecimal.ZERO));
  }
}
