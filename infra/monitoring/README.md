# Local observability (Stage 4)

Prometheus and Grafana run only when `--profile monitoring` is selected.
They listen on **127.0.0.1** host ports. Prometheus scrapes the BFF's
Spring Boot Actuator endpoint using HTTP Basic agent credentials; anonymous
and customer access to `/actuator/prometheus` is blocked by Spring Security.
Do not publish these services publicly or reuse the demo account in production.

Before `docker compose --profile monitoring up -d`, create these untracked files:

- `infra/monitoring/agent-password.txt`: one-line **current** `demo.agent` password.
- `infra/monitoring/grafana-admin-password.txt`: a separate strong password.

The former must match `CATFI_DEMO_AGENT_PASSWORD` used by the BFF demo profile.
Both files are gitignored. For Windows PowerShell, avoid saving extra UTF-8 BOM:

```powershell
[IO.File]::WriteAllText('infra/monitoring/agent-password.txt', $env:CATFI_DEMO_AGENT_PASSWORD.Trim())
[IO.File]::WriteAllText('infra/monitoring/grafana-admin-password.txt', $env:CATFI_GRAFANA_ADMIN_PASSWORD.Trim())
docker compose --profile monitoring up -d prometheus grafana
```

Prometheus: http://127.0.0.1:9091; Grafana: http://127.0.0.1:3000.
Grafana username: `admin`, password: the file above. The datasource and
BFF dashboard (requests/sec, 5xx errors, p95 latency) are provisioned.
Useful PromQL:

```promql
sum by (route,outcome) (rate(catfi_bff_requests_total[5m]))
sum(rate(http_server_requests_seconds_count{application="catfi-bff"}[5m]))
sum(rate(http_server_requests_seconds_sum{application="catfi-bff"}[5m])) / clamp_min(sum(rate(http_server_requests_seconds_count{application="catfi-bff"}[5m])), 0.001)
```

The built-in `http.server.requests` timer provides timings and optional
histograms; `catfi.bff.requests` counts bounded route classes and outcomes.
No client IDs, usernames, raw URLs or secrets appear in custom metric labels.
Monitor targets and the `up` query before interpreting load-test results.
**Current scope:** BFF metrics and local dashboards, not full distributed tracing
or Kafka lag instrumentation. Add trace propagation and consumer metrics before
claiming end-to-end latency SLOs.
