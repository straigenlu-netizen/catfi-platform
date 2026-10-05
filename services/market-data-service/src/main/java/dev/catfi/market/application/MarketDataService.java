package dev.catfi.market.application;

import dev.catfi.market.domain.MarketSnapshot;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MarketDataService {
  private final Random random = new Random();

  // Initial simulated prices
  private final Map<String, BigDecimal> basePrices =
      new ConcurrentHashMap<>(
          Map.of(
              "BTC", new BigDecimal("60000"),
              "ETH", new BigDecimal("3000"),
              "GOLD", new BigDecimal("2000"),
              "BOND_US10Y", new BigDecimal("100")));

  public MarketSnapshot getAssetPrice(String symbol) {
    BigDecimal basePrice = basePrices.getOrDefault(symbol, new BigDecimal("100"));

    // Add random fluctuation +/- 1%
    double fluctuation = 0.99 + (random.nextDouble() * 0.02);
    BigDecimal currentPrice = basePrice.multiply(BigDecimal.valueOf(fluctuation));

    return MarketSnapshot.builder()
        .assetSymbol(symbol)
        .price(currentPrice)
        .currency("USD")
        .timestamp(Instant.now())
        .source("SimulatedMarketExchange")
        .build();
  }
}
