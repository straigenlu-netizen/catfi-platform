package dev.catfi.common.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class Position {
  UUID propertyId;
  String assetSymbol;
  BigDecimal quantity;
  BigDecimal averagePurchasePrice;
  Instant lastUpdated;
}
