package dev.catfi.compensation.adapter.web;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.catfi.compensation.configuration.ApplicationConfiguration;
import dev.catfi.compensation.configuration.SecurityConfiguration;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CompensationController.class)
@Import({SecurityConfiguration.class, ApplicationConfiguration.class})
class CompensationControllerTest {
  private static final String URL = "/api/v1/compensations/preview";
  private static final String REQUEST =
      """
      {"damage":20000,"coverageRate":0.9,"deductible":5000,"policyLimit":100000,"alreadyPaid":0}
      """;
  @Autowired private MockMvc mvc;
  @MockitoBean private JwtDecoder jwtDecoder;

  @Test
  void anonymousRequestIsUnauthorized() throws Exception {
    mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(REQUEST))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void unknownEndpointsAreDeniedByDefault() throws Exception {
    mvc.perform(
            get("/api/v1/internal/admin")
                .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_compensation:preview"))))
        .andExpect(status().isForbidden());
  }

  @Test
  void wrongHttpMethodDoesNotBypassPreviewAuthorization() throws Exception {
    mvc.perform(
            get(URL)
                .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_compensation:preview"))))
        .andExpect(status().isForbidden());
  }

  @Test
  void securityHeadersArePresentOnProtectedResponses() throws Exception {
    mvc.perform(
            post(URL)
                .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_compensation:preview")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(REQUEST))
        .andExpect(status().isOk())
        .andExpect(header().string("X-Content-Type-Options", "nosniff"))
        .andExpect(header().string("X-Frame-Options", Matchers.not(Matchers.emptyOrNullString())))
        .andExpect(header().string("Cache-Control", Matchers.containsString("no-store")));
  }

  @Test
  void metricsScopeDoesNotGrantFinancialPreviewAccess() throws Exception {
    mvc.perform(
            post(URL)
                .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_metrics:read")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(REQUEST))
        .andExpect(status().isForbidden());
  }

  @Test
  void missingScopeIsForbidden() throws Exception {
    mvc.perform(post(URL).with(jwt()).contentType(MediaType.APPLICATION_JSON).content(REQUEST))
        .andExpect(status().isForbidden());
  }

  @Test
  void authorizedPreviewReturnsCalculatedAmount() throws Exception {
    mvc.perform(
            post(URL)
                .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_compensation:preview")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(REQUEST))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.amount").value(13000))
        .andExpect(jsonPath("$.currency").value("EUR"));
  }

  @Test
  void negativeAmountIsRejected() throws Exception {
    mvc.perform(
            post(URL)
                .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_compensation:preview")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(REQUEST.replace("20000", "-1")))
        .andExpect(status().isBadRequest());
  }

  @Test
  void paidAmountAboveLimitIsRejected() throws Exception {
    mvc.perform(
            post(URL)
                .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_compensation:preview")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(REQUEST.replace("\"alreadyPaid\":0", "\"alreadyPaid\":100001")))
        .andExpect(status().isBadRequest());
  }
}
