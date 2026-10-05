package dev.catfi.market.application;

import static org.assertj.core.api.Assertions.*;

import java.math.BigDecimal;
import java.util.*;

class MarketDataServiceTest {

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.CsvSource({
    "BTC,60000",
    "ETH,3000",
    "GOLD,2000",
    "BOND_US10Y,100"
  })
  void shouldReturnSimulatedPriceWithinConfiguredFluctuation(String symbol, String base) {
    var result = new MarketDataService().getAssetPrice(symbol);
    var price = new BigDecimal(base);
    assertThat(result.getPrice())
        .isBetween(price.multiply(new BigDecimal("0.99")), price.multiply(new BigDecimal("1.01")));
    assertThat(result.getAssetSymbol()).isEqualTo(symbol);
    assertThat(result.getSource()).isEqualTo("SimulatedMarketExchange");
    assertThat(result.getCurrency()).isEqualTo("USD");
  }
}
