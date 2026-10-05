package dev.catfi.investment.application;

import static org.assertj.core.api.Assertions.*;

import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.Test;

class InvestmentServiceTest {

  private final InvestmentService service = new InvestmentService();

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(
      strings = {"CONSERVATIVE", "MODERATE", "AGGRESSIVE", "conservative"})
  void shouldPreserveEveryCentForSmallAndLargeInvestments(String risk) {
    for (String value : List.of("0", "0.01", "0.02", "0.03", "1.01", "10000.01")) {
      var amount = new BigDecimal(value);
      var result = service.allocateFunds(UUID.randomUUID(), UUID.randomUUID(), amount, risk);
      assertThat(result.getAllocation().values())
          .allSatisfy(v -> assertThat(v).isGreaterThanOrEqualTo(BigDecimal.ZERO).hasScaleOf(2));
      assertThat(result.getAllocation().values().stream().reduce(BigDecimal.ZERO, BigDecimal::add))
          .isEqualByComparingTo(amount);
    }
  }

  @Test
  void shouldAllocateConservativePortfolio() {
    var result =
        service.allocateFunds(
            UUID.randomUUID(), UUID.randomUUID(), new BigDecimal("100"), "CONSERVATIVE");
    assertThat(result.getAllocation().get("BOND_US10Y")).isEqualByComparingTo("70");
    assertThat(result.getAllocation().get("GOLD")).isEqualByComparingTo("20");
    assertThat(result.getAllocation().get("BTC")).isEqualByComparingTo("10");
  }

  @Test
  void shouldRejectUnknownRisk() {
    assertThatIllegalArgumentException()
        .isThrownBy(
            () ->
                service.allocateFunds(
                    UUID.randomUUID(), UUID.randomUUID(), BigDecimal.TEN, "UNKNOWN"));
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(strings = {"-1", "0.001"})
  void shouldRejectInvalidAmount(String amount) {
    assertThatIllegalArgumentException()
        .isThrownBy(
            () ->
                service.allocateFunds(
                    UUID.randomUUID(), UUID.randomUUID(), new BigDecimal(amount), "MODERATE"));
  }
}
