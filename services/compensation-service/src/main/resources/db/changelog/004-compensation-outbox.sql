--liquibase formatted sql
--changeset catfi:004
CREATE TABLE compensation_outbox (
    id UUID PRIMARY KEY REFERENCES compensation_results(id),
    property_id UUID NOT NULL,
    payload JSONB NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    published_at TIMESTAMPTZ
);
CREATE INDEX ix_compensation_outbox_pending ON compensation_outbox(created_at, id)
  WHERE published_at IS NULL;
-- Historical compensation rows were already published by the old implementation;
-- do not blindly re-emit them. Reconciliation is documented separately.
--rollback DROP TABLE compensation_outbox;
