# investment-service

## Role in CatFi

Allocates the investment portion of compensation according to the customer’s risk profile.

## How it works

Consumes catfi.compensation.calculated, skips zero investment, loads the property owner and client profile, computes exact-cent allocations and publishes catfi.investment.allocated.

## Communication and purpose

Property-policy maps property to owner; user-service supplies risk profile. Portfolio and reporting consume the allocation decision.

## Important details

No exchange execution. Conservative, moderate and aggressive allocations use different fixed weights. Rounding remainder goes to GOLD so allocations sum exactly to the investment amount. Customer payout funds are never used as the investment source.

## Verification and operation

Build and run commands, service ports and environment variables are maintained in the [platform README](../../README.md). Test scope and remaining limitations are maintained in [VALIDATION.md](../../VALIDATION.md) and [REVIEW.md](../../REVIEW.md). Update this page when the service behavior or dependencies change.
