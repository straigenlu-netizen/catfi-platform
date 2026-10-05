package dev.catfi.payment.adapter.kafka;

import dev.catfi.common.domain.CompensationResult;
import dev.catfi.common.domain.Payment;
import dev.catfi.payment.adapter.persistence.PaymentEntity;
import dev.catfi.payment.adapter.persistence.PaymentRepository;
import dev.catfi.payment.application.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventProcessor {
  private final PaymentService paymentService;
  private final PaymentRepository repository;
  private final KafkaTemplate<String, Payment> kafkaTemplate;

  private static final String INPUT_TOPIC = "catfi.compensation.calculated";
  private static final String OUTPUT_TOPIC = "catfi.payment.completed";

  @KafkaListener(topics = INPUT_TOPIC, groupId = "payment-service-group")
  public void handleCompensationCalculated(CompensationResult result) {
    log.info(
        "Initiating payment for compensation: {}. Amount: {} EUR",
        result.getResultId(),
        result.getAmountToPay());

    Payment payment =
        paymentService.initiatePayment(
            result.getResultId(), result.getPropertyId(), result.getAmountToPay());

    // Save to DB
    PaymentEntity entity =
        PaymentEntity.builder()
            .id(payment.getPaymentId())
            .compensationResultId(payment.getCompensationResultId())
            .propertyId(payment.getPropertyId())
            .amount(payment.getAmount())
            .status(payment.getStatus())
            .initiatedAt(payment.getInitiatedAt())
            .completedAt(payment.getCompletedAt())
            .build();
    repository.save(entity);

    log.info(
        "Payment completed for property {}: {} EUR", payment.getPropertyId(), payment.getAmount());
    kafkaTemplate.send(OUTPUT_TOPIC, payment.getPropertyId().toString(), payment);
  }
}
