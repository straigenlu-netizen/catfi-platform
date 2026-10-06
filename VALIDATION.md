# Validation

## 2026-10-06 — AWS implementation, current verification

Environment: Java 21.0.12.1, Maven 3.9.11, Node 24, Chromium 141, DynamoDB Local 1.25.1. Current evidence is also recorded in [docs/aws/VERIFICATION_RESULTS.json](docs/aws/VERIFICATION_RESULTS.json).

| Check | Result |
| --- | --- |
| Java unit/web/component tests, all 18 child modules | 114 passed, 0 failures/errors/skips |
| DamageKafkaIT | Passed, real embedded Kafka, mocked MongoDB |
| PipelineKafkaIT | Passed, real HTTP/Feign/Kafka, mocked persistence boundaries |
| DynamoReportingIT | Passed, actual DynamoDB Local API, restart/concurrency/transactional outbox/export checks |
| Live browser → BFF → Kafka → reporting | 1 Playwright scenario passed |
| React unit tests | 7 passed, including export/login/access-denied |
| Control-room browser contracts | 3 passed |
| AWS export browser contract | 1 passed, mocked Cognito/export API |
| TypeScript + production frontend build | Passed; existing large-chunk advisory remains |
| Standalone Lambda shaded artifact | Built successfully; handler classes packaged |
| Complete 19-project reactor packaging | BUILD SUCCESS; final packaging pass reused already-passed tests |
| SAM template + prepared release template | Both sam validate --lint passed |
| OIDC bootstrap CloudFormation | cfn-lint passed, no errors/warnings |
| GitHub workflow YAML + Python scripts | Parsed/compiled successfully |

The integration run selected DamageKafkaIT, PipelineKafkaIT and DynamoReportingIT. It did not run DatabaseMigrationIT, because this environment has no Docker. The first packaging attempt found an inherited Maven Shade transformer conflict; it was fixed with explicit transformer override and successful packaging. The existing browser launcher was fixed to resolve a default web-ui path when catfi.web-ui is omitted.

Not executed: PostgreSQL Testcontainers/Liquibase runtime, Compose/container runtime, real AWS cloud_e2e.py, actual Cognito JWT signature verification in AWS, SES/OpenWeather calls, GitHub-hosted workflows, Sonar/CodeQL scans. Cloud account and repository setup are required. Lambda unit tests and browser contracts do not prove real IAM, API Gateway, SQS, Streams, Step Functions or S3 deployment behavior.

DynamoDB Local checks actual SDK/storage operations but not real-cloud IAM, throughput, eventual GSI timing or Streams behavior. Reporting retains bounded correlation state; financial services still need separate inbox/outbox/budget/position locking before competing production writers.

Reproduce using [AWS implementation guide](docs/aws/AWS_IMPLEMENTATION_RU.md). Full integration profile requires Docker and CATFI_DYNAMODB_ENDPOINT for the DynamoDB test. Run cloud_e2e.py only against the configured learning stack.

## Earlier validation records

## 2026-10-05 — uploaded project review and control room

Environment: Linux, Java 21, Maven 3.9.11, Node.js 24, Chromium 141, Spring Boot 3.5.16 and Spring Cloud 2025.0.3. Uploaded CatFi.zip and CatFi(1).zip contained identical source/configuration files.

### Executed checks

- Full reactor `mvn clean verify` / subsequent `mvn verify`: all 17 projects succeeded; 99 Java unit/web/component tests, zero failures/errors/skips.
- Financial checks cover coverage, deductibles, remaining limits, HALF_EVEN ties, one-cent splits, allocation conservation, invalid inputs, payout/investment separation and position accumulation.
- Shared-event JSON round trips and the actual policy-to-compensation HTTP DTO contract pass.
- BFF tests cover aggregation before/after reports, validated trigger input and allowed/rejected CORS origins. Simulator HTTP tests require Kafka acknowledgement and reject invalid coordinates.
- `PipelineKafkaIT`: HTTP BFF trigger -> actual OpenFeign HTTP call -> simulator -> embedded Kafka -> sensor/damage/compensation/payment/investment/portfolio/reporting processors -> dashboard/details. Real Kafka, HTTP and application logic; persistence adapters and policy/user/reporting client boundaries use test fixtures/mocks.
- The same integration test launched Playwright against Vite connected to the running test BFF. Browser-triggered publication produced a fresh correlated report; the financial chart rendered. One live browser test passed.
- `DamageKafkaIT`: real embedded Kafka serialization/consumption/publication with mocked MongoDB repository. Passed after correcting immutable-contract deserialization.
- `npm test`: four React tests passed.
- `npm run build`: TypeScript check and production build passed. ECharts loads separately; the main bundle still produces Vite's advisory about a chunk exceeding 500 kB.
- `npm run test:e2e`: three deterministic browser tests passed, covering dashboard/detail navigation, chart canvas rendering, numeric form payloads and a 390 px mobile layout.
- Screenshots were inspected at desktop and mobile widths. `docs/ui-preview/live-property.png` comes from the real test BFF/Kafka flow; other preview images use deterministic API fixtures.
- `npm audit --omit=dev`: zero reported production dependency vulnerabilities at review time. This does not verify application security or development dependencies.
- POM, Compose YAML and migration XML parsing checked. Container builds/runtime remain unverified.

