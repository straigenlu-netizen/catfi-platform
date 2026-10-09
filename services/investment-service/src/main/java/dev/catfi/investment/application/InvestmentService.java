package dev.catfi.investment.application;

import dev.catfi.common.domain.InvestmentDecision;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class InvestmentService {

  public InvestmentDecision allocateFunds(
      UUID compensationId, UUID propertyId, BigDecimal amount, String riskProfile) {
    if (amount == null || amount.signum() < 0 || amount.stripTrailingZeros().scale() > 2) {
      throw new IllegalArgumentException(
          "amount must be non-negative with at most two decimal places");
    }
    if (riskProfile == null
        || !java.util.Set.of("CONSERVATIVE", "MODERATE", "AGGRESSIVE")
            .contains(riskProfile.toUpperCase(java.util.Locale.ROOT))) {
      throw new IllegalArgumentException("Unsupported risk profile");
    }
    Map<String, BigDecimal> allocation = new java.util.LinkedHashMap<>();

    // Strategy based on risk profile
    if ("CONSERVATIVE".equalsIgnoreCase(riskProfile)) {
      allocation.put("BOND_US10Y", amount.multiply(new BigDecimal("0.70")));
      allocation.put("GOLD", amount.multiply(new BigDecimal("0.20")));
      allocation.put("BTC", amount.multiply(new BigDecimal("0.10")));
    } else if ("AGGRESSIVE".equalsIgnoreCase(riskProfile)) {
      allocation.put("BTC", amount.multiply(new BigDecimal("0.60")));
      allocation.put("ETH", amount.multiply(new BigDecimal("0.30")));
      allocation.put("GOLD", amount.multiply(new BigDecimal("0.10")));
    } else { // MODERATE
      allocation.put("BOND_US10Y", amount.multiply(new BigDecimal("0.40")));
      allocation.put("GOLD", amount.multiply(new BigDecimal("0.30")));
      allocation.put("BTC", amount.multiply(new BigDecimal("0.20")));
      allocation.put("ETH", amount.multiply(new BigDecimal("0.10")));
    }

    // Ensure precision
    allocation.replaceAll((k, v) -> v.setScale(2, RoundingMode.HALF_EVEN));
    BigDecimal allocated = allocation.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
    String remainderAsset = "GOLD";
    allocation.put(remainderAsset, allocation.get(remainderAsset).add(amount.subtract(allocated)));

    if (compensationId == null || propertyId == null) {
      throw new IllegalArgumentException("Compensation and property IDs are required");
    }
    // A Kafka replay of the same compensation must preserve the portfolio idempotency key.
    UUID decisionId = UUID.nameUUIDFromBytes(
        ("catfi:investment:v1:" + compensationId).getBytes(java.nio.charset.StandardCharsets.UTF_8));
    return InvestmentDecision.builder()
        .decisionId(decisionId)
        .compensationResultId(compensationId)
        .propertyId(propertyId)
        .allocation(allocation)
        .totalInvested(amount)
        .strategyVersion("v1.0")
        .riskProfile(riskProfile)
        .decidedAt(Instant.now())
        .build();
  }
}
