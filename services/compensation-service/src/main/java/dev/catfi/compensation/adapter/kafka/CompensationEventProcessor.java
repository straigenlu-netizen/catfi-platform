package dev.catfi.compensation.adapter.kafka;

import dev.catfi.common.domain.CompensationResult;
import dev.catfi.common.domain.CompensationTerms;
import dev.catfi.common.domain.DamageAssessment;
import dev.catfi.common.domain.PayoutScenario;
import dev.catfi.compensation.adapter.persistence.CompensationRepository;
import dev.catfi.compensation.adapter.persistence.CompensationResultEntity;
import dev.catfi.compensation.application.CompensationApplicationService;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component
@RequiredArgsConstructor
public class CompensationEventProcessor {
  private final CompensationApplicationService applicationService;
  private final CompensationRepository repository;
  private final KafkaTemplate<String, CompensationResult> kafkaTemplate;
  private final RestTemplate restTemplate; // Injected for API calls

  private static final String INPUT_TOPIC = "catfi.damage.assessed";
  private static final String OUTPUT_TOPIC = "catfi.compensation.calculated";

  @org.springframework.beans.factory.annotation.Value(
      "${services.policy.url:http://localhost:8084}")
  private String policyUrl;

  @KafkaListener(topics = INPUT_TOPIC, groupId = "compensation-service-group")
  public void handleDamageAssessed(DamageAssessment assessment) {
    log.info(
        "Received damage assessment for property {}: {} EUR",
        assessment.getPropertyId(),
        assessment.getDamageAmount());

    String url = policyUrl + "/api/v1/policies/property/" + assessment.getPropertyId() + "/active";
    TrustedPolicy policy = restTemplate.getForObject(url, TrustedPolicy.class);
    if (policy == null
        || !policy.active()
        || !assessment.getPropertyId().equals(policy.propertyId())) {
      throw new IllegalStateException("No matching active policy returned");
    }
    CompensationTerms terms =
        new CompensationTerms(
            assessment.getDamageAmount(),
            policy.coverageRate(),
            policy.deductible(),
            policy.policyLimit(),
            policy.alreadyPaid());

    // In a real app, scenario is fetched from property-policy-service or user-service
    PayoutScenario scenario = PayoutScenario.HYBRID;

    CompensationResult result =
        applicationService.calculateCompensation(
            assessment.getPropertyId(),
            assessment.getAssessmentId(),
            assessment.getDamageAmount(),
            terms,
            scenario);

    // Save to DB
    CompensationResultEntity entity =
        CompensationResultEntity.builder()
            .id(result.getResultId())
            .propertyId(result.getPropertyId())
            .damageAssessmentId(result.getDamageAssessmentId())
            .amount(result.getTotalCompensation())
            .damageAmount(assessment.getDamageAmount())
            .coverageRate(terms.getCoverageRate())
            .deductible(terms.getDeductible())
            .calculatedAt(result.getCalculatedAt())
            .build();

    repository.save(entity);

    log.info(
        "Compensation calculated for property {}: Total {} EUR (Pay: {}, Invest: {})",
        result.getPropertyId(),
        result.getTotalCompensation(),
        result.getAmountToPay(),
        result.getAmountToInvest());

    kafkaTemplate.send(OUTPUT_TOPIC, result.getPropertyId().toString(), result);
  }

  @com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
  public record TrustedPolicy(
      UUID propertyId,
      BigDecimal coverageRate,
      BigDecimal deductible,
      BigDecimal policyLimit,
      BigDecimal alreadyPaid,
      boolean active) {}
}
