-- =============================================================================
-- Bill&Pay Tenant Schema - 통합 초기화
-- =============================================================================
-- 버전: 1.0 (마이그레이션 통합)
-- 설명: 테넌트별 비즈니스 데이터 스키마
-- =============================================================================

-- =============================================================================
-- ENUM Types
-- =============================================================================
CREATE TYPE account_status AS ENUM ('ACTIVE', 'INACTIVE', 'PENDING_VERIFICATION');
CREATE TYPE contact_entity_type AS ENUM ('BUSINESS_ENTITY', 'MERCHANT');
CREATE TYPE contact_role AS ENUM ('PRIMARY', 'SECONDARY', 'SETTLEMENT', 'TECHNICAL');
CREATE TYPE user_status AS ENUM ('ACTIVE', 'INACTIVE', 'SUSPENDED', 'PENDING');

-- =============================================================================
-- Organizations Table (Tenant-specific hierarchy)
-- =============================================================================
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

COMMENT ON TABLE organizations IS '영업점 조직 계층 (테넌트별 - ltree 기반 5단계)';
COMMENT ON COLUMN organizations.id IS '영업점 고유 ID (UUID v7)';
COMMENT ON COLUMN organizations.org_code IS '영업점 코드 (고유)';
COMMENT ON COLUMN organizations.name IS '영업점 이름';
COMMENT ON COLUMN organizations.org_type IS '조직 유형 (DISTRIBUTOR, AGENCY, DEALER, SELLER, VENDOR)';
COMMENT ON COLUMN organizations.path IS 'ltree 계층 경로 (예: dist_001.agcy_001)';
COMMENT ON COLUMN organizations.parent_id IS '상위 조직 ID (최상위 DISTRIBUTOR는 NULL)';
COMMENT ON COLUMN organizations.level IS '계층 레벨 (1:총판, 2:대리점, 3:딜러, 4:셀러, 5:벤더)';
COMMENT ON COLUMN organizations.business_number IS '사업자등록번호 (숫자만)';
COMMENT ON COLUMN organizations.business_name IS '사업자명 (상호)';
COMMENT ON COLUMN organizations.representative_name IS '대표자명';
COMMENT ON COLUMN organizations.email IS '이메일';
COMMENT ON COLUMN organizations.phone IS '전화번호 (숫자만)';
COMMENT ON COLUMN organizations.address IS '주소';
COMMENT ON COLUMN organizations.status IS '상태 (ACTIVE, SUSPENDED, DELETED)';
COMMENT ON COLUMN organizations.config IS '영업점 추가 설정 (JSONB)';
COMMENT ON COLUMN organizations.business_entity_id IS '연결된 사업자 정보 ID';
COMMENT ON COLUMN organizations.created_at IS '생성일시';
COMMENT ON COLUMN organizations.updated_at IS '수정일시';
COMMENT ON COLUMN organizations.deleted_at IS '삭제일시 (소프트 삭제)';

-- =============================================================================
-- Business Entities Table
-- =============================================================================
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

ALTER TABLE organizations ADD CONSTRAINT organizations_business_entity_fk
  FOREIGN KEY (business_entity_id) REFERENCES business_entities(id);

COMMENT ON TABLE business_entities IS '사업자 정보';
COMMENT ON COLUMN business_entities.id IS '사업자 고유 ID (UUID v7)';
COMMENT ON COLUMN business_entities.business_type IS '사업자 유형 (CORPORATION, INDIVIDUAL, NON_BUSINESS)';
COMMENT ON COLUMN business_entities.business_number IS '사업자등록번호 (숫자만, 최대 12자리)';
COMMENT ON COLUMN business_entities.corporate_number IS '법인등록번호 (숫자만, 최대 14자리)';
COMMENT ON COLUMN business_entities.business_name IS '상호명';
COMMENT ON COLUMN business_entities.representative_name IS '대표자명';
COMMENT ON COLUMN business_entities.open_date IS '개업일';
COMMENT ON COLUMN business_entities.business_address IS '사업장 주소';
COMMENT ON COLUMN business_entities.actual_address IS '실제 주소';
COMMENT ON COLUMN business_entities.business_category IS '업종';
COMMENT ON COLUMN business_entities.business_sub_category IS '업태';
COMMENT ON COLUMN business_entities.main_phone IS '대표 전화번호 (숫자만)';
COMMENT ON COLUMN business_entities.manager_name IS '담당자명';
COMMENT ON COLUMN business_entities.manager_phone IS '담당자 전화번호 (숫자만)';
COMMENT ON COLUMN business_entities.email IS '이메일';
COMMENT ON COLUMN business_entities.created_at IS '생성일시';
COMMENT ON COLUMN business_entities.updated_at IS '수정일시';
COMMENT ON COLUMN business_entities.deleted_at IS '삭제일시 (소프트 삭제)';

