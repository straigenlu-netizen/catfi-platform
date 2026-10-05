package dev.catfi.user.application;

import dev.catfi.common.domain.ClientProfile;
import dev.catfi.common.domain.RiskProfile;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {
  private final Map<UUID, ClientProfile> clientStore = new ConcurrentHashMap<>();

  public ClientProfile getClientProfile(UUID clientId) {
    return clientStore.getOrDefault(
        clientId,
        ClientProfile.builder()
            .clientId(clientId)
            .email("default@catfi.com")
            .fullName("Default Client")
            .riskProfile(RiskProfile.MODERATE)
            .build());
  }

  public void saveClientProfile(ClientProfile profile) {
    clientStore.put(profile.getClientId(), profile);
  }
}
