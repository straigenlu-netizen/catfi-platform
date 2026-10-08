package dev.catfi.bff.security;

import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

/** Synthetic accounts exist ONLY in the demo profile. Rotate passwords before public hosting. */
@Configuration
@Profile("demo")
public class DemoPortalAccountsConfiguration {
  @Bean
  ApplicationRunner seedPortalAccounts(
      JdbcTemplate jdbc,
      PasswordEncoder passwordEncoder,
      @Value("${catfi.demo.customer.password:CatFiLocalCustomer2026!}") String customerPassword,
      @Value("${catfi.demo.agent.password:CatFiLocalAgent2026!}") String agentPassword) {
    return args -> {
      for (int i = 1; i <= 3; i++) {
        UUID clientId = UUID.fromString("20000000-0000-0000-0000-00000000000" + i);
        jdbc.update("""
            insert into customer_accounts(username,password_hash,client_id,role,enabled)
            values (?,?,?,'ROLE_CUSTOMER',true) on conflict (username) do nothing
            """, "demo" + i + "@example.com", passwordEncoder.encode(customerPassword), clientId);
      }
      jdbc.update("""
          insert into agent_accounts(username,password_hash,role,enabled)
          values (?,?,'ROLE_AGENT',true) on conflict (username) do nothing
          """, "demo.agent", passwordEncoder.encode(agentPassword));
    };
  }
}