-- =============================================================================
-- Merchants Table
-- =============================================================================
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

COMMENT ON TABLE merchants IS '가맹점';
COMMENT ON COLUMN merchants.id IS '가맹점 고유 ID (UUID v7)';
COMMENT ON COLUMN merchants.merchant_code IS '가맹점 코드 (고유)';
COMMENT ON COLUMN merchants.name IS '가맹점명';
COMMENT ON COLUMN merchants.org_id IS '소속 영업점 ID';
COMMENT ON COLUMN merchants.org_path IS '소속 영업점 ltree 경로 (계층 조회용)';
COMMENT ON COLUMN merchants.business_number IS '사업자등록번호 (숫자만)';
COMMENT ON COLUMN merchants.business_type IS '업종';
COMMENT ON COLUMN merchants.contact_name IS '담당자명';
COMMENT ON COLUMN merchants.contact_email IS '담당자 이메일';
COMMENT ON COLUMN merchants.contact_phone IS '담당자 전화번호 (숫자만)';
COMMENT ON COLUMN merchants.address IS '주소';
COMMENT ON COLUMN merchants.config IS '가맹점 추가 설정 (JSONB)';
COMMENT ON COLUMN merchants.status IS '상태 (ACTIVE, SUSPENDED, DELETED)';
COMMENT ON COLUMN merchants.created_at IS '생성일시';
COMMENT ON COLUMN merchants.updated_at IS '수정일시';
COMMENT ON COLUMN merchants.deleted_at IS '삭제일시 (소프트 삭제)';

-- =============================================================================
-- Merchant PG Mappings Table
-- =============================================================================
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

COMMENT ON TABLE merchant_pg_mappings IS '가맹점-PG 매핑 (가맹점별 PG 연동 정보)';
COMMENT ON COLUMN merchant_pg_mappings.id IS '매핑 고유 ID (UUID v7)';
COMMENT ON COLUMN merchant_pg_mappings.merchant_id IS '가맹점 ID';
COMMENT ON COLUMN merchant_pg_mappings.pg_connection_id IS 'PG 연결 ID (public.pg_connections 참조)';
COMMENT ON COLUMN merchant_pg_mappings.mid IS 'PG사 가맹점 ID (MID)';
COMMENT ON COLUMN merchant_pg_mappings.terminal_id IS 'PG사 터미널 ID (TID)';
COMMENT ON COLUMN merchant_pg_mappings.cat_id IS 'CAT ID (단말기 식별자)';
COMMENT ON COLUMN merchant_pg_mappings.config IS '매핑별 추가 설정 (JSONB)';
COMMENT ON COLUMN merchant_pg_mappings.status IS '상태 (ACTIVE, INACTIVE)';
COMMENT ON COLUMN merchant_pg_mappings.created_at IS '생성일시';
COMMENT ON COLUMN merchant_pg_mappings.updated_at IS '수정일시';

-- =============================================================================
-- Payment Methods Table
-- =============================================================================
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

COMMENT ON TABLE payment_methods IS '결제 수단';
COMMENT ON COLUMN payment_methods.id IS '결제 수단 고유 ID (UUID v7)';
COMMENT ON COLUMN payment_methods.method_code IS '결제 수단 코드 (고유)';
COMMENT ON COLUMN payment_methods.name IS '결제 수단명';
COMMENT ON COLUMN payment_methods.category IS '카테고리 (CARD, BANK, VIRTUAL, OTHER)';
COMMENT ON COLUMN payment_methods.config IS '추가 설정 (JSONB)';
COMMENT ON COLUMN payment_methods.display_order IS '화면 표시 순서';
COMMENT ON COLUMN payment_methods.status IS '상태 (ACTIVE, INACTIVE)';
COMMENT ON COLUMN payment_methods.created_at IS '생성일시';
COMMENT ON COLUMN payment_methods.updated_at IS '수정일시';

