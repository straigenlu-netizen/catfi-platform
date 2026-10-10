package dev.catfi.bff.security;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.catfi.bff.adapter.web.UiController;
import dev.catfi.bff.application.UiAggregationService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
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
  void shouldRejectUnauthenticatedMetricsScrape() throws Exception {
    mvc.perform(get("/actuator/prometheus")).andExpect(status().isUnauthorized());
  }

  @Test
  void shouldRejectCustomerMetricsScrape() throws Exception {
    mvc.perform(get("/actuator/prometheus").with(user("customer").roles("CUSTOMER")))
        .andExpect(status().isForbidden());
  }

  @Test
  void shouldRejectAnonymousUiRequest() throws Exception {
    mvc.perform(get("/api/ui/dashboard"))
        .andExpect(status().isUnauthorized());
    verifyNoInteractions(service);
  }

  @Test
  void shouldAllowAgentUiRequest() throws Exception {
    when(service.getDashboardData()).thenReturn(List.of());

    mvc.perform(get("/api/ui/dashboard")
            .with(user("AgentCRM1").roles("AGENT")))
        .andExpect(status().isOk());
  }
  @Test
  void shouldRejectAnonymousSimulationRequest() throws Exception {
    mvc.perform(post("/api/ui/trigger-disaster").with(csrf()))
        .andExpect(status().isUnauthorized());
    verifyNoInteractions(service);
  }

  @Test
  void shouldRejectAgentSimulationWithoutCsrf() throws Exception {
    mvc.perform(post("/api/ui/trigger-disaster")
            .with(user("AgentCRM1").roles("AGENT")))
        .andExpect(status().isForbidden());
    verifyNoInteractions(service);
  }

  @Test
  void shouldRejectAgentSimulationWithInvalidCsrf() throws Exception {
    mvc.perform(post("/api/ui/trigger-disaster")
            .with(user("AgentCRM1").roles("AGENT"))
            .with(csrf().useInvalidToken()))
        .andExpect(status().isForbidden());
    verifyNoInteractions(service);
  }

  @Test
  void shouldRejectNonAgentDashboardAccess() throws Exception {
    mvc.perform(get("/api/ui/dashboard").with(user("customer").roles("CUSTOMER")))
        .andExpect(status().isForbidden());
    verifyNoInteractions(service);
  }

  @Test
  void shouldRejectNonAgentSimulationWithCsrf() throws Exception {
    mvc.perform(post("/api/ui/trigger-disaster")
            .with(user("customer").roles("CUSTOMER"))
            .with(csrf()))
        .andExpect(status().isForbidden());
    verifyNoInteractions(service);
  }
}
