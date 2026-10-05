package dev.catfi.bff.application;

import dev.catfi.common.domain.*;
import java.math.BigDecimal;
import java.util.UUID;

public final class UiModels {
  private UiModels() {}

  public record PropertyView(
      UUID propertyId,
      UUID clientId,
      String address,
      String propertyType,
      double latitude,
      double longitude,
      BigDecimal estimatedValue) {}

  public record PolicyView(
      UUID policyId,
      UUID propertyId,
      BigDecimal coverageRate,
      BigDecimal deductible,
      BigDecimal policyLimit,
      BigDecimal alreadyPaid,
      boolean active) {}

  public record DashboardRow(
      UUID propertyId,
      String address,
      String clientName,
      String riskProfile,
      String status,
      BigDecimal totalDamage,
      BigDecimal totalCompensation,
      BigDecimal actualPayout,
      PolicyView policy) {}

  public record PropertyDetails(
      PropertyView property, PolicyView policy, ClientProfile client, PropertyFullReport report) {}
}
