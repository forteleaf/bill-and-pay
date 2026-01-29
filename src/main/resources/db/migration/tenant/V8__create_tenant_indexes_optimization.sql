-- =============================================================================
-- Bill&Pay Tenant Schema - Index Optimization
-- =============================================================================
-- Description: Additional performance indexes for common query patterns
-- - Covering indexes for frequently accessed columns
-- - Partial indexes for filtered queries
-- - Expression indexes for computed values
-- =============================================================================

-- =============================================================================
-- Organizations - Additional Indexes
-- =============================================================================
-- Covering index for organization list queries with status filter
CREATE INDEX idx_organizations_list_active ON organizations(org_type, created_at DESC) 
  INCLUDE (org_code, name, level) 
  WHERE status = 'ACTIVE';

-- Expression index for path depth calculation
CREATE INDEX idx_organizations_path_depth ON organizations(nlevel(path), org_type);

COMMENT ON INDEX idx_organizations_list_active IS 'Covering index for active organization list queries';
COMMENT ON INDEX idx_organizations_path_depth IS 'Index on path depth for hierarchy depth queries';

-- =============================================================================
-- Merchants - Additional Indexes
-- =============================================================================
-- Covering index for merchant list with active filter
CREATE INDEX idx_merchants_list_active ON merchants(org_id, created_at DESC) 
  INCLUDE (merchant_code, name, business_type) 
  WHERE status = 'ACTIVE';

COMMENT ON INDEX idx_merchants_list_active IS 'Covering index for active merchant list queries';

-- =============================================================================
-- Transactions - Additional Indexes
-- =============================================================================
-- Partial index for failed transactions (for monitoring)
CREATE INDEX idx_transactions_failed ON transactions(created_at DESC) 
  WHERE status = 'FAILED';

-- Partial index for cancelled transactions
CREATE INDEX idx_transactions_cancelled ON transactions(created_at DESC) 
  WHERE status IN ('CANCELLED', 'PARTIAL_CANCELLED');

-- Covering index for transaction amount reporting
CREATE INDEX idx_transactions_amount_report ON transactions(created_at, status) 
  INCLUDE (amount, currency, merchant_id) 
  WHERE status IN ('APPROVED', 'PARTIAL_CANCELLED');

-- Index for approval number lookups (unique constraint alternative)
CREATE UNIQUE INDEX idx_transactions_approval_number_unique ON transactions(approval_number) 
  WHERE approval_number IS NOT NULL AND status != 'FAILED';

COMMENT ON INDEX idx_transactions_failed IS 'Partial index for monitoring failed transactions';
COMMENT ON INDEX idx_transactions_cancelled IS 'Partial index for cancelled transaction analysis';
COMMENT ON INDEX idx_transactions_amount_report IS 'Covering index for amount reporting queries';
COMMENT ON INDEX idx_transactions_approval_number_unique IS 'Ensure approval number uniqueness for successful transactions';

-- =============================================================================
-- Transaction Events - Additional Indexes
-- =============================================================================
-- NOTE: These are templates - must be created on each partition

-- Partial index for settlement processing (approved events only)
-- CREATE INDEX idx_transaction_events_settlement_pending ON transaction_events(created_at, merchant_id) 
--   WHERE event_type = 'APPROVAL' AND new_status = 'APPROVED';

-- Covering index for event summary queries
-- CREATE INDEX idx_transaction_events_summary ON transaction_events(merchant_id, event_type, created_at) 
--   INCLUDE (amount, currency);

COMMENT ON TABLE transaction_events IS 'NOTE: Additional indexes must be created on each partition for optimal performance';

-- =============================================================================
-- Settlements - Additional Indexes
-- =============================================================================
-- Partial index for pending settlements
CREATE INDEX idx_settlements_pending ON settlements(entity_id, created_at DESC) 
  WHERE status = 'PENDING';

-- Partial index for completed settlements with settlement date
CREATE INDEX idx_settlements_completed ON settlements(entity_id, settled_at DESC) 
  WHERE status = 'COMPLETED' AND settled_at IS NOT NULL;

-- Covering index for settlement amount aggregation
CREATE INDEX idx_settlements_amount_agg ON settlements(entity_id, entry_type, created_at) 
  INCLUDE (amount, fee_amount, net_amount, currency) 
  WHERE status = 'COMPLETED';

-- Index for Zero-Sum validation queries
CREATE INDEX idx_settlements_zero_sum_check ON settlements(transaction_event_id, entry_type) 
  INCLUDE (amount);

COMMENT ON INDEX idx_settlements_pending IS 'Partial index for pending settlement processing';
COMMENT ON INDEX idx_settlements_completed IS 'Partial index for completed settlement queries';
COMMENT ON INDEX idx_settlements_amount_agg IS 'Covering index for settlement amount aggregation';
COMMENT ON INDEX idx_settlements_zero_sum_check IS 'Index for Zero-Sum validation queries';

-- =============================================================================
-- Fee Configurations - Additional Indexes
-- =============================================================================
-- Covering index for active fee lookups
CREATE INDEX idx_fee_configurations_active_lookup ON fee_configurations(
  entity_id, 
  status,
  valid_from,
  valid_until
) INCLUDE (fee_type, fee_rate, fixed_fee, priority)
  WHERE status = 'ACTIVE';

COMMENT ON INDEX idx_fee_configurations_active_lookup IS 'Covering index for active fee configuration lookups';

-- =============================================================================
-- Webhook Logs - Additional Indexes
-- =============================================================================
-- Partial index for failed webhooks (for retry processing)
CREATE INDEX idx_webhook_logs_failed ON webhook_logs(received_at DESC) 
  WHERE status = 'FAILED' AND retry_count < 3;

-- Partial index for unprocessed webhooks
CREATE INDEX idx_webhook_logs_unprocessed ON webhook_logs(received_at ASC) 
  WHERE status IN ('RECEIVED', 'PROCESSING');

COMMENT ON INDEX idx_webhook_logs_failed IS 'Partial index for failed webhook retry processing';
COMMENT ON INDEX idx_webhook_logs_unprocessed IS 'Partial index for unprocessed webhook queue';

-- =============================================================================
-- API Keys - Additional Indexes
-- =============================================================================
-- Covering index for active API key lookups
CREATE INDEX idx_api_keys_active_lookup ON api_keys(key_hash) 
  INCLUDE (org_id, org_path, scopes, rate_limit_per_minute, rate_limit_per_hour) 
  WHERE status = 'ACTIVE' AND (expires_at IS NULL OR expires_at > CURRENT_TIMESTAMP);

COMMENT ON INDEX idx_api_keys_active_lookup IS 'Covering index for active API key authentication';

-- =============================================================================
-- Performance Tuning Notes
-- =============================================================================
-- 1. GiST indexes on ltree columns are crucial for hierarchical queries
-- 2. Partial indexes with WHERE clauses reduce index size and improve write performance
-- 3. INCLUDE columns in indexes create covering indexes (index-only scans)
-- 4. For transaction_events partitions, create indexes after partition creation
-- 5. Monitor index usage with pg_stat_user_indexes
-- 6. Consider BRIN indexes for very large time-series data (transaction_events)
-- =============================================================================
