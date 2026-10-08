# catfi-bff

## Role in CatFi

The browser-facing entry point for the Control Room. It joins property, insurance, client and reporting data into UI DTOs.


## Customer and agent portals

`/api/customer/profile` (GET/PUT), `/api/customer/policies` (GET),
`/api/customer/policies/{id}` (GET), `/api/customer/payments` (GET) are restricted to
`ROLE_CUSTOMER`. Client IDs are resolved by querying `customer_accounts` by the
Spring Security session principal, not from query parameters or submitted JSON.
The policy detail endpoint rejects unowned and nonexistent property IDs equally with 404.
Agent-only `/api/agent/clients`, `/api/agent/clients/{id}`, `/api/agent/properties`,
`/api/agent/properties/{id}` and `/api/agent/payments` display the insured portfolio.
`/api/auth/me` exposes the authenticated role, not the stored password.

With Spring demo profile only, synthetic agent/customer logins are created via an
`ApplicationRunner`. `customer_accounts` persists the mapping in PostgreSQL. Never
use these default passwords for publicly accessible deployments. Internal service APIs
must remain on a private network because their authorization boundary is the BFF.

## How it works

GET /api/ui/dashboard and GET /api/ui/properties/{id} use OpenFeign clients. POST /api/ui/trigger-disaster validates the request and forwards it to the simulator.

## Communication and purpose

Property-policy service supplies registered properties and active contracts. User service supplies owners and risk profiles. Reporting service supplies the latest financial state. Disaster simulator starts the asynchronous pipeline.

## Important details

PostgreSQL stores agent/customer accounts and audit records. Feign calls have connect/read timeouts. Missing reports mean SAFE; missing active policies are explicit null values. CORS is a browser rule, not authentication. Gateway failures are sanitized.

## Verification and operation

Build and run commands, service ports and environment variables are maintained in the [platform README](../../README.md). Test scope and remaining limitations are maintained in [VALIDATION.md](../../VALIDATION.md) and [REVIEW.md](../../REVIEW.md). Update this page when the service behavior or dependencies change.

## AWS reporting compatibility

The existing report endpoints work with reporting-service's memory or DynamoDB adapter. AWS export requests go directly from the optional UI panel to the separate JWT-protected Export API; the BFF does not store Cognito credentials or proxy signed download URLs. The BFF itself remains a local demo entry point. See [AWS setup](../../docs/aws/AWS_IMPLEMENTATION_RU.md).
