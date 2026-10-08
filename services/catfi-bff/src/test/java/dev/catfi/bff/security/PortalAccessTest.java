package dev.catfi.bff.security;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import dev.catfi.bff.adapter.web.AgentPortalController;
import dev.catfi.bff.adapter.web.CustomerPortalController;
import dev.catfi.bff.application.PortalService;
import dev.catfi.bff.application.UiAggregationService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest({CustomerPortalController.class, AgentPortalController.class})
@Import(AgentSecurityConfiguration.class)
class PortalAccessTest {
  @Autowired MockMvc mvc;
  @MockitoBean PortalService portal;
  @MockitoBean UiAggregationService aggregation;
  @MockitoBean AuditService audit;
  @MockitoBean javax.sql.DataSource database;

  @Test void anonymousCannotReadCustomerPolicies() throws Exception {
    mvc.perform(get("/api/customer/policies")).andExpect(status().isUnauthorized());
    verifyNoInteractions(portal);
  }

  @Test void agentCannotReadCustomerProfile() throws Exception {
    mvc.perform(get("/api/customer/profile").with(user("agent").roles("AGENT")))
        .andExpect(status().isForbidden());
    verifyNoInteractions(portal);
  }

  @Test void customerCannotReadAllAgentClients() throws Exception {
    mvc.perform(get("/api/agent/clients").with(user("customer").roles("CUSTOMER")))
        .andExpect(status().isForbidden());
    verifyNoInteractions(portal);
  }

  @Test void customerProfileUpdateRequiresCsrf() throws Exception {
    mvc.perform(put("/api/customer/profile").with(user("customer").roles("CUSTOMER"))
        .contentType("application/json")
        .content("{\"fullName\":\"Valid Name\",\"email\":\"test@example.com\"}"))
        .andExpect(status().isForbidden());
    verifyNoInteractions(portal);
  }

  @Test void agentCanListClients() throws Exception {
    when(portal.agentClients()).thenReturn(List.of());
    mvc.perform(get("/api/agent/clients").with(user("agent").roles("AGENT")))
        .andExpect(status().isOk()).andExpect(content().json("[]"));
  }

  @Test void customerCanListOnlyPoliciesProvidedByOwnershipService() throws Exception {
    when(portal.myPolicies(any())).thenReturn(List.of());
    mvc.perform(get("/api/customer/policies").with(user("customer").roles("CUSTOMER")))
        .andExpect(status().isOk()).andExpect(content().json("[]"));
  }
}
