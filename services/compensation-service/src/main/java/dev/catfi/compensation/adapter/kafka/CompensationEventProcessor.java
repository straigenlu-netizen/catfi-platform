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
  private final RestTemplate restTemplate;

  private static final String INPUT_TOPIC = "catfi.damage.assessed";
  private static final String OUTPUT_TOPIC = "catfi.compensation.calculated";

  @org.springframework.beans.factory.annotation.Value("${services.policy.url:http://localhost:8084}")
  private String policyUrl;

  @KafkaListener(topics = INPUT_TOPIC, groupId = "compensation-service-group")
  public void handleDamageAssessed(DamageAssessment assessment) {
    log.info("Received damage assessment for property {}: {} EUR",
        assessment.getPropertyId(), assessment.getDamageAmount());
    if (assessment.getAssessmentId() == null) {
      throw new IllegalArgumentException("Assessment ID is required for idempotency");
    }
    var previouslyCalculated = repository.findByDamageAssessmentId(assessment.getAssessmentId());
    if (previouslyCalculated.isPresent()) {
      var saved = previouslyCalculated.get();
      if (!saved.getPropertyId().equals(assessment.getPropertyId())
          || saved.getDamageAmount().compareTo(assessment.getDamageAmount()) != 0) {
        throw new IllegalStateException("Conflicting replay for damage assessment " + assessment.getAssessmentId());
      }
      var amountToPay = saved.getAmount().multiply(new BigDecimal("0.5"))
          .setScale(2, java.math.RoundingMode.HALF_EVEN);
      var replay = CompensationResult.builder().resultId(saved.getId())
          .damageAssessmentId(saved.getDamageAssessmentId()).propertyId(saved.getPropertyId())
          .totalCompensation(saved.getAmount()).amountToPay(amountToPay)
          .amountToInvest(saved.getAmount().subtract(amountToPay))
          .coverageRate(saved.getCoverageRate()).deductible(saved.getDeductible())
          .scenario(PayoutScenario.HYBRID).calculatedAt(saved.getCalculatedAt()).build();
      kafkaTemplate.send(OUTPUT_TOPIC, replay.getPropertyId().toString(), replay);
      return;
    }

    String policyEndpoint = policyUrl + "/api/v1/policies/property/" + assessment.getPropertyId() + "/active";
    TrustedPolicy policy = restTemplate.getForObject(policyEndpoint, TrustedPolicy.class);
    if (policy == null || !policy.active() || !assessment.getPropertyId().equals(policy.propertyId())) {
      throw new IllegalStateException("No matching active policy returned");
    }

    CompensationTerms terms = new CompensationTerms(
        assessment.getDamageAmount(), policy.coverageRate(), policy.deductible(),
        policy.policyLimit(), policy.alreadyPaid());

    CompensationResult result = applicationService.calculateCompensation(
        assessment.getPropertyId(), assessment.getAssessmentId(), assessment.getDamageAmount(),
        terms, PayoutScenario.HYBRID);

    reservePolicyBudget(result.getPropertyId(), result.getTotalCompensation(), assessment.getAssessmentId());

    CompensationResultEntity entity = CompensationResultEntity.builder()
        .id(result.getResultId()).propertyId(result.getPropertyId())
        .damageAssessmentId(result.getDamageAssessmentId()).amount(result.getTotalCompensation())
        .damageAmount(assessment.getDamageAmount()).coverageRate(terms.getCoverageRate())
        .deductible(terms.getDeductible()).calculatedAt(result.getCalculatedAt()).build();

    repository.saveAndFlush(entity);
    log.info("Compensation calculated for property {}: Total {} EUR (Pay: {}, Invest: {})",
        result.getPropertyId(), result.getTotalCompensation(),
        result.getAmountToPay(), result.getAmountToInvest());
    kafkaTemplate.send(OUTPUT_TOPIC, result.getPropertyId().toString(), result);
  }

  private void reservePolicyBudget(UUID propertyId, BigDecimal amount, UUID assessmentId) {
    String endpoint = policyUrl + "/api/v1/policies/property/" + propertyId + "/reserve";
    restTemplate.postForObject(endpoint, new PolicyReservationRequest(amount, assessmentId), TrustedPolicy.class);
  }

  public record PolicyReservationRequest(BigDecimal amount, UUID reservationId) {}

  @com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
  public record TrustedPolicy(
      UUID propertyId, BigDecimal coverageRate, BigDecimal deductible,
      BigDecimal policyLimit, BigDecimal alreadyPaid, boolean active) {}
}
