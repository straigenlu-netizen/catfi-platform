# CatFi architecture

## Current implementation

The React control room calls the BFF. OpenFeign adapters aggregate registered properties, owners, active policies and reports. Manual simulation requests are validated and proxied to the disaster simulator, which waits for Kafka publication acknowledgement. Domain calculations remain separate from HTTP, Kafka and database adapters.

Disaster events produce sensor observations for registered properties. Observations preserve the disaster ID. Damage assessments feed policy-backed compensation calculation. Compensation fans out into payout and investment paths. Investment decisions update portfolio positions; reporting correlates current assessment, compensation, payment and investment IDs. See PROJECT_DIAGRAM.md.

All 16 child modules participate in the Maven reactor. Fourteen are Spring Boot services; catfi-common provides immutable contracts and monetary terms; e2e-tests provides component and Kafka/HTTP integration checks. Ordinary jars remain reusable in tests; executable jars use the exec classifier. Shared contracts contain no Spring/JPA/Kafka/HTTP annotations. Lombok is compile-only; JDK constructor metadata supports JSON deserialization.

## Layers and data ownership

- Domain: immutable models, value validation and financial calculations.
- Application: use cases and allocation/calculation orchestration.
- Adapters: REST, OpenFeign, Kafka listeners/producers and repositories.
- Configuration: dependency wiring, security, client timeouts and demo fixtures.

PostgreSQL stores compensation, payment and portfolio records. The compensation entity has a Liquibase migration; payment and portfolio currently use Hibernate schema update. MongoDB stores damage assessments. Users, policies and reporting snapshots are currently in memory. Services communicate through APIs/events and do not query another service's tables. Shared transport contracts are not persistence entities.

## Reliability boundary

The current pipeline is an educational demo. Financial database writes and event publication are not one reliable transaction; no outbox/inbox or duplicate-safe effects are implemented. Policy aggregate limits require budget updates and concurrency protection. ADR-008 remains a proposed transaction boundary. Reporting correlates events and handles arrival order, but its state is not durable and has no history-retention policy.

The Kafka/HTTP/browser integration test exercises actual event processing with mocked persistence and policy/user boundaries. PostgreSQL Testcontainers and Compose runtime still require verification on a Docker-enabled machine.

## Implemented and planned technology

Implemented: Java 21, Spring Boot, targeted Lombok, Kafka adapters, PostgreSQL/JPA, compensation Liquibase migration, MongoDB adapter, Spring Cloud OpenFeign, React/TypeScript/Vite/MUI/TanStack Query/Axios/ECharts, JUnit/Mockito/AssertJ, React Testing Library and Playwright. JWT/scope protection currently covers compensation preview only.

Planned: Keycloak control-room login, ownership checks, Redis TTL caching, real market sources, geographic/intensity models, observability across services, Besu audit hashes, notifications, Gatling workloads, Kubernetes/Helm/Terraform/GitHub Actions/AWS. These are not implemented capabilities. A blockchain hash would establish an anchor, not prove that a financial calculation was correct.
