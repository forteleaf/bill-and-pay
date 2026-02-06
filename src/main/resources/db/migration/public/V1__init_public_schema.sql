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

COMMENT ON TABLE public.tenants IS '테넌트 (멀티테넌시 단위)';
COMMENT ON COLUMN public.tenants.id IS '테넌트 고유 식별자';
COMMENT ON COLUMN public.tenants.name IS '테넌트 이름';
COMMENT ON COLUMN public.tenants.schema_name IS '테넌트 전용 DB 스키마명 (tenant_xxx)';
COMMENT ON COLUMN public.tenants.status IS '상태 (ACTIVE, SUSPENDED, DELETED)';
COMMENT ON COLUMN public.tenants.contact_email IS '테넌트 담당자 이메일';
COMMENT ON COLUMN public.tenants.contact_phone IS '테넌트 담당자 전화번호';
COMMENT ON COLUMN public.tenants.config IS '테넌트별 설정 (JSONB)';
COMMENT ON COLUMN public.tenants.created_at IS '생성일시';
COMMENT ON COLUMN public.tenants.updated_at IS '수정일시';
COMMENT ON COLUMN public.tenants.deleted_at IS '삭제일시 (소프트 삭제)';

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

COMMENT ON TABLE public.pg_connections IS 'PG사 연결 설정';
COMMENT ON COLUMN public.pg_connections.id IS 'PG 연결 고유 ID (BIGINT 자동증가)';
COMMENT ON COLUMN public.pg_connections.pg_code IS 'PG사 고유 코드 (예: KORPAY, NICE)';
COMMENT ON COLUMN public.pg_connections.pg_name IS 'PG사 이름';
COMMENT ON COLUMN public.pg_connections.api_base_url IS 'PG API 기본 URL';
COMMENT ON COLUMN public.pg_connections.webhook_base_url IS '웹훅 수신 기본 URL';
COMMENT ON COLUMN public.pg_connections.tenant_id IS '소속 테넌트 ID (NULL이면 공용)';
COMMENT ON COLUMN public.pg_connections.credentials IS 'PG 인증 정보 (API Key, Secret 등 - JSONB)';
COMMENT ON COLUMN public.pg_connections.config IS 'PG 연결 추가 설정 (JSONB)';
COMMENT ON COLUMN public.pg_connections.status IS '상태 (ACTIVE, INACTIVE, MAINTENANCE)';
COMMENT ON COLUMN public.pg_connections.created_at IS '생성일시';
COMMENT ON COLUMN public.pg_connections.updated_at IS '수정일시';

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

COMMENT ON TABLE public.holidays IS '공휴일 정보 (정산일 계산용)';
COMMENT ON COLUMN public.holidays.id IS '공휴일 고유 ID (UUID v7)';
COMMENT ON COLUMN public.holidays.holiday_date IS '공휴일 날짜';
COMMENT ON COLUMN public.holidays.name IS '공휴일 이름';
COMMENT ON COLUMN public.holidays.country_code IS '국가 코드 (ISO 3166-1 alpha-2, 기본값: KR)';
COMMENT ON COLUMN public.holidays.is_recurring IS '매년 반복 여부';
COMMENT ON COLUMN public.holidays.created_at IS '생성일시';

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

COMMENT ON TABLE public.organizations IS '영업점 조직 계층 (public - ltree 기반 5단계)';
COMMENT ON COLUMN public.organizations.id IS '영업점 고유 ID (UUID v7)';
COMMENT ON COLUMN public.organizations.org_code IS '영업점 코드 (고유)';
COMMENT ON COLUMN public.organizations.name IS '영업점 이름';
COMMENT ON COLUMN public.organizations.org_type IS '조직 유형 (DISTRIBUTOR, AGENCY, DEALER, SELLER, VENDOR)';
COMMENT ON COLUMN public.organizations.path IS 'ltree 계층 경로 (예: dist_001.agcy_001.deal_001)';
COMMENT ON COLUMN public.organizations.parent_id IS '상위 조직 ID (최상위는 NULL)';
COMMENT ON COLUMN public.organizations.level IS '계층 레벨 (1:총판 ~ 5:벤더)';
COMMENT ON COLUMN public.organizations.business_number IS '사업자등록번호 (숫자만)';
COMMENT ON COLUMN public.organizations.business_name IS '사업자명 (상호)';
COMMENT ON COLUMN public.organizations.representative_name IS '대표자명';
COMMENT ON COLUMN public.organizations.email IS '이메일';
COMMENT ON COLUMN public.organizations.phone IS '전화번호 (숫자만)';
COMMENT ON COLUMN public.organizations.address IS '주소';
COMMENT ON COLUMN public.organizations.status IS '상태 (ACTIVE, SUSPENDED, DELETED)';
COMMENT ON COLUMN public.organizations.config IS '영업점 추가 설정 (JSONB)';
COMMENT ON COLUMN public.organizations.created_at IS '생성일시';
COMMENT ON COLUMN public.organizations.updated_at IS '수정일시';
COMMENT ON COLUMN public.organizations.deleted_at IS '삭제일시 (소프트 삭제)';

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

COMMENT ON TABLE public.users IS '인증 사용자 (public - 로그인 계정)';
COMMENT ON COLUMN public.users.id IS '사용자 고유 ID (UUID)';
COMMENT ON COLUMN public.users.username IS '로그인 ID (고유)';
COMMENT ON COLUMN public.users.password IS '암호화된 비밀번호';
COMMENT ON COLUMN public.users.tenant_id IS '소속 테넌트 ID';
COMMENT ON COLUMN public.users.status IS '상태 (ACTIVE, SUSPENDED, DELETED)';
COMMENT ON COLUMN public.users.created_at IS '생성일시';
COMMENT ON COLUMN public.users.updated_at IS '수정일시';
