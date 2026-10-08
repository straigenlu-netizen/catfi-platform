package dev.catfi.bff.adapter.client;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "payment-service", url = "${services.payment.url:http://localhost:8082}")
public interface PaymentClient {
  @GetMapping("/api/v1/payments/property/{id}")
  List<PaymentView> byProperty(@PathVariable("id") UUID propertyId);

  record PaymentView(UUID paymentId, UUID propertyId, UUID compensationResultId,
                     BigDecimal amount, String status, Instant initiatedAt, Instant completedAt) {}
}
