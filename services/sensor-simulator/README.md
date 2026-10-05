# sensor-simulator

## Role in CatFi

Produces property-level observations when a disaster occurs.

## How it works

Consumes catfi.disasters, retrieves registered properties, creates observations appropriate to the disaster type and publishes catfi.sensors with property keys and the original disaster identifier.

## Communication and purpose

Property-policy service supplies the property inventory. Disaster simulator supplies disasters. Damage service consumes observations.

## Important details

No physical device or external sensor integration. All registered properties participate; coordinates do not yet filter the affected area. One disaster can produce multiple readings per property.

## Verification and operation

Build and run commands, service ports and environment variables are maintained in the [platform README](../../README.md). Test scope and remaining limitations are maintained in [VALIDATION.md](../../VALIDATION.md) and [REVIEW.md](../../REVIEW.md). Update this page when the service behavior or dependencies change.
