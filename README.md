# CatFi · Control Room

An educational disaster recovery platform for Japan: synthetic properties and clients, simulated insurance compensation, payouts and investment allocations. The control room makes the asynchronous pipeline visible and lets you trigger a disaster manually.

## Browser security hardening

The agent UI uses a session cookie and session-backed CSRF validation for
state-changing requests. The frontend calls `GET /api/auth/csrf` before
publishing a disaster simulation, then sends the returned token in the
`X-XSRF-TOKEN` request header. Anonymous users and users without `ROLE_AGENT`
cannot access `/api/ui/**`. The CSRF endpoint requires authentication.

Run `./mvnw -B -ntp -pl services/catfi-bff -am test` and
`cd web-ui && npm test && npm run build` after applying the patch.
This focused BFF change does not resolve Kafka/idempotency,
payment consistency, or all public-deployment security concerns.

## Security CI

The separate `Code security` workflow (`.github/workflows/security.yml`) runs on pushes,
pull requests, manual dispatch and Mondays at 07:00 UTC. It checks Git history with
Gitleaks, Java dependencies with OWASP Dependency-Check, the repository/IaC with
Trivy, and the built container image with Trivy. HIGH and CRITICAL findings in
Trivy fail their respective jobs; Java dependencies fail at CVSS 7 or above.

CodeQL and Sonar are **opt-in**: set repository variables
`CATFI_ENABLE_CODEQL=true` (only when GitHub Code Security is available)
and `CATFI_ENABLE_SONAR=true` after configuring `SONAR_TOKEN` (secret),
`SONAR_HOST_URL`, `SONAR_PROJECT_KEY`, and `SONAR_ORGANIZATION` (variables).
Configure the `NVD_API_KEY` secret for more reliable OWASP NVD updates.
An intentionally skipped job is **not** evidence of a completed scan.

Before a public deployment, review every security finding, validate
authentication and authorization separately, and use synthetic demo data.
Run the workflow regression check locally:

```bash
python -m unittest discover -s tests -p 'test_security_workflow.py'
```

**Current state:** all 19 Maven reactor projects build; the BFF, React UI and Kafka demo flow are implemented. Current verification results and runtime limits are in [VALIDATION.md](VALIDATION.md). Last updated: 2026-10-06.

## Product experience

- **Dashboard:** every registered property, owner, risk profile, active policy and latest report. Properties without a report remain visible as `SAFE`.
- **Simulation lab:** validated disaster type, intensity and coordinates. Success means Kafka acknowledged publication; subsequent processing is asynchronous.
- **Property details:** policy terms, damage, compensation, payout and asset allocations. ECharts displays the financial breakdown in EUR.
- **Reporting:** `IMPACTED` becomes `REPORTED` when the matching compensation and its required payout/investment events arrive. Events are correlated by assessment and compensation IDs, including out-of-order arrivals.

![Control room dashboard](docs/ui-preview/dashboard.png)

## Run the complete local demo

Requirements: Docker Desktop with Linux containers and Docker Compose. Run commands from the directory containing this README and `pom.xml`.

```powershell
Copy-Item .env.example .env
```

Set `CATFI_DB_PASSWORD` in `.env`, then:

```powershell
docker compose build catfi-bff web-ui
docker compose up -d --wait
```

Open **http://localhost:5173**. The demo profile seeds three Tokyo properties and matching clients. Trigger a simulation, return to the dashboard and open a property. Reports refresh every two seconds. Initial application startup may take longer than infrastructure health checks; the UI provides Retry.

Backend applications share the `catfi-backend:local` image built by `catfi-bff`. Only the UI and BFF publish HTTP ports; internal service endpoints are not published to the host. Infrastructure ports bind to loopback. This is a local demo, with no production BFF authentication.

Existing PostgreSQL volumes are preserved. Initialization scripts run only for a fresh volume. If an older volume predates the three demo databases, create the missing databases in PostgreSQL; do not delete an existing volume to repair configuration.

Weather and market services are optional:

```powershell
docker compose --profile enrichment up -d
```

Set `WEATHER_API_KEY` before calling the weather provider. Market prices and shocks are currently simulated.

## Development and checks

Java 21 and Maven Wrapper 3.9.11; Node.js 24 for frontend development.

```powershell
.\mvnw.cmd clean verify
.\mvnw.cmd verify -Pintegration
```

The first command runs Java unit, web and component tests (see VALIDATION.md for current counts) without Docker. The integration profile includes embedded Kafka tests and PostgreSQL Testcontainers, so the full profile requires Docker.

