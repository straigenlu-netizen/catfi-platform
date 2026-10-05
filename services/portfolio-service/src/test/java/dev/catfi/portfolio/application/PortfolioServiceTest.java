package dev.catfi.portfolio.application;

import static org.assertj.core.api.Assertions.*;

import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.Test;

class PortfolioServiceTest {

  private final PortfolioService service = new PortfolioService();

  @Test
  void shouldCalculateQuantityWithEightDecimalPlaces() {
    var property = UUID.randomUUID();
    var result = service.updatePosition(property, "BTC", BigDecimal.ONE, new BigDecimal("3"));
    assertThat(result.getQuantity()).isEqualByComparingTo("0.33333333").hasScaleOf(8);
    assertThat(result.getPropertyId()).isEqualTo(property);
    assertThat(result.getAveragePurchasePrice()).isEqualByComparingTo("3");
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(strings = {"0", "-1"})
  void shouldRejectNonPositivePrice(String price) {
    assertThatIllegalArgumentException()
        .isThrownBy(
            () ->
                service.updatePosition(
                    UUID.randomUUID(), "BTC", BigDecimal.ONE, new BigDecimal(price)));
  }
}
