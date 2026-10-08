package dev.catfi.user.application;

import static org.assertj.core.api.Assertions.*;

import dev.catfi.common.domain.ClientProfile;
import dev.catfi.common.domain.RiskProfile;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class UserServiceContactTest {
  @Test void changingContactDetailsPreservesIdAndRisk() {
    var service = new UserService();
    UUID id = UUID.randomUUID();
    service.saveClientProfile(ClientProfile.builder().clientId(id)
        .fullName("Previous").email("previous@example.com").riskProfile(RiskProfile.AGGRESSIVE).build());
    ClientProfile updated = service.updateContact(id, " New Name ", " new@example.com ");
    assertThat(updated.getClientId()).isEqualTo(id);
    assertThat(updated.getRiskProfile()).isEqualTo(RiskProfile.AGGRESSIVE);
    assertThat(updated.getFullName()).isEqualTo("New Name");
    assertThat(updated.getEmail()).isEqualTo("new@example.com");
  }

  @Test void updateDoesNotCreateUnknownAccount() {
    assertThatThrownBy(() -> new UserService().updateContact(UUID.randomUUID(), "Name", "a@example.com"))
        .isInstanceOf(ResponseStatusException.class).hasMessageContaining("404");
  }
}
