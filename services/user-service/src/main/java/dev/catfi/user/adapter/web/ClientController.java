package dev.catfi.user.adapter.web;

import dev.catfi.common.domain.ClientProfile;
import dev.catfi.user.application.UserService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/clients")
@RequiredArgsConstructor
public class ClientController {
  private final UserService userService;

  @GetMapping("/{clientId}")
  public ClientProfile getClient(@PathVariable UUID clientId) {
    return userService.getClientProfile(clientId);
  }

  @PostMapping
  public void saveClient(@RequestBody ClientProfile profile) {
    userService.saveClientProfile(profile);
  }
}
