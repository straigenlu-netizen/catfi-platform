# reporting-service

Builds the UI read model for damage, compensation, payout and investment per property. Kafka consumers correlate assessment and compensation IDs, including out-of-order arrivals and zero-amount branches. BFF reads the HTTP projection.

## Persistence and concurrency

The application depends on ReportStore. Default memory storage preserves the small local demo. `aws` profile selects DynamoReportStore using `CATFI_REPORTING_TABLE` and AWS_REGION/default credentials chain. The entire property correlation state, duplicate receipts and latest projection are persisted with an optimistic version condition. Independent JVMs retry conflicting writes; persistence succeeds before Kafka record offset commit.

Damage assessment timestamps plus assessment IDs determine latest process; legacy fixtures without timestamps use arrival order. Repeated identical events do not update the projection. Conflicting payloads for the same logical event/process are rejected. A process from an older timestamp does not overwrite the newest report.

Bounded educational aggregate: maximum 512 receipts and 350 KB serialized state/report. Overflow fails closed and requires archival/migration. Kafka poison errors retain the failing partition for recovery, rather than silently acknowledging it. This is not financial settlement idempotency for other services.

## HTTP and AWS communication

- GET /api/v1/reports/property/{id}: latest public report or 404.
- GET /api/v1/reports/all: compatibility endpoint used by current BFF, internally pages storage.
- GET /api/v1/reports/page?limit=50&cursor=...: paginated public reports; no private correlation state.
- GET /api/v1/reports/property/{id}/history: reconstructed retained process history.

DynamoDB reports-index powers the small dashboard; GSI results are eventually consistent. Point reads use strongly consistent reads. ReportingTable Streams feed optional report notifications; the separate authenticated Export API freezes the report into a job. The local reporting HTTP API remains an internal demo boundary and must not be exposed publicly without authorization.

Tests cover restart, duplicate/conflicting events, all event permutations, concurrent writers and old process ordering. DynamoReportingIT exercises the real DynamoDB API. See [AWS setup](../../docs/aws/AWS_IMPLEMENTATION_RU.md) and [validation](../../VALIDATION.md).
