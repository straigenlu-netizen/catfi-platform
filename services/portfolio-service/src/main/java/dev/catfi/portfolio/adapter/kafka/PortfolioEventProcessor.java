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
  private final dev.catfi.portfolio.adapter.persistence.ProcessedInvestmentRepository receipts;
  private final org.springframework.jdbc.core.JdbcTemplate jdbc;

  private static final String INPUT_TOPIC = "catfi.investment.allocated";

  /** The receipt and all positions commit together. The PostgreSQL advisory lock serializes
   * competing writers for one property, including the first insert when no position exists. */
  @org.springframework.transaction.annotation.Transactional
  @KafkaListener(topics = INPUT_TOPIC, groupId = "portfolio-service-group")
  public void handleInvestmentAllocated(InvestmentDecision decision) {
    if (decision == null || decision.getDecisionId() == null || decision.getPropertyId() == null
        || decision.getCompensationResultId() == null || decision.getTotalInvested() == null
        || decision.getAllocation() == null || decision.getAllocation().isEmpty()) {
      throw new IllegalArgumentException("Investment decision, IDs, total and allocation are required");
    }
    if (decision.getTotalInvested().signum() < 0 || decision.getAllocation().entrySet().stream()
        .anyMatch(e -> e.getKey() == null || e.getKey().isBlank() || e.getValue() == null
            || e.getValue().signum() < 0)) {
      throw new IllegalArgumentException("Invalid investment allocation");
    }
    BigDecimal sum = decision.getAllocation().values().stream()
        .reduce(BigDecimal.ZERO, BigDecimal::add);
    if (sum.compareTo(decision.getTotalInvested()) != 0) {
      throw new IllegalArgumentException("Allocation does not match total invested");
    }

    // Advisory locks are transaction-scoped and shared across all service replicas.
    // Hash collisions only reduce concurrency; they do not permit concurrent writes.
    jdbc.query("SELECT pg_advisory_xact_lock(hashtext(?))", (org.springframework.jdbc.core.RowCallbackHandler) rs -> {},
        decision.getPropertyId().toString());
    String fingerprint = fingerprint(decision);
    var existing = receipts.findById(decision.getDecisionId());
    if (existing.isPresent()) {
      if (!existing.get().getPayloadHash().equals(fingerprint)) {
        throw new IllegalStateException("Conflicting replay for investment " + decision.getDecisionId());
      }
      log.debug("Skipping duplicate investment decision {}", decision.getDecisionId());
      return;
    }

    decision.getAllocation().forEach((symbol, amount) -> {
      Position position = portfolioService.updatePosition(
          decision.getPropertyId(), symbol, amount, getSimulatedPrice(symbol));
      PositionEntity entity = repository.findByPropertyIdAndAssetSymbol(decision.getPropertyId(), symbol);
      if (entity == null) {
        entity = PositionEntity.builder()
            .propertyId(position.getPropertyId()).assetSymbol(symbol)
            .quantity(position.getQuantity()).averagePurchasePrice(position.getAveragePurchasePrice())
            .lastUpdated(position.getLastUpdated()).build();
      } else {
        BigDecimal totalQuantity = entity.getQuantity().add(position.getQuantity());
        BigDecimal cost = entity.getQuantity().multiply(entity.getAveragePurchasePrice())
            .add(position.getQuantity().multiply(position.getAveragePurchasePrice()));
        if (totalQuantity.signum() > 0) {
          entity.setAveragePurchasePrice(cost.divide(totalQuantity, 8, java.math.RoundingMode.HALF_EVEN));
        }
        entity.setQuantity(totalQuantity);
        entity.setLastUpdated(position.getLastUpdated());
      }
      repository.save(entity);
    });
    receipts.save(new dev.catfi.portfolio.adapter.persistence.ProcessedInvestmentEntity(
        decision.getDecisionId(), fingerprint));
  }

  private static String fingerprint(InvestmentDecision decision) {
    StringBuilder canonical = new StringBuilder()
        .append(decision.getPropertyId()).append('|')
        .append(decision.getCompensationResultId()).append('|')
        .append(decision.getTotalInvested().stripTrailingZeros().toPlainString());
    decision.getAllocation().entrySet().stream().sorted(java.util.Map.Entry.comparingByKey())
        .forEach(e -> canonical.append('|').append(e.getKey()).append('=')
            .append(e.getValue().stripTrailingZeros().toPlainString()));
    try {
      byte[] digest = java.security.MessageDigest.getInstance("SHA-256")
          .digest(canonical.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
      return java.util.HexFormat.of().formatHex(digest);
    } catch (java.security.NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 is unavailable", e);
    }
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