-- =============================================================================
-- Card Companies Table
-- =============================================================================
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

COMMENT ON TABLE card_companies IS '카드사';
COMMENT ON COLUMN card_companies.id IS '카드사 고유 ID (UUID v7)';
COMMENT ON COLUMN card_companies.company_code IS '카드사 코드 (고유)';
COMMENT ON COLUMN card_companies.company_name IS '카드사명';
COMMENT ON COLUMN card_companies.config IS '추가 설정 (JSONB)';
COMMENT ON COLUMN card_companies.display_order IS '화면 표시 순서';
COMMENT ON COLUMN card_companies.status IS '상태 (ACTIVE, INACTIVE)';
COMMENT ON COLUMN card_companies.created_at IS '생성일시';
COMMENT ON COLUMN card_companies.updated_at IS '수정일시';

-- =============================================================================
-- Transactions Table
-- =============================================================================
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

CREATE UNIQUE INDEX idx_transactions_pg_txn_unique
  ON transactions(pg_connection_id, pg_transaction_id)
  WHERE pg_transaction_id IS NOT NULL;

CREATE INDEX idx_transactions_pg_txn_lookup
  ON transactions(pg_connection_id, pg_transaction_id)
  INCLUDE (id, status, amount, merchant_id)
  WHERE pg_transaction_id IS NOT NULL;

COMMENT ON TABLE transactions IS '거래 내역 (현재 상태)';
COMMENT ON COLUMN transactions.id IS '거래 고유 ID (UUID v7)';
COMMENT ON COLUMN transactions.transaction_id IS '거래 고유번호 (외부 노출용)';
COMMENT ON COLUMN transactions.merchant_id IS '가맹점 ID';
COMMENT ON COLUMN transactions.merchant_pg_mapping_id IS '가맹점-PG 매핑 ID';
COMMENT ON COLUMN transactions.pg_connection_id IS 'PG 연결 ID';
COMMENT ON COLUMN transactions.org_path IS '소속 영업점 ltree 경로 (계층 조회용)';
COMMENT ON COLUMN transactions.payment_method_id IS '결제 수단 ID';
COMMENT ON COLUMN transactions.card_company_id IS '카드사 ID (카드 결제 시)';
COMMENT ON COLUMN transactions.amount IS '거래 금액 (양수)';
COMMENT ON COLUMN transactions.currency IS '통화 코드 (ISO 4217, 기본값: KRW)';
COMMENT ON COLUMN transactions.status IS '거래 상태 (PENDING, APPROVED, CANCELLED, PARTIAL_CANCELLED, FAILED)';
COMMENT ON COLUMN transactions.pg_transaction_id IS 'PG사 거래 고유번호';
COMMENT ON COLUMN transactions.approval_number IS '승인번호';
COMMENT ON COLUMN transactions.approved_at IS '승인일시';
COMMENT ON COLUMN transactions.cancelled_at IS '취소일시';
COMMENT ON COLUMN transactions.cat_id IS 'CAT ID (단말기 식별자)';
COMMENT ON COLUMN transactions.metadata IS '거래 부가 정보 (JSONB)';
COMMENT ON COLUMN transactions.created_at IS '생성일시';
COMMENT ON COLUMN transactions.updated_at IS '수정일시';

-- =============================================================================
-- Transaction Events Table (Partitioned)
-- =============================================================================
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

