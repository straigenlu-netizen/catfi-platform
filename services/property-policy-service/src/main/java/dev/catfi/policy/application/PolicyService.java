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

  public Property getProperty(UUID propertyId) {
    return propertyStore.get(propertyId);
  }

}
