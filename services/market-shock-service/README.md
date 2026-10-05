# market-shock-service

## Role in CatFi

Demonstrates how disasters might influence asset prices independently of the insurance settlement pipeline.

## How it works

Consumes catfi.disasters, applies type-specific shock multipliers to four fixed asset prices and publishes catfi.market.shocked keyed by symbol.

## Communication and purpose

Disaster simulator supplies events. Kafka receives shocked prices. Market-data service is a planned base-price source but is not currently called.

## Important details

No production exchange or current consumer that changes portfolio valuation. Fixed prices and multipliers are an illustrative model. This optional branch does not delay final insurance reporting.

## Verification and operation

Build and run commands, service ports and environment variables are maintained in the [platform README](../../README.md). Test scope and remaining limitations are maintained in [VALIDATION.md](../../VALIDATION.md) and [REVIEW.md](../../REVIEW.md). Update this page when the service behavior or dependencies change.
