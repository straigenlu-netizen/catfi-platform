package dev.catfi.compensation.application;

import dev.catfi.common.domain.CompensationResult;
import dev.catfi.common.domain.CompensationTerms;
import dev.catfi.common.domain.PayoutScenario;
import dev.catfi.compensation.domain.CompensationCalculator;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CompensationApplicationService {
  private final CompensationCalculator calculator;

  public CompensationResult calculateCompensation(
      UUID propertyId,
      UUID damageAssessmentId,
      BigDecimal damageAmount,
      CompensationTerms terms,
      PayoutScenario scenario) {
    CompensationTerms updatedTerms =
        new CompensationTerms(
            damageAmount,
            terms.getCoverageRate(),
            terms.getDeductible(),
            terms.getPolicyLimit(),
            terms.getAlreadyPaid());
    BigDecimal totalCompensation = calculator.calculate(updatedTerms);

    BigDecimal amountToPay = BigDecimal.ZERO.setScale(2);
    BigDecimal amountToInvest = BigDecimal.ZERO.setScale(2);

    switch (scenario) {
      case FULL_PAYOUT -> amountToPay = totalCompensation;
      case FULL_INVESTMENT -> amountToInvest = totalCompensation;
      case HYBRID -> {
        // Hybrid: 50% payout, 50% investment (as a default scenario for MVP)
        amountToPay =
            totalCompensation
                .multiply(new BigDecimal("0.5"))
                .setScale(2, java.math.RoundingMode.HALF_EVEN);
        amountToInvest = totalCompensation.subtract(amountToPay);
      }
    }

    return CompensationResult.builder()
        .resultId(UUID.randomUUID())
        .propertyId(propertyId)
        .damageAssessmentId(damageAssessmentId)
        .totalCompensation(totalCompensation)
        .amountToPay(amountToPay)
        .amountToInvest(amountToInvest)
        .scenario(scenario)
        .coverageRate(terms.getCoverageRate())
        .deductible(terms.getDeductible())
        .calculatedAt(Instant.now())
        .build();
  }
}
