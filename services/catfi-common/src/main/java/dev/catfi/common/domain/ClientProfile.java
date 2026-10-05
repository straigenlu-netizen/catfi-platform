package dev.catfi.common.domain;

import java.util.UUID;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ClientProfile {
  UUID clientId;
  String email;
  String fullName;
  RiskProfile riskProfile;
}
