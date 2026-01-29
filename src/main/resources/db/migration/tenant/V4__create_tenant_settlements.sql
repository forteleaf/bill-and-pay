-- =============================================================================
-- Bill&Pay Tenant Schema - Settlements (Double-Entry Ledger)
-- =============================================================================
-- Description: Settlement processing with double-entry bookkeeping
-- - settlements (ledger entries with CREDIT/DEBIT)
-- - settlement_batches (grouping for batch processing)
--
-- Double-Entry Bookkeeping Rules (per PRD-03):
-- - APPROVAL: CREDIT to all entities (distributor, agency, dealer, seller, vendor)
-- - CANCEL/REFUND: DEBIT to all entities (reverse entry)
-- - Zero-Sum Validation: |event amount| = SUM(settlement amounts)
-- =============================================================================

-- =============================================================================
-- Settlement Batches Table
-- =============================================================================
-- Groups settlements for batch processing and reporting
-- Typically one batch per settlement period (daily, weekly, monthly)
-- =============================================================================
CREATE TABLE settlement_batches (
  id UUID PRIMARY KEY DEFAULT uuidv7(),
  
  -- Identity
  batch_number VARCHAR(100) NOT NULL UNIQUE,
  
  -- Period
  settlement_date DATE NOT NULL,
  period_start TIMESTAMPTZ NOT NULL,
  period_end TIMESTAMPTZ NOT NULL,
  
  -- Status
  status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
  
  -- Summary
  total_transactions INTEGER NOT NULL DEFAULT 0,
  total_amount BIGINT NOT NULL DEFAULT 0,
  total_fee_amount BIGINT NOT NULL DEFAULT 0,
  
  -- Processing
  processed_at TIMESTAMPTZ,
  approved_at TIMESTAMPTZ,
  
  -- Metadata
  metadata JSONB,
  
  -- Audit
  created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  
  CONSTRAINT settlement_batches_status_check CHECK (
    status IN ('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED', 'CANCELLED')
  )
);

CREATE INDEX idx_settlement_batches_batch_number ON settlement_batches(batch_number);
CREATE INDEX idx_settlement_batches_settlement_date ON settlement_batches(settlement_date DESC);
CREATE INDEX idx_settlement_batches_status ON settlement_batches(status);
CREATE INDEX idx_settlement_batches_period ON settlement_batches(period_start, period_end);

COMMENT ON TABLE settlement_batches IS 'Settlement batch grouping for processing and reporting';
COMMENT ON COLUMN settlement_batches.batch_number IS 'Format: YYYYMMDD-XXX (e.g., 20260130-001)';
COMMENT ON COLUMN settlement_batches.metadata IS 'JSON: {approver_id, notes, reconciliation_status}';

-- =============================================================================
-- Settlements Table (Double-Entry Ledger)
-- =============================================================================
-- Double-entry bookkeeping ledger for all settlements
-- Each transaction_event creates multiple settlement entries (one per entity)
--
-- CRITICAL: Zero-Sum Validation
-- For each transaction_event: |event.amount| = SUM(settlement.amount)
-- =============================================================================
CREATE TABLE settlements (
  id UUID PRIMARY KEY DEFAULT uuidv7(),
  
  -- Batch reference
  settlement_batch_id UUID,
  
  -- Event reference (source of truth)
  transaction_event_id UUID NOT NULL,
  transaction_id UUID NOT NULL,
  
  -- Merchant reference
  merchant_id UUID NOT NULL,
  merchant_path ltree NOT NULL,
  
  -- Entity receiving settlement
  entity_id UUID NOT NULL,
  entity_type VARCHAR(20) NOT NULL,
  entity_path ltree NOT NULL,
  
  -- Double-entry fields
  entry_type VARCHAR(10) NOT NULL,
  
  -- Settlement amounts
  amount BIGINT NOT NULL,
  fee_amount BIGINT NOT NULL DEFAULT 0,
  net_amount BIGINT NOT NULL,
  currency VARCHAR(3) NOT NULL DEFAULT 'KRW',
  
  -- Fee calculation details
  fee_rate DECIMAL(10, 6),
  fee_config JSONB,
  
  -- Status
  status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
  
  -- Timing
  settled_at TIMESTAMPTZ,
  
  -- Metadata
  metadata JSONB,
  
  -- Audit
  created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  
  CONSTRAINT settlements_entry_type_check CHECK (entry_type IN ('CREDIT', 'DEBIT')),
  CONSTRAINT settlements_entity_type_check CHECK (
    entity_type IN ('DISTRIBUTOR', 'AGENCY', 'DEALER', 'SELLER', 'VENDOR')
  ),
  CONSTRAINT settlements_status_check CHECK (
    status IN ('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED', 'CANCELLED')
  ),
  CONSTRAINT settlements_amount_sign_check CHECK (
    (entry_type = 'CREDIT' AND amount > 0) OR
    (entry_type = 'DEBIT' AND amount < 0)
  ),
  CONSTRAINT settlements_batch_fk FOREIGN KEY (settlement_batch_id) REFERENCES settlement_batches(id),
  CONSTRAINT settlements_entity_fk FOREIGN KEY (entity_id) REFERENCES organizations(id)
);

