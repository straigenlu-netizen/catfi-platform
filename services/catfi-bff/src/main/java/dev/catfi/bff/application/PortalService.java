package dev.catfi.bff.application;

import dev.catfi.bff.adapter.client.PaymentClient;
import dev.catfi.bff.adapter.client.PolicyClient;
import dev.catfi.bff.adapter.client.UserClient;
import dev.catfi.bff.application.UiModels.*;
import dev.catfi.common.domain.ClientProfile;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/** Enforces per-customer ownership before querying details or payment histories. */
@Service
@RequiredArgsConstructor
public class PortalService {
  private final JdbcTemplate jdbc;
  private final UserClient users;
  private final PolicyClient policies;
  private final PaymentClient payments;
  private final UiAggregationService aggregation;

  public UUID currentClientId(Authentication authentication) {
    List<UUID> ids = jdbc.query("select client_id from customer_accounts where username = ? and enabled = true",
        (rs, row) -> rs.getObject("client_id", UUID.class), authentication.getName());
    if (ids.size() != 1) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Account is not linked to a client");
    return ids.get(0);
  }

  public ClientProfile myProfile(Authentication authentication) {
    return users.getClient(currentClientId(authentication));
  }

  public ClientProfile updateProfile(Authentication authentication, UserClient.ProfileUpdate request) {
    return users.updateProfile(currentClientId(authentication), request);
  }

  public List<PropertyDetails> myPolicies(Authentication authentication) {
    UUID owner = currentClientId(authentication);
    return policies.getProperties().stream()
        .filter(p -> owner.equals(p.clientId()))
        .map(p -> aggregation.getPropertyDetails(p.propertyId())).toList();
  }

  public PropertyDetails myPolicy(Authentication authentication, UUID propertyId) {
    UUID owner = currentClientId(authentication);
    PropertyView property = policies.getProperty(propertyId);
    if (property == null || !owner.equals(property.clientId())) {
      // Deliberately identical for absent and non-owned properties (IDOR prevention).
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Policy not found");
    }
    return aggregation.getPropertyDetails(propertyId);
  }

  public List<PaymentClient.PaymentView> myPayments(Authentication authentication) {
    UUID owner = currentClientId(authentication);
    return policies.getProperties().stream()
        .filter(p -> owner.equals(p.clientId()))
        .flatMap(p -> payments.byProperty(p.propertyId()).stream())
        .sorted(Comparator.comparing(PaymentClient.PaymentView::initiatedAt,
            Comparator.nullsLast(Comparator.reverseOrder())))
        .toList();
  }

  public List<AgentClientView> agentClients() {
    List<PropertyView> all = policies.getProperties();
    return all.stream()
        .map(PropertyView::clientId).distinct()
        .map(id -> {
          ClientProfile profile = users.getClient(id);
          long count = all.stream().filter(p -> id.equals(p.clientId())).count();
          return new AgentClientView(id, profile.getFullName(), profile.getEmail(),
              profile.getRiskProfile().name(), count);
        }).toList();
  }

  public AgentClientDetails agentClient(UUID clientId) {
    List<PropertyDetails> details = policies.getProperties().stream()
        .filter(p -> clientId.equals(p.clientId()))
        .map(p -> aggregation.getPropertyDetails(p.propertyId())).toList();
    if (details.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Client not found");
    return new AgentClientDetails(users.getClient(clientId), details);
  }

  public List<PaymentClient.PaymentView> agentPayments() {
    return policies.getProperties().stream()
        .flatMap(p -> payments.byProperty(p.propertyId()).stream())
        .sorted(Comparator.comparing(PaymentClient.PaymentView::initiatedAt,
            Comparator.nullsLast(Comparator.reverseOrder())))
        .toList();
  }

  public record AgentClientView(UUID clientId, String fullName, String email,
                                String riskProfile, long propertiesCount) {}
  public record AgentClientDetails(ClientProfile profile, List<PropertyDetails> policies) {}
}
