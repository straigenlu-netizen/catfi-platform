# Decision log

## ADR-001: Java 21 and a Maven monorepo

Java 21 keeps learning and runtime environments consistent. The Maven parent manages versions; each service is an independent application. There is currently one module. Spring Boot 3.5.16 is retained from the scaffold; it is not claimed to be the latest release.

Reference: https://docs.spring.io/spring-boot/3.5/system-requirements.html

## ADR-002: Dependencies point inward

The domain remains plain Java. Use constructor injection, immutable records, and separate HTTP DTOs. Add interfaces at concrete boundaries such as persistence, Kafka, or external APIs. Strategy is appropriate when multiple investment policies exist; Factory is appropriate when creation genuinely needs implementation selection. Avoid patterns added only to increase their count.

## ADR-003: Money and formula v1

Use BigDecimal, EUR, and two decimal places. Apply HALF_EVEN only to the final result. Coverage precedes the deductible: 20000 × 0.9 − 5000 = 13000. This educational rule is not a universal insurance formula. Changing operation order requires a new rule version and updated tests.

## ADR-004: Preview first

Preview validates business rules and HTTP/security boundaries without implementing transfers prematurely. Callers supply preview terms; actual claims must load trusted policy snapshots.

## ADR-005: Bearer-only security

Use a JWT resource server, issuer/audience validation, scopes, stateless sessions, and deny-by-default permissions. CSRF is disabled for the bearer-only API without cookie authentication. Reassess this for cookies/BFF. Health exposes only overall status.

Reference: https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/jwt.html

## ADR-006: Root documentation

Business and architectural explanations live in root Markdown documents. Keep comments minimal; retain required Liquibase directives. Update tests and documentation when behavior changes. All project textual content is English.

## ADR-007: Targeted Lombok use

Use @RequiredArgsConstructor in CompensationController and PreviewCompensation to generate constructors for final dependencies. Keep records and the plain Java domain unchanged. Use Spring Boot's managed Lombok version for both the optional dependency and the explicit Maven compiler annotation processor. Exclude Lombok from the executable application archive.

Avoid @Data on entities and avoid unnecessary builders, getters, or logging. Equality, lazy associations, and sensitive-field exposure remain deliberate decisions. IDE behavior must be checked separately from Maven compilation.

## ADR-008: Proposed aggregate policy-limit concurrency boundary

Status: designed for the next persistence slice; not implemented.

The Compensation Service will own a policy-budget row per policy containing the trusted snapshot reference and aggregate allocated compensation. This aggregate represents already committed compensation, including pending payout, so a second claim cannot spend a committed amount again. The preview's alreadyPaid input remains unchanged.

Within one PostgreSQL transaction, insert a unique inbox event identifier, lock the policy-budget row with SELECT FOR UPDATE, load the trusted terms, calculate against the remaining budget, update the aggregate, and persist the compensation, audit record, and outbox event. A unique claim identifier additionally prevents a duplicate claim arriving under a different event identifier. Any failure rolls back all changes, including inbox insertion. Concurrent claims for the same policy serialize; different policies can proceed independently.

A separate payout lifecycle and reservation release rules still need design. Do not use this proposal to claim payouts have occurred. Define snapshot versioning and policy-budget initialization before implementation. Publish committed outbox records with retries and stable identifiers; consumers remain idempotent. Tests must cover concurrent claims, duplicate identifiers, rollback, and recovery against real PostgreSQL/Kafka.

## ADR-009 — Control room and verifiable transport contracts

Status: implemented for the local demo.

A BFF keeps browser contracts and CORS policy in one place. OpenFeign adapters read property/policy/user/reporting APIs; React uses Axios and TanStack Query polling. ECharts is lazy-loaded on the property page. Startup demo data uses fixed, synthetic client/property IDs.

Lombok-generated JDK ConstructorProperties metadata supports immutable shared-contract JSON deserialization without coupling domain models to Jackson annotations. The explicit CompensationTerms constructor carries the same JDK metadata. Round-trip tests and real Kafka tests verify the behavior.

Ordinary service jars remain consumable by the reactor; executable Spring Boot jars use the exec classifier. A separate test module composes application services and runs an HTTP/Kafka pipeline integration test. Persistence is mocked in that test; a PostgreSQL Testcontainers test separately validates the migration and JPA boundary when Docker is available. CORS is not treated as authorization.
