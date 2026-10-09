package dev.catfi.payment.adapter.kafka;

import dev.catfi.common.domain.CompensationResult;
import dev.catfi.common.domain.Payment;
import dev.catfi.payment.adapter.persistence.PaymentEntity;
import dev.catfi.payment.adapter.persistence.PaymentRepository;
import dev.catfi.payment.application.PaymentService;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** At-least-once payment publication backed by a durable payment row. */
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
    if (result.getResultId() == null || result.getPropertyId() == null) {
      throw new IllegalArgumentException("Compensation and property IDs are required");
    }
    var existing = repository.findByCompensationResultId(result.getResultId());
    PaymentEntity entity;
    if (existing.isPresent()) {
      entity = existing.get();
      if (!entity.getPropertyId().equals(result.getPropertyId())
          || entity.getAmount().compareTo(result.getAmountToPay()) != 0) {
        throw new IllegalStateException("Conflicting replay for compensation " + result.getResultId());
      }
    } else {
      Payment payment = paymentService.initiatePayment(
          result.getResultId(), result.getPropertyId(), result.getAmountToPay());
      entity = PaymentEntity.builder()
          .id(payment.getPaymentId())
          .compensationResultId(payment.getCompensationResultId())
          .propertyId(payment.getPropertyId())
          .amount(payment.getAmount())
          .status(payment.getStatus())
          .initiatedAt(payment.getInitiatedAt())
          .completedAt(payment.getCompletedAt())
          .eventPublished(false)
          .build();
      // Flush the unique compensation constraint before publishing to Kafka.
      repository.saveAndFlush(entity);
    }
    if (!entity.isEventPublished()) publish(entity);
  }

  @Scheduled(fixedDelayString = "${catfi.payment.outbox.poll-ms:5000}")
  public void retryUnpublishedPayments() {
    for (PaymentEntity entity : repository.findTop100ByEventPublishedFalseOrderByInitiatedAtAsc()) {
      try {
        publish(entity);
      } catch (RuntimeException exception) {
        log.warn("Payment event {} remains pending", entity.getId(), exception);
      }
    }
  }

  private void publish(PaymentEntity entity) {
    Payment payment = Payment.builder()
        .paymentId(entity.getId())
        .compensationResultId(entity.getCompensationResultId())
        .propertyId(entity.getPropertyId())
        .amount(entity.getAmount())
        .status(entity.getStatus())
        .initiatedAt(entity.getInitiatedAt())
        .completedAt(entity.getCompletedAt())
        .build();
    try {
      kafkaTemplate.send(OUTPUT_TOPIC, payment.getPropertyId().toString(), payment)
          .get(10, TimeUnit.SECONDS);
      entity.setEventPublished(true);
      repository.save(entity);
      log.info("Published payment event {} for property {}", payment.getPaymentId(), payment.getPropertyId());
    } catch (Exception exception) {
      throw new IllegalStateException("Payment event publication failed for " + entity.getId(), exception);
    }
  }
}
