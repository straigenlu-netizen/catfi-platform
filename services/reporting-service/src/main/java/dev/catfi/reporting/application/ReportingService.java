package dev.catfi.reporting.application;

import dev.catfi.common.domain.PropertyFullReport;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportingService {
  // In a real app, this would query multiple services via API or use a read-model (CQRS)
  // For MVP, we use an in-memory store that aggregates Kafka events
  private final Map<UUID, PropertyFullReport> reportCache = new ConcurrentHashMap<>();

  public void updateReport(
      UUID propertyId,
      BigDecimal damage,
      BigDecimal comp,
      BigDecimal payout,
      Map<String, BigDecimal> portfolio) {
    updateReport(propertyId, damage, comp, payout, portfolio, "REPORTED");
  }

  public void updateReport(
      UUID propertyId,
      BigDecimal damage,
      BigDecimal comp,
      BigDecimal payout,
      Map<String, BigDecimal> portfolio,
      String status) {
    PropertyFullReport report =
        PropertyFullReport.builder()
            .propertyId(propertyId)
            .totalDamage(damage)
            .totalCompensation(comp)
            .actualPayout(payout)
            .currentPortfolio(java.util.Collections.unmodifiableMap(new java.util.LinkedHashMap<>(portfolio)))
            .reportGeneratedAt(Instant.now())
            .status(status)
            .build();

    reportCache.put(propertyId, report);
    log.info(
        "Updated report for property {}: Damage={}, Comp={}, Payout={}",
        propertyId,
        damage,
        comp,
        payout);
  }

  public PropertyFullReport getReport(UUID propertyId) {
    return reportCache.get(propertyId);
  }

  public Map<UUID, PropertyFullReport> getAllReports() {
    return Map.copyOf(reportCache);
  }
}
