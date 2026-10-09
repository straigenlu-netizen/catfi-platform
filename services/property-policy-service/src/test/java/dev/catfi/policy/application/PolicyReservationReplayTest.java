package dev.catfi.policy.application;

import static org.assertj.core.api.Assertions.*;

import dev.catfi.policy.domain.InsurancePolicy;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PolicyReservationReplayTest {
  @Test
  void retryOfSameAssessmentDoesNotConsumePolicyLimitTwice() {
    var service = new PolicyService();
    var property = UUID.randomUUID();
    service.savePolicy(InsurancePolicy.builder().policyId(UUID.randomUUID()).propertyId(property)
        .policyLimit(new BigDecimal("100.00")).alreadyPaid(BigDecimal.ZERO).active(true).build());
    var requestId = UUID.randomUUID();
    service.reserveCompensation(property, new BigDecimal("25.00"), requestId);
    var result = service.reserveCompensation(property, new BigDecimal("25.0"), requestId);
    assertThat(result.getAlreadyPaid()).isEqualByComparingTo("25.00");
    assertThatThrownBy(() -> service.reserveCompensation(property, BigDecimal.TEN, requestId))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
