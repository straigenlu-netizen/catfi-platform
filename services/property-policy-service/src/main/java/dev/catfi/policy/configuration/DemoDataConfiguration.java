package dev.catfi.policy.configuration;

import dev.catfi.policy.application.PolicyService;
import dev.catfi.policy.domain.*;
import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.*;

@Configuration
@Profile("demo")
public class DemoDataConfiguration {
  @Bean
  ApplicationRunner seedProperties(PolicyService service) {
    return args -> {
      String[] addresses = {"Chiyoda, Tokyo", "Shinjuku, Tokyo", "Minato, Tokyo"};
      for (int i = 1; i <= 3; i++) {
        UUID property = UUID.fromString("10000000-0000-0000-0000-00000000000" + i);
        UUID client = UUID.fromString("20000000-0000-0000-0000-00000000000" + i);
        service.saveProperty(
            Property.builder()
                .propertyId(property)
                .clientId(client)
                .address(addresses[i - 1])
                .latitude(35.67 + i * 0.01)
                .longitude(139.70 + i * 0.01)
                .propertyType("RESIDENTIAL")
                .estimatedValue(new BigDecimal("250000"))
                .build());
        service.savePolicy(
            InsurancePolicy.builder()
                .policyId(UUID.fromString("30000000-0000-0000-0000-00000000000" + i))
                .propertyId(property)
                .clientId(client)
                .coverageRate(new BigDecimal("0.9"))
                .deductible(new BigDecimal("5000"))
                .policyLimit(new BigDecimal("100000"))
                .alreadyPaid(BigDecimal.ZERO)
                .active(true)
                .build());
      }
    };
  }
}
