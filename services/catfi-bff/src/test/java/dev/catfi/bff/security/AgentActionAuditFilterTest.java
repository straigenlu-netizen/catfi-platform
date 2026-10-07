package dev.catfi.bff.security;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

class AgentActionAuditFilterTest {
  @Test void shouldNormalizeUuidResourceIds() {
    assertThat(AgentActionAuditFilter.normalize("/api/ui/properties/20000000-0000-0000-0000-000000000001"))
        .isEqualTo("/api/ui/properties/{id}");
  }
}
