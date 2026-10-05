# damage-service

## Role in CatFi

Converts simulated sensor observations into monetary damage assessments.

## How it works

Consumes catfi.sensors, validates finite nonnegative observations, applies the configured sensor coefficients, rounds damage to cents, stores assessments in MongoDB and publishes catfi.damage.assessed.

## Communication and purpose

Sensor simulator supplies observations. Compensation and reporting consume assessments. MongoDB stores assessment history.

## Important details

Property and disaster identifiers are propagated. The damage formula is educational, not an actuarial model. Kafka integration tests verify the real serialization and consume/produce boundary with mocked persistence.

## Verification and operation

Build and run commands, service ports and environment variables are maintained in the [platform README](../../README.md). Test scope and remaining limitations are maintained in [VALIDATION.md](../../VALIDATION.md) and [REVIEW.md](../../REVIEW.md). Update this page when the service behavior or dependencies change.
