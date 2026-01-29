-- =============================================================================
-- Bill&Pay Tenant Schema - Additional Constraints & Validation
-- =============================================================================
-- Description: Data integrity constraints and validation rules
-- - Additional CHECK constraints
-- - Unique constraints
-- - Data validation triggers (placeholder for future implementation)
-- =============================================================================

-- =============================================================================
-- Organizations - Additional Constraints
-- =============================================================================
-- Ensure path consistency with level
ALTER TABLE organizations 
  ADD CONSTRAINT organizations_path_level_consistency 
  CHECK (nlevel(path) = level);

-- Ensure parent exists for non-root organizations
ALTER TABLE organizations 
  ADD CONSTRAINT organizations_parent_required 
  CHECK ((level = 1 AND parent_id IS NULL) OR (level > 1 AND parent_id IS NOT NULL));

-- Ensure DISTRIBUTOR is always level 1
ALTER TABLE organizations 
  ADD CONSTRAINT organizations_distributor_level 
  CHECK ((org_type = 'DISTRIBUTOR' AND level = 1) OR org_type != 'DISTRIBUTOR');

COMMENT ON CONSTRAINT organizations_path_level_consistency ON organizations IS 'Ensures ltree path depth matches level column';
COMMENT ON CONSTRAINT organizations_parent_required ON organizations IS 'Root (level 1) has no parent, others must have parent';
COMMENT ON CONSTRAINT organizations_distributor_level ON organizations IS 'DISTRIBUTOR must be level 1 (root)';

-- =============================================================================
-- Merchants - Additional Constraints
-- =============================================================================
-- Ensure merchants belong to SELLER or VENDOR level organizations
ALTER TABLE merchants 
  ADD CONSTRAINT merchants_org_level_check 
  CHECK (nlevel(org_path) >= 4);

COMMENT ON CONSTRAINT merchants_org_level_check ON merchants IS 'Merchants must belong to SELLER (level 4) or VENDOR (level 5)';

-- =============================================================================
-- Transactions - Additional Constraints
-- =============================================================================
-- Ensure approval_number exists for approved transactions
ALTER TABLE transactions 
  ADD CONSTRAINT transactions_approval_number_required 
  CHECK (
    (status IN ('APPROVED', 'PARTIAL_CANCELLED') AND approval_number IS NOT NULL) OR
    status NOT IN ('APPROVED', 'PARTIAL_CANCELLED')
  );

-- Ensure approved_at timestamp for approved transactions
ALTER TABLE transactions 
  ADD CONSTRAINT transactions_approved_at_required 
  CHECK (
    (status IN ('APPROVED', 'PARTIAL_CANCELLED') AND approved_at IS NOT NULL) OR
    status NOT IN ('APPROVED', 'PARTIAL_CANCELLED')
  );

-- Ensure cancelled_at timestamp for cancelled transactions
ALTER TABLE transactions 
  ADD CONSTRAINT transactions_cancelled_at_required 
  CHECK (
    (status IN ('CANCELLED', 'PARTIAL_CANCELLED') AND cancelled_at IS NOT NULL) OR
    status NOT IN ('CANCELLED', 'PARTIAL_CANCELLED')
  );

-- Ensure card_company_id for card payments
ALTER TABLE transactions 
  ADD CONSTRAINT transactions_card_company_for_card_payments 
  CHECK (
    (payment_method_id IN (
      SELECT id FROM payment_methods WHERE category = 'CARD'
    ) AND card_company_id IS NOT NULL) OR
    payment_method_id NOT IN (
      SELECT id FROM payment_methods WHERE category = 'CARD'
    )
  );

COMMENT ON CONSTRAINT transactions_approval_number_required ON transactions IS 'Approved transactions must have approval number';
COMMENT ON CONSTRAINT transactions_approved_at_required ON transactions IS 'Approved transactions must have approval timestamp';
COMMENT ON CONSTRAINT transactions_cancelled_at_required ON transactions IS 'Cancelled transactions must have cancellation timestamp';

