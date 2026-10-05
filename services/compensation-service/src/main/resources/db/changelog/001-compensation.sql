--liquibase formatted sql
--changeset catfi:001
CREATE TABLE compensation_result (
    id UUID PRIMARY KEY,
    claim_id UUID NOT NULL UNIQUE,
    customer_id UUID NOT NULL,
    amount NUMERIC(18,2) NOT NULL CHECK (amount >= 0),
    currency CHAR(3) NOT NULL CHECK (currency = 'EUR'),
    rule_version VARCHAR(100) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);
--rollback DROP TABLE compensation_result;
