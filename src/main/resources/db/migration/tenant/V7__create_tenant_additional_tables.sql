-- =============================================================================
-- Bill&Pay Tenant Schema - Additional Supporting Tables
-- =============================================================================
-- Description: Supporting tables for operations and auditing
-- - webhook_logs (PG webhook event tracking)
-- - audit_logs (system-wide audit trail)
-- - api_keys (API authentication)
-- =============================================================================

-- =============================================================================
-- Webhook Logs Table
-- =============================================================================
-- Tracks all incoming webhook events from PG companies
-- Used for debugging, reconciliation, and replay
-- =============================================================================
CREATE TABLE webhook_logs (
  id UUID PRIMARY KEY DEFAULT uuidv7(),
  
  -- Source
  pg_connection_id UUID NOT NULL,
  
  -- Event details
  event_type VARCHAR(50) NOT NULL,
  
  -- Webhook payload
  payload JSONB NOT NULL,
  headers JSONB,
  
  -- Processing
  status VARCHAR(20) NOT NULL DEFAULT 'RECEIVED',
  processed_at TIMESTAMPTZ,
  
  -- Linked transaction (if identified)
  transaction_id UUID,
  transaction_event_id UUID,
  
  -- Error tracking
  error_message TEXT,
  retry_count INTEGER NOT NULL DEFAULT 0,
  
  -- Security
  signature VARCHAR(500),
  signature_verified BOOLEAN,
  
  -- Audit
  received_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  
  CONSTRAINT webhook_logs_status_check CHECK (
    status IN ('RECEIVED', 'PROCESSING', 'PROCESSED', 'FAILED', 'IGNORED')
  )
);

CREATE INDEX idx_webhook_logs_pg_connection_id ON webhook_logs(pg_connection_id, received_at DESC);
CREATE INDEX idx_webhook_logs_event_type ON webhook_logs(event_type, received_at DESC);
CREATE INDEX idx_webhook_logs_status ON webhook_logs(status, received_at DESC);
CREATE INDEX idx_webhook_logs_transaction_id ON webhook_logs(transaction_id);
CREATE INDEX idx_webhook_logs_received_at ON webhook_logs(received_at DESC);

-- GIN index for JSONB payload searches
CREATE INDEX idx_webhook_logs_payload_gin ON webhook_logs USING GIN(payload);

COMMENT ON TABLE webhook_logs IS 'PG webhook event tracking for debugging and reconciliation';
COMMENT ON COLUMN webhook_logs.event_type IS 'Examples: payment.approved, payment.cancelled, payment.failed';
COMMENT ON COLUMN webhook_logs.payload IS 'Full webhook payload from PG';
COMMENT ON COLUMN webhook_logs.signature IS 'HMAC signature for webhook verification';

-- =============================================================================
-- Audit Logs Table
-- =============================================================================
-- System-wide audit trail for security and compliance
-- Tracks all sensitive operations (create, update, delete)
-- =============================================================================
CREATE TABLE audit_logs (
  id UUID PRIMARY KEY DEFAULT uuidv7(),
  
  -- Actor
  user_id UUID,
  username VARCHAR(100),
  
  -- Action
  action VARCHAR(50) NOT NULL,
  entity_type VARCHAR(100) NOT NULL,
  entity_id UUID NOT NULL,
  
  -- Changes (before/after snapshots)
  old_values JSONB,
  new_values JSONB,
  
  -- Context
  ip_address INET,
  user_agent TEXT,
  
  -- Request details
  request_id VARCHAR(100),
  
  -- Audit
  created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  
  CONSTRAINT audit_logs_action_check CHECK (
    action IN ('CREATE', 'UPDATE', 'DELETE', 'LOGIN', 'LOGOUT', 'APPROVE', 'REJECT', 'EXPORT')
  )
);

CREATE INDEX idx_audit_logs_user_id ON audit_logs(user_id, created_at DESC);
CREATE INDEX idx_audit_logs_entity ON audit_logs(entity_type, entity_id, created_at DESC);
CREATE INDEX idx_audit_logs_action ON audit_logs(action, created_at DESC);
CREATE INDEX idx_audit_logs_created_at ON audit_logs(created_at DESC);

-- GIN indexes for JSONB searches
CREATE INDEX idx_audit_logs_old_values_gin ON audit_logs USING GIN(old_values);
CREATE INDEX idx_audit_logs_new_values_gin ON audit_logs USING GIN(new_values);

COMMENT ON TABLE audit_logs IS 'System-wide audit trail for compliance and security';
COMMENT ON COLUMN audit_logs.old_values IS 'JSON snapshot before change';
COMMENT ON COLUMN audit_logs.new_values IS 'JSON snapshot after change';

