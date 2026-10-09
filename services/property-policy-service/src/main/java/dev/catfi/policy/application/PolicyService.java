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
  // Demo-only inbox: stable across Kafka replays within one service lifetime.
  private final Map<UUID, Reservation> reservations = new ConcurrentHashMap<>();
  private record Reservation(UUID propertyId, BigDecimal amount) {}

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
    return reserveCompensation(propertyId, amount, null);
  }

  public synchronized InsurancePolicy reserveCompensation(UUID propertyId, BigDecimal amount,
                                                           UUID reservationId) {
    if (amount == null || amount.signum() < 0) {
      throw new IllegalArgumentException("Reservation amount must be non-negative");
    }
    if (reservationId != null) {
      Reservation prior = reservations.get(reservationId);
      if (prior != null) {
        if (!prior.propertyId().equals(propertyId) || prior.amount().compareTo(amount) != 0) {
          throw new IllegalArgumentException("Reservation ID reused with different terms");
        }
        return getActivePolicyForProperty(propertyId);
      }
    }

    InsurancePolicy current = getActivePolicyForProperty(propertyId);
    InsurancePolicy updated = policyStore.compute(
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
    if (reservationId != null) reservations.put(reservationId, new Reservation(propertyId, amount));
    return updated;
  }

  public Property getProperty(UUID propertyId) {
    return propertyStore.get(propertyId);
  }

  private BigDecimal zeroIfNull(BigDecimal value) {
    return value == null ? BigDecimal.ZERO : value;
  }
}
