package dev.catfi.user.application;

import dev.catfi.common.domain.ClientProfile;
import dev.catfi.common.domain.RiskProfile;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

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

  // Only editable profile fields; risk profile and ownership remain unchanged.
  public ClientProfile updateContact(UUID clientId, String name, String email) {
    ClientProfile updated = clientStore.computeIfPresent(clientId, (id, existing) ->
        ClientProfile.builder()
            .clientId(id)
            .fullName(name.trim())
            .email(email.trim())
            .riskProfile(existing.getRiskProfile())
            .build());
    if (updated == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Client not found");
    return updated;
  }

  public void saveClientProfile(ClientProfile profile) {
    clientStore.put(profile.getClientId(), profile);
  }
}
