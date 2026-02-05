# PRD-05: 데이터베이스 스키마

## 1. 개요

Bill&Pay 시스템의 PostgreSQL 18 데이터베이스 스키마를 정의합니다.

---

## 2. 스키마 구조

### 2.1 멀티테넌시 스키마 분리

```
PostgreSQL Database: billpay
│
├── public (공통 스키마)
│   ├── tenants                 -- 테넌트(총판) 목록
│   ├── pg_connections          -- PG사 연동 설정
│   ├── holidays                -- 공휴일 (영업일 계산용)
│   ├── organizations           -- 조직 구조 (공용)
│   ├── users                   -- 인증 사용자 (공용)
│   └── demo_requests           -- 데모 요청
│
├── tenant_001 (총판A 스키마)
│   ├── organizations           -- 조직 구조 (테넌트별)
│   ├── business_entities       -- 사업자 정보
│   ├── merchants               -- 가맹점
│   ├── merchant_pg_mappings    -- 가맹점-PG 매핑
│   ├── payment_methods         -- 결제 수단 마스터
│   ├── card_companies          -- 카드사 마스터
│   ├── transactions            -- 거래 현재 상태
│   ├── transaction_events      -- 거래 이벤트 이력 (파티셔닝)
│   ├── settlements             -- 정산 원장 (이벤트 기준)
│   ├── settlement_batches      -- 정산 배치
│   ├── fee_configurations      -- 수수료 설정
│   ├── contacts                -- 담당자 정보
│   ├── settlement_accounts     -- 정산계좌 정보
│   ├── terminals               -- 단말기 관리
│   ├── users                   -- 사용자 (테넌트별)
│   ├── api_keys                -- API 키 관리
│   ├── webhook_logs            -- 웹훅 로그
│   ├── audit_logs              -- 감사 로그
│   ├── settlement_reports      -- 정산 리포트
│   └── merchant_org_history    -- 가맹점 이관 이력
│
├── tenant_002 (총판B 스키마)
│   └── ... (동일 구조)
│
└── tenant_NNN
    └── ...
```

---

## 3. 공통 스키마 (public)

### 3.1 UUID v7 생성 함수

```sql
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
```

### 3.2 tenants - 테넌트 목록

```sql
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
```

### 3.3 pg_connections - PG사 연동

```sql
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
```

**credentials JSONB 구조 예시**:
```json
{
  "api_key": "encrypted_key",
  "api_secret": "encrypted_secret",
  "merchant_id": "KORPAY_MID",
  "webhook_secret": "webhook_verification_key"
}
```

### 3.4 holidays - 공휴일

```sql
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
```

### 3.5 organizations - 조직 (공용)

```sql
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
```

### 3.6 users - 인증 사용자 (공용)

```sql
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
```

### 3.7 demo_requests - 데모 요청

```sql
CREATE TABLE public.demo_requests (
  id UUID PRIMARY KEY DEFAULT uuidv7(),
  company_name VARCHAR(200) NOT NULL,
  contact_name VARCHAR(100) NOT NULL,
  email VARCHAR(255) NOT NULL,
  phone VARCHAR(20) NOT NULL,
  company_size VARCHAR(50),
  industry VARCHAR(100),
  message TEXT,
  status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
  processed_at TIMESTAMPTZ,
  processed_by VARCHAR(100),
  notes TEXT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT demo_requests_status_check CHECK (status IN ('PENDING', 'CONTACTED', 'DEMO_SCHEDULED', 'COMPLETED', 'REJECTED'))
);

CREATE INDEX idx_demo_requests_status ON public.demo_requests(status);
CREATE INDEX idx_demo_requests_email ON public.demo_requests(email);
CREATE INDEX idx_demo_requests_created_at ON public.demo_requests(created_at DESC);
```

---

## 4. 테넌트 스키마 (tenant_xxx)

### 4.1 ENUM Types

```sql
CREATE TYPE account_status AS ENUM ('ACTIVE', 'INACTIVE', 'PENDING_VERIFICATION');
CREATE TYPE contact_entity_type AS ENUM ('BUSINESS_ENTITY', 'MERCHANT');
CREATE TYPE contact_role AS ENUM ('PRIMARY', 'SECONDARY', 'SETTLEMENT', 'TECHNICAL');
CREATE TYPE user_status AS ENUM ('ACTIVE', 'INACTIVE', 'SUSPENDED', 'PENDING');
```

### 4.2 organizations - 조직 (테넌트별)

```sql
CREATE TABLE organizations (
  id UUID PRIMARY KEY DEFAULT uuidv7(),
  org_code VARCHAR(50) NOT NULL UNIQUE,
  name VARCHAR(200) NOT NULL,
  org_type VARCHAR(20) NOT NULL,
  path public.ltree NOT NULL UNIQUE,
  parent_id UUID REFERENCES organizations(id),
  level INTEGER NOT NULL,
  business_number VARCHAR(20),
  business_name VARCHAR(200),
  representative_name VARCHAR(100),
  email VARCHAR(255),
  phone VARCHAR(20),
  address TEXT,
  status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  config JSONB,
  business_entity_id UUID,
  created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted_at TIMESTAMPTZ,
  CONSTRAINT organizations_type_check CHECK (org_type IN ('DISTRIBUTOR', 'AGENCY', 'DEALER', 'SELLER', 'VENDOR')),
  CONSTRAINT organizations_level_check CHECK (level >= 1 AND level <= 5),
  CONSTRAINT organizations_status_check CHECK (status IN ('ACTIVE', 'SUSPENDED', 'DELETED')),
  CONSTRAINT organizations_distributor_level CHECK ((org_type = 'DISTRIBUTOR' AND level = 1) OR org_type <> 'DISTRIBUTOR'),
  CONSTRAINT organizations_parent_required CHECK ((level = 1 AND parent_id IS NULL) OR (level > 1 AND parent_id IS NOT NULL)),
  CONSTRAINT organizations_path_level_consistency CHECK (public.nlevel(path) = level)
);

CREATE INDEX idx_organizations_path_gist ON organizations USING GIST(path);
CREATE INDEX idx_organizations_level ON organizations(level);
CREATE INDEX idx_organizations_org_type ON organizations(org_type);
CREATE INDEX idx_organizations_status ON organizations(status) WHERE status = 'ACTIVE';
```

