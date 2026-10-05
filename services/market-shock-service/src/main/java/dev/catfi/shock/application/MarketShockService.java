package dev.catfi.shock.application;

import dev.catfi.shock.domain.MarketShockSnapshot;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MarketShockService {

  // Asset class shock coefficients based on disaster type
  // Earthquake: Gold up, Crypto down, Bonds stable
  private static final Map<String, Map<String, BigDecimal>> SHOCK_MATRIX =
      Map.of(
          "EARTHQUAKE",
              Map.of(
                  "GOLD", new BigDecimal("1.10"), // +10%
                  "BTC", new BigDecimal("0.85"), // -15%
                  "ETH", new BigDecimal("0.80"), // -20%
                  "BOND_US10Y", new BigDecimal("1.02") // +2%
                  ),
          "FLOOD",
              Map.of(
                  "GOLD", new BigDecimal("1.05"),
                  "BTC", new BigDecimal("0.95"),
                  "ETH", new BigDecimal("0.95"),
                  "BOND_US10Y", new BigDecimal("1.00")),
          "TYPHOON",
              Map.of(
                  "GOLD", new BigDecimal("1.05"),
                  "BTC", new BigDecimal("0.90"),
                  "ETH", new BigDecimal("0.90"),
                  "BOND_US10Y", new BigDecimal("1.00")));

  public MarketShockSnapshot applyShock(
      UUID disasterId, String disasterType, String assetSymbol, BigDecimal basePrice) {
    Map<String, BigDecimal> coefficients = SHOCK_MATRIX.getOrDefault(disasterType, Map.of());
    BigDecimal coefficient = coefficients.getOrDefault(assetSymbol, BigDecimal.ONE);

    BigDecimal shockedPrice = basePrice.multiply(coefficient);

    return MarketShockSnapshot.builder()
        .shockId(UUID.randomUUID())
        .disasterEventId(disasterId)
        .assetSymbol(assetSymbol)
        .originalPrice(basePrice)
        .shockCoefficient(coefficient)
        .shockedPrice(shockedPrice)
        .timestamp(Instant.now())
        .build();
  }
}
