# market-data-service

## Role in CatFi

Provides optional simulated market prices for investment demonstrations.

## How it works

GET /api/v1/market/price/{symbol} returns a simulated snapshot. A scheduler broadcasts four asset prices every 30 seconds to catfi.market.prices.

## Communication and purpose

Kafka receives price snapshots. Future portfolio/market-shock integrations may consume these prices; the current pipeline uses fixed prices and has no such live connection.

## Important details

No real market provider. Prices fluctuate around fixed bases and are educational. Unknown symbols use a legacy default base price. This service does not execute trades.

## Verification and operation

Build and run commands, service ports and environment variables are maintained in the [platform README](../../README.md). Test scope and remaining limitations are maintained in [VALIDATION.md](../../VALIDATION.md) and [REVIEW.md](../../REVIEW.md). Update this page when the service behavior or dependencies change.
