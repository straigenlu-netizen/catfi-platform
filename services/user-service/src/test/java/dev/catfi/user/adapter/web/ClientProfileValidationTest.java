package dev.catfi.user.adapter.web;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.catfi.common.domain.ClientProfile;
import dev.catfi.common.domain.RiskProfile;
import dev.catfi.user.application.UserService;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ClientController.class)
class ClientProfileValidationTest {
  @Autowired MockMvc mvc;
  @MockitoBean UserService service;

  @Test void rejectsInvalidContactUpdatesBeforePersisting() throws Exception {
    mvc.perform(put("/api/v1/clients/{id}/profile", UUID.randomUUID())
        .contentType("application/json")
        .content("{\"fullName\":\"\",\"email\":\"invalid\"}"))
        .andExpect(status().isBadRequest());
    verifyNoInteractions(service);
  }

  @Test void delegatesValidContactUpdateWithoutAcceptingRiskProfile() throws Exception {
    UUID id = UUID.randomUUID();
    when(service.updateContact(id, "New Name", "new@example.com"))
        .thenReturn(ClientProfile.builder().clientId(id).fullName("New Name")
            .email("new@example.com").riskProfile(RiskProfile.MODERATE).build());
    mvc.perform(put("/api/v1/clients/{id}/profile", id)
        .contentType("application/json")
        .content("{\"fullName\":\"New Name\",\"email\":\"new@example.com\",\"riskProfile\":\"AGGRESSIVE\"}"))
        .andExpect(status().isOk());
    verify(service).updateContact(id, "New Name", "new@example.com");
  }
}
