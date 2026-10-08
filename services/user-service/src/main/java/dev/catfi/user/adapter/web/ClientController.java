package dev.catfi.user.adapter.web;

import dev.catfi.common.domain.ClientProfile;
import dev.catfi.user.application.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
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

  @PutMapping("/{clientId}/profile")
  public ClientProfile updateProfile(@PathVariable UUID clientId, @Valid @RequestBody ProfileUpdate request) {
    return userService.updateContact(clientId, request.fullName(), request.email());
  }

  public record ProfileUpdate(
      @NotBlank @Size(max = 120) String fullName,
      @NotBlank @Email @Size(max = 254) String email) {}

  @PostMapping
  public void saveClient(@RequestBody ClientProfile profile) {
    userService.saveClientProfile(profile);
  }
}
