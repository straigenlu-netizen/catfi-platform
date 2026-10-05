# Roadmap

## Delivered local demo

- All Maven modules build with Java 21 and consistent dependency/test configuration.
- Manual simulation, registered-property sensors, damage, compensation, separate payout/investment paths, positions and correlated reports.
- BFF aggregation, React dashboard/simulation/property views, financial charts and responsive layout.
- Unit/web/component tests, real Kafka/HTTP integration and a browser-driven pipeline check.
- Compose/Docker wiring and synthetic demo fixtures; runtime Docker/PostgreSQL validation remains outstanding.

## Next work

1. Start Compose on a Docker-enabled machine and run the PostgreSQL migration/persistence test.
2. Add payment/portfolio migrations and durable user, policy and reporting storage.
3. Implement policy budget initialization/updates, locking, inbox/outbox and duplicate-safe financial effects. Test concurrency, rollback, retries and restarts.
4. Add OIDC login, role/scope and object ownership enforcement to BFF and service boundaries; validate real signed tokens.
5. Connect geographic/intensity models, trusted policy versions and real market snapshots. Finalize explainable investment/fee rules.
6. Add tracing, metrics, dependency/static checks and measured Gatling scenarios.
7. Add notifications and optional blockchain anchoring before considering Kubernetes/AWS delivery.

Tests, documentation and security checks accompany each behavior change. Proposed tools and load targets are not achieved results.
