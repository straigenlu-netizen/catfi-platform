package dev.catfi.bff.application;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import dev.catfi.bff.adapter.client.*;
import dev.catfi.bff.application.UiModels.*;
import dev.catfi.common.domain.*;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.server.ResponseStatusException;

class PortalServiceTest {
  private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
  private final UserClient users = mock(UserClient.class);
  private final PolicyClient policies = mock(PolicyClient.class);
  private final PaymentClient payments = mock(PaymentClient.class);
  private final UiAggregationService aggregation = mock(UiAggregationService.class);
  private final PortalService service = new PortalService(jdbc, users, policies, payments, aggregation);
  private final UUID owner = UUID.randomUUID();
  private final UUID outsider = UUID.randomUUID();
  private final UUID ownProperty = UUID.randomUUID();
  private final UUID foreignProperty = UUID.randomUUID();
  private final UsernamePasswordAuthenticationToken login =
      new UsernamePasswordAuthenticationToken("demo1@example.com", "unused");

  @BeforeEach
  @SuppressWarnings({"unchecked", "rawtypes"})
  void setup() {
    doReturn(List.of(owner)).when(jdbc).query(
        eq("select client_id from customer_accounts where username = ? and enabled = true"),
        any(RowMapper.class), eq("demo1@example.com"));
  }

  private PropertyView property(UUID propertyId, UUID clientId) {
    return new PropertyView(propertyId, clientId, "Tokyo", "RESIDENTIAL", 35, 139, BigDecimal.TEN);
  }

  @Test
  void customerNeverReceivesOtherPeoplesPolicies() {
    when(policies.getProperties()).thenReturn(List.of(property(ownProperty, owner), property(foreignProperty, outsider)));
    when(aggregation.getPropertyDetails(ownProperty)).thenReturn(mock(PropertyDetails.class));
    assertThat(service.myPolicies(login)).hasSize(1);
    verify(aggregation).getPropertyDetails(ownProperty);
    verify(aggregation, never()).getPropertyDetails(foreignProperty);
  }

  @Test
  void foreignPolicyIs404AndNeverQueriesSensitiveDetails() {
    when(policies.getProperty(foreignProperty)).thenReturn(property(foreignProperty, outsider));
    assertThatThrownBy(() -> service.myPolicy(login, foreignProperty))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("404");
    verifyNoInteractions(aggregation, payments);
  }

  @Test
  void customerPaymentsAreLimitedToOwnedProperties() {
    when(policies.getProperties()).thenReturn(List.of(property(ownProperty, owner), property(foreignProperty, outsider)));
    when(payments.byProperty(ownProperty)).thenReturn(List.of());
    assertThat(service.myPayments(login)).isEmpty();
    verify(payments).byProperty(ownProperty);
    verify(payments, never()).byProperty(foreignProperty);
  }

  @Test
  void profileUpdatesCannotChangeTheBoundClientId() {
    UserClient.ProfileUpdate update = new UserClient.ProfileUpdate("New name", "new@example.com");
    ClientProfile profile = ClientProfile.builder().clientId(owner).riskProfile(RiskProfile.MODERATE).build();
    when(users.updateProfile(owner, update)).thenReturn(profile);
    assertThat(service.updateProfile(login, update)).isEqualTo(profile);
    verify(users).updateProfile(owner, update);
  }

  @Test
  void unlinkedAccountIsForbidden() {
    var unknown = new UsernamePasswordAuthenticationToken("unknown", "unused");
    assertThatThrownBy(() -> service.myProfile(unknown))
        .isInstanceOf(ResponseStatusException.class).hasMessageContaining("403");
    verifyNoInteractions(users);
  }
}