### 4.3 business_entities - 사업자 정보

**조직(organizations)과 사업자 정보 분리**. 동일 사업자번호가 여러 조직 유형으로 등록될 수 있음.

```sql
CREATE TABLE business_entities (
  id UUID PRIMARY KEY DEFAULT uuidv7(),
  business_type VARCHAR(20) NOT NULL,
  business_number VARCHAR(12),
  corporate_number VARCHAR(14),
  business_name VARCHAR(200) NOT NULL,
  representative_name VARCHAR(100) NOT NULL,
  open_date DATE,
  business_address TEXT,
  actual_address TEXT,
  business_category VARCHAR(100),
  business_sub_category VARCHAR(100),
  main_phone VARCHAR(20),
  manager_name VARCHAR(100),
  manager_phone VARCHAR(20),
  email VARCHAR(255),
  created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted_at TIMESTAMPTZ,
  CONSTRAINT business_entities_type_check CHECK (business_type IN ('CORPORATION', 'INDIVIDUAL', 'NON_BUSINESS'))
);

-- FK for organizations.business_entity_id
ALTER TABLE organizations ADD CONSTRAINT organizations_business_entity_fk
  FOREIGN KEY (business_entity_id) REFERENCES business_entities(id);
```

**business_type 유형**:

| 유형 | 코드 | 설명 |
|------|------|------|
| 법인사업자 | CORPORATION | 법인등록번호 필수 |
| 개인사업자 | INDIVIDUAL | 사업자번호 필수 |
| 비사업자 | NON_BUSINESS | 사업자번호 없음 |

### 4.4 contacts - 담당자 정보

```sql
CREATE TABLE contacts (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  name VARCHAR(100) NOT NULL,
  phone VARCHAR(20),
  email VARCHAR(255),
  role contact_role NOT NULL DEFAULT 'PRIMARY',
  entity_type contact_entity_type NOT NULL,
  entity_id UUID NOT NULL,
  is_primary BOOLEAN NOT NULL DEFAULT FALSE,
  created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted_at TIMESTAMPTZ
);

CREATE INDEX idx_contacts_entity ON contacts(entity_type, entity_id);
CREATE INDEX idx_contacts_primary ON contacts(entity_type, entity_id, is_primary) WHERE is_primary = TRUE;
```

**담당자 역할(Role)**:

| Role | 한글명 | 설명 |
|------|--------|------|
| PRIMARY | 주담당자 | 주요 연락 담당자 |
| SECONDARY | 부담당자 | 보조 담당자 |
| SETTLEMENT | 정산담당 | 정산/결제 관련 담당자 |
| TECHNICAL | 기술담당 | 기술 지원 담당자 |

### 4.5 settlement_accounts - 정산계좌 정보

```sql
CREATE TABLE settlement_accounts (
  id UUID PRIMARY KEY DEFAULT uuidv7(),
  bank_code VARCHAR(10) NOT NULL,
  bank_name VARCHAR(50) NOT NULL,
  account_number VARCHAR(50) NOT NULL,
  account_holder VARCHAR(100) NOT NULL,
  entity_type contact_entity_type NOT NULL,
  entity_id UUID NOT NULL,
  is_primary BOOLEAN NOT NULL DEFAULT FALSE,
  status account_status NOT NULL DEFAULT 'PENDING_VERIFICATION',
  verified_at TIMESTAMPTZ,
  memo TEXT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted_at TIMESTAMPTZ
);

CREATE INDEX idx_settlement_accounts_entity ON settlement_accounts(entity_type, entity_id);
CREATE INDEX idx_settlement_accounts_primary ON settlement_accounts(entity_type, entity_id, is_primary)
  WHERE is_primary = TRUE AND deleted_at IS NULL;
```

**계좌 상태(Status)**:

| Status | 한글명 | 설명 |
|--------|--------|------|
| ACTIVE | 사용중 | 검증 완료, 정산 가능 |
| INACTIVE | 미사용 | 비활성화 상태 |
| PENDING_VERIFICATION | 검증대기 | 계좌 검증 대기 중 |

**주요 은행코드**:

| 코드 | 은행명 |
|------|--------|
| 004 | KB국민은행 |
| 011 | NH농협은행 |
| 020 | 우리은행 |
| 088 | 신한은행 |
| 081 | 하나은행 |
| 003 | IBK기업은행 |

### 4.6 merchants - 가맹점

```sql
CREATE TABLE merchants (
  id UUID PRIMARY KEY DEFAULT uuidv7(),
  merchant_code VARCHAR(50) NOT NULL UNIQUE,
  name VARCHAR(200) NOT NULL,
  org_id UUID NOT NULL REFERENCES organizations(id),
  org_path public.ltree NOT NULL,
  business_number VARCHAR(20),
  business_type VARCHAR(50),
  contact_name VARCHAR(100),
  contact_email VARCHAR(255),
  contact_phone VARCHAR(20),
  address TEXT,
  config JSONB,
  status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted_at TIMESTAMPTZ,
  CONSTRAINT merchants_status_check CHECK (status IN ('ACTIVE', 'SUSPENDED', 'DELETED'))
);

CREATE INDEX idx_merchants_org_path_gist ON merchants USING GIST(org_path);
CREATE INDEX idx_merchants_org_id ON merchants(org_id);
CREATE INDEX idx_merchants_status ON merchants(status) WHERE status = 'ACTIVE';
```

