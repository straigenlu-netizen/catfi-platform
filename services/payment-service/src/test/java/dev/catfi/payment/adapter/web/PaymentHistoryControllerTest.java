package dev.catfi.payment.adapter.web;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import dev.catfi.common.domain.PaymentStatus;
import dev.catfi.payment.adapter.persistence.PaymentEntity;
import dev.catfi.payment.adapter.persistence.PaymentRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PaymentHistoryController.class)
class PaymentHistoryControllerTest {
  @Autowired MockMvc mvc;
  @MockitoBean PaymentRepository repository;

  @Test void returnsPaymentHistoryForRequestedProperty() throws Exception {
    UUID property = UUID.randomUUID();
    var saved = PaymentEntity.builder().id(UUID.randomUUID()).propertyId(property)
        .compensationResultId(UUID.randomUUID()).amount(new BigDecimal("45.00"))
        .status(PaymentStatus.COMPLETED).initiatedAt(Instant.parse("2026-10-08T10:00:00Z"))
        .completedAt(Instant.parse("2026-10-08T10:01:00Z")).build();
    when(repository.findByPropertyIdOrderByInitiatedAtDesc(property)).thenReturn(List.of(saved));
    mvc.perform(get("/api/v1/payments/property/{id}", property))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].propertyId").value(property.toString()))
        .andExpect(jsonPath("$[0].amount").value(45.0))
        .andExpect(jsonPath("$[0].status").value("COMPLETED"));
  }
}
