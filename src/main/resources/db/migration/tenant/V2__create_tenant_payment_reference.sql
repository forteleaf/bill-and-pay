-- =============================================================================
-- Bill&Pay Tenant Schema - Payment Reference Data
-- =============================================================================
-- Description: Reference tables for payment processing
-- - payment_methods (CREDIT, DEBIT, OVERSEAS, TRANSFER, VIRTUAL)
-- - card_companies (BC, KB, SS, SH, HD, LT, HN, WR, NH)
-- =============================================================================

-- =============================================================================
-- Payment Methods Table
-- =============================================================================
-- Defines supported payment methods
-- Used for routing, fee calculation, and reporting
-- =============================================================================
CREATE TABLE payment_methods (
  id UUID PRIMARY KEY DEFAULT uuidv7(),
  
  -- Identity
  method_code VARCHAR(20) NOT NULL UNIQUE,
  name VARCHAR(100) NOT NULL,
  
  -- Classification
  category VARCHAR(20) NOT NULL,
  
  -- Configuration
  config JSONB,
  
  -- Display
  display_order INTEGER NOT NULL DEFAULT 0,
  
  -- Status
  status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  
  -- Audit
  created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  
  CONSTRAINT payment_methods_category_check CHECK (category IN ('CARD', 'BANK', 'VIRTUAL', 'OTHER')),
  CONSTRAINT payment_methods_status_check CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

CREATE INDEX idx_payment_methods_method_code ON payment_methods(method_code);
CREATE INDEX idx_payment_methods_category ON payment_methods(category);
CREATE INDEX idx_payment_methods_status ON payment_methods(status) WHERE status = 'ACTIVE';
CREATE INDEX idx_payment_methods_display_order ON payment_methods(display_order);

COMMENT ON TABLE payment_methods IS 'Supported payment methods for transaction processing';
COMMENT ON COLUMN payment_methods.category IS 'High-level grouping: CARD, BANK, VIRTUAL, OTHER';
COMMENT ON COLUMN payment_methods.config IS 'JSON: {requires_auth, settlement_delay_days, features}';

-- =============================================================================
-- Card Companies Table
-- =============================================================================
-- Korean credit/debit card issuer companies
-- Used for card transaction processing and fee calculation
-- =============================================================================
CREATE TABLE card_companies (
  id UUID PRIMARY KEY DEFAULT uuidv7(),
  
  -- Identity
  company_code VARCHAR(10) NOT NULL UNIQUE,
  company_name VARCHAR(100) NOT NULL,
  
  -- Configuration
  config JSONB,
  
  -- Display
  display_order INTEGER NOT NULL DEFAULT 0,
  
  -- Status
  status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  
  -- Audit
  created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  
  CONSTRAINT card_companies_status_check CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

CREATE INDEX idx_card_companies_company_code ON card_companies(company_code);
CREATE INDEX idx_card_companies_status ON card_companies(status) WHERE status = 'ACTIVE';
CREATE INDEX idx_card_companies_display_order ON card_companies(display_order);

COMMENT ON TABLE card_companies IS 'Korean credit/debit card issuer companies';
COMMENT ON COLUMN card_companies.company_code IS 'Standard 2-letter codes (BC, KB, SS, etc)';
COMMENT ON COLUMN card_companies.config IS 'JSON: {full_name, logo_url, bin_ranges}';
