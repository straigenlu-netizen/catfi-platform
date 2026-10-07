package dev.catfi.bff.security;

import java.sql.Timestamp;
import java.time.*;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuditService {
  private final JdbcTemplate jdbc;

  public void recordSecurity(String username, String eventType) {
    jdbc.update("insert into agent_audit_events(username,event_type,resource,event_count,first_at,last_at) values (?,?,?,1,now(),now())",
        safe(username), eventType, "AUTH");
  }

  public void recordAction(String username, String method, String resource) {
    if ("GET".equals(method)) {
      Instant bucket = Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MINUTES);
      bucket = bucket.minusSeconds((bucket.getEpochSecond() % 300));
      jdbc.update("""
          insert into agent_audit_events(username,event_type,resource,bucket_start,event_count,first_at,last_at)
          values (?,?,?, ?,1,now(),now())
          on conflict (username,event_type,resource,bucket_start) where bucket_start is not null
          do update set event_count=agent_audit_events.event_count+1,last_at=now()
          """, safe(username), "READ", resource, Timestamp.from(bucket));
    } else {
      jdbc.update("insert into agent_audit_events(username,event_type,resource,event_count,first_at,last_at) values (?,?,?,1,now(),now())",
          safe(username), method, resource);
    }
  }

  private String safe(String username) { return username == null || username.isBlank() ? "UNKNOWN" : username; }
}
