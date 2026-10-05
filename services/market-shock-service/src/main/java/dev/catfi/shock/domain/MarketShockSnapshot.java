package dev.catfi.shock.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class MarketShockSnapshot {
  UUID shockId;
  UUID disasterEventId;
  String assetSymbol;
  BigDecimal originalPrice;
  BigDecimal shockCoefficient;
  BigDecimal shockedPrice;
  Instant timestamp;
}