-- =============================================================================
-- Transaction Events - Additional Constraints
-- =============================================================================
-- Ensure amount sign matches event type
ALTER TABLE transaction_events 
  ADD CONSTRAINT transaction_events_amount_sign_matches_type 
  CHECK (
    (event_type = 'APPROVAL' AND amount > 0) OR
    (event_type IN ('CANCEL', 'PARTIAL_CANCEL', 'REFUND') AND amount < 0)
  );

-- Ensure event_sequence is positive
ALTER TABLE transaction_events 
  ADD CONSTRAINT transaction_events_sequence_positive 
  CHECK (event_sequence > 0);

-- Ensure occurred_at is not in future
ALTER TABLE transaction_events 
  ADD CONSTRAINT transaction_events_occurred_at_not_future 
  CHECK (occurred_at <= CURRENT_TIMESTAMP);

COMMENT ON CONSTRAINT transaction_events_amount_sign_matches_type ON transaction_events IS 'APPROVAL: positive, CANCEL/REFUND: negative';
COMMENT ON CONSTRAINT transaction_events_occurred_at_not_future ON transaction_events IS 'Event cannot occur in the future';

-- =============================================================================
-- Settlements - Additional Constraints
-- =============================================================================
-- Ensure net_amount calculation is correct
ALTER TABLE settlements 
  ADD CONSTRAINT settlements_net_amount_calculation 
  CHECK (
    (entry_type = 'CREDIT' AND net_amount = amount - fee_amount) OR
    (entry_type = 'DEBIT' AND net_amount = amount + fee_amount)
  );

-- Ensure fee_amount is non-negative
ALTER TABLE settlements 
  ADD CONSTRAINT settlements_fee_amount_non_negative 
  CHECK (fee_amount >= 0);

-- Ensure settled_at exists for completed settlements
ALTER TABLE settlements 
  ADD CONSTRAINT settlements_settled_at_required 
  CHECK (
    (status = 'COMPLETED' AND settled_at IS NOT NULL) OR
    status != 'COMPLETED'
  );

COMMENT ON CONSTRAINT settlements_net_amount_calculation ON settlements IS 'Validates net_amount = amount - fee_amount for CREDIT, amount + fee_amount for DEBIT';
COMMENT ON CONSTRAINT settlements_fee_amount_non_negative ON settlements IS 'Fee amount must always be non-negative';

-- =============================================================================
-- Fee Configurations - Additional Constraints
-- =============================================================================
-- Ensure at least one fee configuration is defined
ALTER TABLE fee_configurations 
  ADD CONSTRAINT fee_configurations_has_fee_definition 
  CHECK (
    (fee_type = 'PERCENTAGE' AND fee_rate IS NOT NULL) OR
    (fee_type = 'FIXED' AND fixed_fee IS NOT NULL) OR
    (fee_type = 'TIERED' AND tier_config IS NOT NULL) OR
    (fee_type = 'PERCENTAGE_PLUS_FIXED' AND fee_rate IS NOT NULL AND fixed_fee IS NOT NULL)
  );

-- Ensure fee_rate is between 0 and 1 (0% to 100%)
ALTER TABLE fee_configurations 
  ADD CONSTRAINT fee_configurations_rate_range 
  CHECK (fee_rate IS NULL OR (fee_rate >= 0 AND fee_rate <= 1));

-- Ensure min_fee <= max_fee
ALTER TABLE fee_configurations 
  ADD CONSTRAINT fee_configurations_min_max_fee 
  CHECK (min_fee IS NULL OR max_fee IS NULL OR min_fee <= max_fee);

-- Ensure valid_from <= valid_until
ALTER TABLE fee_configurations 
  ADD CONSTRAINT fee_configurations_valid_period 
  CHECK (valid_until IS NULL OR valid_from <= valid_until);

COMMENT ON CONSTRAINT fee_configurations_has_fee_definition ON fee_configurations IS 'Ensures appropriate fee fields are set based on fee_type';
COMMENT ON CONSTRAINT fee_configurations_rate_range ON fee_configurations IS 'Fee rate must be between 0 (0%) and 1 (100%)';

-- =============================================================================
-- Settlement Batches - Additional Constraints
-- =============================================================================
-- Ensure period_start < period_end
ALTER TABLE settlement_batches 
  ADD CONSTRAINT settlement_batches_period_order 
  CHECK (period_start < period_end);

