-- =============================================================================
-- Bill&Pay Public Schema - Foundation
-- =============================================================================
-- Description: Creates shared infrastructure used across all tenants
-- - ltree extension for hierarchical organization paths
-- - uuidv7() function for time-ordered UUIDs
-- - tenants registry
-- - pg_connections (PG company configurations)
-- - holidays (business day calculations)
-- =============================================================================

-- Enable ltree extension for hierarchical paths
CREATE EXTENSION IF NOT EXISTS ltree;

-- =============================================================================
-- UUID v7 Generation Function
-- =============================================================================
-- Creates time-ordered UUIDs for better indexing performance
-- Format: timestamp (48 bits) + version (4 bits) + random (76 bits)
-- =============================================================================
CREATE OR REPLACE FUNCTION uuidv7() RETURNS uuid AS $$
DECLARE
  unix_ts_ms BIGINT;
  uuid_bytes BYTEA;
BEGIN
  unix_ts_ms = FLOOR(EXTRACT(EPOCH FROM clock_timestamp()) * 1000);
  uuid_bytes = 
    substring(int8send(unix_ts_ms) from 3 for 6) || -- 48 bits timestamp
    substring(int4send(((random() * 65535)::int & 4095) | 28672) from 3 for 2) || -- version 7
    gen_random_bytes(8); -- random bits
  RETURN encode(uuid_bytes, 'hex')::uuid;
END;
$$ LANGUAGE plpgsql VOLATILE;

COMMENT ON FUNCTION uuidv7() IS 'Generates time-ordered UUID v7 for better B-tree indexing';

-- =============================================================================
-- Tenants Table
-- =============================================================================
-- Central registry of all tenants in the system
-- Each tenant gets its own schema (tenant_{id})
-- =============================================================================
CREATE TABLE public.tenants (
  id VARCHAR(50) PRIMARY KEY,
  name VARCHAR(200) NOT NULL,
  schema_name VARCHAR(63) NOT NULL UNIQUE,
  status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  
  -- Contact information
  contact_email VARCHAR(255),
  contact_phone VARCHAR(20),
  
  -- Configuration
  config JSONB,
  
  -- Audit
  created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted_at TIMESTAMPTZ,
  
  CONSTRAINT tenants_status_check CHECK (status IN ('ACTIVE', 'SUSPENDED', 'DELETED'))
);

CREATE INDEX idx_tenants_status ON public.tenants(status) WHERE status = 'ACTIVE';
CREATE INDEX idx_tenants_schema_name ON public.tenants(schema_name);

COMMENT ON TABLE public.tenants IS 'Multi-tenant registry - each tenant gets schema tenant_{id}';
COMMENT ON COLUMN public.tenants.schema_name IS 'PostgreSQL schema name (e.g., tenant_korpay)';
COMMENT ON COLUMN public.tenants.config IS 'JSON configuration: {timezone, currency, features}';

-- =============================================================================
-- PG Connections Table
-- =============================================================================
-- Stores PG (Payment Gateway) company connection details
-- Shared across tenants for PG integration configuration
-- =============================================================================
CREATE TABLE public.pg_connections (
  id UUID PRIMARY KEY DEFAULT uuidv7(),
  pg_code VARCHAR(20) NOT NULL UNIQUE,
  pg_name VARCHAR(100) NOT NULL,
  
  -- Connection details
  api_base_url VARCHAR(500) NOT NULL,
  webhook_base_url VARCHAR(500),
  
  -- Credentials (encrypt in application layer)
  credentials JSONB NOT NULL,
  
  -- Configuration
  config JSONB,
  
  -- Status
  status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  
  -- Audit
  created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  
  CONSTRAINT pg_connections_status_check CHECK (status IN ('ACTIVE', 'INACTIVE', 'MAINTENANCE'))
);

CREATE INDEX idx_pg_connections_code ON public.pg_connections(pg_code);
CREATE INDEX idx_pg_connections_status ON public.pg_connections(status) WHERE status = 'ACTIVE';

COMMENT ON TABLE public.pg_connections IS 'Payment Gateway connection configurations (KORPAY, NICE, etc)';
COMMENT ON COLUMN public.pg_connections.pg_code IS 'Unique PG identifier (e.g., KORPAY, NICE_PAY)';
COMMENT ON COLUMN public.pg_connections.credentials IS 'Encrypted API keys, secrets: {api_key, secret_key, merchant_id}';
COMMENT ON COLUMN public.pg_connections.config IS 'PG-specific settings: {timeout_ms, retry_count, webhook_secret}';

-- =============================================================================
-- Holidays Table
-- =============================================================================
-- Business day calculation support
-- Used for settlement date calculations (T+N business days)
-- =============================================================================
CREATE TABLE public.holidays (
  id UUID PRIMARY KEY DEFAULT uuidv7(),
  holiday_date DATE NOT NULL,
  name VARCHAR(100) NOT NULL,
  country_code VARCHAR(2) NOT NULL DEFAULT 'KR',
  is_recurring BOOLEAN NOT NULL DEFAULT FALSE,
  
  -- Audit
  created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  
  CONSTRAINT holidays_date_country_unique UNIQUE (holiday_date, country_code)
);

CREATE INDEX idx_holidays_date ON public.holidays(holiday_date);
CREATE INDEX idx_holidays_country ON public.holidays(country_code, holiday_date);

COMMENT ON TABLE public.holidays IS 'Holiday calendar for business day calculations';
COMMENT ON COLUMN public.holidays.is_recurring IS 'True for recurring holidays (e.g., every Jan 1)';