### 4.7 merchant_pg_mappings - 가맹점 PG 매핑

```sql
CREATE TABLE merchant_pg_mappings (
  id UUID PRIMARY KEY DEFAULT uuidv7(),
  merchant_id UUID NOT NULL REFERENCES merchants(id),
  pg_connection_id BIGINT NOT NULL,
  mid VARCHAR(50) NOT NULL,
  terminal_id VARCHAR(50),
  cat_id VARCHAR(50),
  config JSONB,
  status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT merchant_pg_mappings_status_check CHECK (status IN ('ACTIVE', 'INACTIVE')),
  CONSTRAINT merchant_pg_mappings_mid_pg_unique UNIQUE (mid, pg_connection_id)
);

CREATE INDEX idx_merchant_pg_mappings_merchant_id ON merchant_pg_mappings(merchant_id);
CREATE INDEX idx_merchant_pg_mappings_pg_connection_id ON merchant_pg_mappings(pg_connection_id);
```

### 4.8 payment_methods - 결제 수단

```sql
CREATE TABLE payment_methods (
  id UUID PRIMARY KEY DEFAULT uuidv7(),
  method_code VARCHAR(20) NOT NULL UNIQUE,
  name VARCHAR(100) NOT NULL,
  category VARCHAR(20) NOT NULL,
  config JSONB,
  display_order INTEGER NOT NULL DEFAULT 0,
  status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT payment_methods_category_check CHECK (category IN ('CARD', 'BANK', 'VIRTUAL', 'OTHER')),
  CONSTRAINT payment_methods_status_check CHECK (status IN ('ACTIVE', 'INACTIVE'))
);
```

**기본 데이터**:

| method_code | name | category |
|-------------|------|----------|
| CREDIT | 신용카드 | CARD |
| DEBIT | 체크카드 | CARD |
| OVERSEAS | 해외카드 | CARD |
| TRANSFER | 계좌이체 | BANK |
| VIRTUAL | 가상계좌 | VIRTUAL |

### 4.9 card_companies - 카드사

```sql
CREATE TABLE card_companies (
  id UUID PRIMARY KEY DEFAULT uuidv7(),
  company_code VARCHAR(10) NOT NULL UNIQUE,
  company_name VARCHAR(100) NOT NULL,
  config JSONB,
  display_order INTEGER NOT NULL DEFAULT 0,
  status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT card_companies_status_check CHECK (status IN ('ACTIVE', 'INACTIVE'))
);
```

**기본 데이터**:

| company_code | company_name |
|--------------|--------------|
| BC | BC카드 |
| KB | KB국민카드 |
| SS | 삼성카드 |
| SH | 신한카드 |
| HD | 현대카드 |
| LT | 롯데카드 |
| HN | 하나카드 |
| WR | 우리카드 |
| NH | NH농협카드 |

### 4.10 terminals - 단말기

```sql
CREATE TABLE terminals (
  id UUID PRIMARY KEY DEFAULT uuidv7(),
  cat_id VARCHAR(50) NOT NULL UNIQUE,
  terminal_type VARCHAR(20) NOT NULL,
  merchant_id UUID NOT NULL REFERENCES merchants(id),
  organization_id UUID REFERENCES organizations(id),
  serial_number VARCHAR(100),
  model VARCHAR(100),
  manufacturer VARCHAR(100),
  install_address TEXT,
  install_date DATE,
  status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  config JSONB,
  last_transaction_at TIMESTAMPTZ,
  created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted_at TIMESTAMPTZ,
  CONSTRAINT terminals_type_check CHECK (terminal_type IN ('CAT', 'POS', 'MOBILE', 'KIOSK', 'ONLINE')),
  CONSTRAINT terminals_status_check CHECK (status IN ('ACTIVE', 'INACTIVE', 'SUSPENDED', 'TERMINATED'))
);

CREATE INDEX idx_terminals_merchant_id ON terminals(merchant_id);
CREATE INDEX idx_terminals_organization_id ON terminals(organization_id) WHERE organization_id IS NOT NULL;
CREATE INDEX idx_terminals_status ON terminals(status) WHERE status = 'ACTIVE';
CREATE INDEX idx_terminals_cat_id ON terminals(cat_id);
```

**단말기 유형**:

| terminal_type | 설명 |
|---------------|------|
| CAT | 신용카드 조회기 |
| POS | 포스 단말기 |
| MOBILE | 모바일 단말기 |
| KIOSK | 키오스크 |
| ONLINE | 온라인 |

### 4.11 transactions - 거래 현재 상태

