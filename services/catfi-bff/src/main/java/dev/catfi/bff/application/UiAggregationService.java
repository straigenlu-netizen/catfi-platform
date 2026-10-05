package dev.catfi.bff.application;

import dev.catfi.bff.adapter.client.*;
import dev.catfi.bff.application.UiModels.*;
import dev.catfi.common.domain.*;
import feign.FeignException;
import java.math.BigDecimal;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UiAggregationService {
  private final ReportingClient reportingClient;
  private final DisasterClient disasterClient;
  private final PolicyClient policyClient;
  private final UserClient userClient;

  public List<DashboardRow> getDashboardData() {
    Map<UUID, PropertyFullReport> reports = new HashMap<>();
    for (PropertyFullReport report : reportingClient.getAllReports())
      reports.put(report.getPropertyId(), report);
    return policyClient.getProperties().stream()
        .map(
            property -> {
              var report = reports.get(property.propertyId());
              var client = userClient.getClient(property.clientId());
              return new DashboardRow(
                  property.propertyId(),
                  property.address(),
                  client.getFullName(),
                  client.getRiskProfile().name(),
                  report == null ? "SAFE" : report.getStatus(),
                  report == null ? BigDecimal.ZERO : report.getTotalDamage(),
                  report == null ? BigDecimal.ZERO : report.getTotalCompensation(),
                  report == null ? BigDecimal.ZERO : report.getActualPayout(),
                  activePolicy(property.propertyId()));
            })
        .toList();
  }

  public PropertyDetails getPropertyDetails(UUID id) {
    var property = policyClient.getProperty(id);
    return new PropertyDetails(
        property, activePolicy(id), userClient.getClient(property.clientId()), latestReport(id));
  }

  private PolicyView activePolicy(UUID id) {
    try {
      return policyClient.getPolicy(id);
    } catch (FeignException.NotFound exception) {
      return null;
    }
  }

  private PropertyFullReport latestReport(UUID id) {
    try {
      return reportingClient.getReportByProperty(id.toString());
    } catch (FeignException.NotFound exception) {
      return null;
    }
  }

  public Map<String, Object> triggerDisaster(Map<String, Object> request) {
    return disasterClient.triggerDisaster(request);
  }
}
