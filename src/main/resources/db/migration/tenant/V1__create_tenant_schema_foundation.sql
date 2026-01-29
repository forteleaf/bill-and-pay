-- =============================================================================
-- Bill&Pay Tenant Schema - Foundation
-- =============================================================================
-- Description: Creates tenant-specific schema structure
-- This migration runs for each new tenant schema (e.g., tenant_korpay)
-- 
-- Key Components:
-- - organizations (ltree hierarchy: DISTRIBUTOR > AGENCY > DEALER > SELLER > VENDOR)
-- - users (authentication, role-based access)
-- - merchants (payment acceptance points)
-- - merchant_pg_mappings (MID to PG connection mapping)
-- =============================================================================

-- =============================================================================
-- Organizations Table (ltree hierarchy)
-- =============================================================================
-- 5-level hierarchy: DISTRIBUTOR > AGENCY > DEALER > SELLER > VENDOR
-- Uses ltree for efficient hierarchical queries
-- Path format: dist_001.agcy_001.deal_001.sell_001.vend_001
-- =============================================================================
CREATE TABLE organizations (
  id UUID PRIMARY KEY DEFAULT uuidv7(),
  
  -- Identity
  org_code VARCHAR(50) NOT NULL UNIQUE,
  name VARCHAR(200) NOT NULL,
  org_type VARCHAR(20) NOT NULL,
  
  -- ltree hierarchy
  path ltree NOT NULL UNIQUE,
  parent_id UUID,
  level INTEGER NOT NULL,
  
  -- Business details
  business_number VARCHAR(20),
  business_name VARCHAR(200),
  representative_name VARCHAR(100),
  
  -- Contact
  email VARCHAR(255),
  phone VARCHAR(20),
  address TEXT,
  
  -- Status
  status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  
  -- Configuration
  config JSONB,
  
  -- Audit
  created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted_at TIMESTAMPTZ,
  
  CONSTRAINT organizations_type_check CHECK (org_type IN ('DISTRIBUTOR', 'AGENCY', 'DEALER', 'SELLER', 'VENDOR')),
  CONSTRAINT organizations_status_check CHECK (status IN ('ACTIVE', 'SUSPENDED', 'DELETED')),
  CONSTRAINT organizations_level_check CHECK (level BETWEEN 1 AND 5),
  CONSTRAINT organizations_parent_fk FOREIGN KEY (parent_id) REFERENCES organizations(id)
);

-- GiST index for ltree path queries (ancestor/descendant operations)
CREATE INDEX idx_organizations_path_gist ON organizations USING GIST(path);

-- B-tree indexes for common lookups
CREATE INDEX idx_organizations_parent_id ON organizations(parent_id) WHERE parent_id IS NOT NULL;
CREATE INDEX idx_organizations_org_code ON organizations(org_code);
CREATE INDEX idx_organizations_org_type ON organizations(org_type);
CREATE INDEX idx_organizations_status ON organizations(status) WHERE status = 'ACTIVE';
CREATE INDEX idx_organizations_level ON organizations(level);

COMMENT ON TABLE organizations IS 'ltree-based 5-level organization hierarchy';
COMMENT ON COLUMN organizations.path IS 'ltree path: dist_001.agcy_001.deal_001.sell_001.vend_001';
COMMENT ON COLUMN organizations.level IS '1=DISTRIBUTOR, 2=AGENCY, 3=DEALER, 4=SELLER, 5=VENDOR';
COMMENT ON COLUMN organizations.config IS 'JSON: {fee_rates, settlement_cycle, features}';

-- =============================================================================
-- Users Table
-- =============================================================================
-- User authentication and authorization
-- Linked to organizations for hierarchical access control
-- =============================================================================
CREATE TABLE users (
  id UUID PRIMARY KEY DEFAULT uuidv7(),
  
  -- Identity
  username VARCHAR(100) NOT NULL UNIQUE,
  email VARCHAR(255) NOT NULL UNIQUE,
  
  -- Authentication (hash password in application layer)
  password_hash VARCHAR(255) NOT NULL,
  
  -- Organization link
  org_id UUID NOT NULL,
  org_path ltree NOT NULL,
  
  -- Profile
  full_name VARCHAR(200) NOT NULL,
  phone VARCHAR(20),
  
  -- Authorization
  role VARCHAR(50) NOT NULL,
  permissions JSONB,
  
  -- Security
  two_factor_enabled BOOLEAN NOT NULL DEFAULT FALSE,
  two_factor_secret VARCHAR(255),
  last_login_at TIMESTAMPTZ,
  password_changed_at TIMESTAMPTZ,
  
  -- Status
  status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  
  -- Audit
  created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted_at TIMESTAMPTZ,
  
  CONSTRAINT users_status_check CHECK (status IN ('ACTIVE', 'SUSPENDED', 'DELETED')),
  CONSTRAINT users_org_fk FOREIGN KEY (org_id) REFERENCES organizations(id)
);

-- GiST index for hierarchical access control
CREATE INDEX idx_users_org_path_gist ON users USING GIST(org_path);