-- GiST indexes for ltree
CREATE INDEX idx_settlements_merchant_path_gist ON settlements USING GIST(merchant_path);
CREATE INDEX idx_settlements_entity_path_gist ON settlements USING GIST(entity_path);

-- B-tree indexes for queries
CREATE INDEX idx_settlements_settlement_batch_id ON settlements(settlement_batch_id);
CREATE INDEX idx_settlements_transaction_event_id ON settlements(transaction_event_id);
CREATE INDEX idx_settlements_transaction_id ON settlements(transaction_id);
CREATE INDEX idx_settlements_merchant_id ON settlements(merchant_id);
CREATE INDEX idx_settlements_entity_id ON settlements(entity_id, created_at DESC);
CREATE INDEX idx_settlements_entity_type ON settlements(entity_type, created_at DESC);
CREATE INDEX idx_settlements_entry_type ON settlements(entry_type);
CREATE INDEX idx_settlements_status ON settlements(status);
CREATE INDEX idx_settlements_created_at ON settlements(created_at DESC);

-- Composite indexes for common queries
CREATE INDEX idx_settlements_entity_status_created ON settlements(entity_id, status, created_at DESC);
CREATE INDEX idx_settlements_batch_status ON settlements(settlement_batch_id, status);

COMMENT ON TABLE settlements IS 'Double-entry ledger - CREDIT for approval, DEBIT for cancel';
COMMENT ON COLUMN settlements.entry_type IS 'CREDIT: money in (approval), DEBIT: money out (cancel/refund)';
COMMENT ON COLUMN settlements.amount IS 'Signed: positive for CREDIT, negative for DEBIT';
COMMENT ON COLUMN settlements.fee_amount IS 'Fee charged to entity (always >= 0)';
COMMENT ON COLUMN settlements.net_amount IS 'amount - fee_amount (what entity actually receives/pays)';
COMMENT ON COLUMN settlements.fee_config IS 'JSON: {rate, fixed_fee, min_fee, max_fee, calculation_method}';
COMMENT ON COLUMN settlements.metadata IS 'JSON: {rounding_adjustment, proration_ratio, notes}';

-- =============================================================================
-- Zero-Sum Validation View
-- =============================================================================
-- Helper view to verify Zero-Sum constraint
-- For each transaction_event: SUM(settlement.amount) should equal event.amount
-- =============================================================================
CREATE VIEW settlement_zero_sum_validation AS
SELECT 
  te.id AS transaction_event_id,
  te.transaction_id,
  te.event_type,
  te.amount AS event_amount,
  COALESCE(SUM(s.amount), 0) AS total_settlement_amount,
  te.amount - COALESCE(SUM(s.amount), 0) AS zero_sum_diff,
  CASE 
    WHEN te.amount - COALESCE(SUM(s.amount), 0) = 0 THEN TRUE
    ELSE FALSE
  END AS is_zero_sum_valid,
  COUNT(s.id) AS settlement_count
FROM transaction_events te
LEFT JOIN settlements s ON s.transaction_event_id = te.id
GROUP BY te.id, te.transaction_id, te.event_type, te.amount;

COMMENT ON VIEW settlement_zero_sum_validation IS 'Validates Zero-Sum constraint: |event.amount| = SUM(settlement.amount)';
