-- =============================================================================
-- Bill&Pay Tenant Schema - Change pg_connection_id from UUID to BIGINT
-- =============================================================================
-- Description: Changes pg_connection_id columns from UUID to BIGINT
-- Affected tables:
--   - merchant_pg_mappings
--   - transactions
--   - transaction_events
--   - webhook_logs
-- =============================================================================

-- =============================================================================
-- 1. merchant_pg_mappings table
-- =============================================================================
-- Drop dependent constraints first
ALTER TABLE merchant_pg_mappings 
  DROP CONSTRAINT IF EXISTS merchant_pg_mappings_merchant_pg_unique,
  DROP CONSTRAINT IF EXISTS merchant_pg_mappings_mid_pg_unique,
  DROP CONSTRAINT IF EXISTS merchant_pg_mappings_korpay_catid;

-- Drop indexes
DROP INDEX IF EXISTS idx_merchant_pg_mappings_pg_connection_id;

ALTER TABLE merchant_pg_mappings 
  ALTER COLUMN pg_connection_id TYPE BIGINT USING 1;

-- Recreate constraints
ALTER TABLE merchant_pg_mappings
  ADD CONSTRAINT merchant_pg_mappings_merchant_pg_unique UNIQUE (merchant_id, pg_connection_id),
  ADD CONSTRAINT merchant_pg_mappings_mid_pg_unique UNIQUE (mid, pg_connection_id);

-- Recreate index
CREATE INDEX idx_merchant_pg_mappings_pg_connection_id ON merchant_pg_mappings(pg_connection_id);

ALTER TABLE merchant_pg_mappings
  ADD CONSTRAINT merchant_pg_mappings_korpay_catid CHECK (
    (pg_connection_id IN (SELECT id FROM public.pg_connections WHERE pg_code = 'KORPAY') AND cat_id IS NOT NULL)
    OR
    pg_connection_id NOT IN (SELECT id FROM public.pg_connections WHERE pg_code = 'KORPAY')
  );

-- =============================================================================
-- 2. transactions table
-- =============================================================================
DROP INDEX IF EXISTS idx_transactions_pg_connection_id;

ALTER TABLE transactions 
  ALTER COLUMN pg_connection_id TYPE BIGINT USING 1;

CREATE INDEX idx_transactions_pg_connection_id ON transactions(pg_connection_id);

-- =============================================================================
-- 3. transaction_events table
-- =============================================================================
DROP INDEX IF EXISTS idx_transaction_events_pg_connection_id;

ALTER TABLE transaction_events 
  ALTER COLUMN pg_connection_id TYPE BIGINT USING 1;

CREATE INDEX idx_transaction_events_pg_connection_id ON transaction_events(pg_connection_id, created_at DESC);

-- =============================================================================
-- 4. webhook_logs table
-- =============================================================================
DROP INDEX IF EXISTS idx_webhook_logs_pg_connection_id;

ALTER TABLE webhook_logs 
  ALTER COLUMN pg_connection_id TYPE BIGINT USING 1;

CREATE INDEX idx_webhook_logs_pg_connection_id ON webhook_logs(pg_connection_id, received_at DESC);

-- =============================================================================
-- Add comments
-- =============================================================================
COMMENT ON COLUMN merchant_pg_mappings.pg_connection_id IS 'References public.pg_connections(id) - BIGINT foreign key';
COMMENT ON COLUMN transactions.pg_connection_id IS 'References public.pg_connections(id) - BIGINT foreign key';
COMMENT ON COLUMN transaction_events.pg_connection_id IS 'References public.pg_connections(id) - BIGINT foreign key';
COMMENT ON COLUMN webhook_logs.pg_connection_id IS 'References public.pg_connections(id) - BIGINT foreign key';
