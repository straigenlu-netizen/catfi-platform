# portfolio-service

## Role in CatFi

Maintains simulated asset positions for each insured property.

## How it works

Consumes catfi.investment.allocated, converts invested amounts into quantities at fixed simulated prices and updates PostgreSQL positions with weighted average purchase price.

## Communication and purpose

Investment supplies allocations. PostgreSQL stores positions. Market-data service is a future price source; it is not currently called. Reporting builds its allocation snapshot from investment events, not from this database.

## Important details

No broker/exchange orders. Quantities and prices use eight decimal places. Read-modify-write currently needs additional database locking/idempotency before multiple competing writers can be safely enabled.

## Verification and operation

Build and run commands, service ports and environment variables are maintained in the [platform README](../../README.md). Test scope and remaining limitations are maintained in [VALIDATION.md](../../VALIDATION.md) and [REVIEW.md](../../REVIEW.md). Update this page when the service behavior or dependencies change.
