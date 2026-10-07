package dev.catfi.bff.security;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;

import dev.catfi.bff.adapter.web.UiController;
import dev.catfi.bff.application.UiAggregationService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(UiController.class)
@Import(AgentSecurityConfiguration.class)
class AgentSecurityConfigurationTest {
  @Autowired MockMvc mvc;

  @MockitoBean UiAggregationService service;
  @MockitoBean AuditService audit;
  @MockitoBean javax.sql.DataSource dataSource;

  @Test
  void shouldRejectAnonymousUiRequest() throws Exception {
    mvc.perform(get("/api/ui/dashboard"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrlPattern("**/login"));
    verifyNoInteractions(service);
  }

  @Test
  void shouldAllowAgentUiRequest() throws Exception {
    when(service.getDashboardData()).thenReturn(List.of());

    mvc.perform(get("/api/ui/dashboard")
            .with(user("AgentCRM1").roles("AGENT")))
        .andExpect(status().isOk());
  }
}
