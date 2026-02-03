-- =============================================================================
-- Bill&Pay Tenant Schema - Terminals Table
-- =============================================================================
-- Description: Creates terminals table for payment device management
-- 
-- Terminal Types:
-- - CAT: Card Authorization Terminal (신용카드 단말기)
-- - POS: Point of Sale system
-- - MOBILE: Mobile payment device
-- - KIOSK: Self-service kiosk
-- - ONLINE: Online/Virtual terminal
-- =============================================================================

CREATE TABLE terminals (
  id UUID PRIMARY KEY DEFAULT uuidv7(),
  
  -- Identity
  tid VARCHAR(50) NOT NULL UNIQUE,
  cat_id VARCHAR(50),
  
  -- Type
  terminal_type VARCHAR(20) NOT NULL,
  
  -- Relationships
  merchant_id UUID NOT NULL,
  organization_id UUID,
  
  -- Device info
  serial_number VARCHAR(100),
  model VARCHAR(100),
  manufacturer VARCHAR(100),
  
  -- Installation
  install_address TEXT,
  install_date DATE,
  
  -- Status
  status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  
  -- Configuration
  config JSONB,
  
  -- Activity tracking
  last_transaction_at TIMESTAMPTZ,
  
  -- Audit
  created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted_at TIMESTAMPTZ,
  
  -- Constraints
  CONSTRAINT terminals_type_check CHECK (terminal_type IN ('CAT', 'POS', 'MOBILE', 'KIOSK', 'ONLINE')),
  CONSTRAINT terminals_status_check CHECK (status IN ('ACTIVE', 'INACTIVE', 'SUSPENDED', 'TERMINATED')),
  CONSTRAINT terminals_merchant_fk FOREIGN KEY (merchant_id) REFERENCES merchants(id),
  CONSTRAINT terminals_organization_fk FOREIGN KEY (organization_id) REFERENCES organizations(id)
);

-- Indexes
CREATE INDEX idx_terminals_tid ON terminals(tid);
CREATE INDEX idx_terminals_cat_id ON terminals(cat_id) WHERE cat_id IS NOT NULL;
CREATE INDEX idx_terminals_merchant_id ON terminals(merchant_id);
CREATE INDEX idx_terminals_organization_id ON terminals(organization_id) WHERE organization_id IS NOT NULL;
CREATE INDEX idx_terminals_terminal_type ON terminals(terminal_type);
CREATE INDEX idx_terminals_status ON terminals(status) WHERE status = 'ACTIVE';
CREATE INDEX idx_terminals_last_transaction_at ON terminals(last_transaction_at DESC NULLS LAST);

-- Comments
COMMENT ON TABLE terminals IS 'Payment terminals/devices registered to merchants';
COMMENT ON COLUMN terminals.tid IS 'Terminal ID - unique identifier across PG systems';
COMMENT ON COLUMN terminals.cat_id IS 'CAT ID for card authorization terminals';
COMMENT ON COLUMN terminals.terminal_type IS 'Device type: CAT, POS, MOBILE, KIOSK, ONLINE';
COMMENT ON COLUMN terminals.config IS 'JSON: device settings, capabilities, features';
COMMENT ON COLUMN terminals.last_transaction_at IS 'Timestamp of most recent transaction';
