package dev.catfi.compensation.adapter.kafka;

import dev.catfi.common.domain.DamageAssessment;
import dev.catfi.compensation.application.DurableCompensationLedger;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@RequiredArgsConstructor
public class CompensationEventProcessor {
  private final DurableCompensationLedger ledger;
  private final RestTemplate restTemplate;

  @org.springframework.beans.factory.annotation.Value("${services.policy.url:http://localhost:8084}")
  private String policyUrl;

  @KafkaListener(topics = "catfi.damage.assessed", groupId = "compensation-service-group")
  public void handleDamageAssessed(DamageAssessment assessment) {
    if (assessment == null || assessment.getAssessmentId() == null || assessment.getPropertyId() == null) {
      throw new IllegalArgumentException("Assessment and stable IDs are required");
    }
    // A replay can fail to reach policy-service even though it has already been committed.
    // The ledger is authoritative, but current policy terms are still required for new work.
    String url = policyUrl + "/api/v1/policies/property/" + assessment.getPropertyId() + "/active";
    TrustedPolicy policy = restTemplate.getForObject(url, TrustedPolicy.class);
    ledger.record(assessment, policy);
    // Outbox polling publishes independently of the Kafka input acknowledgement.
  }

  @com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
  public record TrustedPolicy(UUID propertyId, BigDecimal coverageRate, BigDecimal deductible,
      BigDecimal policyLimit, BigDecimal alreadyPaid, boolean active) {}
}
