--liquibase formatted sql
--changeset catfi:002
CREATE TABLE compensation_results (
    id UUID PRIMARY KEY,
    property_id UUID NOT NULL,
    damage_assessment_id UUID NOT NULL,
    amount NUMERIC(18,2) NOT NULL CHECK (amount >= 0),
    damage_amount NUMERIC(18,2) NOT NULL CHECK (damage_amount >= 0),
    coverage_rate NUMERIC(7,6) NOT NULL CHECK (coverage_rate BETWEEN 0 AND 1),
    deductible NUMERIC(18,2) NOT NULL CHECK (deductible >= 0),
    calculated_at TIMESTAMPTZ NOT NULL
);
--rollback DROP TABLE compensation_results;