```sql
CREATE TABLE transactions (
  id UUID PRIMARY KEY DEFAULT uuidv7(),
  transaction_id VARCHAR(100) NOT NULL UNIQUE,
  merchant_id UUID NOT NULL REFERENCES merchants(id),
  merchant_pg_mapping_id UUID NOT NULL REFERENCES merchant_pg_mappings(id),
  pg_connection_id BIGINT NOT NULL,
  org_path public.ltree NOT NULL,
  payment_method_id UUID NOT NULL REFERENCES payment_methods(id),
  card_company_id UUID REFERENCES card_companies(id),
  amount BIGINT NOT NULL,
  currency VARCHAR(3) NOT NULL DEFAULT 'KRW',
  status VARCHAR(20) NOT NULL,
  pg_transaction_id VARCHAR(200),
  approval_number VARCHAR(50),
  approved_at TIMESTAMPTZ,
  cancelled_at TIMESTAMPTZ,
  cat_id VARCHAR(50),
  metadata JSONB,
  created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT transactions_status_check CHECK (status IN ('PENDING', 'APPROVED', 'CANCELLED', 'PARTIAL_CANCELLED', 'FAILED')),
  CONSTRAINT transactions_amount_check CHECK (amount > 0)
);

CREATE INDEX idx_transactions_org_path_gist ON transactions USING GIST(org_path);
CREATE INDEX idx_transactions_merchant_id ON transactions(merchant_id);
CREATE INDEX idx_transactions_pg_connection_id ON transactions(pg_connection_id);
CREATE INDEX idx_transactions_status ON transactions(status);
CREATE INDEX idx_transactions_created_status ON transactions(created_at DESC, status);
CREATE INDEX idx_transactions_merchant_status_created ON transactions(merchant_id, status, created_at DESC);
CREATE INDEX idx_transactions_cat_id ON transactions(cat_id);

-- PG별 거래고유번호 유니크 인덱스
CREATE UNIQUE INDEX idx_transactions_pg_txn_unique
  ON transactions(pg_connection_id, pg_transaction_id)
  WHERE pg_transaction_id IS NOT NULL;

-- Covering 인덱스 (Index-Only Scan)
CREATE INDEX idx_transactions_pg_txn_lookup
  ON transactions(pg_connection_id, pg_transaction_id)
  INCLUDE (id, status, amount, merchant_id)
  WHERE pg_transaction_id IS NOT NULL;
```

**거래 상태**:

| status | 설명 |
|--------|------|
| PENDING | 대기 |
| APPROVED | 승인 |
| CANCELLED | 전액취소 |
| PARTIAL_CANCELLED | 부분취소 |
| FAILED | 실패 |

### 4.12 transaction_events - 거래 이벤트 이력 (파티셔닝)

```sql
CREATE TABLE transaction_events (
  id UUID NOT NULL DEFAULT uuidv7(),
  event_type VARCHAR(20) NOT NULL,
  event_sequence INTEGER NOT NULL,
  transaction_id UUID NOT NULL,
  merchant_id UUID NOT NULL,
  merchant_pg_mapping_id UUID NOT NULL,
  pg_connection_id BIGINT NOT NULL,
  org_path public.ltree NOT NULL,
  payment_method_id UUID NOT NULL,
  card_company_id UUID,
  amount BIGINT NOT NULL,
  currency VARCHAR(3) NOT NULL DEFAULT 'KRW',
  previous_status VARCHAR(20),
  new_status VARCHAR(20) NOT NULL,
  pg_transaction_id VARCHAR(200),
  approval_number VARCHAR(50),
  cat_id VARCHAR(50),
  metadata JSONB,
  occurred_at TIMESTAMPTZ NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT transaction_events_pk PRIMARY KEY (id, created_at),
  CONSTRAINT transaction_events_event_type_check CHECK (event_type IN ('APPROVAL', 'CANCEL', 'PARTIAL_CANCEL', 'REFUND')),
  CONSTRAINT transaction_events_amount_check CHECK (amount != 0),
  CONSTRAINT transaction_events_sequence_positive CHECK (event_sequence > 0),
  CONSTRAINT transaction_events_amount_sign_matches_type CHECK (
    (event_type = 'APPROVAL' AND amount > 0) OR
    (event_type IN ('CANCEL', 'PARTIAL_CANCEL', 'REFUND') AND amount < 0)
  ),
  CONSTRAINT transaction_events_occurred_at_not_future CHECK (occurred_at <= CURRENT_TIMESTAMP)
) PARTITION BY RANGE (created_at);

CREATE INDEX idx_transaction_events_org_path_gist ON transaction_events USING GIST(org_path);
CREATE INDEX idx_transaction_events_transaction_id ON transaction_events(transaction_id, event_sequence);
CREATE INDEX idx_transaction_events_merchant_id ON transaction_events(merchant_id, created_at DESC);
CREATE INDEX idx_transaction_events_event_type ON transaction_events(event_type, created_at DESC);
CREATE INDEX idx_transaction_events_pg_connection_id ON transaction_events(pg_connection_id, created_at DESC);
CREATE INDEX idx_transaction_events_cat_id ON transaction_events(cat_id, created_at DESC);
CREATE INDEX idx_transaction_events_occurred_at ON transaction_events(occurred_at DESC);
CREATE INDEX idx_transaction_events_pg_txn
  ON transaction_events(pg_connection_id, pg_transaction_id, created_at DESC)
  WHERE pg_transaction_id IS NOT NULL;

-- 일별 파티션 자동 생성 (과거 7일 ~ 향후 30일)
```

**이벤트 유형**:

| event_type | amount 부호 | 설명 |
|------------|-------------|------|
| APPROVAL | + (양수) | 최초 결제 승인 |
| CANCEL | - (음수) | 전액 취소 |
| PARTIAL_CANCEL | - (음수) | 부분 취소 |
| REFUND | - (음수) | 환불 |

### 4.13 settlements - 정산 원장 (복식부기)

