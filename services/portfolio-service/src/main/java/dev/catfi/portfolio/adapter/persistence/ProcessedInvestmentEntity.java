package dev.catfi.portfolio.adapter.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** A committed receipt means every allocation position was committed in the same transaction. */
@Entity
@Table(name = "processed_investments")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ProcessedInvestmentEntity {
  @Id private UUID decisionId;
  @Column(nullable = false, length = 64) private String payloadHash;
}
