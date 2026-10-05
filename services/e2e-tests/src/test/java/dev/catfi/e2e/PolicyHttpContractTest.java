package dev.catfi.e2e;

import static org.assertj.core.api.Assertions.*;

import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.Test;

class PolicyHttpContractTest {

  @Test
  void shouldReadTheActualPolicyJsonInTheCompensationAdapter() throws Exception {
    var mapper = new com.fasterxml.jackson.databind.ObjectMapper().findAndRegisterModules();
    var property = UUID.randomUUID();
    var policy =
        dev.catfi.policy.domain.InsurancePolicy.builder()
            .policyId(UUID.randomUUID())
            .propertyId(property)
            .coverageRate(new BigDecimal("0.9"))
            .deductible(new BigDecimal("5000"))
            .policyLimit(new BigDecimal("100000"))
            .alreadyPaid(BigDecimal.ZERO)
            .active(true)
            .build();
    var response =
        mapper.readValue(
            mapper.writeValueAsBytes(policy),
            dev.catfi.compensation.adapter.kafka.CompensationEventProcessor.TrustedPolicy.class);
    assertThat(
            mapper.readValue(
                mapper.writeValueAsBytes(policy), dev.catfi.policy.domain.InsurancePolicy.class))
        .isEqualTo(policy);
    assertThat(response.active()).isTrue();
    assertThat(response.propertyId()).isEqualTo(property);
  }
}
