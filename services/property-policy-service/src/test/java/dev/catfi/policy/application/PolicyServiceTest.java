package dev.catfi.policy.application;

import static org.assertj.core.api.Assertions.*;

import dev.catfi.policy.domain.InsurancePolicy;
import java.util.*;
import org.junit.jupiter.api.Test;

class PolicyServiceTest {

  @Test
  void shouldSelectOnlyActivePolicyForRequestedProperty() {
    var service = new PolicyService();
    var property = UUID.randomUUID();
    service.savePolicy(
        InsurancePolicy.builder()
            .policyId(UUID.randomUUID())
            .propertyId(property)
            .active(false)
            .build());
    service.savePolicy(
        InsurancePolicy.builder()
            .policyId(UUID.randomUUID())
            .propertyId(UUID.randomUUID())
            .active(true)
            .build());
    var active =
        InsurancePolicy.builder()
            .policyId(UUID.randomUUID())
            .propertyId(property)
            .active(true)
            .build();
    service.savePolicy(active);
    assertThat(service.getActivePolicyForProperty(property)).isEqualTo(active);
  }

  @Test
  void shouldFailWhenNoActivePolicyExists() {
    assertThatThrownBy(() -> new PolicyService().getActivePolicyForProperty(UUID.randomUUID()))
        .isInstanceOf(PolicyNotFoundException.class);
  }
}
