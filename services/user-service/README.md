# user-service

## Role in CatFi

Provides customer identities and investment risk profiles. A customer profile is separate from an employee login.

## How it works

GET /api/v1/clients/{id} returns a profile; POST registers a profile. Demo profile seeds clients matching the demonstration properties. The current store is in memory.

## Communication and purpose

BFF uses names and risk profiles for the UI. Investment service retrieves the property owner’s risk profile to choose allocation weights.

## Important details

Employee/agent authentication is enforced at the BFF boundary and is intentionally separate from these customer profiles. No external identity provider. Unknown clients currently receive the legacy default moderate profile; do not interpret this as an authenticated identity. Customer persistence and authorization require further hardening.

## Verification and operation

Build and run commands, service ports and environment variables are maintained in the [platform README](../../README.md). Test scope and remaining limitations are maintained in [VALIDATION.md](../../VALIDATION.md) and [REVIEW.md](../../REVIEW.md). Update this page when the service behavior or dependencies change.
