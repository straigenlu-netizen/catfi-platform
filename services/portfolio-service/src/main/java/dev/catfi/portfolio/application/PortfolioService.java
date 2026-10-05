package dev.catfi.portfolio.application;

import dev.catfi.common.domain.Position;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PortfolioService {

  public Position updatePosition(
      UUID propertyId, String symbol, BigDecimal investedAmount, BigDecimal currentPrice) {
    if (currentPrice == null
        || currentPrice.signum() <= 0
        || investedAmount == null
        || investedAmount.signum() < 0) {
      throw new IllegalArgumentException(
          "Price must be positive and investment must be non-negative");
    }
    // Calculate quantity based on price: quantity = amount / price
    BigDecimal quantity = investedAmount.divide(currentPrice, 8, RoundingMode.HALF_EVEN);

    return Position.builder()
        .propertyId(propertyId)
        .assetSymbol(symbol)
        .quantity(quantity)
        .averagePurchasePrice(currentPrice)
        .lastUpdated(Instant.now())
        .build();
  }
}
