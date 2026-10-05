# Security

## Implemented baseline

The bearer-only JWT resource server is configured with issuer, JWKS, and audience `compensation-service`. Scope `compensation:preview` grants preview access. Other APIs in the compensation-service HTTP security chain are denied by default; this does not protect endpoints in separate services. Prometheus requires `metrics:read`; health is public without details. Sessions are stateless; CSRF is disabled for the current bearer-only model and must be reassessed if cookies/BFF are introduced.

Host-mode compensation HTTP and published Compose ports bind to localhost. The template contains no password; .env is ignored, and Compose rejects an empty password. Application code does not explicitly persist or log tokens, personal data, or financial request bodies. Debug framework logging may expose DTO values; do not enable it with sensitive data. Domain errors return generic ProblemDetail without stack traces. Demonstrations use synthetic data and simulated money.

## Current tests and limitations

Anonymous preview requests receive 401; JWTs without scope receive 403; authorized preview receives 200; invalid amounts/limits receive 400. These web tests use a mocked JwtDecoder and test authentication. They do not verify real signatures, expiration, issuer, audience, or Keycloak interoperability. Configured JWT validation is not equivalent to integration-test coverage.

## Planned work

- Provision Keycloak and test actual signed tokens, expiry, issuer, and audience.
- Enforce object ownership for customer claims, policies, and portfolios.
- Define CUSTOMER, INSURER, FINANCIAL_ANALYST, ADMIN, AUDITOR roles and least privilege; add MFA for sensitive actions.
- Implement audit, inbox/outbox, replay protection, and duplicate-payment prevention.
- Add deployment TLS, service authentication and potentially mTLS, separate migration/runtime database users, and Kafka TLS/authentication/ACLs.
- Define rate limits, request size limits, secrets management, and sensitive-log/trace redaction.
- Add dependency scans, Trivy, ZAP, and suitable static checks before external deployment; SonarQube is planned. A home Checkmarx installation is not required.
- Restrict operational/admin dashboard metrics by role and minimize personal data in financial processing.

This baseline is not complete security coverage. Real customers, payments, investments, and production financial services are outside the current scope.

## Control room security status — 2026-10-05

The local BFF validates trigger requests and permits CORS only for local Vite origins. CORS does not block curl/server callers or implement authorization. Compose publishes only UI/BFF HTTP endpoints on loopback; internal service HTTP ports are unpublished. The control room has no OIDC login or ownership enforcement yet. Protected compensation preview remains a separate JWT/scope feature.

Weather integration returns sanitized provider errors and does not explicitly log exception messages that may contain API keys. API keys belong in environment variables. BFF failures return generic gateway errors. Production authentication, TLS, Kafka ACLs and durable audit remain planned.
