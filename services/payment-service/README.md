# payment-service

## Role in CatFi

Implements the cash-payout branch of a calculated compensation.

## How it works

Consumes catfi.compensation.calculated, skips zero payout, constructs/persists a simulated payment in PostgreSQL and publishes catfi.payment.completed.

The internal `GET /api/v1/payments/property/{id}` endpoint on port 8082 returns
persisted simulated payouts, ordered newest first. The BFF checks the session's
ownership before requesting customer histories; never publish this internal endpoint.

## Communication and purpose

Compensation supplies amountToPay. Reporting consumes payment completion to update the report. PostgreSQL stores the payment record.

## Important details

No bank, payment provider or real money transfer. Investment is a parallel branch, not a consumer of customer payouts. Durable idempotency and outbox handling are still required before real financial use.

## Verification and operation

Build and run commands, service ports and environment variables are maintained in the [platform README](../../README.md). Test scope and remaining limitations are maintained in [VALIDATION.md](../../VALIDATION.md) and [REVIEW.md](../../REVIEW.md). Update this page when the service behavior or dependencies change.
