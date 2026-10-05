package dev.catfi.user.configuration;

import dev.catfi.common.domain.*;
import dev.catfi.user.application.UserService;
import java.util.UUID;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.*;

@Configuration
@Profile("demo")
public class DemoDataConfiguration {
  @Bean
  ApplicationRunner seedClients(UserService service) {
    return args -> {
      RiskProfile[] risks = {
        RiskProfile.CONSERVATIVE, RiskProfile.MODERATE, RiskProfile.AGGRESSIVE
      };
      for (int i = 1; i <= 3; i++)
        service.saveClientProfile(
            ClientProfile.builder()
                .clientId(UUID.fromString("20000000-0000-0000-0000-00000000000" + i))
                .fullName("Demo Client " + i)
                .email("demo" + i + "@example.com")
                .riskProfile(risks[i - 1])
                .build());
    };
  }
}
