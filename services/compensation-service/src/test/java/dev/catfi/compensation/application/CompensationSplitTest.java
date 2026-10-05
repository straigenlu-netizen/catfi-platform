package dev.catfi.compensation.application;

import static org.assertj.core.api.Assertions.*;

import dev.catfi.common.domain.*;
import dev.catfi.compensation.domain.CompensationCalculator;
import java.math.BigDecimal;
import java.util.*;

class CompensationSplitTest {

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.EnumSource(PayoutScenario.class)
  void shouldPreserveOneCentAcrossAllScenarios(PayoutScenario scenario) {
    var terms =
        new CompensationTerms(
            new BigDecimal("0.01"),
            BigDecimal.ONE,
            BigDecimal.ZERO,
            BigDecimal.TEN,
            BigDecimal.ZERO);
    var result =
        new CompensationApplicationService(new CompensationCalculator())
            .calculateCompensation(
                UUID.randomUUID(), UUID.randomUUID(), new BigDecimal("0.01"), terms, scenario);
    assertThat(result.getAmountToPay().add(result.getAmountToInvest()))
        .isEqualByComparingTo("0.01");
    assertThat(result.getAmountToPay()).hasScaleOf(2);
    assertThat(result.getAmountToInvest()).hasScaleOf(2);
  }
}