COMMENT ON TABLE transaction_events IS '거래 이벤트 이력 (불변, 일별 파티셔닝)';
COMMENT ON COLUMN transaction_events.id IS '이벤트 고유 ID (UUID v7)';
COMMENT ON COLUMN transaction_events.event_type IS '이벤트 유형 (APPROVAL, CANCEL, PARTIAL_CANCEL, REFUND)';
COMMENT ON COLUMN transaction_events.event_sequence IS '이벤트 순번 (거래 내 순서, 1부터 시작)';
COMMENT ON COLUMN transaction_events.transaction_id IS '거래 ID';
COMMENT ON COLUMN transaction_events.merchant_id IS '가맹점 ID';
COMMENT ON COLUMN transaction_events.merchant_pg_mapping_id IS '가맹점-PG 매핑 ID';
COMMENT ON COLUMN transaction_events.pg_connection_id IS 'PG 연결 ID';
COMMENT ON COLUMN transaction_events.org_path IS '소속 영업점 ltree 경로';
COMMENT ON COLUMN transaction_events.payment_method_id IS '결제 수단 ID';
COMMENT ON COLUMN transaction_events.card_company_id IS '카드사 ID (카드 결제 시)';
COMMENT ON COLUMN transaction_events.amount IS '이벤트 금액 (승인: 양수, 취소/환불: 음수)';
COMMENT ON COLUMN transaction_events.currency IS '통화 코드 (ISO 4217, 기본값: KRW)';
COMMENT ON COLUMN transaction_events.previous_status IS '이전 거래 상태';
COMMENT ON COLUMN transaction_events.new_status IS '변경 후 거래 상태';
COMMENT ON COLUMN transaction_events.pg_transaction_id IS 'PG사 거래 고유번호';
COMMENT ON COLUMN transaction_events.approval_number IS '승인번호';
COMMENT ON COLUMN transaction_events.cat_id IS 'CAT ID (단말기 식별자)';
COMMENT ON COLUMN transaction_events.metadata IS '이벤트 부가 정보 (JSONB)';
COMMENT ON COLUMN transaction_events.occurred_at IS '이벤트 발생일시 (PG사 기준)';
COMMENT ON COLUMN transaction_events.created_at IS '생성일시 (파티션 키)';

-- =============================================================================
-- Settlements Table (Double-entry ledger)
-- =============================================================================
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

COMMENT ON TABLE settlements IS '정산 원장 (복식부기)';
COMMENT ON COLUMN settlements.id IS '정산 고유 ID (UUID v7)';
COMMENT ON COLUMN settlements.settlement_batch_id IS '정산 배치 ID';
COMMENT ON COLUMN settlements.transaction_event_id IS '거래 이벤트 ID (정산 근거)';
COMMENT ON COLUMN settlements.transaction_id IS '거래 ID';
COMMENT ON COLUMN settlements.merchant_id IS '가맹점 ID';
COMMENT ON COLUMN settlements.org_path IS '가맹점 소속 영업점 ltree 경로';
COMMENT ON COLUMN settlements.entity_id IS '정산 대상 엔티티 ID (영업점)';
COMMENT ON COLUMN settlements.entity_type IS '정산 대상 유형 (DISTRIBUTOR, AGENCY, DEALER, SELLER, VENDOR)';
COMMENT ON COLUMN settlements.entity_path IS '정산 대상 ltree 경로';
COMMENT ON COLUMN settlements.entry_type IS '분개 유형 (CREDIT: 대변/입금, DEBIT: 차변/출금)';
COMMENT ON COLUMN settlements.amount IS '정산 금액 (CREDIT: 양수, DEBIT: 음수)';
COMMENT ON COLUMN settlements.fee_amount IS '수수료 금액 (항상 0 이상)';
COMMENT ON COLUMN settlements.net_amount IS '순 정산 금액 (amount - fee_amount)';
COMMENT ON COLUMN settlements.currency IS '통화 코드 (ISO 4217, 기본값: KRW)';
COMMENT ON COLUMN settlements.fee_rate IS '적용된 수수료율';
COMMENT ON COLUMN settlements.fee_config IS '적용된 수수료 설정 스냅샷 (JSONB)';
COMMENT ON COLUMN settlements.status IS '정산 상태 (PENDING, PROCESSING, COMPLETED, FAILED, CANCELLED)';
COMMENT ON COLUMN settlements.settled_at IS '정산 완료일시 (COMPLETED 시 필수)';
COMMENT ON COLUMN settlements.metadata IS '정산 부가 정보 (JSONB)';
COMMENT ON COLUMN settlements.created_at IS '생성일시';
COMMENT ON COLUMN settlements.updated_at IS '수정일시';

-- =============================================================================
-- Settlement Batches Table
-- =============================================================================
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

