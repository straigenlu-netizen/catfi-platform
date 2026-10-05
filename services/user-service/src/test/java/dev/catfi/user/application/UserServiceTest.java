package dev.catfi.user.application;

import static org.assertj.core.api.Assertions.*;

import dev.catfi.common.domain.ClientProfile;
import dev.catfi.common.domain.RiskProfile;
import java.util.*;
import org.junit.jupiter.api.Test;

class UserServiceTest {

  @Test
  void shouldReturnSavedProfileWithoutChangingRisk() {
    var service = new UserService();
    var id = UUID.randomUUID();
    var profile =
        ClientProfile.builder()
            .clientId(id)
            .riskProfile(RiskProfile.AGGRESSIVE)
            .email("test@example.com")
            .build();
    service.saveClientProfile(profile);
    assertThat(service.getClientProfile(id)).isEqualTo(profile);
  }

  @Test
  void shouldUseModerateDefaultForUnknownClient() {
    var id = UUID.randomUUID();
    var profile = new UserService().getClientProfile(id);
    assertThat(profile.getClientId()).isEqualTo(id);
    assertThat(profile.getRiskProfile()).isEqualTo(RiskProfile.MODERATE);
  }
}