-- Ensure total_transactions is non-negative
ALTER TABLE settlement_batches 
  ADD CONSTRAINT settlement_batches_transactions_non_negative 
  CHECK (total_transactions >= 0);

-- Ensure processed_at exists for non-pending batches
ALTER TABLE settlement_batches 
  ADD CONSTRAINT settlement_batches_processed_at_required 
  CHECK (
    (status IN ('COMPLETED', 'FAILED') AND processed_at IS NOT NULL) OR
    status NOT IN ('COMPLETED', 'FAILED')
  );

COMMENT ON CONSTRAINT settlement_batches_period_order ON settlement_batches IS 'Period start must be before period end';

-- =============================================================================
-- Merchant PG Mappings - Additional Constraints
-- =============================================================================
-- Ensure cat_id is provided for KORPAY connections
ALTER TABLE merchant_pg_mappings 
  ADD CONSTRAINT merchant_pg_mappings_korpay_catid 
  CHECK (
    (pg_connection_id IN (
      SELECT id FROM public.pg_connections WHERE pg_code = 'KORPAY'
    ) AND cat_id IS NOT NULL) OR
    pg_connection_id NOT IN (
      SELECT id FROM public.pg_connections WHERE pg_code = 'KORPAY'
    )
  );

COMMENT ON CONSTRAINT merchant_pg_mappings_korpay_catid ON merchant_pg_mappings IS 'KORPAY requires cat_id (1:1 with MID per PRD-04)';

-- =============================================================================
-- Webhook Logs - Additional Constraints
-- =============================================================================
-- Ensure retry_count is non-negative
ALTER TABLE webhook_logs 
  ADD CONSTRAINT webhook_logs_retry_count_non_negative 
  CHECK (retry_count >= 0);

-- Ensure processed_at exists for processed webhooks
ALTER TABLE webhook_logs 
  ADD CONSTRAINT webhook_logs_processed_at_required 
  CHECK (
    (status IN ('PROCESSED', 'FAILED', 'IGNORED') AND processed_at IS NOT NULL) OR
    status NOT IN ('PROCESSED', 'FAILED', 'IGNORED')
  );

COMMENT ON CONSTRAINT webhook_logs_processed_at_required ON webhook_logs IS 'Processed webhooks must have processing timestamp';

-- =============================================================================
-- API Keys - Additional Constraints
-- =============================================================================
-- Ensure key_prefix matches pattern (e.g., bp_live_, bp_test_)
ALTER TABLE api_keys 
  ADD CONSTRAINT api_keys_prefix_pattern 
  CHECK (key_prefix ~ '^bp_(live|test)_[a-zA-Z0-9]{4}$');

-- Ensure rate limits are positive
ALTER TABLE api_keys 
  ADD CONSTRAINT api_keys_rate_limits_positive 
  CHECK (
    (rate_limit_per_minute IS NULL OR rate_limit_per_minute > 0) AND
    (rate_limit_per_hour IS NULL OR rate_limit_per_hour > 0)
  );

COMMENT ON CONSTRAINT api_keys_prefix_pattern ON api_keys IS 'API key prefix format: bp_live_XXXX or bp_test_XXXX';

-- =============================================================================
-- Users - Additional Constraints
-- =============================================================================
-- Ensure email format
ALTER TABLE users 
  ADD CONSTRAINT users_email_format 
  CHECK (email ~* '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$');

-- Ensure DISTRIBUTOR users have 2FA enabled
ALTER TABLE users 
  ADD CONSTRAINT users_distributor_2fa_required 
  CHECK (
    (org_id IN (
      SELECT id FROM organizations WHERE org_type = 'DISTRIBUTOR'
    ) AND two_factor_enabled = TRUE) OR
    org_id NOT IN (
      SELECT id FROM organizations WHERE org_type = 'DISTRIBUTOR'
    )
  );

COMMENT ON CONSTRAINT users_email_format ON users IS 'Validates email format';
COMMENT ON CONSTRAINT users_distributor_2fa_required ON users IS 'DISTRIBUTOR (MASTER) users must have 2FA enabled per PRD';
