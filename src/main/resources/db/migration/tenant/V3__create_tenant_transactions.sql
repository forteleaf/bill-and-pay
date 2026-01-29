-- =============================================================================
-- Bill&Pay Tenant Schema - Transaction Tables
-- =============================================================================
-- Description: Core transaction processing tables
-- - transactions (current state, fast queries)
-- - transaction_events (immutable event log, partitioned by day)
--
-- Hybrid Event Sourcing Pattern:
-- - transactions: mutable current state for fast lookups
-- - transaction_events: immutable event history for audit and settlement
-- =============================================================================

-- =============================================================================
-- Transactions Table (Current State)
-- =============================================================================
-- Stores current state of each transaction for fast queries
-- Updated on each status change
-- =============================================================================
CREATE TABLE transactions (
  id UUID PRIMARY KEY DEFAULT uuidv7(),
  
  -- Identity
  transaction_id VARCHAR(100) NOT NULL UNIQUE,
  
  -- Relationships
  merchant_id UUID NOT NULL,
  merchant_pg_mapping_id UUID NOT NULL,
  pg_connection_id UUID NOT NULL,
  
  -- Hierarchy paths (cached for fast queries)
  merchant_path ltree NOT NULL,
  org_path ltree NOT NULL,
  
  -- Payment details
  payment_method_id UUID NOT NULL,
  card_company_id UUID,
  
  -- Amounts (store in smallest currency unit, e.g., KRW = won)
  amount BIGINT NOT NULL,
  currency VARCHAR(3) NOT NULL DEFAULT 'KRW',
  
  -- Status tracking
  status VARCHAR(20) NOT NULL,
  
  -- PG transaction reference
  pg_transaction_id VARCHAR(200),
  approval_number VARCHAR(50),
  
  -- Timing
  approved_at TIMESTAMPTZ,
  cancelled_at TIMESTAMPTZ,
  
  -- KORPAY specific fields (per PRD-04, PRD-06)
  cat_id VARCHAR(50),
  tid VARCHAR(100),
  
  -- Additional data
  metadata JSONB,
  
  -- Audit
  created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  
  CONSTRAINT transactions_status_check CHECK (
    status IN ('PENDING', 'APPROVED', 'CANCELLED', 'PARTIAL_CANCELLED', 'FAILED')
  ),
  CONSTRAINT transactions_amount_check CHECK (amount > 0),
  CONSTRAINT transactions_merchant_fk FOREIGN KEY (merchant_id) REFERENCES merchants(id),
  CONSTRAINT transactions_merchant_pg_mapping_fk FOREIGN KEY (merchant_pg_mapping_id) REFERENCES merchant_pg_mappings(id),
  CONSTRAINT transactions_payment_method_fk FOREIGN KEY (payment_method_id) REFERENCES payment_methods(id),
  CONSTRAINT transactions_card_company_fk FOREIGN KEY (card_company_id) REFERENCES card_companies(id)
);

-- GiST indexes for ltree hierarchy queries
CREATE INDEX idx_transactions_merchant_path_gist ON transactions USING GIST(merchant_path);
CREATE INDEX idx_transactions_org_path_gist ON transactions USING GIST(org_path);

-- B-tree indexes for common queries
CREATE INDEX idx_transactions_merchant_id ON transactions(merchant_id);
CREATE INDEX idx_transactions_merchant_pg_mapping_id ON transactions(merchant_pg_mapping_id);
CREATE INDEX idx_transactions_pg_connection_id ON transactions(pg_connection_id);
CREATE INDEX idx_transactions_payment_method_id ON transactions(payment_method_id);
CREATE INDEX idx_transactions_card_company_id ON transactions(card_company_id);
CREATE INDEX idx_transactions_status ON transactions(status);
CREATE INDEX idx_transactions_pg_transaction_id ON transactions(pg_transaction_id);
CREATE INDEX idx_transactions_approval_number ON transactions(approval_number);
CREATE INDEX idx_transactions_cat_id ON transactions(cat_id);
CREATE INDEX idx_transactions_tid ON transactions(tid);

-- Composite indexes for common query patterns
CREATE INDEX idx_transactions_merchant_status_created ON transactions(merchant_id, status, created_at DESC);
CREATE INDEX idx_transactions_created_status ON transactions(created_at DESC, status);
CREATE INDEX idx_transactions_approved_at ON transactions(approved_at DESC) WHERE approved_at IS NOT NULL;

COMMENT ON TABLE transactions IS 'Current state of transactions - mutable for fast queries';
COMMENT ON COLUMN transactions.merchant_path IS 'Cached ltree path from merchants table';
COMMENT ON COLUMN transactions.org_path IS 'Cached ltree path from organizations table';
COMMENT ON COLUMN transactions.amount IS 'Amount in smallest currency unit (KRW = won)';
COMMENT ON COLUMN transactions.cat_id IS 'KORPAY terminal ID (catId) - for quick lookup';
COMMENT ON COLUMN transactions.tid IS 'KORPAY transaction ID (tid)';
COMMENT ON COLUMN transactions.metadata IS 'JSON: customer info, order details, PG raw response';

