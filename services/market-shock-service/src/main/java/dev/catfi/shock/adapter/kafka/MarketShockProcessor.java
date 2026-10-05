package dev.catfi.shock.adapter.kafka;

import dev.catfi.common.domain.DisasterEvent;
import dev.catfi.shock.application.MarketShockService;
import dev.catfi.shock.domain.MarketShockSnapshot;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MarketShockProcessor {
  private final MarketShockService shockService;
  private final KafkaTemplate<String, MarketShockSnapshot> kafkaTemplate;

  private static final String DISASTER_TOPIC = "catfi.disasters";
  private static final String OUTPUT_TOPIC = "catfi.market.shocked";

  @KafkaListener(topics = DISASTER_TOPIC, groupId = "market-shock-group")
  public void handleDisasterEvent(DisasterEvent event) {
    log.info("Applying market shock for disaster: {} ({})", event.getEventId(), event.getType());

    // In a real app, we would fetch the latest prices from Market Data Service
    // For MVP, we use a predefined list of assets and simulated base prices
    List<AssetPrice> currentMarket =
        List.of(
            new AssetPrice("BTC", 60000),
            new AssetPrice("ETH", 3000),
            new AssetPrice("GOLD", 2000),
            new AssetPrice("BOND_US10Y", 100));

    currentMarket.forEach(
        asset -> {
          MarketShockSnapshot shocked =
              shockService.applyShock(
                  event.getEventId(), event.getType(), asset.symbol(), asset.price());
          log.info(
              "Shock applied to {}: {} -> {}",
              asset.symbol(),
              asset.price(),
              shocked.getShockedPrice());
          kafkaTemplate.send(OUTPUT_TOPIC, shocked.getAssetSymbol(), shocked);
        });
  }

  private record AssetPrice(String symbol, java.math.BigDecimal price) {
    AssetPrice(String symbol, double price) {
      this(symbol, java.math.BigDecimal.valueOf(price));
    }
  }
}