```sql
CREATE TABLE settlements (
  id UUID PRIMARY KEY DEFAULT uuidv7(),
  settlement_batch_id UUID,
  transaction_event_id UUID NOT NULL,
  transaction_id UUID NOT NULL,
  merchant_id UUID NOT NULL REFERENCES merchants(id),
  org_path public.ltree NOT NULL,
  entity_id UUID NOT NULL,
  entity_type VARCHAR(20) NOT NULL,
  entity_path public.ltree NOT NULL,
  entry_type VARCHAR(10) NOT NULL,
  amount BIGINT NOT NULL,
  fee_amount BIGINT NOT NULL DEFAULT 0,
  net_amount BIGINT NOT NULL,
  currency VARCHAR(3) NOT NULL DEFAULT 'KRW',
  fee_rate NUMERIC(10,6),
  fee_config JSONB,
  status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
  settled_at TIMESTAMPTZ,
  metadata JSONB,
  created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT settlements_entity_type_check CHECK (entity_type IN ('DISTRIBUTOR', 'AGENCY', 'DEALER', 'SELLER', 'VENDOR')),
  CONSTRAINT settlements_entry_type_check CHECK (entry_type IN ('CREDIT', 'DEBIT')),
  CONSTRAINT settlements_status_check CHECK (status IN ('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED', 'CANCELLED')),
  CONSTRAINT settlements_amount_sign_check CHECK ((entry_type = 'CREDIT' AND amount > 0) OR (entry_type = 'DEBIT' AND amount < 0)),
  CONSTRAINT settlements_fee_amount_non_negative CHECK (fee_amount >= 0),
  CONSTRAINT settlements_net_amount_calculation CHECK (
    (entry_type = 'CREDIT' AND net_amount = amount - fee_amount) OR
    (entry_type = 'DEBIT' AND net_amount = amount + fee_amount)
  ),
  CONSTRAINT settlements_settled_at_required CHECK ((status = 'COMPLETED' AND settled_at IS NOT NULL) OR status <> 'COMPLETED')
);

CREATE INDEX idx_settlements_org_path_gist ON settlements USING GIST(org_path);
CREATE INDEX idx_settlements_entity_path_gist ON settlements USING GIST(entity_path);
CREATE INDEX idx_settlements_transaction_id ON settlements(transaction_id);
CREATE INDEX idx_settlements_entity_id ON settlements(entity_id, entity_type);
CREATE INDEX idx_settlements_status ON settlements(status);
CREATE INDEX idx_settlements_batch_id ON settlements(settlement_batch_id) WHERE settlement_batch_id IS NOT NULL;
```

**정산 규칙**:
- `entry_type = 'CREDIT'`: 승인 시 정산 (+), `amount > 0`
- `entry_type = 'DEBIT'`: 취소 시 정산 (-), `amount < 0`
- `net_amount`: CREDIT 시 `amount - fee_amount`, DEBIT 시 `amount + fee_amount`

### 4.14 settlement_batches - 정산 배치

```sql
CREATE TABLE settlement_batches (
  id UUID PRIMARY KEY DEFAULT uuidv7(),
  batch_number VARCHAR(100) NOT NULL UNIQUE,
  settlement_date DATE NOT NULL,
  period_start TIMESTAMPTZ NOT NULL,
  period_end TIMESTAMPTZ NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
  total_transactions INTEGER NOT NULL DEFAULT 0,
  total_amount BIGINT NOT NULL DEFAULT 0,
  total_fee_amount BIGINT NOT NULL DEFAULT 0,
  processed_at TIMESTAMPTZ,
  approved_at TIMESTAMPTZ,
  metadata JSONB,
  created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT settlement_batches_status_check CHECK (status IN ('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED', 'CANCELLED')),
  CONSTRAINT settlement_batches_period_order CHECK (period_start < period_end),
  CONSTRAINT settlement_batches_transactions_non_negative CHECK (total_transactions >= 0),
  CONSTRAINT settlement_batches_processed_at_required CHECK (
    (status IN ('COMPLETED', 'FAILED') AND processed_at IS NOT NULL) OR
    status NOT IN ('COMPLETED', 'FAILED')
  )
);

ALTER TABLE settlements ADD CONSTRAINT settlements_batch_fk
  FOREIGN KEY (settlement_batch_id) REFERENCES settlement_batches(id);
```

### 4.15 fee_configurations - 수수료 설정

```sql
CREATE TABLE fee_configurations (
  id UUID PRIMARY KEY DEFAULT uuidv7(),
  entity_id UUID NOT NULL,
  entity_type VARCHAR(20) NOT NULL,
  entity_path public.ltree NOT NULL,
  payment_method_id UUID REFERENCES payment_methods(id),
  card_company_id UUID REFERENCES card_companies(id),
  fee_type VARCHAR(20) NOT NULL,
  fee_rate NUMERIC(10,6),
  fixed_fee BIGINT,
  tier_config JSONB,
  min_fee BIGINT,
  max_fee BIGINT,
  priority INTEGER NOT NULL DEFAULT 0,
  valid_from TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  valid_until TIMESTAMPTZ,
  status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  metadata JSONB,
  created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fee_configurations_entity_type_check CHECK (entity_type IN ('DISTRIBUTOR', 'AGENCY', 'DEALER', 'SELLER', 'VENDOR')),
  CONSTRAINT fee_configurations_fee_type_check CHECK (fee_type IN ('PERCENTAGE', 'FIXED', 'TIERED', 'PERCENTAGE_PLUS_FIXED')),
  CONSTRAINT fee_configurations_status_check CHECK (status IN ('ACTIVE', 'INACTIVE', 'EXPIRED')),
  CONSTRAINT fee_configurations_rate_range CHECK (fee_rate IS NULL OR (fee_rate >= 0 AND fee_rate <= 1)),
  CONSTRAINT fee_configurations_min_max_fee CHECK (min_fee IS NULL OR max_fee IS NULL OR min_fee <= max_fee),
  CONSTRAINT fee_configurations_valid_period CHECK (valid_until IS NULL OR valid_from <= valid_until),
  CONSTRAINT fee_configurations_has_fee_definition CHECK (
    (fee_type = 'PERCENTAGE' AND fee_rate IS NOT NULL) OR
    (fee_type = 'FIXED' AND fixed_fee IS NOT NULL) OR
    (fee_type = 'TIERED' AND tier_config IS NOT NULL) OR
    (fee_type = 'PERCENTAGE_PLUS_FIXED' AND fee_rate IS NOT NULL AND fixed_fee IS NOT NULL)
  )
);

CREATE INDEX idx_fee_configurations_entity_path_gist ON fee_configurations USING GIST(entity_path);
CREATE INDEX idx_fee_configurations_entity ON fee_configurations(entity_id, entity_type);
CREATE INDEX idx_fee_configurations_status ON fee_configurations(status) WHERE status = 'ACTIVE';
```

