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
    var decision = InvestmentDecision.builder()
        .decisionId(UUID.randomUUID()).compensationResultId(UUID.randomUUID())
        .propertyId(property).totalInvested(new BigDecimal("2000"))
        .allocation(Map.of("GOLD", new BigDecimal("2000"))).build();
    var receipts = mock(ProcessedInvestmentRepository.class);
    var jdbc = mock(org.springframework.jdbc.core.JdbcTemplate.class);
    var processor = new PortfolioEventProcessor(new PortfolioService(), repository, receipts, jdbc);
    processor.handleInvestmentAllocated(decision);
    verify(repository).save(existing);
    verify(receipts).save(any(ProcessedInvestmentEntity.class));
    assertThat(existing.getQuantity()).isEqualByComparingTo("2");
    assertThat(existing.getAveragePurchasePrice()).isEqualByComparingTo("1500");

    // A matching replay does not modify positions or create another receipt.
    var receipt = org.mockito.ArgumentCaptor.forClass(ProcessedInvestmentEntity.class);
    verify(receipts).save(receipt.capture());
    when(receipts.findById(decision.getDecisionId())).thenReturn(Optional.of(receipt.getValue()));
    processor.handleInvestmentAllocated(decision);
    verify(repository, times(1)).save(any(PositionEntity.class));
    verify(receipts, times(1)).save(any(ProcessedInvestmentEntity.class));

    var conflicting = InvestmentDecision.builder()
        .decisionId(decision.getDecisionId()).compensationResultId(decision.getCompensationResultId())
        .propertyId(property).totalInvested(new BigDecimal("1000"))
        .allocation(Map.of("GOLD", new BigDecimal("1000"))).build();
    assertThatThrownBy(() -> processor.handleInvestmentAllocated(conflicting))
        .isInstanceOf(IllegalStateException.class);
    verify(repository, times(1)).save(any(PositionEntity.class));
  }
}
