-- =============================================================================
-- Bill&Pay Tenant Schema - Fee Configurations
-- =============================================================================
-- Description: Fee calculation rules for multi-level settlements
-- - fee_configurations (define fee rules per organization/payment method)
--
-- Fee Structure:
-- - Percentage-based: rate * amount
-- - Fixed: flat fee per transaction
-- - Tiered: different rates based on amount ranges
-- - Can combine percentage + fixed
-- =============================================================================

-- =============================================================================
-- Fee Configurations Table
-- =============================================================================
-- Defines fee calculation rules for each organization entity
-- Supports multiple fee types and complex calculation logic
-- =============================================================================
CREATE TABLE fee_configurations (
  id UUID PRIMARY KEY DEFAULT uuidv7(),
  
  -- Entity receiving fee
  entity_id UUID NOT NULL,
  entity_type VARCHAR(20) NOT NULL,
  entity_path ltree NOT NULL,
  
  -- Fee scope (can be specific to payment method, card company, etc.)
  payment_method_id UUID,
  card_company_id UUID,
  
  -- Fee calculation
  fee_type VARCHAR(20) NOT NULL,
  
  -- Percentage fee
  fee_rate DECIMAL(10, 6),
  
  -- Fixed fee
  fixed_fee BIGINT,
  
  -- Tiered fee configuration
  tier_config JSONB,
  
  -- Limits
  min_fee BIGINT,
  max_fee BIGINT,
  
  -- Priority (higher = more specific, used for rule matching)
  priority INTEGER NOT NULL DEFAULT 0,
  
  -- Validity period
  valid_from TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  valid_until TIMESTAMPTZ,
  
  -- Status
  status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  
  -- Metadata
  metadata JSONB,
  
  -- Audit
  created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  
  CONSTRAINT fee_configurations_entity_type_check CHECK (
    entity_type IN ('DISTRIBUTOR', 'AGENCY', 'DEALER', 'SELLER', 'VENDOR')
  ),
  CONSTRAINT fee_configurations_fee_type_check CHECK (
    fee_type IN ('PERCENTAGE', 'FIXED', 'TIERED', 'PERCENTAGE_PLUS_FIXED')
  ),
  CONSTRAINT fee_configurations_status_check CHECK (
    status IN ('ACTIVE', 'INACTIVE', 'EXPIRED')
  ),
  CONSTRAINT fee_configurations_entity_fk FOREIGN KEY (entity_id) REFERENCES organizations(id),
  CONSTRAINT fee_configurations_payment_method_fk FOREIGN KEY (payment_method_id) REFERENCES payment_methods(id),
  CONSTRAINT fee_configurations_card_company_fk FOREIGN KEY (card_company_id) REFERENCES card_companies(id)
);

-- GiST index for ltree
CREATE INDEX idx_fee_configurations_entity_path_gist ON fee_configurations USING GIST(entity_path);

-- B-tree indexes
CREATE INDEX idx_fee_configurations_entity_id ON fee_configurations(entity_id);
CREATE INDEX idx_fee_configurations_entity_type ON fee_configurations(entity_type);
CREATE INDEX idx_fee_configurations_payment_method_id ON fee_configurations(payment_method_id);
CREATE INDEX idx_fee_configurations_card_company_id ON fee_configurations(card_company_id);
CREATE INDEX idx_fee_configurations_status ON fee_configurations(status) WHERE status = 'ACTIVE';
CREATE INDEX idx_fee_configurations_priority ON fee_configurations(priority DESC);

-- Composite index for fee matching
CREATE INDEX idx_fee_configurations_lookup ON fee_configurations(
  entity_id, 
  payment_method_id, 
  card_company_id, 
  status, 
  valid_from, 
  valid_until,
  priority DESC
);

COMMENT ON TABLE fee_configurations IS 'Fee calculation rules for multi-level settlements';
COMMENT ON COLUMN fee_configurations.fee_type IS 'PERCENTAGE: rate only, FIXED: flat fee, TIERED: amount ranges, PERCENTAGE_PLUS_FIXED: both';
COMMENT ON COLUMN fee_configurations.fee_rate IS 'Percentage as decimal (e.g., 0.03 = 3%)';
COMMENT ON COLUMN fee_configurations.fixed_fee IS 'Flat fee in smallest currency unit';
COMMENT ON COLUMN fee_configurations.tier_config IS 'JSON array: [{min_amount, max_amount, rate, fixed_fee}]';
COMMENT ON COLUMN fee_configurations.priority IS 'Higher number = more specific rule (e.g., card-specific > payment-method-specific > default)';
COMMENT ON COLUMN fee_configurations.metadata IS 'JSON: {description, notes, contract_reference}';

-- =============================================================================
-- Fee Configuration Example Data Structure
-- =============================================================================
-- tier_config JSONB example:
-- [
--   {"min_amount": 0, "max_amount": 100000, "rate": 0.035, "fixed_fee": 100},
--   {"min_amount": 100001, "max_amount": 1000000, "rate": 0.030, "fixed_fee": 0},
--   {"min_amount": 1000001, "max_amount": null, "rate": 0.025, "fixed_fee": 0}
-- ]
-- =============================================================================