-- =============================================================================
-- API Keys Table
-- =============================================================================
-- API authentication tokens for programmatic access
-- Used for merchant integrations, external systems
-- =============================================================================
CREATE TABLE api_keys (
  id UUID PRIMARY KEY DEFAULT uuidv7(),
  
  -- Identity
  key_name VARCHAR(200) NOT NULL,
  key_hash VARCHAR(255) NOT NULL UNIQUE,
  key_prefix VARCHAR(20) NOT NULL,
  
  -- Owner
  user_id UUID,
  org_id UUID NOT NULL,
  org_path ltree NOT NULL,
  
  -- Scope and permissions
  scopes JSONB NOT NULL,
  
  -- Rate limiting
  rate_limit_per_minute INTEGER,
  rate_limit_per_hour INTEGER,
  
  -- Usage tracking
  last_used_at TIMESTAMPTZ,
  usage_count BIGINT NOT NULL DEFAULT 0,
  
  -- Expiration
  expires_at TIMESTAMPTZ,
  
  -- Status
  status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  
  -- Audit
  created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  
  CONSTRAINT api_keys_status_check CHECK (status IN ('ACTIVE', 'REVOKED', 'EXPIRED')),
  CONSTRAINT api_keys_user_fk FOREIGN KEY (user_id) REFERENCES users(id),
  CONSTRAINT api_keys_org_fk FOREIGN KEY (org_id) REFERENCES organizations(id)
);

-- GiST index for ltree
CREATE INDEX idx_api_keys_org_path_gist ON api_keys USING GIST(org_path);

-- B-tree indexes
CREATE INDEX idx_api_keys_key_hash ON api_keys(key_hash);
CREATE INDEX idx_api_keys_key_prefix ON api_keys(key_prefix);
CREATE INDEX idx_api_keys_user_id ON api_keys(user_id);
CREATE INDEX idx_api_keys_org_id ON api_keys(org_id);
CREATE INDEX idx_api_keys_status ON api_keys(status) WHERE status = 'ACTIVE';
CREATE INDEX idx_api_keys_expires_at ON api_keys(expires_at) WHERE expires_at IS NOT NULL;

COMMENT ON TABLE api_keys IS 'API authentication tokens for programmatic access';
COMMENT ON COLUMN api_keys.key_hash IS 'SHA-256 hash of the API key (store hash, not plaintext)';
COMMENT ON COLUMN api_keys.key_prefix IS 'First 8 chars of key for identification (e.g., bp_live_)';
COMMENT ON COLUMN api_keys.scopes IS 'JSON array of permissions: ["transactions:read", "settlements:read"]';

-- =============================================================================
-- Settlement Reports Table
-- =============================================================================
-- Pre-generated settlement reports for quick access
-- Reduces need for complex queries on large datasets
-- =============================================================================
CREATE TABLE settlement_reports (
  id UUID PRIMARY KEY DEFAULT uuidv7(),
  
  -- Report details
  report_type VARCHAR(50) NOT NULL,
  
  -- Scope
  entity_id UUID NOT NULL,
  entity_type VARCHAR(20) NOT NULL,
  entity_path ltree NOT NULL,
  
  -- Period
  period_start TIMESTAMPTZ NOT NULL,
  period_end TIMESTAMPTZ NOT NULL,
  
  -- Summary data
  report_data JSONB NOT NULL,
  
  -- File reference (if exported to file)
  file_path VARCHAR(500),
  file_size BIGINT,
  
  -- Status
  status VARCHAR(20) NOT NULL DEFAULT 'GENERATED',
  
  -- Audit
  generated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  generated_by UUID,
  
  CONSTRAINT settlement_reports_report_type_check CHECK (
    report_type IN ('DAILY', 'WEEKLY', 'MONTHLY', 'CUSTOM', 'RECONCILIATION')
  ),
  CONSTRAINT settlement_reports_entity_type_check CHECK (
    entity_type IN ('DISTRIBUTOR', 'AGENCY', 'DEALER', 'SELLER', 'VENDOR')
  ),
  CONSTRAINT settlement_reports_status_check CHECK (
    status IN ('GENERATED', 'EXPORTED', 'DELIVERED', 'FAILED')
  ),
  CONSTRAINT settlement_reports_entity_fk FOREIGN KEY (entity_id) REFERENCES organizations(id),
  CONSTRAINT settlement_reports_generated_by_fk FOREIGN KEY (generated_by) REFERENCES users(id)
);

-- GiST index for ltree
CREATE INDEX idx_settlement_reports_entity_path_gist ON settlement_reports USING GIST(entity_path);

-- B-tree indexes
CREATE INDEX idx_settlement_reports_entity_id ON settlement_reports(entity_id, generated_at DESC);
CREATE INDEX idx_settlement_reports_report_type ON settlement_reports(report_type, generated_at DESC);
CREATE INDEX idx_settlement_reports_period ON settlement_reports(period_start, period_end);
CREATE INDEX idx_settlement_reports_status ON settlement_reports(status);

-- GIN index for report data searches
CREATE INDEX idx_settlement_reports_data_gin ON settlement_reports USING GIN(report_data);

COMMENT ON TABLE settlement_reports IS 'Pre-generated settlement reports for quick access';
COMMENT ON COLUMN settlement_reports.report_data IS 'JSON: {total_transactions, total_amount, total_fees, breakdowns, summaries}';
