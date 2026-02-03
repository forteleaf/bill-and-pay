-- =============================================================================
-- Bill&Pay Public Schema - 통합 초기화
-- =============================================================================
-- 버전: 1.0 (마이그레이션 통합)
-- 설명: 모든 테넌트가 공유하는 인프라스트럭처
-- =============================================================================

-- Enable ltree extension for hierarchical paths
CREATE EXTENSION IF NOT EXISTS ltree;

-- =============================================================================
-- UUID v7 Generation Function
-- =============================================================================
CREATE OR REPLACE FUNCTION uuidv7() RETURNS uuid AS $$
DECLARE
  unix_ts_ms BIGINT;
  uuid_bytes BYTEA;
BEGIN
  unix_ts_ms = FLOOR(EXTRACT(EPOCH FROM clock_timestamp()) * 1000);
  uuid_bytes =
    substring(int8send(unix_ts_ms) from 3 for 6) ||
    substring(int4send(((random() * 65535)::int & 4095) | 28672) from 3 for 2) ||
    gen_random_bytes(8);
  RETURN encode(uuid_bytes, 'hex')::uuid;
END;
$$ LANGUAGE plpgsql VOLATILE;

-- =============================================================================
-- Tenants Table
-- =============================================================================
CREATE TABLE public.tenants (
  id VARCHAR(50) PRIMARY KEY,
  name VARCHAR(200) NOT NULL,
  schema_name VARCHAR(63) NOT NULL UNIQUE,
  status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  contact_email VARCHAR(255),
  contact_phone VARCHAR(20),
  config JSONB,
  created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted_at TIMESTAMPTZ,
  CONSTRAINT tenants_status_check CHECK (status IN ('ACTIVE', 'SUSPENDED', 'DELETED'))
);

CREATE INDEX idx_tenants_status ON public.tenants(status) WHERE status = 'ACTIVE';
CREATE INDEX idx_tenants_schema_name ON public.tenants(schema_name);

-- =============================================================================
-- PG Connections Table (BIGINT ID)
-- =============================================================================
CREATE TABLE public.pg_connections (
  id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  pg_code VARCHAR(20) NOT NULL UNIQUE,
  pg_name VARCHAR(100) NOT NULL,
  api_base_url VARCHAR(500) NOT NULL,
  webhook_base_url VARCHAR(500),
  tenant_id VARCHAR(50) REFERENCES public.tenants(id) ON DELETE SET NULL,
  credentials JSONB NOT NULL,
  config JSONB,
  status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT pg_connections_status_check CHECK (status IN ('ACTIVE', 'INACTIVE', 'MAINTENANCE'))
);

CREATE INDEX idx_pg_connections_code ON public.pg_connections(pg_code);
CREATE INDEX idx_pg_connections_status ON public.pg_connections(status) WHERE status = 'ACTIVE';
CREATE INDEX idx_pg_connections_tenant_id ON public.pg_connections(tenant_id) WHERE tenant_id IS NOT NULL;

COMMENT ON TABLE public.pg_connections IS 'Payment Gateway connection configurations';
COMMENT ON COLUMN public.pg_connections.id IS 'Auto-incremented BIGINT primary key';

-- =============================================================================
-- Holidays Table
-- =============================================================================
CREATE TABLE public.holidays (
  id UUID PRIMARY KEY DEFAULT uuidv7(),
  holiday_date DATE NOT NULL,
  name VARCHAR(100) NOT NULL,
  country_code VARCHAR(2) NOT NULL DEFAULT 'KR',
  is_recurring BOOLEAN NOT NULL DEFAULT FALSE,
  created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT holidays_date_country_unique UNIQUE (holiday_date, country_code)
);

CREATE INDEX idx_holidays_date ON public.holidays(holiday_date);
CREATE INDEX idx_holidays_country ON public.holidays(country_code, holiday_date);

-- =============================================================================
-- Organizations Table (ltree hierarchy)
-- =============================================================================
CREATE TABLE public.organizations (
  id UUID PRIMARY KEY DEFAULT uuidv7(),
  org_code VARCHAR(50) NOT NULL UNIQUE,
  name VARCHAR(200) NOT NULL,
  org_type VARCHAR(20) NOT NULL,
  path ltree NOT NULL UNIQUE,
  parent_id UUID REFERENCES public.organizations(id),
  level INTEGER NOT NULL,
  business_number VARCHAR(20),
  business_name VARCHAR(200),
  representative_name VARCHAR(100),
  email VARCHAR(255),
  phone VARCHAR(20),
  address TEXT,
  status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  config JSONB,
  created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted_at TIMESTAMPTZ,
  CONSTRAINT organizations_type_check CHECK (org_type IN ('DISTRIBUTOR', 'AGENCY', 'DEALER', 'SELLER', 'VENDOR')),
  CONSTRAINT organizations_level_check CHECK (level >= 1 AND level <= 5),
  CONSTRAINT organizations_status_check CHECK (status IN ('ACTIVE', 'SUSPENDED', 'DELETED'))
);

CREATE INDEX idx_organizations_path_gist ON public.organizations USING GIST(path);
CREATE INDEX idx_organizations_level ON public.organizations(level);
CREATE INDEX idx_organizations_org_type ON public.organizations(org_type);
CREATE INDEX idx_organizations_org_code ON public.organizations(org_code);
CREATE INDEX idx_organizations_parent_id ON public.organizations(parent_id) WHERE parent_id IS NOT NULL;
CREATE INDEX idx_organizations_status ON public.organizations(status) WHERE status = 'ACTIVE';

-- =============================================================================
-- Users Table (Authentication)
-- =============================================================================
CREATE TABLE public.users (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  username VARCHAR(50) NOT NULL UNIQUE,
  password VARCHAR(255) NOT NULL,
  tenant_id VARCHAR(50) NOT NULL REFERENCES public.tenants(id),
  status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT users_status_check CHECK (status IN ('ACTIVE', 'SUSPENDED', 'DELETED'))
);

CREATE INDEX idx_users_tenant_id ON public.users(tenant_id);
CREATE INDEX idx_users_username ON public.users(username);
