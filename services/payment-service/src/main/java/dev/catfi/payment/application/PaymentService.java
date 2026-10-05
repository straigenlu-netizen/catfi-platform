package dev.catfi.payment.application;

import dev.catfi.common.domain.Payment;
import dev.catfi.common.domain.PaymentStatus;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PaymentService {

  public Payment initiatePayment(
      UUID compensationId, UUID propertyId, java.math.BigDecimal amount) {
    if (amount == null || amount.signum() < 0 || amount.stripTrailingZeros().scale() > 2) {
      throw new IllegalArgumentException(
          "amount must be non-negative with at most two decimal places");
    }
    // In a real system, this would integrate with a bank API
    // For simulation, we immediately mark it as completed for the happy path
    return Payment.builder()
        .paymentId(UUID.randomUUID())
        .compensationResultId(compensationId)
        .propertyId(propertyId)
        .amount(amount)
        .status(PaymentStatus.COMPLETED)
        .initiatedAt(Instant.now())
        .completedAt(Instant.now())
        .build();
  }
}
