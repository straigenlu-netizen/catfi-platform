package dev.catfi.portfolio.adapter.persistence;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.*;

@Entity
@Table(name = "portfolio_positions", uniqueConstraints =
    @jakarta.persistence.UniqueConstraint(name = "uq_portfolio_property_symbol", columnNames = {"property_id", "asset_symbol"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PositionEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private UUID id;

  @Column(name = "property_id", nullable = false)
  private UUID propertyId;
  @Column(name = "asset_symbol", nullable = false)
  private String assetSymbol;

  @Column(precision = 28, scale = 8, nullable = false)
  private BigDecimal quantity;

  @Column(precision = 28, scale = 8, nullable = false)
  private BigDecimal averagePurchasePrice;

  private Instant lastUpdated;
}
