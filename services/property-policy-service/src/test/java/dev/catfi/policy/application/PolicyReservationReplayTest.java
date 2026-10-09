package dev.catfi.policy.application;

import static org.assertj.core.api.Assertions.assertThat;
import dev.catfi.policy.adapter.web.PolicyController;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.util.UUID;

class PolicyReservationReplayTest {
  @Test
  void legacyReservationEndpointCannotMutateInMemoryBudget() {
    var controller = new PolicyController(new PolicyService());
    try {
      controller.reserveCompensation(UUID.randomUUID());
      org.junit.jupiter.api.Assertions.fail("Expected HTTP 410");
    } catch (ResponseStatusException ex) {
      assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.GONE);
    }
  }
}