COMMENT ON TABLE settlement_batches IS '정산 배치 (일괄 정산 처리 단위)';
COMMENT ON COLUMN settlement_batches.id IS '배치 고유 ID (UUID v7)';
COMMENT ON COLUMN settlement_batches.batch_number IS '배치 번호 (고유)';
COMMENT ON COLUMN settlement_batches.settlement_date IS '정산 기준일';
COMMENT ON COLUMN settlement_batches.period_start IS '정산 대상 기간 시작일시';
COMMENT ON COLUMN settlement_batches.period_end IS '정산 대상 기간 종료일시';
COMMENT ON COLUMN settlement_batches.status IS '배치 상태 (PENDING, PROCESSING, COMPLETED, FAILED, CANCELLED)';
COMMENT ON COLUMN settlement_batches.total_transactions IS '총 거래 건수';
COMMENT ON COLUMN settlement_batches.total_amount IS '총 거래 금액';
COMMENT ON COLUMN settlement_batches.total_fee_amount IS '총 수수료 금액';
COMMENT ON COLUMN settlement_batches.processed_at IS '처리 완료일시 (COMPLETED/FAILED 시 필수)';
COMMENT ON COLUMN settlement_batches.approved_at IS '승인일시';
COMMENT ON COLUMN settlement_batches.metadata IS '배치 부가 정보 (JSONB)';
COMMENT ON COLUMN settlement_batches.created_at IS '생성일시';
COMMENT ON COLUMN settlement_batches.updated_at IS '수정일시';

-- =============================================================================
-- Fee Configurations Table
-- =============================================================================
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

COMMENT ON TABLE fee_configurations IS '수수료 설정 (조직별/결제수단별 수수료율)';
COMMENT ON COLUMN fee_configurations.id IS '수수료 설정 고유 ID (UUID v7)';
COMMENT ON COLUMN fee_configurations.entity_id IS '대상 영업점 ID';
COMMENT ON COLUMN fee_configurations.entity_type IS '대상 유형 (DISTRIBUTOR, AGENCY, DEALER, SELLER, VENDOR)';
COMMENT ON COLUMN fee_configurations.entity_path IS '대상 ltree 경로';
COMMENT ON COLUMN fee_configurations.payment_method_id IS '결제 수단 ID (NULL이면 전체 적용)';
COMMENT ON COLUMN fee_configurations.card_company_id IS '카드사 ID (NULL이면 전체 적용)';
COMMENT ON COLUMN fee_configurations.fee_type IS '수수료 유형 (PERCENTAGE, FIXED, TIERED, PERCENTAGE_PLUS_FIXED)';
COMMENT ON COLUMN fee_configurations.fee_rate IS '수수료율 (0~1, PERCENTAGE 유형 시 필수)';
COMMENT ON COLUMN fee_configurations.fixed_fee IS '고정 수수료 금액 (FIXED 유형 시 필수)';
COMMENT ON COLUMN fee_configurations.tier_config IS '구간별 수수료 설정 (TIERED 유형 시 필수, JSONB)';
COMMENT ON COLUMN fee_configurations.min_fee IS '최소 수수료 금액';
COMMENT ON COLUMN fee_configurations.max_fee IS '최대 수수료 금액';
COMMENT ON COLUMN fee_configurations.priority IS '우선순위 (높을수록 우선 적용)';
COMMENT ON COLUMN fee_configurations.valid_from IS '유효 시작일시';
COMMENT ON COLUMN fee_configurations.valid_until IS '유효 종료일시 (NULL이면 무기한)';
COMMENT ON COLUMN fee_configurations.status IS '상태 (ACTIVE, INACTIVE, EXPIRED)';
COMMENT ON COLUMN fee_configurations.metadata IS '추가 정보 (JSONB)';
COMMENT ON COLUMN fee_configurations.created_at IS '생성일시';
COMMENT ON COLUMN fee_configurations.updated_at IS '수정일시';

-- =============================================================================
-- Contacts Table
-- =============================================================================
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

COMMENT ON TABLE contacts IS '담당자 연락처';
COMMENT ON COLUMN contacts.id IS '담당자 고유 ID (UUID)';
COMMENT ON COLUMN contacts.name IS '담당자명';
COMMENT ON COLUMN contacts.phone IS '전화번호 (숫자만)';
COMMENT ON COLUMN contacts.email IS '이메일';
COMMENT ON COLUMN contacts.role IS '역할 (PRIMARY, SECONDARY, SETTLEMENT, TECHNICAL)';
COMMENT ON COLUMN contacts.entity_type IS '소속 엔티티 유형 (BUSINESS_ENTITY, MERCHANT)';
COMMENT ON COLUMN contacts.entity_id IS '소속 엔티티 ID';
COMMENT ON COLUMN contacts.is_primary IS '대표 담당자 여부';
COMMENT ON COLUMN contacts.created_at IS '생성일시';
COMMENT ON COLUMN contacts.updated_at IS '수정일시';
COMMENT ON COLUMN contacts.deleted_at IS '삭제일시 (소프트 삭제)';

