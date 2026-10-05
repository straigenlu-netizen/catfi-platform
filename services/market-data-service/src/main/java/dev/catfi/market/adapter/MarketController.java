package dev.catfi.market.adapter;

import dev.catfi.market.application.MarketDataService;
import dev.catfi.market.domain.MarketSnapshot;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/market")
@RequiredArgsConstructor
public class MarketController {
  private final MarketDataService marketDataService;
  private final KafkaTemplate<String, MarketSnapshot> kafkaTemplate;

  @GetMapping("/price/{symbol}")
  public MarketSnapshot getPrice(@PathVariable String symbol) {
    return marketDataService.getAssetPrice(symbol);
  }

  @Scheduled(fixedRate = 30000) // Every 30 seconds
  public void broadcastMarketPrices() {
    List<String> assets = List.of("BTC", "ETH", "GOLD", "BOND_US10Y");
    assets.forEach(
        symbol -> {
          MarketSnapshot snapshot = marketDataService.getAssetPrice(symbol);
          kafkaTemplate.send("catfi.market.prices", symbol, snapshot);
        });
  }
}
