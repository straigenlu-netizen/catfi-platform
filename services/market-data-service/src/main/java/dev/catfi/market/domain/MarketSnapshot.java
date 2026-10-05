package dev.catfi.market.domain;

import java.math.BigDecimal;
import java.time.Instant;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class MarketSnapshot {
  String assetSymbol; // e.g., BTC, ETH, GOLD, BOND_US10Y
  BigDecimal price;
  String currency;
  Instant timestamp;
  String source;
}