**수수료 유형**:

| fee_type | 설명 | 필수 필드 |
|----------|------|-----------|
| PERCENTAGE | 정률 수수료 | fee_rate |
| FIXED | 정액 수수료 | fixed_fee |
| TIERED | 구간별 수수료 | tier_config |
| PERCENTAGE_PLUS_FIXED | 정률 + 정액 | fee_rate, fixed_fee |

### 4.16 users - 사용자 (테넌트별)

```sql
CREATE TABLE users (
  id UUID PRIMARY KEY DEFAULT uuidv7(),
  username VARCHAR(100) NOT NULL UNIQUE,
  email VARCHAR(255) NOT NULL UNIQUE,
  password_hash VARCHAR(255) NOT NULL,
  org_id UUID NOT NULL REFERENCES organizations(id),
  org_path public.ltree NOT NULL,
  full_name VARCHAR(200) NOT NULL,
  phone VARCHAR(20),
  role VARCHAR(50) NOT NULL,
  permissions JSONB DEFAULT '[]',
  two_factor_enabled BOOLEAN NOT NULL DEFAULT FALSE,
  two_factor_secret VARCHAR(255),
  last_login_at TIMESTAMPTZ,
  password_changed_at TIMESTAMPTZ,
  status user_status NOT NULL DEFAULT 'ACTIVE',
  created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted_at TIMESTAMPTZ
);

CREATE INDEX idx_users_org_path_gist ON users USING GIST(org_path);
CREATE INDEX idx_users_org_id ON users(org_id);
CREATE INDEX idx_users_status ON users(status) WHERE status = 'ACTIVE';
CREATE INDEX idx_users_email ON users(email);
```

### 4.17 api_keys - API 키 관리

```sql
CREATE TABLE api_keys (
  id UUID PRIMARY KEY DEFAULT uuidv7(),
  key_name VARCHAR(200) NOT NULL,
  key_hash VARCHAR(255) NOT NULL UNIQUE,
  key_prefix VARCHAR(20) NOT NULL,
  user_id UUID,
  org_id UUID NOT NULL,
  org_path public.ltree NOT NULL,
  scopes JSONB NOT NULL,
  rate_limit_per_minute INTEGER,
  rate_limit_per_hour INTEGER,
  last_used_at TIMESTAMPTZ,
  usage_count BIGINT NOT NULL DEFAULT 0,
  expires_at TIMESTAMPTZ,
  status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT api_keys_status_check CHECK (status IN ('ACTIVE', 'REVOKED', 'EXPIRED')),
  CONSTRAINT api_keys_prefix_pattern CHECK (key_prefix ~ '^bp_(live|test)_[a-zA-Z0-9]{4}$'),
  CONSTRAINT api_keys_rate_limits_positive CHECK (
    (rate_limit_per_minute IS NULL OR rate_limit_per_minute > 0) AND
    (rate_limit_per_hour IS NULL OR rate_limit_per_hour > 0)
  )
);

CREATE INDEX idx_api_keys_org_path_gist ON api_keys USING GIST(org_path);
CREATE INDEX idx_api_keys_key_prefix ON api_keys(key_prefix);
CREATE INDEX idx_api_keys_status ON api_keys(status) WHERE status = 'ACTIVE';
```

### 4.18 webhook_logs - 웹훅 로그

```sql
CREATE TABLE webhook_logs (
  id UUID PRIMARY KEY DEFAULT uuidv7(),
  pg_connection_id BIGINT NOT NULL,
  event_type VARCHAR(50) NOT NULL,
  payload JSONB NOT NULL,
  headers JSONB,
  status VARCHAR(20) NOT NULL DEFAULT 'RECEIVED',
  processed_at TIMESTAMPTZ,
  transaction_id UUID,
  transaction_event_id UUID,
  error_message TEXT,
  retry_count INTEGER NOT NULL DEFAULT 0,
  signature VARCHAR(500),
  signature_verified BOOLEAN,
  received_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT webhook_logs_status_check CHECK (status IN ('RECEIVED', 'PROCESSING', 'PROCESSED', 'FAILED', 'IGNORED')),
  CONSTRAINT webhook_logs_retry_count_non_negative CHECK (retry_count >= 0),
  CONSTRAINT webhook_logs_processed_at_required CHECK (
    (status IN ('PROCESSED', 'FAILED', 'IGNORED') AND processed_at IS NOT NULL) OR
    status NOT IN ('PROCESSED', 'FAILED', 'IGNORED')
  )
);

CREATE INDEX idx_webhook_logs_pg_connection ON webhook_logs(pg_connection_id, received_at DESC);
CREATE INDEX idx_webhook_logs_status ON webhook_logs(status, received_at DESC);
CREATE INDEX idx_webhook_logs_transaction ON webhook_logs(transaction_id) WHERE transaction_id IS NOT NULL;
```

### 4.19 audit_logs - 감사 로그

```sql
CREATE TABLE audit_logs (
  id UUID PRIMARY KEY DEFAULT uuidv7(),
  user_id UUID,
  username VARCHAR(100),
  action VARCHAR(50) NOT NULL,
  entity_type VARCHAR(100) NOT NULL,
  entity_id UUID NOT NULL,
  old_values JSONB,
  new_values JSONB,
  ip_address INET,
  user_agent TEXT,
  request_id VARCHAR(100),
  created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT audit_logs_action_check CHECK (action IN ('CREATE', 'UPDATE', 'DELETE', 'LOGIN', 'LOGOUT', 'APPROVE', 'REJECT', 'EXPORT'))
);

CREATE INDEX idx_audit_logs_entity ON audit_logs(entity_type, entity_id);
CREATE INDEX idx_audit_logs_user ON audit_logs(user_id) WHERE user_id IS NOT NULL;
CREATE INDEX idx_audit_logs_created ON audit_logs(created_at DESC);
```

