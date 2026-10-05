package dev.catfi.policy.domain;

import java.math.BigDecimal;
import java.util.UUID;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class Property {
  UUID propertyId;
  UUID clientId;
  String address;
  double latitude;
  double longitude;
  String propertyType; // e.g., RESIDENTIAL, COMMERCIAL
  BigDecimal estimatedValue;
}
