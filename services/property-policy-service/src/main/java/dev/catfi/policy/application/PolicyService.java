package dev.catfi.policy.application;

import dev.catfi.policy.domain.InsurancePolicy;
import dev.catfi.policy.domain.Property;
import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PolicyService {
  private final Map<UUID, Property> propertyStore = new ConcurrentHashMap<>();
  private final Map<UUID, InsurancePolicy> policyStore = new ConcurrentHashMap<>();

  public java.util.List<Property> getAllProperties() {
    return propertyStore.values().stream()
        .sorted(java.util.Comparator.comparing(Property::getAddress))
        .toList();
  }

  public void saveProperty(Property property) {
    propertyStore.put(property.getPropertyId(), property);
  }

  public void savePolicy(InsurancePolicy policy) {
    policyStore.put(policy.getPolicyId(), policy);
  }

  public InsurancePolicy getActivePolicyForProperty(UUID propertyId) {
    return policyStore.values().stream()
        .filter(p -> p.getPropertyId().equals(propertyId))
        .filter(InsurancePolicy::isActive)
        .findFirst()
        .orElseThrow(
            () -> new PolicyNotFoundException("No active policy found for property " + propertyId));
  }

  public InsurancePolicy reserveCompensation(UUID propertyId, BigDecimal amount) {
    if (amount == null || amount.signum() < 0) {
      throw new IllegalArgumentException("Reservation amount must be non-negative");
    }

    InsurancePolicy current = getActivePolicyForProperty(propertyId);
    return policyStore.compute(
        current.getPolicyId(),
        (policyId, policy) -> {
          if (policy == null || !policy.isActive() || !propertyId.equals(policy.getPropertyId())) {
            throw new PolicyNotFoundException("No active policy found for property " + propertyId);
          }

          BigDecimal alreadyPaid = zeroIfNull(policy.getAlreadyPaid());
          BigDecimal policyLimit = zeroIfNull(policy.getPolicyLimit());
          if (alreadyPaid.add(amount).compareTo(policyLimit) > 0) {
            throw new PolicyBudgetExceededException(propertyId, policyLimit, alreadyPaid, amount);
          }

          return InsurancePolicy.builder()
              .policyId(policy.getPolicyId())
              .propertyId(policy.getPropertyId())
              .clientId(policy.getClientId())
              .coverageRate(policy.getCoverageRate())
              .deductible(policy.getDeductible())
              .policyLimit(policy.getPolicyLimit())
              .alreadyPaid(alreadyPaid.add(amount))
              .startDate(policy.getStartDate())
              .endDate(policy.getEndDate())
              .active(policy.isActive())
              .build();
        });
  }

  public Property getProperty(UUID propertyId) {
    return propertyStore.get(propertyId);
  }

  private BigDecimal zeroIfNull(BigDecimal value) {
    return value == null ? BigDecimal.ZERO : value;
  }
}
