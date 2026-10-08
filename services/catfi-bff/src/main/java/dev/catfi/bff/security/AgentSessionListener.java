package dev.catfi.bff.security;

import jakarta.servlet.http.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AgentSessionListener implements HttpSessionListener {
  private final AuditService audit;
  @Override public void sessionDestroyed(HttpSessionEvent event) {
    var session = event.getSession();
    Object username = session.getAttribute("catfi.agent");
    if (username == null) username = session.getAttribute("catfi.customer");
    Object explicitLogout = session.getAttribute("catfi.explicitLogout");
    if (username != null && !Boolean.TRUE.equals(explicitLogout))
      audit.recordSecurity(username.toString(), "SESSION_TIMEOUT");
  }
}