-- =============================================================================
-- Transaction Events Table (Immutable Event Log)
-- =============================================================================
-- Stores all transaction state changes as immutable events
-- Partitioned by created_at (daily) for performance
-- Source of truth for settlement calculations
-- =============================================================================
CREATE TABLE transaction_events (
  id UUID NOT NULL DEFAULT uuidv7(),
  
  -- Event identity
  event_type VARCHAR(20) NOT NULL,
  event_sequence INTEGER NOT NULL,
  
  -- Transaction reference
  transaction_id UUID NOT NULL,
  
  -- Relationships (denormalized for partition performance)
  merchant_id UUID NOT NULL,
  merchant_pg_mapping_id UUID NOT NULL,
  pg_connection_id UUID NOT NULL,
  
  -- Hierarchy paths (denormalized)
  merchant_path ltree NOT NULL,
  org_path ltree NOT NULL,
  
  -- Payment details (denormalized)
  payment_method_id UUID NOT NULL,
  card_company_id UUID,
  
  -- Event-specific amounts
  amount BIGINT NOT NULL,
  currency VARCHAR(3) NOT NULL DEFAULT 'KRW',
  
  -- Previous and new status
  previous_status VARCHAR(20),
  new_status VARCHAR(20) NOT NULL,
  
  -- PG reference
  pg_transaction_id VARCHAR(200),
  approval_number VARCHAR(50),
  
  -- KORPAY fields
  cat_id VARCHAR(50),
  tid VARCHAR(100),
  
  -- Event metadata
  metadata JSONB,
  
  -- Timing
  occurred_at TIMESTAMPTZ NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  
  CONSTRAINT transaction_events_event_type_check CHECK (
    event_type IN ('APPROVAL', 'CANCEL', 'PARTIAL_CANCEL', 'REFUND')
  ),
  CONSTRAINT transaction_events_amount_check CHECK (amount != 0),
  CONSTRAINT transaction_events_pk PRIMARY KEY (id, created_at)
) PARTITION BY RANGE (created_at);

-- GiST indexes for ltree (created on partitions)
CREATE INDEX idx_transaction_events_merchant_path_gist ON transaction_events USING GIST(merchant_path);
CREATE INDEX idx_transaction_events_org_path_gist ON transaction_events USING GIST(org_path);

-- B-tree indexes (created on partitions)
CREATE INDEX idx_transaction_events_transaction_id ON transaction_events(transaction_id, event_sequence);
CREATE INDEX idx_transaction_events_merchant_id ON transaction_events(merchant_id, created_at DESC);
CREATE INDEX idx_transaction_events_event_type ON transaction_events(event_type, created_at DESC);
CREATE INDEX idx_transaction_events_pg_connection_id ON transaction_events(pg_connection_id, created_at DESC);
CREATE INDEX idx_transaction_events_cat_id ON transaction_events(cat_id, created_at DESC);
CREATE INDEX idx_transaction_events_occurred_at ON transaction_events(occurred_at DESC);

COMMENT ON TABLE transaction_events IS 'Immutable event log - partitioned by created_at (daily)';
COMMENT ON COLUMN transaction_events.event_type IS 'APPROVAL: initial payment, CANCEL: full cancel, PARTIAL_CANCEL: partial refund';
COMMENT ON COLUMN transaction_events.event_sequence IS 'Monotonic sequence per transaction_id for ordering';
COMMENT ON COLUMN transaction_events.amount IS 'Signed amount: positive for APPROVAL, negative for CANCEL/PARTIAL_CANCEL';
COMMENT ON COLUMN transaction_events.metadata IS 'JSON: PG webhook payload, user context, cancel reason';

-- =============================================================================
-- Create Initial Partitions (7 days ahead)
-- =============================================================================
-- Daily partitions for transaction_events
-- Production: automate partition creation with pg_cron or application job
-- =============================================================================

-- Today
CREATE TABLE transaction_events_20260130 PARTITION OF transaction_events
  FOR VALUES FROM ('2026-01-30 00:00:00+00') TO ('2026-01-31 00:00:00+00');

-- Tomorrow
CREATE TABLE transaction_events_20260131 PARTITION OF transaction_events
  FOR VALUES FROM ('2026-01-31 00:00:00+00') TO ('2026-02-01 00:00:00+00');

-- Next 5 days
CREATE TABLE transaction_events_20260201 PARTITION OF transaction_events
  FOR VALUES FROM ('2026-02-01 00:00:00+00') TO ('2026-02-02 00:00:00+00');

CREATE TABLE transaction_events_20260202 PARTITION OF transaction_events
  FOR VALUES FROM ('2026-02-02 00:00:00+00') TO ('2026-02-03 00:00:00+00');

CREATE TABLE transaction_events_20260203 PARTITION OF transaction_events
  FOR VALUES FROM ('2026-02-03 00:00:00+00') TO ('2026-02-04 00:00:00+00');

CREATE TABLE transaction_events_20260204 PARTITION OF transaction_events
  FOR VALUES FROM ('2026-02-04 00:00:00+00') TO ('2026-02-05 00:00:00+00');

CREATE TABLE transaction_events_20260205 PARTITION OF transaction_events
  FOR VALUES FROM ('2026-02-05 00:00:00+00') TO ('2026-02-06 00:00:00+00');

COMMENT ON TABLE transaction_events_20260130 IS 'Partition for 2026-01-30 transaction events';
