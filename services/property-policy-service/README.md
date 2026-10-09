# property-policy-service

## Reservation retries

`POST /api/v1/policies/property/{id}/reserve` accepts an optional `reservationId` UUID in addition to `amount`. Repeating the same ID, amount, and property in the running demo process returns the current policy without charging the limit again. Conflicting reuse is rejected. The reservation ledger and policies currently live in memory and reset on restart; production needs transactional durable storage.

## Role in CatFi

Owns the property inventory and insurance-contract data used for trusted financial calculations.

## How it works

HTTP endpoints register/read properties and policies and locate an active policy by property. Demo profile seeds three synthetic properties and policies. Stores currently use ConcurrentHashMap.

## Communication and purpose

BFF reads inventory and policy details. Sensor simulator reads properties to generate observations. Compensation reads contract terms. Investment reads the property owner before requesting a risk profile.

## Important details

No external provider or durable database yet. Restarting loses non-demo records. Active means the active flag; policy-date enforcement, unique active contracts and aggregate budget reservation are not implemented. This service does not authenticate CRM agents.

## Verification and operation

Build and run commands, service ports and environment variables are maintained in the [platform README](../../README.md). Test scope and remaining limitations are maintained in [VALIDATION.md](../../VALIDATION.md) and [REVIEW.md](../../REVIEW.md). Update this page when the service behavior or dependencies change.
