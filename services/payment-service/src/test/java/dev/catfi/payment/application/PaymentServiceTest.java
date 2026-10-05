package dev.catfi.payment.application;

import static org.assertj.core.api.Assertions.*;

import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.Test;

class PaymentServiceTest {

  @Test
  void shouldPreservePaymentReferencesAndAmount() {
    var compensation = UUID.randomUUID();
    var property = UUID.randomUUID();
    var result =
        new PaymentService().initiatePayment(compensation, property, new BigDecimal("12.34"));
    assertThat(result.getCompensationResultId()).isEqualTo(compensation);
    assertThat(result.getPropertyId()).isEqualTo(property);
    assertThat(result.getAmount()).isEqualByComparingTo("12.34");
    assertThat(result.getStatus()).isEqualTo(dev.catfi.common.domain.PaymentStatus.COMPLETED);
    assertThat(result.getCompletedAt()).isAfterOrEqualTo(result.getInitiatedAt());
  }

  @Test
  void shouldRejectNegativePayment() {
    assertThatIllegalArgumentException()
        .isThrownBy(
            () ->
                new PaymentService()
                    .initiatePayment(UUID.randomUUID(), UUID.randomUUID(), new BigDecimal("-1")));
  }
}
