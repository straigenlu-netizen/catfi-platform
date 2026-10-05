package dev.catfi.portfolio.adapter.persistence;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.*;

@Entity
@Table(name = "portfolio_positions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PositionEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private UUID id;

  private UUID propertyId;
  private String assetSymbol;

  @Column(precision = 28, scale = 8, nullable = false)
  private BigDecimal quantity;

  @Column(precision = 28, scale = 8, nullable = false)
  private BigDecimal averagePurchasePrice;

  private Instant lastUpdated;
}
