package dev.catfi.bff.security;

import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Returns the session-backed CSRF token before a state-changing browser request. */
@RestController
public class CsrfController {
  @GetMapping("/api/auth/csrf")
  public CsrfToken csrf(CsrfToken token) {
    return token;
  }
}
