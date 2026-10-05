package dev.catfi.shock.application;

import static org.assertj.core.api.Assertions.*;

import java.math.BigDecimal;
import java.util.*;

class MarketShockServiceTest {

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.CsvSource({
    "EARTHQUAKE,GOLD,110",
    "EARTHQUAKE,BTC,85",
    "EARTHQUAKE,ETH,80",
    "FLOOD,GOLD,105",
    "TYPHOON,BTC,90",
    "UNKNOWN,BTC,100",
    "EARTHQUAKE,UNKNOWN,100"
  })
  void shouldApplySimulatedShockAndPreserveOriginalPrice(
      String disaster, String asset, String expected) {
    var id = UUID.randomUUID();
    var result = new MarketShockService().applyShock(id, disaster, asset, new BigDecimal("100"));
    assertThat(result.getShockedPrice()).isEqualByComparingTo(expected);
    assertThat(result.getOriginalPrice()).isEqualByComparingTo("100");
    assertThat(result.getDisasterEventId()).isEqualTo(id);
  }
}
