# portfolio-service

## Role in CatFi

Maintains simulated asset positions for each insured property.

## How it works

Consumes catfi.investment.allocated, converts invested amounts into quantities at fixed simulated prices and updates PostgreSQL positions with weighted average purchase price.

## Communication and purpose

Investment supplies allocations. PostgreSQL stores positions. Market-data service is a future price source; it is not currently called. Reporting builds its allocation snapshot from investment events, not from this database.

## Important details

No broker/exchange orders. Quantities and prices use eight decimal places. Each investment decision must include a non-null decisionId, compensationResultId, propertyId, totalInvested and nonempty allocation. PostgreSQL records a SHA-256 payload receipt keyed by decisionId in the same transaction as position updates; duplicate identical events do nothing, conflicting replays fail. A transaction-scoped PostgreSQL advisory lock serializes writes per property across replicas. A unique property/symbol index prevents duplicate positions.

Before deploying to an existing DB, reconcile duplicate positions and apply `infra/postgres/migrations/20261009-portfolio-replay.sql` during a maintenance window. Historical events have no receipts and must not be blindly replayed. Run `./mvnw -pl services/portfolio-service -am verify -Pintegration` with Docker for PostgreSQL concurrency and Kafka E2E tests. `ddl-auto=update` remains a demo convenience, not a production migration policy.

## Verification and operation

Build and run commands, service ports and environment variables are maintained in the [platform README](../../README.md). Test scope and remaining limitations are maintained in [VALIDATION.md](../../VALIDATION.md) and [REVIEW.md](../../REVIEW.md). Update this page when the service behavior or dependencies change.