### Java test inventory

| Module | Passing tests |
| --- | ---: |
| catfi-bff | 7 |
| catfi-common | 7 |
| compensation-service | 20 |
| damage-service | 10 |
| disaster-simulator | 5 |
| e2e-tests | 2 |
| investment-service | 10 |
| market-data-service | 4 |
| market-shock-service | 8 |
| payment-service | 4 |
| portfolio-service | 4 |
| property-policy-service | 2 |
| reporting-service | 2 |
| sensor-simulator | 6 |
| user-service | 2 |
| weather-service | 6 |

### Reproduce

```powershell
.\mvnw.cmd clean verify
.\mvnw.cmd -pl services/e2e-tests -am -Pintegration -Dit.test=PipelineKafkaIT -Dfailsafe.failIfNoSpecifiedTests=false verify
.\mvnw.cmd -pl services/damage-service -am -Pintegration -Dit.test=DamageKafkaIT -Dfailsafe.failIfNoSpecifiedTests=false verify
```

For the live browser/Kafka check, install frontend dependencies and Chromium first, then run from the Maven root:

```powershell
$uiPath = Join-Path (Get-Location) 'web-ui'
.\mvnw.cmd -pl services/e2e-tests -am -Pintegration -Dit.test=PipelineKafkaIT -Dfailsafe.failIfNoSpecifiedTests=false -Dcatfi.browser.e2e=true "-Dcatfi.web-ui=$uiPath" verify
```

With Docker available, execute the PostgreSQL migration/persistence test and all integration tests:

```powershell
.\mvnw.cmd verify -Pintegration
```

The live test uses a test BFF, real HTTP/Kafka and mocked database boundaries. It does not substitute for starting every deployed service with PostgreSQL/MongoDB and validating recovery, duplicate delivery or concurrent policy claims.

### Not executed

- PostgreSQL Testcontainers runtime, Liquibase against PostgreSQL and actual JPA persistence.
- Compose service/container builds, Docker runtime or Windows execution.
- Real OpenWeather HTTP calls or JWT signature/issuer/audience checks against Keycloak.

These checks are not reported as passed. See REVIEW.md for remaining functional gaps.

## Historical validation records


## 2026-10-04 — Windows workspace

Environment: Windows, Maven Wrapper 3.9.11, Temurin JDK 21.0.12.1, Spring Boot 3.5.16.

- Inspected source code, configuration, migrations, tests, wrapper, and root documents. Maven root: C:\Users\yuliy\Downloads\Main\Private\myPetPro\CatFi\CatFi.
- No AGENTS.md, Git repository, or remote was found in the inspected workspace.
- Initial sandboxed verify could not write dependency files to the user Maven cache. The authorized retry reached Maven Enforcer and revealed JAVA_HOME pointed to JDK 17 although PATH selected JDK 21.
- Set JAVA_HOME to the installed JDK 21 for the build process; did not change permanent user/system settings.
- `.\mvnw.cmd -B -ntp clean verify`: BUILD SUCCESS after Lombok changes. 16 domain tests and 5 web/security tests passed; 21 total, zero failures/errors/skips. The migration integration test compiled but was not included in this command.
- The clean build compiled generated constructors and produced an executable Spring Boot JAR. Interactive IDE diagnostics were not verified.
- Docker CLI 28.5.1 is installed, but docker version could not connect to the Docker Desktop Linux engine named pipe.
- `.\mvnw.cmd -B -ntp verify -Pintegration`: BUILD FAILURE. The 21 unit/web tests passed again. DatabaseMigrationIT reported one initialization error, zero skips: Testcontainers could not obtain a usable Docker environment and therefore could not start PostgreSQL. No Liquibase migration was executed against PostgreSQL.
- Compose runtime, application/database startup, actual JWT signature/expiry/issuer/audience validation, and Keycloak interoperability remain unverified.

Web/security tests use mocked JWT decoding. Passing those tests verifies scope/HTTP behavior only, not cryptographic or identity-provider integration. Persistence, concurrency protection, inbox/outbox, and Kafka remain planned; ADR-008 is a design proposal.

After making Docker Desktop available, rerun `.\mvnw.cmd verify -Pintegration` and verify Compose/application runtime separately.

## 2026-10-03 — Historical scaffold validation

Reported generation environment: Linux, Maven 3.9.11, Temurin JDK 21.0.12.1. Maven verify passed and produced an executable JAR. 16 domain and 5 web/security tests passed: 21 total, zero failures/errors/skips. XML/YAML syntax was checked. The Testcontainers integration test compiled but was not run because Docker was unavailable.

PostgreSQL/Liquibase runtime, Compose, Keycloak, and Windows execution were not verified in that historical environment. These historical results do not substitute for current local validation.