### 4.20 settlement_reports - 정산 리포트

```sql
CREATE TABLE settlement_reports (
  id UUID PRIMARY KEY DEFAULT uuidv7(),
  report_type VARCHAR(50) NOT NULL,
  entity_id UUID NOT NULL,
  entity_type VARCHAR(20) NOT NULL,
  entity_path public.ltree NOT NULL,
  period_start TIMESTAMPTZ NOT NULL,
  period_end TIMESTAMPTZ NOT NULL,
  report_data JSONB NOT NULL,
  file_path VARCHAR(500),
  file_size BIGINT,
  status VARCHAR(20) NOT NULL DEFAULT 'GENERATED',
  generated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  generated_by UUID,
  CONSTRAINT settlement_reports_entity_type_check CHECK (entity_type IN ('DISTRIBUTOR', 'AGENCY', 'DEALER', 'SELLER', 'VENDOR')),
  CONSTRAINT settlement_reports_report_type_check CHECK (report_type IN ('DAILY', 'WEEKLY', 'MONTHLY', 'CUSTOM', 'RECONCILIATION')),
  CONSTRAINT settlement_reports_status_check CHECK (status IN ('GENERATED', 'EXPORTED', 'DELIVERED', 'FAILED'))
);

CREATE INDEX idx_settlement_reports_entity_path_gist ON settlement_reports USING GIST(entity_path);
CREATE INDEX idx_settlement_reports_entity ON settlement_reports(entity_id, entity_type);
CREATE INDEX idx_settlement_reports_period ON settlement_reports(period_start, period_end);
```

### 4.21 merchant_org_history - 가맹점 이관 이력

```sql
CREATE TABLE merchant_org_history (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  merchant_id UUID NOT NULL REFERENCES merchants(id),
  from_org_id UUID NOT NULL,
  from_org_path public.ltree NOT NULL,
  to_org_id UUID NOT NULL,
  to_org_path public.ltree NOT NULL,
  moved_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  moved_by VARCHAR(100),
  reason TEXT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_merchant_org_history_merchant_id ON merchant_org_history(merchant_id);
```

---

## 5. Views

### 5.1 Zero-Sum 검증 뷰

```sql
CREATE VIEW settlement_zero_sum_validation AS
SELECT
  te.id AS transaction_event_id,
  te.transaction_id,
  te.event_type,
  te.amount AS event_amount,
  COALESCE(SUM(s.amount), 0) AS total_settlement_amount,
  te.amount - COALESCE(SUM(s.amount), 0) AS zero_sum_diff,
  CASE WHEN te.amount - COALESCE(SUM(s.amount), 0) = 0 THEN TRUE ELSE FALSE END AS is_zero_sum_valid,
  COUNT(s.id) AS settlement_count
FROM transaction_events te
LEFT JOIN settlements s ON s.transaction_event_id = te.id
GROUP BY te.id, te.transaction_id, te.event_type, te.amount;

COMMENT ON VIEW settlement_zero_sum_validation IS 'Validates Zero-Sum constraint: |event.amount| = SUM(settlement.amount)';
```

---

## 6. Functions & Procedures

### 6.1 파티션 자동 생성

```sql
-- 매일 실행: 일별 파티션 자동 생성
DO $$
DECLARE
  start_date DATE := CURRENT_DATE - INTERVAL '7 days';
  end_date DATE := CURRENT_DATE + INTERVAL '30 days';
  partition_date DATE;
  partition_name TEXT;
  start_ts TEXT;
  end_ts TEXT;
BEGIN
  partition_date := start_date;
  WHILE partition_date < end_date LOOP
    partition_name := 'transaction_events_' || to_char(partition_date, 'YYYYMMDD');
    start_ts := partition_date::TEXT || ' 00:00:00+00';
    end_ts := (partition_date + INTERVAL '1 day')::DATE::TEXT || ' 00:00:00+00';

    EXECUTE format(
      'CREATE TABLE IF NOT EXISTS %I PARTITION OF transaction_events FOR VALUES FROM (%L) TO (%L)',
      partition_name, start_ts, end_ts
    );

    partition_date := partition_date + INTERVAL '1 day';
  END LOOP;
END $$;
```

---

