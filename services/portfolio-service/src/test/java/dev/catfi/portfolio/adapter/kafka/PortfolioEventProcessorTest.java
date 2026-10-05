package dev.catfi.portfolio.adapter.kafka;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import dev.catfi.common.domain.*;
import dev.catfi.portfolio.adapter.persistence.*;
import dev.catfi.portfolio.application.PortfolioService;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.Test;

class PortfolioEventProcessorTest {

  @Test
  void shouldAccumulateAnExistingPosition() {
    var repository = mock(PortfolioRepository.class);
    var property = UUID.randomUUID();
    var existing =
        PositionEntity.builder()
            .id(UUID.randomUUID())
            .propertyId(property)
            .assetSymbol("GOLD")
            .quantity(BigDecimal.ONE)
            .averagePurchasePrice(new BigDecimal("1000"))
            .build();
    when(repository.findByPropertyIdAndAssetSymbol(property, "GOLD")).thenReturn(existing);
    var decision =
        InvestmentDecision.builder()
            .propertyId(property)
            .allocation(Map.of("GOLD", new BigDecimal("2000")))
            .build();
    new PortfolioEventProcessor(new PortfolioService(), repository)
        .handleInvestmentAllocated(decision);
    verify(repository).save(existing);
    assertThat(existing.getQuantity()).isEqualByComparingTo("2");
    assertThat(existing.getAveragePurchasePrice()).isEqualByComparingTo("1500");
  }
}