```powershell
cd web-ui
npm ci
npm test
npm run build
npx playwright install chromium
npm run test:e2e
npm run dev
```

Vite proxies `/api/ui` to the BFF on port 8087. Browser tests use deterministic API fixtures by default; a separate live test exercises the BFF and Kafka pipeline. Reproduction commands are in [VALIDATION.md](VALIDATION.md).

## Service inventory

| Module | Current responsibility |
| --- | --- |
| catfi-common | Shared immutable contracts and monetary terms; constructor metadata supports JSON reading without runtime Lombok |
| catfi-bff | OpenFeign aggregation, validated simulation proxy, agent session authentication and PostgreSQL audit trail |
| user-service | In-memory client profiles; demo clients |
| property-policy-service | In-memory properties and active policies; demo fixtures and property APIs |
| disaster-simulator | Manual REST event publication; scheduled simulation is opt-in |
| sensor-simulator | Observations for registered properties; preserves disaster IDs |
| damage-service | Simulated sensor damage calculation, MongoDB persistence and Kafka publication |
| compensation-service | Policy-backed calculation, compensation persistence, protected preview endpoint |
| payment-service | Simulated completed payout using `amountToPay` |
| investment-service | Owner risk lookup and asset allocation using `amountToInvest` |
| portfolio-service | Position quantities and accumulated weighted purchase prices |
| reporting-service | Correlated reporting with memory or durable DynamoDB storage |
| market-data-service | Simulated price snapshots |
| market-shock-service | Explicit disaster shock coefficients |
| weather-service | OpenWeather mapping or scheduled AWS cache, with expiry checks |
| e2e-tests | Financial/HTTP/Kafka and durable DynamoDB integration tests |
| catfi-aws-core | Reporting/export ports, application services and AWS SDK adapters |
| aws-functions | Six standalone Java Lambda handlers for exports, notifications and weather |

Blockchain anchoring, real market integrations, policy budget locking and financial inbox/outbox remain planned. The AWS branch implements durable reporting, export outbox and optional notifications. The simulator currently applies observations to all registered properties; coordinates and intensity are retained but do not yet drive a geographic or physical model.

Security CI now layers Gitleaks secret detection, OWASP Dependency-Check for Java CVEs, Trivy repository/IaC scanning and Trivy scanning of the built backend container. High/critical findings are intended to block the security workflow; CodeQL and Sonar remain optional repository-level extensions. `SECURITY.md` explains what each control protects, how to run local checks and what remains outside the guarantee.

## Architecture and documentation

Services separate domain models, application logic, adapters and configuration. Constructor injection and targeted Lombok annotations reduce boilerplate; no runtime Lombok is required. Shared domain contracts remain independent of Spring, JPA, Kafka and HTTP. PostgreSQL handles financial records, MongoDB handles damage assessments; policies and users currently use memory; reporting has memory and DynamoDB adapters.

- [ARCHITECTURE.md](ARCHITECTURE.md): boundaries and current integration flow.
- [BUSINESS_RULES.md](BUSINESS_RULES.md): compensation, allocation and rounding.
- [SECURITY.md](SECURITY.md): actual protection and remaining work.
- [DECISIONS.md](DECISIONS.md): architectural decisions.
- [REVIEW.md](REVIEW.md): corrections and known gaps from this review.
- [VALIDATION.md](VALIDATION.md): executed checks and limitations.
- [CHANGELOG.md](CHANGELOG.md): change history.

This README is the living project overview. Update it in the same change whenever project behavior, services, setup or verification status changes. Keep history in CHANGELOG and decision rationale in DECISIONS. All project text remains English.

## Service responsibilities

Every module under `services/` has an English README describing its role, processing, communications and limitations. These pages are living documentation and must be updated alongside behavior changes. There is currently no separate security-service; security is implemented at application boundaries.

## AWS serverless implementation

[Setup and implementation (Russian)](docs/aws/AWS_IMPLEMENTATION_RU.md) explains the implemented DynamoDB reporting adapter, authenticated JSON exports, six Java Lambda handlers, SQS/Step Functions/S3, scheduled weather cache, optional email notifications and GitHub OIDC deployment. [Original learning roadmap](docs/aws/AWS_TRAINING_PLAN_RU.md) distinguishes future advanced labs from this implementation.

The standard local demo still uses memory reporting. AWS mode requires deployed resources and a local temporary-credentials profile or an application role. No AWS account credentials are stored in this archive. Infrastructure is in `infra/aws`; CI/deployment/security workflows are in `.github/workflows`. AWS deployment is manual after tests and account/repository configuration; cloud validation has not been executed here.
