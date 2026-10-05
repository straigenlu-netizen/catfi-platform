package dev.catfi.compensation.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import dev.catfi.common.domain.CompensationResult;
import dev.catfi.common.domain.CompensationTerms;
import dev.catfi.common.domain.PayoutScenario;
import dev.catfi.compensation.domain.CompensationCalculator;
import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CompensationApplicationServiceTest {

  @InjectMocks private CompensationApplicationService applicationService;

  @Mock private CompensationCalculator calculator;

  @Test
  @DisplayName("Should allocate 100% to payout in FULL_PAYOUT scenario")
  void testFullPayoutScenario() {
    // Arrange
    BigDecimal damage = new BigDecimal("20000");
    BigDecimal totalComp = new BigDecimal("13000");
    CompensationTerms terms =
        CompensationTerms.builder()
            .damage(BigDecimal.ZERO)
            .policyLimit(new BigDecimal("100000"))
            .alreadyPaid(BigDecimal.ZERO)
            .coverageRate(new BigDecimal("0.90"))
            .deductible(new BigDecimal("5000"))
            .build();

    when(calculator.calculate(any(CompensationTerms.class))).thenReturn(totalComp);

    // Act
    CompensationResult result =
        applicationService.calculateCompensation(
            java.util.UUID.randomUUID(),
            java.util.UUID.randomUUID(),
            damage,
            terms,
            PayoutScenario.FULL_PAYOUT);

    // Assert
    assertThat(result.getAmountToPay()).isEqualByComparingTo(totalComp);
    assertThat(result.getAmountToInvest()).isEqualByComparingTo(BigDecimal.ZERO);
  }

  @Test
  @DisplayName("Should allocate 100% to investment in FULL_INVESTMENT scenario")
  void testFullInvestmentScenario() {
    BigDecimal damage = new BigDecimal("20000");
    BigDecimal totalComp = new BigDecimal("13000");
    CompensationTerms terms =
        CompensationTerms.builder()
            .damage(BigDecimal.ZERO)
            .policyLimit(new BigDecimal("100000"))
            .alreadyPaid(BigDecimal.ZERO)
            .coverageRate(new BigDecimal("0.90"))
            .deductible(new BigDecimal("5000"))
            .build();

    when(calculator.calculate(any(CompensationTerms.class))).thenReturn(totalComp);

    CompensationResult result =
        applicationService.calculateCompensation(
            java.util.UUID.randomUUID(),
            java.util.UUID.randomUUID(),
            damage,
            terms,
            PayoutScenario.FULL_INVESTMENT);

    assertThat(result.getAmountToPay()).isEqualByComparingTo(BigDecimal.ZERO);
    assertThat(result.getAmountToInvest()).isEqualByComparingTo(totalComp);
  }

  @Test
  @DisplayName("Should split funds 50/50 in HYBRID scenario")
  void testHybridScenario() {
    BigDecimal damage = new BigDecimal("20000");
    BigDecimal totalComp = new BigDecimal("13000");
    CompensationTerms terms =
        CompensationTerms.builder()
            .damage(BigDecimal.ZERO)
            .policyLimit(new BigDecimal("100000"))
            .alreadyPaid(BigDecimal.ZERO)
            .coverageRate(new BigDecimal("0.90"))
            .deductible(new BigDecimal("5000"))
            .build();

    when(calculator.calculate(any(CompensationTerms.class))).thenReturn(totalComp);

    CompensationResult result =
        applicationService.calculateCompensation(
            java.util.UUID.randomUUID(),
            java.util.UUID.randomUUID(),
            damage,
            terms,
            PayoutScenario.HYBRID);

    // 13000 * 0.5 = 6500
    assertThat(result.getAmountToPay()).isEqualByComparingTo(new BigDecimal("6500.00"));
    assertThat(result.getAmountToInvest()).isEqualByComparingTo(new BigDecimal("6500.00"));
  }
}
