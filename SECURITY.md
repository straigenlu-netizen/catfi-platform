# Security

## Implemented baseline

The bearer-only JWT resource server is configured with issuer, JWKS, and audience `compensation-service`. Scope `compensation:preview` grants preview access. Other APIs in the compensation-service HTTP security chain are denied by default; this does not protect endpoints in separate services. Prometheus requires `metrics:read`; health is public without details. Compensation preview uses stateless bearer JWT authentication; the separate browser-facing BFF uses authenticated sessions, CSRF, agent/customer roles and customer ownership checks. Internal service APIs are not independently protected by the BFF session.

Host-mode compensation HTTP and published Compose ports bind to localhost. The template contains no password; .env is ignored, and Compose rejects an empty password. Application code does not explicitly persist or log tokens, personal data, or financial request bodies. Debug framework logging may expose DTO values; do not enable it with sensitive data. Domain errors return generic ProblemDetail without stack traces. Demonstrations use synthetic data and simulated money.

## Current tests and limitations

Anonymous preview requests receive 401; JWTs without scope receive 403; authorized preview receives 200; invalid amounts/limits receive 400. These web tests use a mocked JwtDecoder and test authentication. They do not verify real signatures, expiration, issuer, audience, or Keycloak interoperability. Configured JWT validation is not equivalent to integration-test coverage.

## Planned work

- Provision Keycloak and test actual signed tokens, expiry, issuer, and audience.
- Extend current customer ownership checks to every new object-level API, including any future claims and portfolio endpoints.
- Define CUSTOMER, INSURER, FINANCIAL_ANALYST, ADMIN, AUDITOR roles and least privilege; add MFA for sensitive actions.
- Extend existing audit, payment replay/outbox and portfolio decision receipts to every financial event boundary; make policy reservation durable and reconcile cross-service failures.
- Add deployment TLS, service authentication and potentially mTLS, separate migration/runtime database users, and Kafka TLS/authentication/ACLs.
- Define rate limits, request size limits, secrets management, and sensitive-log/trace redaction.
- Continue reviewing configured Gitleaks, Trivy, dependency scans and opt-in Sonar/CodeQL; add DAST/ZAP before public deployment. A home Checkmarx installation is not required.
- Restrict operational/admin dashboard metrics by role and minimize personal data in financial processing.

This baseline is not complete security coverage. Real customers, payments, investments, and production financial services are outside the current scope.

## Control room security status — 2026-10-05

The local BFF validates trigger requests and permits CORS only for local Vite origins. CORS does not block curl/server callers or implement authorization. Compose publishes only UI/BFF HTTP endpoints on loopback; internal service HTTP ports are unpublished. The BFF now has demo agent/customer session login, CSRF, object ownership checks for customer portal reads and PostgreSQL audit records. Production OIDC, identity lifecycle and internal service authentication are not implemented. Protected compensation preview remains a separate JWT/scope feature.

Weather integration returns sanitized provider errors and does not explicitly log exception messages that may contain API keys. API keys belong in environment variables. BFF failures return generic gateway errors. Production authentication, TLS, Kafka ACLs and durable audit remain planned.

## AWS branch security boundary

The new Export API uses API Gateway JWT validation and explicit Cognito-sub/property grants checked again at job read/download. Users cannot export arbitrary CatFi client records based on profile IDs. S3 is private, encrypted and HTTPS-only; signed URLs grant access to their bearer and must not be logged. Roles use temporary credentials; GitHub OIDC trust is restricted to one repository/environment, and environment deployment branches must be main. The bootstrap CloudFormation role is powerful provisioning authority for a dedicated learning stack and is not a production least-privilege policy.

Secrets Manager stores OpenWeather credentials. Schedule/SES notification activation is explicit through deployment parameters. Provider failures are sanitized. Unit tests never send email; cloud smoke uses a suppressed temporary Cognito account. Existing local BFF/internal reporting endpoints retain demo security and must not be exposed as production APIs. No real money movement is introduced.

GitHub security.yml offers opt-in Sonar quality gate and CodeQL; private CodeQL licensing and Sonar settings must be configured. Dependabot and npm audit complement these checks. Review permissions and dependency alerts before activating deployment. Recovery scripts are operator-only; runtime roles do not receive their administrative permissions.
