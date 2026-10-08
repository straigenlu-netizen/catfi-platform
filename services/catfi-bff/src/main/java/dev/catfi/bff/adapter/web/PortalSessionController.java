package dev.catfi.bff.adapter.web;

import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class PortalSessionController {
  @GetMapping("/me")
  public SessionView current(Authentication authentication) {
    Set<String> roles = authentication.getAuthorities().stream()
        .map(a -> a.getAuthority()).collect(java.util.stream.Collectors.toSet());
    String role = roles.contains("ROLE_AGENT") ? "AGENT" : roles.contains("ROLE_CUSTOMER") ? "CUSTOMER" : null;
    if (role == null) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Unknown portal role");
    return new SessionView(authentication.getName(), role);
  }

  public record SessionView(String username, String role) {}
}
