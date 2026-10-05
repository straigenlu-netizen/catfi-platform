package dev.catfi.portfolio.adapter.kafka;

import dev.catfi.common.domain.InvestmentDecision;
import dev.catfi.common.domain.Position;
import dev.catfi.portfolio.adapter.persistence.PortfolioRepository;
import dev.catfi.portfolio.adapter.persistence.PositionEntity;
import dev.catfi.portfolio.application.PortfolioService;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PortfolioEventProcessor {
  private final PortfolioService portfolioService;
  private final PortfolioRepository repository;

  private static final String INPUT_TOPIC = "catfi.investment.allocated";

  @KafkaListener(topics = INPUT_TOPIC, groupId = "portfolio-service-group")
  public void handleInvestmentAllocated(InvestmentDecision decision) {
    log.info(
        "Updating portfolio for property {}. Total invested: {} EUR",
        decision.getPropertyId(),
        decision.getTotalInvested());

    decision
        .getAllocation()
        .forEach(
            (symbol, amount) -> {
              // In a real app, we would fetch the current price from Market Data Service
              // For MVP, we use a simulated current price to calculate quantity
              BigDecimal simulatedPrice = getSimulatedPrice(symbol);

              Position position =
                  portfolioService.updatePosition(
                      decision.getPropertyId(), symbol, amount, simulatedPrice);

              PositionEntity entity =
                  repository.findByPropertyIdAndAssetSymbol(decision.getPropertyId(), symbol);
              if (entity == null) {
                entity =
                    PositionEntity.builder()
                        .propertyId(position.getPropertyId())
                        .assetSymbol(symbol)
                        .quantity(position.getQuantity())
                        .averagePurchasePrice(position.getAveragePurchasePrice())
                        .lastUpdated(position.getLastUpdated())
                        .build();
              } else {
                BigDecimal totalQuantity = entity.getQuantity().add(position.getQuantity());
                BigDecimal cost =
                    entity
                        .getQuantity()
                        .multiply(entity.getAveragePurchasePrice())
                        .add(position.getQuantity().multiply(position.getAveragePurchasePrice()));
                if (totalQuantity.signum() > 0) {
                  entity.setAveragePurchasePrice(
                      cost.divide(totalQuantity, 8, java.math.RoundingMode.HALF_EVEN));
                }
                entity.setQuantity(totalQuantity);
                entity.setLastUpdated(position.getLastUpdated());
              }
              repository.save(entity);

              log.debug("Position updated: {} -> {} units", symbol, position.getQuantity());
            });
  }

  private BigDecimal getSimulatedPrice(String symbol) {
    return switch (symbol) {
      case "BTC" -> new BigDecimal("60000");
      case "ETH" -> new BigDecimal("3000");
      case "GOLD" -> new BigDecimal("2000");
      default -> new BigDecimal("100");
    };
  }
}