-- =============================================================================
-- Settlement Accounts Table
-- =============================================================================
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

COMMENT ON TABLE settlement_accounts IS '정산 계좌';
COMMENT ON COLUMN settlement_accounts.id IS '정산 계좌 고유 ID (UUID v7)';
COMMENT ON COLUMN settlement_accounts.bank_code IS '은행 코드';
COMMENT ON COLUMN settlement_accounts.bank_name IS '은행명';
COMMENT ON COLUMN settlement_accounts.account_number IS '계좌번호 (숫자만)';
COMMENT ON COLUMN settlement_accounts.account_holder IS '예금주명';
COMMENT ON COLUMN settlement_accounts.entity_type IS '소속 엔티티 유형 (BUSINESS_ENTITY, MERCHANT)';
COMMENT ON COLUMN settlement_accounts.entity_id IS '소속 엔티티 ID';
COMMENT ON COLUMN settlement_accounts.is_primary IS '대표 계좌 여부';
COMMENT ON COLUMN settlement_accounts.status IS '상태 (ACTIVE, INACTIVE, PENDING_VERIFICATION)';
COMMENT ON COLUMN settlement_accounts.verified_at IS '계좌 인증일시';
COMMENT ON COLUMN settlement_accounts.memo IS '메모';
COMMENT ON COLUMN settlement_accounts.created_at IS '생성일시';
COMMENT ON COLUMN settlement_accounts.updated_at IS '수정일시';
COMMENT ON COLUMN settlement_accounts.deleted_at IS '삭제일시 (소프트 삭제)';

-- =============================================================================
-- Terminals Table
-- =============================================================================
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

COMMENT ON TABLE terminals IS '단말기';
COMMENT ON COLUMN terminals.id IS '단말기 고유 ID (UUID v7)';
COMMENT ON COLUMN terminals.cat_id IS 'CAT ID (단말기 고유 식별자)';
COMMENT ON COLUMN terminals.terminal_type IS '단말기 유형 (CAT, POS, MOBILE, KIOSK, ONLINE)';
COMMENT ON COLUMN terminals.merchant_id IS '소속 가맹점 ID';
COMMENT ON COLUMN terminals.organization_id IS '소속 영업점 ID';
COMMENT ON COLUMN terminals.serial_number IS '시리얼 번호';
COMMENT ON COLUMN terminals.model IS '모델명';
COMMENT ON COLUMN terminals.manufacturer IS '제조사';
COMMENT ON COLUMN terminals.install_address IS '설치 주소';
COMMENT ON COLUMN terminals.install_date IS '설치일';
COMMENT ON COLUMN terminals.status IS '상태 (ACTIVE, INACTIVE, SUSPENDED, TERMINATED)';
COMMENT ON COLUMN terminals.config IS '단말기 추가 설정 (JSONB)';
COMMENT ON COLUMN terminals.last_transaction_at IS '마지막 거래일시';
COMMENT ON COLUMN terminals.created_at IS '생성일시';
COMMENT ON COLUMN terminals.updated_at IS '수정일시';
COMMENT ON COLUMN terminals.deleted_at IS '삭제일시 (소프트 삭제)';

-- =============================================================================
-- Users Table (Tenant-specific)
-- =============================================================================
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

