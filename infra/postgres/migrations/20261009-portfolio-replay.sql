-- Run during a maintenance window BEFORE deploying the new portfolio-service.
-- Preflight: this query MUST return zero rows; manually reconcile any duplicates.
-- SELECT property_id, asset_symbol, count(*) FROM portfolio_positions
-- GROUP BY property_id, asset_symbol HAVING count(*) > 1;
-- Stop the old portfolio-service writers before migration.
BEGIN;
CREATE TABLE IF NOT EXISTS processed_investments (
    decision_id uuid PRIMARY KEY,
    payload_hash varchar(64) NOT NULL
);
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'uq_portfolio_property_symbol'
    ) THEN
        ALTER TABLE portfolio_positions
            ADD CONSTRAINT uq_portfolio_property_symbol UNIQUE (property_id, asset_symbol);
    END IF;
END $$;
COMMIT;
-- Historical positions cannot be retroactively attributed to decision IDs.
-- Replaying events older than the migration is unsafe until reconciled.