-- B-tree indexes
CREATE INDEX idx_users_org_id ON users(org_id);
CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_status ON users(status) WHERE status = 'ACTIVE';
CREATE INDEX idx_users_role ON users(role);

COMMENT ON TABLE users IS 'User accounts with organization-based access control';
COMMENT ON COLUMN users.org_path IS 'Cached ltree path from organizations for quick hierarchy checks';
COMMENT ON COLUMN users.role IS 'Examples: ADMIN, MANAGER, VIEWER, API_USER';
COMMENT ON COLUMN users.permissions IS 'JSON array of granular permissions';
COMMENT ON COLUMN users.two_factor_enabled IS 'Required for DISTRIBUTOR (MASTER) accounts';

-- =============================================================================
-- Merchants Table
-- =============================================================================
-- Payment acceptance points (stores, websites, apps)
-- Belongs to organizations (typically SELLER or VENDOR level)
-- =============================================================================
CREATE TABLE merchants (
  id UUID PRIMARY KEY DEFAULT uuidv7(),
  
  -- Identity
  merchant_code VARCHAR(50) NOT NULL UNIQUE,
  name VARCHAR(200) NOT NULL,
  
  -- Organization link
  org_id UUID NOT NULL,
  org_path ltree NOT NULL,
  
  -- Business details
  business_number VARCHAR(20),
  business_type VARCHAR(50),
  
  -- Contact
  contact_name VARCHAR(100),
  contact_email VARCHAR(255),
  contact_phone VARCHAR(20),
  
  -- Location
  address TEXT,
  
  -- Configuration
  config JSONB,
  
  -- Status
  status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  
  -- Audit
  created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted_at TIMESTAMPTZ,
  
  CONSTRAINT merchants_status_check CHECK (status IN ('ACTIVE', 'SUSPENDED', 'DELETED')),
  CONSTRAINT merchants_org_fk FOREIGN KEY (org_id) REFERENCES organizations(id)
);

-- GiST index for hierarchical queries
CREATE INDEX idx_merchants_org_path_gist ON merchants USING GIST(org_path);

-- B-tree indexes
CREATE INDEX idx_merchants_org_id ON merchants(org_id);
CREATE INDEX idx_merchants_merchant_code ON merchants(merchant_code);
CREATE INDEX idx_merchants_status ON merchants(status) WHERE status = 'ACTIVE';

COMMENT ON TABLE merchants IS 'Payment acceptance points (stores, websites, apps)';
COMMENT ON COLUMN merchants.org_path IS 'Cached ltree path for hierarchical access control';
COMMENT ON COLUMN merchants.config IS 'JSON: {operating_hours, categories, features}';

-- =============================================================================
-- Merchant PG Mappings Table
-- =============================================================================
-- Maps merchants to PG connections with MID (Merchant ID)
-- Per PRD-04: KORPAY uses MID (mid) 1:1 with terminal (catId)
-- =============================================================================
CREATE TABLE merchant_pg_mappings (
  id UUID PRIMARY KEY DEFAULT uuidv7(),
  
  -- Relationships
  merchant_id UUID NOT NULL,
  pg_connection_id UUID NOT NULL,
  
  -- PG credentials
  mid VARCHAR(50) NOT NULL,
  terminal_id VARCHAR(50),
  
  -- KORPAY specific (per PRD-04, PRD-06)
  cat_id VARCHAR(50),
  
  -- Configuration
  config JSONB,
  
  -- Status
  status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  
  -- Audit
  created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  
  CONSTRAINT merchant_pg_mappings_status_check CHECK (status IN ('ACTIVE', 'INACTIVE')),
  CONSTRAINT merchant_pg_mappings_merchant_fk FOREIGN KEY (merchant_id) REFERENCES merchants(id),
  CONSTRAINT merchant_pg_mappings_merchant_pg_unique UNIQUE (merchant_id, pg_connection_id),
  CONSTRAINT merchant_pg_mappings_mid_pg_unique UNIQUE (mid, pg_connection_id)
);

CREATE INDEX idx_merchant_pg_mappings_merchant_id ON merchant_pg_mappings(merchant_id);
CREATE INDEX idx_merchant_pg_mappings_pg_connection_id ON merchant_pg_mappings(pg_connection_id);
CREATE INDEX idx_merchant_pg_mappings_mid ON merchant_pg_mappings(mid);
CREATE INDEX idx_merchant_pg_mappings_status ON merchant_pg_mappings(status) WHERE status = 'ACTIVE';

COMMENT ON TABLE merchant_pg_mappings IS 'Maps merchants to PG connections with MID';
COMMENT ON COLUMN merchant_pg_mappings.mid IS 'PG Merchant ID (KORPAY: 1:1 with catId)';
COMMENT ON COLUMN merchant_pg_mappings.cat_id IS 'KORPAY terminal ID (catId) - 1:1 with MID per PRD-04';
COMMENT ON COLUMN merchant_pg_mappings.config IS 'JSON: PG-specific settings, fee overrides';