COMMENT ON TABLE users IS '사용자 (테넌트별)';
COMMENT ON COLUMN users.id IS '사용자 고유 ID (UUID v7)';
COMMENT ON COLUMN users.username IS '로그인 ID (고유)';
COMMENT ON COLUMN users.email IS '이메일 (고유)';
COMMENT ON COLUMN users.password_hash IS '암호화된 비밀번호 (BCrypt)';
COMMENT ON COLUMN users.org_id IS '소속 영업점 ID';
COMMENT ON COLUMN users.org_path IS '소속 영업점 ltree 경로 (권한 조회용)';
COMMENT ON COLUMN users.full_name IS '이름';
COMMENT ON COLUMN users.phone IS '전화번호 (숫자만)';
COMMENT ON COLUMN users.role IS '역할 (ADMIN, MANAGER, OPERATOR 등)';
COMMENT ON COLUMN users.permissions IS '세부 권한 목록 (JSONB 배열)';
COMMENT ON COLUMN users.two_factor_enabled IS '2FA 활성화 여부';
COMMENT ON COLUMN users.two_factor_secret IS '2FA 비밀키 (TOTP)';
COMMENT ON COLUMN users.last_login_at IS '마지막 로그인 일시';
COMMENT ON COLUMN users.password_changed_at IS '비밀번호 변경일시';
COMMENT ON COLUMN users.status IS '상태 (ACTIVE, INACTIVE, SUSPENDED, PENDING)';
COMMENT ON COLUMN users.created_at IS '생성일시';
COMMENT ON COLUMN users.updated_at IS '수정일시';
COMMENT ON COLUMN users.deleted_at IS '삭제일시 (소프트 삭제)';

-- =============================================================================
-- Merchant Org History Table
-- =============================================================================
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

COMMENT ON TABLE merchant_org_history IS '가맹점 소속 영업점 변경 이력';
COMMENT ON COLUMN merchant_org_history.id IS '이력 고유 ID (UUID)';
COMMENT ON COLUMN merchant_org_history.merchant_id IS '가맹점 ID';
COMMENT ON COLUMN merchant_org_history.from_org_id IS '이전 영업점 ID';
COMMENT ON COLUMN merchant_org_history.from_org_path IS '이전 영업점 ltree 경로';
COMMENT ON COLUMN merchant_org_history.to_org_id IS '이동 후 영업점 ID';
COMMENT ON COLUMN merchant_org_history.to_org_path IS '이동 후 영업점 ltree 경로';
COMMENT ON COLUMN merchant_org_history.moved_at IS '이동일시';
COMMENT ON COLUMN merchant_org_history.moved_by IS '이동 처리자';
COMMENT ON COLUMN merchant_org_history.reason IS '이동 사유';
COMMENT ON COLUMN merchant_org_history.created_at IS '생성일시';

-- =============================================================================
-- API Keys Table
-- =============================================================================
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

COMMENT ON TABLE api_keys IS 'API 키 (외부 연동용 인증키)';
COMMENT ON COLUMN api_keys.id IS 'API 키 고유 ID (UUID v7)';
COMMENT ON COLUMN api_keys.key_name IS 'API 키 이름 (설명용)';
COMMENT ON COLUMN api_keys.key_hash IS 'API 키 해시값 (SHA-256)';
COMMENT ON COLUMN api_keys.key_prefix IS 'API 키 접두사 (bp_live_xxxx 또는 bp_test_xxxx)';
COMMENT ON COLUMN api_keys.user_id IS '발급 사용자 ID';
COMMENT ON COLUMN api_keys.org_id IS '소속 영업점 ID';
COMMENT ON COLUMN api_keys.org_path IS '소속 영업점 ltree 경로 (권한 범위)';
COMMENT ON COLUMN api_keys.scopes IS '허용 권한 범위 (JSONB 배열)';
COMMENT ON COLUMN api_keys.rate_limit_per_minute IS '분당 요청 제한';
COMMENT ON COLUMN api_keys.rate_limit_per_hour IS '시간당 요청 제한';
COMMENT ON COLUMN api_keys.last_used_at IS '마지막 사용일시';
COMMENT ON COLUMN api_keys.usage_count IS '총 사용 횟수';
COMMENT ON COLUMN api_keys.expires_at IS '만료일시 (NULL이면 무기한)';
COMMENT ON COLUMN api_keys.status IS '상태 (ACTIVE, REVOKED, EXPIRED)';
COMMENT ON COLUMN api_keys.created_at IS '생성일시';
COMMENT ON COLUMN api_keys.updated_at IS '수정일시';

-- =============================================================================
-- Webhook Logs Table
-- =============================================================================
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

