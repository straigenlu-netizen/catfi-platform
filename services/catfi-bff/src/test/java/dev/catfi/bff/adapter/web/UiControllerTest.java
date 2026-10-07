package dev.catfi.bff.adapter.web;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;

@org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest(UiController.class)
@AutoConfigureMockMvc(addFilters = false)
class UiControllerTest {

  @org.springframework.test.context.bean.override.mockito.MockitoBean
  private dev.catfi.bff.security.AuditService auditService;

  @org.springframework.beans.factory.annotation.Autowired
  private org.springframework.test.web.servlet.MockMvc mvc;

  @org.springframework.test.context.bean.override.mockito.MockitoBean
  private dev.catfi.bff.application.UiAggregationService service;

  @Test
  @WithMockUser(username = "AgentCRM1", roles = "AGENT")
  void shouldReturnDashboard() throws Exception {
    when(service.getDashboardData()).thenReturn(List.of());
    mvc.perform(get("/api/ui/dashboard"))
        .andExpect(status().isOk())
        .andExpect(content().json("[]"));
  }

  @Test
  @WithMockUser(username = "AgentCRM1", roles = "AGENT")
  void shouldRejectUnsupportedDisaster() throws Exception {
    mvc.perform(
            post("/api/ui/trigger-disaster")
                .contentType("application/json")
                .content(
                    "{\"type\":\"UNKNOWN\",\"latitude\":35,\"longitude\":139,\"intensity\":7}"))
        .andExpect(status().isBadRequest());
    verifyNoInteractions(service);
  }

  @Test
  @WithMockUser(username = "AgentCRM1", roles = "AGENT")
  void shouldRejectMissingCoordinates() throws Exception {
    mvc.perform(
            post("/api/ui/trigger-disaster")
                .contentType("application/json")
                .content("{\"type\":\"EARTHQUAKE\",\"intensity\":7}"))
        .andExpect(status().isBadRequest());
    verifyNoInteractions(service);
  }

  @Test
  @WithMockUser(username = "AgentCRM1", roles = "AGENT")
  void shouldAllowFrontendCorsAndRejectUntrustedOrigin() throws Exception {
    mvc.perform(
            options("/api/ui/dashboard")
                .header("Origin", "http://localhost:5173")
                .header("Access-Control-Request-Method", "GET"))
        .andExpect(status().isOk())
        .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
    mvc.perform(
            options("/api/ui/dashboard")
                .header("Origin", "https://untrusted.example")
                .header("Access-Control-Request-Method", "GET"))
        .andExpect(status().isForbidden());
  }
}
