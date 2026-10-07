# notification-service

Consumes important claim lifecycle events and sends customer email notifications.

## Stages

- `catfi.damage.assessed` -> claim entered processing
- `catfi.compensation.calculated` -> compensation calculated
- `catfi.payment.completed` -> payment completed
- `catfi.investment.allocated` -> investment allocation completed

The first notification starts at damage assessment because CatFi does not currently publish a separate claim-submitted event.

## Recipient and privacy

Kafka events contain property identifiers, not customer email addresses. The service resolves
`propertyId -> clientId -> ClientProfile` through internal policy and user services. Email
addresses and message bodies are not persisted in the notification ledger and are not written
to application logs.

Local mode uses a log sender that records only the recipient domain, subject and generated
message identifier. AWS mode uses SES.

## Delivery ledger

AWS mode stores compact delivery metadata in DynamoDB: notification id, lifecycle stage,
delivery status, timestamps and SES message id. The deterministic notification id provides
idempotency. Records expire after 90 days using DynamoDB TTL.

The ledger intentionally does not store subject/body or recipient email. This reduces cost,
PII exposure and duplicate delivery risk.

Delivery is asynchronous and at-least-once. DynamoDB suppresses completed duplicates; failed reservations are released so Kafka retries can attempt delivery again. SES acknowledgement followed by a ledger-write failure can still produce a duplicate on retry.
New consumers start at `latest` to avoid emailing historical events on first deployment.
