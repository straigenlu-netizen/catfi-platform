# disaster-simulator

## Role in CatFi

Starts a demonstration disaster, the first event of the main insurance pipeline.

## How it works

POST /api/v1/simulations/disaster accepts type, intensity and coordinates. GET /api/v1/simulations/random creates a random event. The HTTP response waits for Kafka acknowledgement; failures return a safe error. Scheduled simulation is opt-in.

## Communication and purpose

BFF forwards manual triggers. Kafka catfi.disasters delivers events to sensor simulator and market-shock service.

## Important details

No database or real disaster provider. Intensity and coordinates are event metadata: the current sensor model affects all registered properties without geographic filtering. GET /random changes state and should remain internal.

## Verification and operation

Build and run commands, service ports and environment variables are maintained in the [platform README](../../README.md). Test scope and remaining limitations are maintained in [VALIDATION.md](../../VALIDATION.md) and [REVIEW.md](../../REVIEW.md). Update this page when the service behavior or dependencies change.
