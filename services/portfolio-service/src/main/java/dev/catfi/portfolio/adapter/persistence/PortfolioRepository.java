package dev.catfi.portfolio.adapter.persistence;

import jakarta.persistence.*;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PortfolioRepository extends JpaRepository<PositionEntity, UUID> {
  PositionEntity findByPropertyIdAndAssetSymbol(UUID propertyId, String assetSymbol);
}