COMMENT ON TABLE webhook_logs IS '웹훅 수신 로그';
COMMENT ON COLUMN webhook_logs.id IS '로그 고유 ID (UUID v7)';
COMMENT ON COLUMN webhook_logs.pg_connection_id IS 'PG 연결 ID (발신 PG사)';
COMMENT ON COLUMN webhook_logs.event_type IS '이벤트 유형 (PAYMENT, CANCEL 등)';
COMMENT ON COLUMN webhook_logs.payload IS '수신 페이로드 (JSONB)';
COMMENT ON COLUMN webhook_logs.headers IS '수신 HTTP 헤더 (JSONB)';
COMMENT ON COLUMN webhook_logs.status IS '처리 상태 (RECEIVED, PROCESSING, PROCESSED, FAILED, IGNORED)';
COMMENT ON COLUMN webhook_logs.processed_at IS '처리 완료일시';
COMMENT ON COLUMN webhook_logs.transaction_id IS '매핑된 거래 ID';
COMMENT ON COLUMN webhook_logs.transaction_event_id IS '생성된 거래 이벤트 ID';
COMMENT ON COLUMN webhook_logs.error_message IS '오류 메시지';
COMMENT ON COLUMN webhook_logs.retry_count IS '재시도 횟수';
COMMENT ON COLUMN webhook_logs.signature IS '수신 서명값';
COMMENT ON COLUMN webhook_logs.signature_verified IS '서명 검증 결과';
COMMENT ON COLUMN webhook_logs.received_at IS '수신일시';

-- =============================================================================
-- Audit Logs Table
-- =============================================================================
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

COMMENT ON TABLE audit_logs IS '감사 로그 (사용자 활동 추적)';
COMMENT ON COLUMN audit_logs.id IS '로그 고유 ID (UUID v7)';
COMMENT ON COLUMN audit_logs.user_id IS '사용자 ID';
COMMENT ON COLUMN audit_logs.username IS '사용자명 (비정규화, 사용자 삭제 대비)';
COMMENT ON COLUMN audit_logs.action IS '동작 (CREATE, UPDATE, DELETE, LOGIN, LOGOUT, APPROVE, REJECT, EXPORT)';
COMMENT ON COLUMN audit_logs.entity_type IS '대상 엔티티 유형';
COMMENT ON COLUMN audit_logs.entity_id IS '대상 엔티티 ID';
COMMENT ON COLUMN audit_logs.old_values IS '변경 전 값 (JSONB)';
COMMENT ON COLUMN audit_logs.new_values IS '변경 후 값 (JSONB)';
COMMENT ON COLUMN audit_logs.ip_address IS '요청자 IP 주소';
COMMENT ON COLUMN audit_logs.user_agent IS '요청자 User-Agent';
COMMENT ON COLUMN audit_logs.request_id IS '요청 추적 ID';
COMMENT ON COLUMN audit_logs.created_at IS '생성일시';

-- =============================================================================
-- Settlement Reports Table
-- =============================================================================
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

COMMENT ON TABLE settlement_reports IS '정산 리포트';
COMMENT ON COLUMN settlement_reports.id IS '리포트 고유 ID (UUID v7)';
COMMENT ON COLUMN settlement_reports.report_type IS '리포트 유형 (DAILY, WEEKLY, MONTHLY, CUSTOM, RECONCILIATION)';
COMMENT ON COLUMN settlement_reports.entity_id IS '대상 영업점 ID';
COMMENT ON COLUMN settlement_reports.entity_type IS '대상 유형 (DISTRIBUTOR, AGENCY, DEALER, SELLER, VENDOR)';
COMMENT ON COLUMN settlement_reports.entity_path IS '대상 ltree 경로';
COMMENT ON COLUMN settlement_reports.period_start IS '리포트 기간 시작일시';
COMMENT ON COLUMN settlement_reports.period_end IS '리포트 기간 종료일시';
COMMENT ON COLUMN settlement_reports.report_data IS '리포트 데이터 (JSONB)';
COMMENT ON COLUMN settlement_reports.file_path IS '리포트 파일 경로';
COMMENT ON COLUMN settlement_reports.file_size IS '리포트 파일 크기 (bytes)';
COMMENT ON COLUMN settlement_reports.status IS '상태 (GENERATED, EXPORTED, DELIVERED, FAILED)';
COMMENT ON COLUMN settlement_reports.generated_at IS '생성일시';
COMMENT ON COLUMN settlement_reports.generated_by IS '생성자 사용자 ID';

-- =============================================================================
-- Zero-Sum Validation View
-- =============================================================================
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

COMMENT ON VIEW settlement_zero_sum_validation IS 'Zero-Sum 검증 뷰: |이벤트 금액| = SUM(정산 금액)';
