# CatFi architecture

## Current implementation

The React control room calls the BFF. OpenFeign adapters aggregate registered properties, owners, active policies and reports. Manual simulation requests are validated and proxied to the disaster simulator, which waits for Kafka publication acknowledgement. Domain calculations remain separate from HTTP, Kafka and database adapters.

Disaster events produce sensor observations for registered properties. Observations preserve the disaster ID. Damage assessments feed policy-backed compensation calculation. Compensation fans out into payout and investment paths. Investment decisions update portfolio positions; reporting correlates current assessment, compensation, payment and investment IDs. See PROJECT_DIAGRAM.md.

The Maven reactor includes 20 child modules (21 projects including the root POM). Fifteen are Spring Boot services; catfi-common provides immutable contracts and monetary terms; e2e-tests provides component and Kafka/HTTP integration checks. Ordinary jars remain reusable in tests; executable jars use the exec classifier. Shared contracts contain no Spring/JPA/Kafka/HTTP annotations. Lombok is compile-only; JDK constructor metadata supports JSON deserialization.

## Layers and data ownership

- Domain: immutable models, value validation and financial calculations.
- Application: use cases and allocation/calculation orchestration.
- Adapters: REST, OpenFeign, Kafka listeners/producers and repositories.
- Configuration: dependency wiring, security, client timeouts and demo fixtures.

PostgreSQL stores compensation, payment and portfolio records. The compensation entity has a Liquibase migration; payment and portfolio currently use Hibernate schema update. MongoDB stores damage assessments. Users and policies remain in memory. Reporting snapshots use memory by default with an optional DynamoDB adapter. Services communicate through APIs/events and do not query another service's tables. Shared transport contracts are not persistence entities.

## Reliability boundary

The pipeline remains an educational demo. Compensation persists a unique assessment ID and policy reservation uses an in-memory idempotency ledger; payment persists a unique compensation ID with pending-publication retry. Portfolio writes use a PostgreSQL receipt and property-scoped transactional advisory lock to prevent replay and concurrent lost updates. Cross-service compensation/policy reservation is still non-atomic, and not all event producers have durable outboxes. ADR-008 is partially implemented. Reporting persists correlation and receipts atomically in DynamoDB in AWS mode; the default memory demo is volatile. Bounded aggregate retention fails closed rather than discarding history.

PipelineKafkaIT exercises HTTP/Kafka with mocked persistence boundaries. PortfolioConcurrencyIT and PortfolioKafkaIT additionally exercise real PostgreSQL transactions, concurrent writers and embedded Kafka (Docker required). Their presence does not mean they were executed successfully in this review environment.

## Implemented and planned technology

Implemented: Java 21, Spring Boot, targeted Lombok, Kafka adapters, PostgreSQL/JPA, compensation Liquibase migration, MongoDB adapter, Spring Cloud OpenFeign, React/TypeScript/Vite/MUI/TanStack Query/Axios/ECharts, JUnit/Mockito/AssertJ, React Testing Library and Playwright. JWT/scope protection currently covers compensation preview only.

Implemented additionally: BFF agent/customer session authentication, CSRF, customer ownership checks, audit records, optional BFF Prometheus/Grafana, Gatling test scenarios, notification service and optional Redis enrichment. Planned: production identity lifecycle/OIDC, authenticated internal service calls, real market sources, geographic/intensity models, tracing across services, Besu audit hashes, Kubernetes/Helm/Terraform and a full ECS/RDS/MSK deployment. The serverless AWS branch and GitHub Actions are implemented; account deployment remains unverified. A blockchain hash would establish an anchor, not prove that a financial calculation was correct.

## AWS boundary

catfi-aws-core defines storage/access ports and export application services. Reporting uses memory or DynamoDB adapters; aggregate version conditions protect independent JVM writers. aws-functions contains standalone Lambda transport handlers and a shaded Java artifact, without Spring startup.

ExportApiHandler requires Cognito JWT and a property grant, freezes a completed report into a job/outbox transaction, then DynamoDB Streams dispatches SQS. ReportExporterHandler starts one stable-named Standard workflow; ExportWorkflowHandler generates the S3 file and completes the job. Retried generation is deterministic. Streams also feed optional SES report notification. WeatherUpdater retrieves a secret, refreshes expiring cache; weather-service reads it in AWS mode. SAM owns roles/resources; GitHub OIDC obtains temporary deploy credentials.

See [AWS infrastructure](infra/aws/README.md) for the deployable stack and [validation](VALIDATION.md) for verified limits. The financial Kafka boundaries remain distinct from AWS export infrastructure.
