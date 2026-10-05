package dev.catfi.damage.application;

import dev.catfi.common.domain.DamageAssessment;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DamageCalculationService {

  public DamageAssessment calculateDamage(
      UUID propertyId, UUID disasterEventId, String sensorType, double value) {
    if (!Double.isFinite(value) || value < 0) {
      throw new IllegalArgumentException("Sensor value must be finite and non-negative");
    }
    BigDecimal amount = BigDecimal.ZERO;
    String basis = "";

    // Simplified business logic for MVP:
    // Accelerometer > 1.0g -> damage based on intensity
    // Water level > 0.5m -> damage based on depth
    if ("ACCELEROMETER".equals(sensorType)) {
      amount = BigDecimal.valueOf(value).multiply(new BigDecimal("10000"));
      basis = "High seismic acceleration detected: " + value + "g";
    } else if ("WATER_LEVEL".equals(sensorType)) {
      amount = BigDecimal.valueOf(value).multiply(new BigDecimal("15000"));
      basis = "Flood level reached: " + value + "m";
    } else if ("WIND_SPEED".equals(sensorType)) {
      amount = BigDecimal.valueOf(value).multiply(new BigDecimal("500"));
      basis = "Extreme wind speed: " + value + "m/s";
    } else {
      amount = BigDecimal.valueOf(5000);
      basis = "Generic damage assessed from other sensors";
    }

    return DamageAssessment.builder()
        .assessmentId(UUID.randomUUID())
        .propertyId(propertyId)
        .disasterEventId(disasterEventId)
        .damageAmount(amount.setScale(2, java.math.RoundingMode.HALF_EVEN))
        .calculationBasis(basis)
        .assessedAt(Instant.now())
        .build();
  }
}
