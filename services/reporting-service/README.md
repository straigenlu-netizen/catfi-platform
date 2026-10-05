# reporting-service

## Role in CatFi

Builds the UI read model showing damage, compensation, payout and investment state per property.

## How it works

Consumes catfi.damage.assessed, catfi.compensation.calculated, catfi.payment.completed and catfi.investment.allocated. Correlates assessment and compensation identifiers, tolerates out-of-order arrivals and publishes immutable in-memory snapshots through HTTP.

## Communication and purpose

BFF reads GET /api/v1/reports/all and GET /api/v1/reports/property/{id}. Damage, compensation, payment and investment supply lifecycle events.

## Important details

IMPACTED means required branches are incomplete; REPORTED means matching required branches completed. Missing report returns 404. Handlers are synchronized and snapshots are copied. State and correlation history are not durable and do not yet have bounded retention.

## Verification and operation

Build and run commands, service ports and environment variables are maintained in the [platform README](../../README.md). Test scope and remaining limitations are maintained in [VALIDATION.md](../../VALIDATION.md) and [REVIEW.md](../../REVIEW.md). Update this page when the service behavior or dependencies change.
