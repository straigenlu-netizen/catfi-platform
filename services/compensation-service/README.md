# compensation-service

## Replay safety

A unique Liquibase index on `damage_assessment_id` prevents multiple persisted compensations for the same assessment. Redelivery re-emits the original compensation result ID without a second policy reservation. Policy reservation requests carry the assessment ID as an idempotency key; the policy-side reservation inbox is still in memory. A crash after budget reservation and before compensation persistence remains a cross-service consistency risk requiring a durable inbox/outbox and reconciliation.

## Role in CatFi

Calculates compensation from assessed damage and trusted policy terms, then splits compensation into payout and investment branches.

## How it works

Consumes catfi.damage.assessed, loads the active insurance contract, validates property identity and terms, calculates rounded amounts, persists results in PostgreSQL and publishes catfi.compensation.calculated keyed by property.

## Communication and purpose

Damage service supplies assessments. Property-policy service supplies trusted coverage, deductible and limits. Payment and investment services consume the calculated split. Reporting consumes the same event. OAuth2 issuer supplies JWT verification keys for the HTTP preview.

## Important details

POST /api/v1/compensations/preview requires compensation:preview scope. Health is public; metrics require metrics:read. Liquibase manages storage. Coverage is applied before deductible; HALF_EVEN rounding preserves cents. Outbox, duplicate suppression and concurrent policy-budget reservation remain future work.

## Verification and operation

Build and run commands, service ports and environment variables are maintained in the [platform README](../../README.md). Test scope and remaining limitations are maintained in [VALIDATION.md](../../VALIDATION.md) and [REVIEW.md](../../REVIEW.md). Update this page when the service behavior or dependencies change.
