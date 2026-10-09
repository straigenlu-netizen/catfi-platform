# Financial ledger and recovery (local demo)

## Atomic boundary

The compensation service now owns budget consumption in its PostgreSQL database.
It serializes work by property using `pg_advisory_xact_lock`, sums committed
`compensation_results.amount`, calculates the remaining policy budget, and writes
both the compensation result and a JSONB outbox entry **in one transaction**.
The `damage_assessment_id` unique index is the durable input inbox. Replaying an
assessment returns its original result and does not consume budget again.
A conflicting replay fails closed. Separate compensation-service replicas share
the same PostgreSQL lock and ledger.

The property-policy service remains an **in-memory source of demo policy terms**;
its old `/reserve` endpoint now responds with HTTP 410 and cannot change balances.
The policy's `alreadyPaid` must be zero: nonzero legacy baselines are rejected
until reconciled by an operator. For the demo, the sum of committed compensation
results is the authoritative paid-to-date value. A policy change or replacement
is **not** versioned in the ledger; this requires a persistent policy registry
before production use. Existing historical compensation rows count toward the
budget automatically. Never delete them to reset a policy balance.

## Outbox publication and recovery

A scheduled publisher locks pending rows with `FOR UPDATE SKIP LOCKED`, sends
to Kafka, waits for broker acknowledgement, then marks the row published.
A crash after the Kafka acknowledgement but before the database commit can
republish; downstream consumers must deduplicate by stable IDs. This provides
**at-least-once delivery**, not exactly-once end-to-end payments. Publication
failures leave pending rows for automatic retry. Kafka input offsets are only
acknowledged after the DB transaction returns successfully.

Inspect outstanding messages:

```sql
SELECT id, property_id, created_at FROM compensation_outbox
WHERE published_at IS NULL ORDER BY created_at;
```

Inspect property spending:

```sql
SELECT property_id, SUM(amount) AS committed_compensation
FROM compensation_results GROUP BY property_id;
```

## Migration and deployment safety

1. Back up the compensation PostgreSQL database before deploying the migration.
2. Verify `compensation_results` has no duplicate `damage_assessment_id` values.
3. Verify policy `alreadyPaid` is zero in the demo and reconcile nonzero balances.
4. Deploy compensation-service with the `004-compensation-outbox.sql` Liquibase
   change; deploy the property-policy-service version that rejects legacy reserve.
5. Inspect the outbox query and Kafka consumers; restart the publisher if needed.
6. Historical results created before the outbox migration are **not** automatically
   replayed. Reconcile any downstream missing events using an operator-approved
   backfill, not an unconditional replay.

## Explicit remaining limits

- Policy terms are volatile and mutable; real insurance policy persistence,
  versioning, ownership and authenticated inter-service APIs remain required.
- A shared compensation database is the transaction boundary, not a distributed
  transaction across payment, investment, portfolio and reporting services.
- Payment-service and investment-service still require their own durable inbox/
  outbox guarantees and financial reconciliation before production.
- Backups, retention, schema upgrades and disaster recovery must be validated.
