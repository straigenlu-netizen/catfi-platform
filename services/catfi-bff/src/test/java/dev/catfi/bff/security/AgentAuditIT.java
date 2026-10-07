package dev.catfi.bff.security;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@SpringBootTest(properties = "spring.sql.init.mode=always")
class AgentAuditIT {
  @Container @ServiceConnection
  static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17.6-alpine");
  @Autowired JdbcTemplate jdbc;
  @Autowired AuditService audit;

  @Test void shouldSeedDefaultAgentAndPersistAuditEvent() {
    assertThat(jdbc.queryForObject("select count(*) from agent_accounts where username='AgentCRM1'", Integer.class)).isEqualTo(1);
    audit.recordAction("AgentCRM1", "POST", "/api/ui/trigger-disaster");
    assertThat(jdbc.queryForObject("select count(*) from agent_audit_events where username='AgentCRM1' and event_type='POST'", Integer.class)).isEqualTo(1);
  }
}
