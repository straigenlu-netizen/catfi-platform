package dev.catfi.bff.application;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import dev.catfi.bff.adapter.client.*;
import dev.catfi.bff.application.UiModels.*;
import dev.catfi.common.domain.*;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.Test;

class UiAggregationServiceTest {

  private final ReportingClient reporting = mock(ReportingClient.class);
  private final DisasterClient disaster = mock(DisasterClient.class);
  private final PolicyClient policy = mock(PolicyClient.class);
  private final UserClient user = mock(UserClient.class);
  private final UiAggregationService service =
      new UiAggregationService(reporting, disaster, policy, user);

  @Test
  void shouldIncludeSafePropertiesEvenBeforeReportsExist() {
    var id = UUID.randomUUID();
    var owner = UUID.randomUUID();
    when(policy.getProperties())
        .thenReturn(
            List.of(new PropertyView(id, owner, "Tokyo", "RESIDENTIAL", 35, 139, BigDecimal.TEN)));
    when(reporting.getAllReports()).thenReturn(List.of());
    when(user.getClient(owner))
        .thenReturn(
            ClientProfile.builder()
                .fullName("Demo Client")
                .riskProfile(RiskProfile.MODERATE)
                .build());
    var rows = service.getDashboardData();
    assertThat(rows).hasSize(1);
    assertThat(rows.get(0).status()).isEqualTo("SAFE");
    assertThat(rows.get(0).clientName()).isEqualTo("Demo Client");
  }

  @Test
  void shouldMergePropertyPolicyOwnerAndReport() {
    var id = UUID.randomUUID();
    var owner = UUID.randomUUID();
    var property = new PropertyView(id, owner, "Tokyo", "RESIDENTIAL", 35, 139, BigDecimal.TEN);
    var terms =
        new PolicyView(
            UUID.randomUUID(),
            id,
            BigDecimal.ONE,
            BigDecimal.ZERO,
            BigDecimal.TEN,
            BigDecimal.ZERO,
            true);
    var report = PropertyFullReport.builder().propertyId(id).status("REPORTED").build();
    var profile =
        ClientProfile.builder().clientId(owner).riskProfile(RiskProfile.CONSERVATIVE).build();
    when(policy.getProperty(id)).thenReturn(property);
    when(policy.getPolicy(id)).thenReturn(terms);
    when(user.getClient(owner)).thenReturn(profile);
    when(reporting.getReportByProperty(id.toString())).thenReturn(report);
    var result = service.getPropertyDetails(id);
    assertThat(result.property()).isEqualTo(property);
    assertThat(result.policy()).isEqualTo(terms);
    assertThat(result.client()).isEqualTo(profile);
    assertThat(result.report()).isEqualTo(report);
  }

  @Test
  void shouldForwardDisasterRequest() {
    Map<String, Object> request = Map.of("type", "EARTHQUAKE");
    when(disaster.triggerDisaster(request)).thenReturn(Map.of("status", "CREATED"));
    assertThat(service.triggerDisaster(request)).containsEntry("status", "CREATED");
    verify(disaster).triggerDisaster(request);
  }
}
