# catfi-bff

## Role in CatFi

The browser-facing entry point for the Control Room. It joins property, insurance, client and reporting data into UI DTOs.

## How it works

GET /api/ui/dashboard and GET /api/ui/properties/{id} use OpenFeign clients. POST /api/ui/trigger-disaster validates the request and forwards it to the simulator.

## Communication and purpose

Property-policy service supplies registered properties and active contracts. User service supplies owners and risk profiles. Reporting service supplies the latest financial state. Disaster simulator starts the asynchronous pipeline.

## Important details

No database or external provider. Feign calls have connect/read timeouts. Missing reports mean SAFE; missing active policies are explicit null values. CORS is a browser rule, not authentication. Gateway failures are sanitized.

## Verification and operation

Build and run commands, service ports and environment variables are maintained in the [platform README](../../README.md). Test scope and remaining limitations are maintained in [VALIDATION.md](../../VALIDATION.md) and [REVIEW.md](../../REVIEW.md). Update this page when the service behavior or dependencies change.