## 7. ERD (Entity Relationship Diagram)

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              PUBLIC SCHEMA                                   │
├─────────────────────────────────────────────────────────────────────────────┤
│  ┌──────────────┐    ┌──────────────┐    ┌──────────────┐                   │
│  │   tenants    │    │pg_connections│    │   holidays   │                   │
│  ├──────────────┤    ├──────────────┤    ├──────────────┤                   │
│  │ id (VARCHAR) │◄───│ tenant_id    │    │ id (UUID)    │                   │
│  │ schema_name  │    │ pg_code      │    │ holiday_date │                   │
│  │ status       │    │ credentials  │    │ name         │                   │
│  └──────────────┘    └──────────────┘    └──────────────┘                   │
│                                                                              │
│  ┌──────────────┐    ┌──────────────┐                                       │
│  │organizations │    │    users     │                                       │
│  ├──────────────┤    ├──────────────┤                                       │
│  │ id (UUID)    │    │ id (UUID)    │                                       │
│  │ path (ltree) │    │ username     │                                       │
│  │ level        │    │ tenant_id    │◄── tenants.id                        │
│  └──────────────┘    └──────────────┘                                       │
└─────────────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────────────┐
│                             TENANT SCHEMA                                    │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  ┌──────────────┐         ┌──────────────┐         ┌──────────────┐         │
│  │organizations │◄────────│   users      │         │business_     │         │
│  ├──────────────┤  org_id ├──────────────┤         │entities      │         │
│  │ id (UUID)    │         │ id (UUID)    │         ├──────────────┤         │
│  │ path (ltree) │         │ org_path     │         │ id (UUID)    │         │
│  │ level        │         │ role         │         │ business_type│         │
│  │ business_    │─────────────────────────────────►│ business_no  │         │
│  │ entity_id    │                                  └──────┬───────┘         │
│  └──────┬───────┘                                         │                 │
│         │                                                 │                 │
│         │ org_id                              ┌───────────┴─────────────┐   │
│         ▼                                     │                         │   │
│  ┌──────────────┐                     ┌──────────────┐         ┌──────────────┐
│  │  merchants   │                     │  contacts    │         │settlement_   │
│  ├──────────────┤                     ├──────────────┤         │accounts      │
│  │ id (UUID)    │                     │ entity_type  │         ├──────────────┤
│  │ org_id       │─────────────────────│ entity_id    │         │ entity_type  │
│  │ org_path     │                     │ role         │         │ entity_id    │
│  └──────┬───────┘                     └──────────────┘         └──────────────┘
│         │                                                                    │
│         │ merchant_id                                                        │
│         ▼                                                                    │
│  ┌──────────────┐         ┌──────────────┐                                  │
│  │merchant_pg   │         │  terminals   │                                  │
│  │_mappings     │         ├──────────────┤                                  │
│  ├──────────────┤         │ merchant_id  │◄── merchants.id                 │
│  │ mid          │         │ cat_id       │                                  │
│  │ terminal_id  │         │ terminal_type│                                  │
│  └──────┬───────┘         └──────────────┘                                  │
│         │                                                                    │
│         │ merchant_pg_mapping_id                                            │
│         ▼                                                                    │
│  ┌──────────────────┐                                                       │
│  │  transactions    │  (현재 상태)                                          │
│  ├──────────────────┤                                                       │
│  │ id (UUID)        │                                                       │
│  │ transaction_id   │                                                       │
│  │ amount           │                                                       │
│  │ status           │                                                       │
│  └────────┬─────────┘                                                       │
│           │                                                                  │
│           │ transaction_id                                                   │
│           ▼                                                                  │
│  ┌──────────────────┐                                                       │
│  │transaction_events│  (이벤트 이력, 파티셔닝)                              │
│  ├──────────────────┤                                                       │
│  │ id (UUID)        │                                                       │
│  │ event_type       │  APPROVAL / CANCEL / PARTIAL_CANCEL / REFUND         │
│  │ amount           │  +승인 / -취소                                        │
│  │ event_sequence   │                                                       │
│  └────────┬─────────┘                                                       │
│           │                                                                  │
│           │ transaction_event_id                                             │
│           ▼                                                                  │
│  ┌──────────────────┐         ┌──────────────────┐                         │
│  │   settlements    │◄────────│settlement_batches│                         │
│  ├──────────────────┤ batch_id├──────────────────┤                         │
│  │ entity_type      │         │ batch_number     │                         │
│  │ entity_id        │         │ settlement_date  │                         │
│  │ entry_type       │  CREDIT/DEBIT             │                         │
│  │ amount           │  +승인 / -취소             │                         │
│  │ fee_amount       │                            │                         │
│  │ net_amount       │                            │                         │
│  └──────────────────┘                            │                         │
│                                                  │                         │
│  ┌──────────────────┐         ┌──────────────────┐                         │
│  │fee_configurations│         │  webhook_logs    │                         │
│  ├──────────────────┤         ├──────────────────┤                         │
│  │ entity_type      │         │ pg_connection_id │                         │
│  │ fee_type         │         │ payload          │                         │
│  │ fee_rate         │         │ status           │                         │
│  └──────────────────┘         └──────────────────┘                         │
│                                                                             │
│  ┌──────────────────┐         ┌──────────────────┐                         │
│  │   api_keys       │         │   audit_logs     │                         │
│  ├──────────────────┤         ├──────────────────┤                         │
│  │ key_hash         │         │ action           │                         │
│  │ scopes           │         │ entity_type      │                         │
│  │ status           │         │ old_values       │                         │
│  └──────────────────┘         │ new_values       │                         │
│                               └──────────────────┘                         │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 8. 핵심 아키텍처 패턴

### 8.1 Zero-Sum 원장 (복식부기)

모든 정산은 `transaction_events` 기준으로 생성되며, 다음 원칙을 준수합니다:

```
|이벤트 금액| = SUM(정산 amount)
```

- **승인 (APPROVAL)**: 모든 entity에 CREDIT (+)
- **취소 (CANCEL/PARTIAL_CANCEL)**: 모든 entity에 DEBIT (-)

### 8.2 ltree 계층 구조

5단계 조직 계층을 ltree로 관리합니다:

```
DISTRIBUTOR (Level 1) → AGENCY (Level 2) → DEALER (Level 3) → SELLER (Level 4) → VENDOR (Level 5)

경로 예시: dist_001.agcy_001.deal_001.sell_001.vend_001
```

### 8.3 하이브리드 이벤트 소싱

- `transactions`: 현재 상태 (빠른 조회)
- `transaction_events`: 모든 이벤트 이력 (불변, 일별 파티셔닝)
- `settlements`: 이벤트별 복식부기 정산

---

## 변경 이력

| 버전 | 날짜 | 변경 내용 |
|------|------|----------|
| v1.0 | 2026-01-28 | 초안 작성 |
| v2.0 | 2026-02-05 | 실제 마이그레이션 기준 전면 재작성 |
| v2.1 | 2026-02-06 | FeeConfiguration JPA 엔티티 구현 (fee_configurations 테이블 매핑) |
