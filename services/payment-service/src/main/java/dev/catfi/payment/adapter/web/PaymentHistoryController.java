package dev.catfi.payment.adapter.web;

import dev.catfi.payment.adapter.persistence.PaymentEntity;
import dev.catfi.payment.adapter.persistence.PaymentRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Internal-only history endpoint; public ownership checks live at the BFF boundary. */
@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentHistoryController {
  private final PaymentRepository repository;

  @GetMapping("/property/{id}")
  public List<PaymentView> forProperty(@PathVariable UUID id) {
    return repository.findByPropertyIdOrderByInitiatedAtDesc(id).stream().map(PaymentView::from).toList();
  }

  public record PaymentView(UUID paymentId, UUID propertyId, UUID compensationResultId,
                            BigDecimal amount, String status, Instant initiatedAt, Instant completedAt) {
    static PaymentView from(PaymentEntity e) {
      return new PaymentView(e.getId(), e.getPropertyId(), e.getCompensationResultId(),
          e.getAmount(), e.getStatus().name(), e.getInitiatedAt(), e.getCompletedAt());
    }
  }
}
