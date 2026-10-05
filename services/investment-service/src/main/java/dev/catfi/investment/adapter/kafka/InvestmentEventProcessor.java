package dev.catfi.investment.adapter.kafka;

import dev.catfi.common.domain.ClientProfile;
import dev.catfi.common.domain.CompensationResult;
import dev.catfi.common.domain.InvestmentDecision;
import dev.catfi.investment.application.InvestmentService;
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
public class InvestmentEventProcessor {
  private final InvestmentService investmentService;
  private final KafkaTemplate<String, InvestmentDecision> kafkaTemplate;
  private final RestTemplate restTemplate;

  @org.springframework.beans.factory.annotation.Value(
      "${services.policy.url:http://localhost:8084}")
  private String policyUrl;

  private static final String INPUT_TOPIC = "catfi.compensation.calculated";
  private static final String OUTPUT_TOPIC = "catfi.investment.allocated";

  @org.springframework.beans.factory.annotation.Value("${services.user.url:http://localhost:8080}")
  private String userUrl;

  @KafkaListener(topics = INPUT_TOPIC, groupId = "investment-service-group")
  public void handleCompensationCalculated(CompensationResult compensation) {
    if (compensation.getAmountToInvest().signum() == 0) {
      return;
    }
    log.info(
        "Compensation allocated for property {}. Amount: {} EUR. Initiating investment...",
        compensation.getPropertyId(),
        compensation.getAmountToInvest());

    PropertyOwner owner =
        restTemplate.getForObject(
            policyUrl + "/api/v1/policies/properties/" + compensation.getPropertyId(),
            PropertyOwner.class);
    if (owner == null || owner.clientId() == null)
      throw new IllegalStateException("Property owner unavailable");
    ClientProfile profile =
        restTemplate.getForObject(
            userUrl + "/api/v1/clients/" + owner.clientId(), ClientProfile.class);
    if (profile == null || profile.getRiskProfile() == null)
      throw new IllegalStateException("Risk profile unavailable");
    String riskProfile = profile.getRiskProfile().name();

    InvestmentDecision decision =
        investmentService.allocateFunds(
            compensation.getResultId(),
            compensation.getPropertyId(),
            compensation.getAmountToInvest(),
            riskProfile);

    log.info(
        "Investment allocated for property {}. Risk: {}. Allocation: {}",
        decision.getPropertyId(),
        decision.getRiskProfile(),
        decision.getAllocation());

    kafkaTemplate.send(OUTPUT_TOPIC, decision.getPropertyId().toString(), decision);
  }

  @com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
  public record PropertyOwner(UUID clientId) {}
}
