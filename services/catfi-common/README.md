# catfi-common

## Role in CatFi

A shared Java contract library, not a running microservice. It defines the event and financial types used across the platform.

## How it works

Immutable domain objects and enums are compiled into a plain JAR. Lombok generates focused boilerplate; constructor metadata permits JSON deserialization.

## Communication and purpose

All Java services depend on this module at build time. Shared contracts allow Kafka producers and consumers to exchange compatible events.

## Important details

No HTTP, Kafka connection or persistence. Changes to event contracts must be reviewed with all consumers. Domain classes avoid infrastructure annotations.

## Verification and operation

Build and run commands, service ports and environment variables are maintained in the [platform README](../../README.md). Test scope and remaining limitations are maintained in [VALIDATION.md](../../VALIDATION.md) and [REVIEW.md](../../REVIEW.md). Update this page when the service behavior or dependencies change.
