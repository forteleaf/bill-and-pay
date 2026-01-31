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
│   ├── system_configs          -- 시스템 설정
│   ├── payment_methods         -- 결제 수단 마스터
│   ├── card_companies          -- 카드사 마스터
│   ├── holidays                -- 공휴일 (영업일 계산용)
│   └── pg_connections          -- PG사 연동 설정
│
├── tenant_001 (총판A 스키마)
│   ├── organizations           -- 조직 구조
│   ├── users                   -- 사용자
│   ├── businesses              -- 사업자 (1:N 가맹점)
│   ├── fee_policies            -- 수수료 정책 (공유 가능)
│   ├── merchants               -- 가맹점
│   ├── merchant_pg_mappings    -- 가맹점-PG 매핑
│   ├── transactions            -- 거래 현재 상태
│   ├── transaction_events      -- 거래 이벤트 이력 (파티셔닝)
│   ├── settlements             -- 정산 원장 (이벤트 기준)
│   ├── notification_settings   -- 알림 설정
│   ├── audit_logs              -- 감사 로그
│   └── ...
│
├── tenant_002 (총판B 스키마)
│   └── ... (동일 구조)
│
└── tenant_NNN
    └── ...
```

---

## 3. 공통 스키마 (public)

### 3.1 tenants - 테넌트 목록

```sql
CREATE TABLE public.tenants (
    id              BIGSERIAL PRIMARY KEY,
    uuid            UUID NOT NULL DEFAULT uuidv7() UNIQUE,

    -- 테넌트 정보
    code            VARCHAR(20) NOT NULL UNIQUE,  -- tenant_001
    name            VARCHAR(100) NOT NULL,
    business_no     VARCHAR(20),
    representative  VARCHAR(50),

    -- 스키마 정보
    schema_name     VARCHAR(50) NOT NULL UNIQUE,  -- tenant_001

    -- 상태
    status          VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',

    -- 감사
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_tenant_status CHECK (status IN ('ACTIVE', 'SUSPENDED', 'TERMINATED'))
);
```

### 3.2 system_configs - 시스템 설정

```sql
CREATE TABLE public.system_configs (
    id              BIGSERIAL PRIMARY KEY,
    config_key      VARCHAR(100) NOT NULL UNIQUE,
    config_value    JSONB NOT NULL,
    description     TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 초기 데이터
INSERT INTO public.system_configs (config_key, config_value, description) VALUES
('fee_calculation', '{"rounding": "FLOOR", "decimal_places": 0}', '수수료 계산 규칙'),
('settlement_default_cycle', '"D+1"', '기본 정산 주기'),
('webhook_timeout_ms', '5000', 'Webhook 타임아웃'),
('max_retry_count', '3', '최대 재시도 횟수');
```

### 3.3 payment_methods - 결제 수단

```sql
CREATE TABLE public.payment_methods (
    id              SERIAL PRIMARY KEY,
    code            VARCHAR(20) NOT NULL UNIQUE,
    name            VARCHAR(50) NOT NULL,
    category        VARCHAR(20) NOT NULL,         -- CARD, TRANSFER, VIRTUAL
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    display_order   SMALLINT NOT NULL DEFAULT 0
);

-- 초기 데이터
INSERT INTO public.payment_methods (code, name, category, display_order) VALUES
('CREDIT', '신용카드', 'CARD', 1),
('DEBIT', '체크카드', 'CARD', 2),
('OVERSEAS', '해외카드', 'CARD', 3),
('TRANSFER', '계좌이체', 'TRANSFER', 4),
('VIRTUAL', '가상계좌', 'VIRTUAL', 5);
```

### 3.4 card_companies - 카드사

```sql
CREATE TABLE public.card_companies (
    id              SERIAL PRIMARY KEY,
    code            VARCHAR(10) NOT NULL UNIQUE,  -- BC, KB, SS, HD, ...
    name            VARCHAR(50) NOT NULL,
    full_name       VARCHAR(100),
    is_active       BOOLEAN NOT NULL DEFAULT TRUE
);

-- 초기 데이터
INSERT INTO public.card_companies (code, name, full_name) VALUES
('BC', 'BC카드', '비씨카드'),
('KB', '국민카드', 'KB국민카드'),
('SS', '삼성카드', '삼성카드'),
('SH', '신한카드', '신한카드'),
('HD', '현대카드', '현대카드'),
('LT', '롯데카드', '롯데카드'),
('HN', '하나카드', '하나카드'),
('WR', '우리카드', '우리카드'),
('NH', '농협카드', 'NH농협카드'),
```

### 3.5 holidays - 공휴일

```sql
CREATE TABLE public.holidays (
    id              SERIAL PRIMARY KEY,
    holiday_date    DATE NOT NULL UNIQUE,
    name            VARCHAR(50) NOT NULL,
    is_substitute   BOOLEAN NOT NULL DEFAULT FALSE,  -- 대체공휴일 여부
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_holiday_date ON public.holidays (holiday_date);
```

### 3.6 pg_connections - PG사 연동

```sql
CREATE TABLE public.pg_connections (
    id              BIGSERIAL PRIMARY KEY,
    uuid            UUID NOT NULL DEFAULT uuidv7() UNIQUE,

    -- PG사 정보
    pg_code         VARCHAR(20) NOT NULL UNIQUE,
    pg_name         VARCHAR(50) NOT NULL,
    pg_api_version  VARCHAR(20),

    -- 연동 인증 (암호화)
    merchant_id     VARCHAR(100) NOT NULL,
    api_key_enc     BYTEA NOT NULL,
    api_secret_enc  BYTEA NOT NULL,

    -- Webhook
    webhook_path    VARCHAR(100) NOT NULL UNIQUE,
    webhook_secret  VARCHAR(100),

    -- API
    api_base_url    VARCHAR(200),
    api_endpoints   JSONB DEFAULT '{}',

    -- 상태
    status          VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    last_sync_at    TIMESTAMPTZ,
    last_error      TEXT,

    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_pg_status CHECK (status IN ('ACTIVE', 'INACTIVE', 'ERROR'))
);
```

---

## 4. 테넌트 스키마 (tenant_xxx)

### 4.1 organizations - 조직

```sql
CREATE TABLE organizations (
    id              BIGSERIAL PRIMARY KEY,
    uuid            UUID NOT NULL DEFAULT uuidv7() UNIQUE,

    -- 계층 정보
    path            LTREE NOT NULL,
    depth           SMALLINT NOT NULL,
    org_type        VARCHAR(20) NOT NULL,

    -- 기본 정보
    code            VARCHAR(20) NOT NULL UNIQUE,
    name            VARCHAR(100) NOT NULL,
    business_no     VARCHAR(20),
    representative  VARCHAR(50),

    -- 연락처
    phone           VARCHAR(20),
    email           VARCHAR(255),
    address         TEXT,

    -- 상위 조직
    parent_id       BIGINT REFERENCES organizations(id),
    root_id         BIGINT REFERENCES organizations(id),

    -- 수수료 정책
    fee_policy      JSONB NOT NULL DEFAULT '{}',

    -- 상태
    status          VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',

    -- 감사
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by      BIGINT NOT NULL,

    CONSTRAINT chk_org_type CHECK (
        org_type IN ('DISTRIBUTOR', 'AGENCY', 'DEALER', 'SELLER', 'VENDOR')
    ),
    CONSTRAINT chk_depth CHECK (depth BETWEEN 1 AND 5),
    CONSTRAINT chk_org_status CHECK (status IN ('ACTIVE', 'SUSPENDED', 'TERMINATED'))
);

-- 인덱스
CREATE INDEX idx_org_path_gist ON organizations USING GIST (path);
CREATE INDEX idx_org_parent ON organizations (parent_id);
CREATE INDEX idx_org_root ON organizations (root_id);
CREATE INDEX idx_org_type ON organizations (org_type);
CREATE INDEX idx_org_status ON organizations (status);
```

### 4.2 users - 사용자

```sql
CREATE TABLE users (
    id              BIGSERIAL PRIMARY KEY,
    uuid            UUID NOT NULL DEFAULT uuidv7() UNIQUE,

    -- 인증
    email           VARCHAR(255) NOT NULL UNIQUE,
    password_hash   VARCHAR(255),

    -- 소속
    org_id          BIGINT REFERENCES organizations(id),
    role            VARCHAR(20) NOT NULL,

    -- 프로필
    name            VARCHAR(50) NOT NULL,
    phone           VARCHAR(20),

    -- 상태
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    email_verified  BOOLEAN NOT NULL DEFAULT FALSE,
    mfa_enabled     BOOLEAN NOT NULL DEFAULT FALSE,
    mfa_secret_enc  BYTEA,

    -- 감사
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    last_login_at   TIMESTAMPTZ,
    password_changed_at TIMESTAMPTZ,

    CONSTRAINT chk_role CHECK (role IN ('MASTER_ADMIN', 'ORG_ADMIN', 'USER')),
    CONSTRAINT chk_user_status CHECK (status IN ('PENDING', 'ACTIVE', 'SUSPENDED', 'TERMINATED'))
);

CREATE INDEX idx_user_org ON users (org_id);
CREATE INDEX idx_user_email ON users (email);
CREATE INDEX idx_user_status ON users (status);
```

### 4.3 business_entities - 사업자 정보 (분리)

**조직(organizations)과 사업자 정보 분리**. 동일 사업자번호가 여러 조직 유형(DISTRIBUTOR, DEALER 등)으로 등록될 수 있음.

```sql
CREATE TABLE business_entities (
    id              UUID PRIMARY KEY DEFAULT uuidv7(),

    -- 사업자 유형
    business_type   VARCHAR(20) NOT NULL,         -- CORPORATION, INDIVIDUAL, NON_BUSINESS

    -- 사업자등록 정보
    business_number VARCHAR(12),                  -- 000-00-00000 (비사업자는 NULL)
    corporate_number VARCHAR(14),                 -- 000000-0000000 (법인만 필수)

    -- 상호 정보
    business_name   VARCHAR(200) NOT NULL,        -- 상호명
    representative_name VARCHAR(100) NOT NULL,    -- 대표자명
    open_date       DATE,                         -- 개업연월일

    -- 주소
    business_address TEXT,                        -- 사업장 소재지
    actual_address  TEXT,                         -- 실사업장 소재지

    -- 업종/업태
    business_category VARCHAR(100),               -- 업태
    business_sub_category VARCHAR(100),           -- 업종

    -- 연락처
    main_phone      VARCHAR(20),                  -- 대표번호
    manager_name    VARCHAR(100),                 -- 담당자명
    manager_phone   VARCHAR(20),                  -- 담당자 연락처
    email           VARCHAR(255),

    -- 감사
    created_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at      TIMESTAMPTZ,                  -- 소프트 삭제

    CONSTRAINT chk_business_entity_type CHECK (
        business_type IN ('CORPORATION', 'INDIVIDUAL', 'NON_BUSINESS')
    ),
    -- 사업자번호 유일성 (NULL 제외)
    CONSTRAINT uq_business_entity_number UNIQUE NULLS NOT DISTINCT (business_number),
    -- 비사업자는 사업자번호 불가, 사업자는 사업자번호 필수
    CONSTRAINT chk_business_number_rule CHECK (
        (business_type = 'NON_BUSINESS' AND business_number IS NULL) OR
        (business_type != 'NON_BUSINESS' AND business_number IS NOT NULL)
    ),
    -- 법인사업자는 법인등록번호 필수
    CONSTRAINT chk_corporate_number_rule CHECK (
        (business_type = 'CORPORATION' AND corporate_number IS NOT NULL) OR
        (business_type != 'CORPORATION')
    )
);

-- 인덱스
CREATE INDEX idx_business_entity_number ON business_entities (business_number) WHERE business_number IS NOT NULL;
CREATE INDEX idx_business_entity_type ON business_entities (business_type);
CREATE INDEX idx_business_entity_name ON business_entities (business_name);
```

**organizations 테이블에 FK 추가**:

```sql
ALTER TABLE organizations 
    ADD COLUMN business_entity_id UUID REFERENCES business_entities(id);

CREATE INDEX idx_org_business_entity ON organizations (business_entity_id) WHERE business_entity_id IS NOT NULL;
```

### 4.4 businesses - 사업자 (레거시, 가맹점용)

**1 사업자 : N 가맹점 관계**. 동일 사업자가 수수료 체계가 다른 여러 가맹점을 가질 수 있음.

```sql
CREATE TABLE businesses (
    id              BIGSERIAL PRIMARY KEY,
    uuid            UUID NOT NULL DEFAULT uuidv7() UNIQUE,

    -- 사업자 유형: CORPORATE(법인), INDIVIDUAL(일반), NON_BUSINESS(비사업자)
    business_type   VARCHAR(20) NOT NULL,

    -- 사업자 정보
    business_no     VARCHAR(20),                  -- 사업자번호 (비사업자는 NULL)
    business_name   VARCHAR(100) NOT NULL,        -- 상호명
    representative  VARCHAR(50) NOT NULL,         -- 대표자명

    -- 업종/업태
    business_category VARCHAR(50),                -- 업종
    business_item   VARCHAR(100),                 -- 업태

    -- 연락처
    phone           VARCHAR(20),
    email           VARCHAR(255),
    address         TEXT,

    -- 상태
    status          VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',

    -- 감사
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by      BIGINT NOT NULL,

    CONSTRAINT chk_business_type CHECK (
        business_type IN ('CORPORATE', 'INDIVIDUAL', 'NON_BUSINESS')
    ),
    CONSTRAINT chk_business_status CHECK (
        status IN ('ACTIVE', 'SUSPENDED', 'TERMINATED')
    ),
    -- 사업자인 경우 사업자번호 필수
    CONSTRAINT chk_business_no_required CHECK (
        (business_type = 'NON_BUSINESS') OR (business_no IS NOT NULL)
    ),
    -- 사업자번호 유일성 (NULL 제외)
    CONSTRAINT uq_business_no UNIQUE (business_no)
);

-- 인덱스
CREATE INDEX idx_business_type ON businesses (business_type);
CREATE INDEX idx_business_status ON businesses (status);
CREATE INDEX idx_business_business_no ON businesses (business_no) WHERE business_no IS NOT NULL;
```

### 4.4 fee_policies - 수수료 정책

**수수료 정책 공유 및 버전 관리**. 여러 가맹점이 동일한 수수료 정책을 참조할 수 있음.

```sql
CREATE TABLE fee_policies (
    id              BIGSERIAL PRIMARY KEY,
    uuid            UUID NOT NULL DEFAULT uuidv7() UNIQUE,

    -- 정책 기본 정보
    code            VARCHAR(50) NOT NULL,         -- 정책 코드 (예: STANDARD_2026_01)
    name            VARCHAR(100) NOT NULL,        -- 정책명
    description     TEXT,

    -- 버전 관리
    version         INTEGER NOT NULL DEFAULT 1,
    effective_from  DATE NOT NULL,                -- 적용 시작일
    effective_to    DATE,                         -- 적용 종료일 (NULL = 현재 유효)

    -- 수수료율 정의 (결제수단 × 카드등급)
    rates           JSONB NOT NULL,

    -- 상태
    status          VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',

    -- 감사
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by      BIGINT NOT NULL,

    CONSTRAINT chk_fee_policy_status CHECK (
        status IN ('DRAFT', 'ACTIVE', 'INACTIVE', 'ARCHIVED')
    ),
    CONSTRAINT chk_effective_dates CHECK (
        effective_to IS NULL OR effective_to >= effective_from
    ),
    -- 동일 코드 내 버전 유일성
    CONSTRAINT uq_policy_code_version UNIQUE (code, version)
);

-- 인덱스
CREATE INDEX idx_fee_policy_code ON fee_policies (code);
CREATE INDEX idx_fee_policy_status ON fee_policies (status);
CREATE INDEX idx_fee_policy_effective ON fee_policies (effective_from, effective_to);
CREATE INDEX idx_fee_policy_rates ON fee_policies USING gin (rates);
```

**rates JSONB 구조 예시**:

```json
{
  "credit_card": {
    "micro": { "rate": 1.5, "type": "percentage" },
    "small1": { "rate": 2.0, "type": "percentage" },
    "small2": { "rate": 2.3, "type": "percentage" },
    "small3": { "rate": 2.5, "type": "percentage" },
    "normal": { "rate": 3.0, "type": "percentage" }
  },
  "debit_card": {
    "micro": { "rate": 1.0, "type": "percentage" },
    "small1": { "rate": 1.5, "type": "percentage" },
    "small2": { "rate": 1.8, "type": "percentage" },
    "small3": { "rate": 2.0, "type": "percentage" },
    "normal": { "rate": 2.5, "type": "percentage" }
  },
  "bank_transfer": {
    "default": { "amount": 300, "type": "fixed" }
  },
  "virtual_account": {
    "default": { "amount": 200, "type": "fixed" }
  }
}
```

### 4.5 merchants - 가맹점

**가맹점은 수수료 체계의 단위**. 동일 사업자라도 수수료 체계가 다르면 별도 가맹점으로 분리.

```sql
CREATE TABLE merchants (
    id              BIGSERIAL PRIMARY KEY,
    uuid            UUID NOT NULL DEFAULT uuidv7() UNIQUE,

    -- 소속 조직
    org_id          BIGINT NOT NULL REFERENCES organizations(id),
    org_path        LTREE NOT NULL,

    -- 사업자 참조 (1 사업자 : N 가맹점)
    business_id     BIGINT NOT NULL REFERENCES businesses(id),

    -- 기본 정보
    code            VARCHAR(20) NOT NULL UNIQUE,
    name            VARCHAR(100) NOT NULL,        -- 가맹점명 (상호와 다를 수 있음)

    -- 카드사 우대 등급
    card_grade      VARCHAR(20) NOT NULL DEFAULT 'NORMAL',

    -- 수수료 정책 참조 (N 가맹점 : 1 정책, 정책 공유 가능)
    fee_policy_id   BIGINT NOT NULL REFERENCES fee_policies(id),

    -- 정산 설정
    settlement_cycle VARCHAR(10) NOT NULL DEFAULT 'D+1',
    bank_code       VARCHAR(10),
    account_no_enc  BYTEA,                        -- 암호화된 계좌번호
    account_holder  VARCHAR(50),

    -- 상태
    status          VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',

    -- 감사
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by      BIGINT NOT NULL,

    CONSTRAINT chk_card_grade CHECK (
        card_grade IN ('MICRO', 'SMALL1', 'SMALL2', 'SMALL3', 'NORMAL')
    ),
    CONSTRAINT chk_merchant_status CHECK (status IN ('ACTIVE', 'SUSPENDED', 'TERMINATED'))
);

-- 인덱스
CREATE INDEX idx_merchant_org ON merchants (org_id);
CREATE INDEX idx_merchant_org_path ON merchants USING GIST (org_path);
CREATE INDEX idx_merchant_business ON merchants (business_id);
CREATE INDEX idx_merchant_fee_policy ON merchants (fee_policy_id);
CREATE INDEX idx_merchant_status ON merchants (status);
```

### 4.6 merchant_pg_mappings - 가맹점 PG 매핑

**KORPAY 연동 시**: MID(pg_merchant_no)와 단말기번호(terminal_id)는 1:1 매핑 관계.
MID는 온라인승인 계정으로도 사용됨.

```sql
CREATE TABLE merchant_pg_mappings (
    id              BIGSERIAL PRIMARY KEY,
    merchant_id     BIGINT NOT NULL REFERENCES merchants(id),
    pg_connection_id BIGINT NOT NULL,             -- public.pg_connections 참조

    -- PG사 가맹점 정보 (KORPAY: mid)
    pg_merchant_no  VARCHAR(50) NOT NULL,         -- KORPAY MID (온라인승인 계정)
    pg_merchant_key VARCHAR(100),                 -- 가맹점 인증키

    -- 단말기 정보 (KORPAY: catId, MID와 1:1 매핑)
    terminal_id     VARCHAR(50),                  -- 단말기 번호 (예: 1046347583)
    terminal_type   VARCHAR(20),                  -- POS, CAT, ONLINE, MOBILE

    -- 상태
    status          VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',

    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    -- KORPAY: MID와 terminal_id 1:1 관계 보장
    CONSTRAINT uq_pg_merchant UNIQUE (pg_connection_id, pg_merchant_no),
    CONSTRAINT uq_pg_terminal UNIQUE (pg_connection_id, terminal_id),
    CONSTRAINT chk_terminal_type CHECK (terminal_type IN ('POS', 'CAT', 'ONLINE', 'MOBILE'))
);

CREATE INDEX idx_pg_mapping_lookup ON merchant_pg_mappings (pg_connection_id, pg_merchant_no)
    WHERE status = 'ACTIVE';
CREATE INDEX idx_pg_mapping_terminal ON merchant_pg_mappings (pg_connection_id, terminal_id)
    WHERE status = 'ACTIVE';
CREATE INDEX idx_pg_mapping_merchant ON merchant_pg_mappings (merchant_id);
```

### 4.7 transactions - 거래 현재 상태

**하이브리드 방식**: 현재 상태를 저장하여 빠른 조회 제공 (이력은 transaction_events에서 관리)

```sql
CREATE TABLE transactions (
    id              BIGSERIAL PRIMARY KEY,
    uuid            UUID NOT NULL DEFAULT uuidv7() UNIQUE,

    -- PG 거래 정보
    pg_code         VARCHAR(20) NOT NULL,
    pg_tid          VARCHAR(100) NOT NULL,        -- 최초 승인 TID (KORPAY: tid)
    pg_merchant_no  VARCHAR(50) NOT NULL,         -- PG 가맹점 번호 (KORPAY: mid)
    terminal_id     VARCHAR(20),                  -- 단말기 ID (KORPAY: catId, MID와 1:1 매핑)
    channel_type    VARCHAR(10),                  -- 거래 채널 (KORPAY: connCd, 0003:단말기, 0005:온라인)
    van_tid         VARCHAR(50),                  -- VAN 거래고유번호 (KORPAY: ediNo)

    -- Bill&Pay 매핑
    merchant_id     BIGINT NOT NULL,
    merchant_path   LTREE NOT NULL,

    -- 거래 정보
    order_id        VARCHAR(40),                  -- 주문번호 (KORPAY: ordNo)
    original_amount BIGINT NOT NULL,              -- 원거래 금액 (불변, KORPAY: amt)
    current_amount  BIGINT NOT NULL,              -- 현재 유효 금액 (취소 반영)
    payment_method  VARCHAR(20) NOT NULL,         -- 결제수단 (KORPAY: payMethod)
    goods_name      VARCHAR(200),                 -- 상품명 (KORPAY: goodsName)

    -- 카드 정보
    card_code       VARCHAR(10),                  -- 카드사 코드
    card_type       VARCHAR(10),                  -- 카드 유형 (신용/체크)
    card_no_masked  VARCHAR(20),                  -- 마스킹된 카드번호 (KORPAY: cardNo)
    approval_no     VARCHAR(20),                  -- 승인번호 (KORPAY: appNo)
    installment     SMALLINT DEFAULT 0,           -- 할부개월 (KORPAY: quota)
    issuer_code     VARCHAR(10),                  -- 발급사 코드 (KORPAY: appCardCd)
    acquirer_code   VARCHAR(10),                  -- 매입사 코드 (KORPAY: acqCardCd)
    card_company_name VARCHAR(50),                -- 카드사명 (KORPAY: fnNm)

    -- 구매자 정보
    buyer_name      VARCHAR(100),                 -- 구매자명 (KORPAY: ordNm)
    buyer_id        VARCHAR(100),                 -- 구매자 ID (KORPAY: buyerId)

    -- 상태 (최종 상태)
    status          VARCHAR(20) NOT NULL,
    event_count     SMALLINT NOT NULL DEFAULT 1,  -- 이벤트 개수

    -- 시간
    transacted_at   TIMESTAMPTZ NOT NULL,         -- 최초 승인 시간 (KORPAY: appDtm)
    last_event_at   TIMESTAMPTZ NOT NULL,         -- 마지막 이벤트 시간
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_pg_tid UNIQUE (pg_code, pg_tid),
    CONSTRAINT chk_status CHECK (
        status IN ('APPROVED', 'CANCELED', 'PARTIAL_CANCELED')
    ),
    CONSTRAINT chk_channel_type CHECK (channel_type IN ('0003', '0005')),
    CONSTRAINT chk_current_amount CHECK (current_amount >= 0)
);

-- 인덱스
CREATE INDEX idx_txn_merchant ON transactions (merchant_id);
CREATE INDEX idx_txn_merchant_path ON transactions USING GIST (merchant_path);
CREATE INDEX idx_txn_pg_lookup ON transactions (pg_code, pg_merchant_no);
CREATE INDEX idx_txn_status ON transactions (status);
CREATE INDEX idx_txn_transacted ON transactions (transacted_at);
```

### 4.8 transaction_events - 거래 이벤트 이력 (파티셔닝)

**모든 거래 이벤트(승인/취소/부분취소)를 개별 레코드로 저장. 정산은 이 테이블 기준으로 처리.**

```sql
CREATE TABLE transaction_events (
    id              BIGSERIAL,
    uuid            UUID NOT NULL DEFAULT uuidv7(),

    -- 거래 참조
    transaction_id  BIGINT NOT NULL REFERENCES transactions(id),

    -- 이벤트 정보
    event_type      VARCHAR(20) NOT NULL,         -- APPROVED, CANCELED, PARTIAL_CANCELED
    event_seq       SMALLINT NOT NULL,            -- 순서 (1, 2, 3...)

    -- 금액 (부호 포함)
    amount          BIGINT NOT NULL,              -- +승인, -취소 (KORPAY: amt)
    remain_amount   BIGINT,                       -- 잔액 (부분취소 시, KORPAY: remainAmt)

    -- PG 응답 정보
    pg_tid          VARCHAR(100),                 -- 이벤트 TID (KORPAY: tid)
    pg_otid         VARCHAR(100),                 -- 원거래 TID (취소 시, KORPAY: otid)
    pg_response     JSONB,                        -- PG 원본 응답 전체

    -- 취소 정보
    is_cancel       BOOLEAN NOT NULL DEFAULT FALSE, -- 취소 여부 (KORPAY: cancelYN='Y')
    cancel_at       TIMESTAMPTZ,                  -- 취소일시 (KORPAY: ccDnt)

    -- 시간
    event_at        TIMESTAMPTZ NOT NULL,         -- 이벤트 발생시간 (KORPAY: appDtm 또는 ccDnt)
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    -- 복합 PK (파티셔닝용)
    PRIMARY KEY (id, created_at),

    CONSTRAINT uq_txn_event_seq UNIQUE (transaction_id, event_seq),
    CONSTRAINT chk_event_type CHECK (
        event_type IN ('APPROVED', 'CANCELED', 'PARTIAL_CANCELED')
    )
) PARTITION BY RANGE (created_at);

-- 인덱스
CREATE INDEX idx_evt_transaction ON transaction_events (transaction_id);
CREATE INDEX idx_evt_type ON transaction_events (event_type);
CREATE INDEX idx_evt_event_at ON transaction_events (event_at);
CREATE INDEX idx_evt_uuid ON transaction_events (uuid);

-- 파티션 생성 예시 (2026년 1월)
CREATE TABLE transaction_events_2026_01_28 PARTITION OF transaction_events
    FOR VALUES FROM ('2026-01-28') TO ('2026-01-29');
CREATE TABLE transaction_events_2026_01_29 PARTITION OF transaction_events
    FOR VALUES FROM ('2026-01-29') TO ('2026-01-30');
-- ... 계속
```

### 4.9 settlements - 정산 원장 (이벤트 기준)

**각 transaction_event에 대한 정산을 기록. 복식부기 원칙 적용.**

```sql
CREATE TABLE settlements (
    id              BIGSERIAL PRIMARY KEY,
    uuid            UUID NOT NULL DEFAULT uuidv7(),

    -- 이벤트 참조 (정산의 소스)
    transaction_event_id BIGINT NOT NULL,
    transaction_event_at TIMESTAMPTZ NOT NULL,    -- 파티션 조인용

    -- 거래 참조 (조회 편의용)
    transaction_id  BIGINT NOT NULL,

    -- 수취인
    entity_type     VARCHAR(20) NOT NULL,
    entity_id       BIGINT NOT NULL,
    entity_path     LTREE,

    -- 금액
    entry_type      VARCHAR(10) NOT NULL,         -- CREDIT (승인), DEBIT (취소)
    amount          BIGINT NOT NULL,              -- 항상 양수
    fee_rate        DECIMAL(5,4),
    description     VARCHAR(100),

    -- 정산 상태
    settlement_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    settlement_date   DATE,
    settled_at        TIMESTAMPTZ,

    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_entry_type CHECK (entry_type IN ('CREDIT', 'DEBIT')),
    CONSTRAINT chk_entity_type CHECK (entity_type IN ('MERCHANT', 'ORGANIZATION', 'MASTER')),
    CONSTRAINT chk_settlement_status CHECK (
        settlement_status IN ('PENDING', 'CONFIRMED', 'PAID', 'HELD')
    )
);

-- 인덱스
CREATE INDEX idx_stl_event ON settlements (transaction_event_id, transaction_event_at);
CREATE INDEX idx_stl_transaction ON settlements (transaction_id);
CREATE INDEX idx_stl_entity ON settlements (entity_type, entity_id);
CREATE INDEX idx_stl_entity_path ON settlements USING GIST (entity_path);
CREATE INDEX idx_stl_status ON settlements (settlement_status);
CREATE INDEX idx_stl_date ON settlements (settlement_date);
```

### 4.10 unmapped_transactions - 미매핑 거래

```sql
CREATE TABLE unmapped_transactions (
    id              BIGSERIAL PRIMARY KEY,
    uuid            UUID NOT NULL DEFAULT uuidv7() UNIQUE,

    pg_connection_id BIGINT NOT NULL,
    pg_tid          VARCHAR(100) NOT NULL,
    pg_merchant_no  VARCHAR(50) NOT NULL,

    raw_data        JSONB NOT NULL,
    amount          BIGINT NOT NULL,
    transacted_at   TIMESTAMPTZ NOT NULL,

    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    mapped_merchant_id BIGINT REFERENCES merchants(id),
    processed_by    BIGINT REFERENCES users(id),
    processed_at    TIMESTAMPTZ,
    process_note    TEXT,

    received_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_unmapped_status CHECK (status IN ('PENDING', 'MAPPED', 'IGNORED', 'EXPIRED'))
);

CREATE INDEX idx_unmapped_status ON unmapped_transactions (status);
CREATE INDEX idx_unmapped_pg ON unmapped_transactions (pg_connection_id, pg_merchant_no);
```

### 4.11 notification_settings - 알림 설정

```sql
CREATE TABLE notification_settings (
    id              BIGSERIAL PRIMARY KEY,

    target_type     VARCHAR(20) NOT NULL,
    target_id       BIGINT,

    -- 총판용
    pg_error_enabled        BOOLEAN DEFAULT TRUE,
    unmapped_txn_enabled    BOOLEAN DEFAULT TRUE,
    batch_error_enabled     BOOLEAN DEFAULT TRUE,
    daily_report_enabled    BOOLEAN DEFAULT TRUE,

    -- 대리점용
    payment_success_enabled BOOLEAN DEFAULT TRUE,
    payment_cancel_enabled  BOOLEAN DEFAULT TRUE,
    settlement_enabled      BOOLEAN DEFAULT TRUE,

    -- 채널
    push_enabled    BOOLEAN DEFAULT TRUE,
    email_enabled   BOOLEAN DEFAULT FALSE,
    sms_enabled     BOOLEAN DEFAULT FALSE,

    -- Webhook
    webhook_url     VARCHAR(500),
    webhook_secret  VARCHAR(100),
    webhook_enabled BOOLEAN DEFAULT FALSE,

    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_notification_target UNIQUE (target_type, target_id),
    CONSTRAINT chk_target_type CHECK (target_type IN ('MASTER', 'ORGANIZATION'))
);
```

### 4.12 audit_logs - 감사 로그

```sql
CREATE TABLE audit_logs (
    id              BIGSERIAL PRIMARY KEY,
    uuid            UUID NOT NULL DEFAULT uuidv7(),

    -- 행위자
    user_id         BIGINT,
    user_email      VARCHAR(255),
    ip_address      INET,
    user_agent      TEXT,

    -- 대상
    entity_type     VARCHAR(50) NOT NULL,
    entity_id       BIGINT,

    -- 행위
    action          VARCHAR(50) NOT NULL,
    action_detail   TEXT,

    -- 변경 내용
    old_values      JSONB,
    new_values      JSONB,

    -- 시간
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_action CHECK (
        action IN ('CREATE', 'UPDATE', 'DELETE', 'LOGIN', 'LOGOUT', 'EXPORT', 'APPROVE', 'REJECT')
    )
);

CREATE INDEX idx_audit_user ON audit_logs (user_id);
CREATE INDEX idx_audit_entity ON audit_logs (entity_type, entity_id);
CREATE INDEX idx_audit_action ON audit_logs (action);
CREATE INDEX idx_audit_created ON audit_logs (created_at);

-- 파티셔닝 고려 (대량 데이터 시)
```

---

## 5. Materialized Views

### 5.1 일별 정산 요약 (이벤트 기준)

```sql
CREATE MATERIALIZED VIEW mv_daily_settlement_summary AS
SELECT
    DATE(e.event_at) AS event_date,
    s.entity_type,
    s.entity_id,
    s.entity_path,
    COUNT(DISTINCT e.transaction_id) AS transaction_count,
    COUNT(e.id) AS event_count,
    SUM(CASE WHEN s.entry_type = 'CREDIT' THEN s.amount ELSE 0 END) AS credit_total,
    SUM(CASE WHEN s.entry_type = 'DEBIT' THEN s.amount ELSE 0 END) AS debit_total,
    SUM(CASE WHEN s.entry_type = 'CREDIT' THEN s.amount ELSE -s.amount END) AS net_total
FROM transaction_events e
JOIN settlements s ON e.id = s.transaction_event_id
GROUP BY DATE(e.event_at), s.entity_type, s.entity_id, s.entity_path;

CREATE UNIQUE INDEX idx_mv_daily_unique
    ON mv_daily_settlement_summary (event_date, entity_type, entity_id);
CREATE INDEX idx_mv_daily_path ON mv_daily_settlement_summary USING GIST (entity_path);

-- 갱신 (매일 자정)
-- REFRESH MATERIALIZED VIEW CONCURRENTLY mv_daily_settlement_summary;
```

### 5.2 가맹점별 월간 매출 (현재 상태 기준)

```sql
CREATE MATERIALIZED VIEW mv_monthly_merchant_sales AS
SELECT
    DATE_TRUNC('month', t.transacted_at) AS month,
    t.merchant_id,
    m.name AS merchant_name,
    m.org_path,
    COUNT(*) AS transaction_count,
    SUM(t.original_amount) AS total_original,   -- 원거래 금액 합계
    SUM(t.current_amount) AS total_current,     -- 현재 유효 금액 합계
    AVG(t.original_amount) AS avg_transaction
FROM transactions t
JOIN merchants m ON t.merchant_id = m.id
GROUP BY DATE_TRUNC('month', t.transacted_at), t.merchant_id, m.name, m.org_path;

CREATE UNIQUE INDEX idx_mv_monthly_unique
    ON mv_monthly_merchant_sales (month, merchant_id);
CREATE INDEX idx_mv_monthly_path ON mv_monthly_merchant_sales USING GIST (org_path);
```

---

## 6. Functions & Procedures

### 6.1 테넌트 스키마 생성

```sql
CREATE OR REPLACE FUNCTION public.create_tenant_schema(
    p_tenant_code VARCHAR(20),
    p_tenant_name VARCHAR(100)
)
RETURNS VARCHAR AS $$
DECLARE
    v_schema_name VARCHAR(50);
BEGIN
    v_schema_name := 'tenant_' || p_tenant_code;

    -- 스키마 생성
    EXECUTE format('CREATE SCHEMA IF NOT EXISTS %I', v_schema_name);

    -- 테넌트 테이블에 등록
    INSERT INTO public.tenants (code, name, schema_name)
    VALUES (p_tenant_code, p_tenant_name, v_schema_name);

    -- 테이블 생성 (별도 마이그레이션 스크립트 호출)
    -- CALL create_tenant_tables(v_schema_name);

    RETURN v_schema_name;
END;
$$ LANGUAGE plpgsql;
```

### 6.2 파티션 자동 생성 (transaction_events)

```sql
CREATE OR REPLACE PROCEDURE create_daily_event_partition(
    p_schema_name VARCHAR(50),
    p_date DATE
)
AS $$
DECLARE
    v_partition_name VARCHAR(100);
    v_start_date DATE;
    v_end_date DATE;
BEGIN
    v_partition_name := 'transaction_events_' || TO_CHAR(p_date, 'YYYY_MM_DD');
    v_start_date := p_date;
    v_end_date := p_date + INTERVAL '1 day';

    EXECUTE format(
        'CREATE TABLE IF NOT EXISTS %I.%I PARTITION OF %I.transaction_events
         FOR VALUES FROM (%L) TO (%L)',
        p_schema_name, v_partition_name, p_schema_name,
        v_start_date, v_end_date
    );
END;
$$ LANGUAGE plpgsql;

-- 매일 실행: 7일 후 파티션 미리 생성
-- CALL create_daily_event_partition('tenant_001', CURRENT_DATE + INTERVAL '7 days');
```

### 6.3 이벤트-거래 정합성 검증 함수

```sql
CREATE OR REPLACE FUNCTION verify_transaction_event_consistency(
    p_transaction_id BIGINT
)
RETURNS BOOLEAN AS $$
DECLARE
    v_current_amount BIGINT;
    v_event_total BIGINT;
BEGIN
    -- 거래 현재 금액 조회
    SELECT current_amount INTO v_current_amount
    FROM transactions
    WHERE id = p_transaction_id;

    -- 이벤트 합계 조회
    SELECT COALESCE(SUM(amount), 0) INTO v_event_total
    FROM transaction_events
    WHERE transaction_id = p_transaction_id;

    RETURN v_current_amount = v_event_total;
END;
$$ LANGUAGE plpgsql;
```

### 6.4 이벤트-정산 Zero-Sum 검증 함수

```sql
CREATE OR REPLACE FUNCTION verify_event_zero_sum(
    p_event_id BIGINT
)
RETURNS BOOLEAN AS $$
DECLARE
    v_event_amount BIGINT;
    v_stl_total BIGINT;
BEGIN
    -- 이벤트 금액 조회
    SELECT ABS(amount) INTO v_event_amount
    FROM transaction_events
    WHERE id = p_event_id;

    -- 정산 합계 조회
    SELECT COALESCE(SUM(amount), 0) INTO v_stl_total
    FROM settlements
    WHERE transaction_event_id = p_event_id;

    RETURN v_event_amount = v_stl_total;
END;
$$ LANGUAGE plpgsql;
```

---

## 7. ERD (Entity Relationship Diagram)

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              PUBLIC SCHEMA                                   │
├─────────────────────────────────────────────────────────────────────────────┤
│  ┌──────────────┐    ┌──────────────┐    ┌──────────────┐                  │
│  │   tenants    │    │ pg_connections│    │ card_companies│                  │
│  ├──────────────┤    ├──────────────┤    ├──────────────┤                  │
│  │ id           │    │ id           │    │ id           │                  │
│  │ code         │    │ pg_code      │    │ code         │                  │
│  │ schema_name  │    │ webhook_path │    │ name         │                  │
│  └──────────────┘    └──────────────┘    └──────────────┘                  │
└─────────────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────────────┐
│                             TENANT SCHEMA                                    │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  ┌──────────────┐         ┌──────────────┐                                  │
│  │organizations │◄────────│   users      │                                  │
│  ├──────────────┤    org_id├──────────────┤                                  │
│  │ id           │         │ id           │                                  │
│  │ path (ltree) │         │ email        │                                  │
│  │ parent_id    │─────────│ org_id       │                                  │
│  │ fee_policy   │    self │ role         │                                  │
│  └──────┬───────┘         └──────────────┘                                  │
│         │                                                                    │
│         │ org_id                                                             │
│         ▼                                                                    │
│  ┌──────────────┐    ┌──────────────┐    ┌──────────────┐                  │
│  │  merchants   │◄───│  businesses  │    │ fee_policies │                  │
│  ├──────────────┤ N:1├──────────────┤    ├──────────────┤                  │
│  │ id           │    │ id           │    │ id           │                  │
│  │ org_id       │    │ business_type│    │ code         │                  │
│  │ org_path     │    │ business_no  │    │ version      │                  │
│  │ business_id ─┼───►│ business_name│    │ rates (JSONB)│                  │
│  │ fee_policy_id├───────────────────────►│ effective_from│                  │
│  │ card_grade   │ N:1│ representative│    │ status       │                  │
│  │ name         │    │ phone, email │    └──────────────┘                  │
│  └──────┬───────┘    │ address      │                                      │
│         │            └──────────────┘                                      │
│         │ merchant_id                                                       │
│         ▼                                                                   │
│  ┌──────────────┐                                                          │
│  │merchant_pg   │                                                          │
│  │_mappings     │  (1 merchant : N MID/단말기)                              │
│  ├──────────────┤                                                          │
│  │ pg_merchant_no│                                                          │
│  │ terminal_id  │                                                          │
│  └──────────────┘                                                          │
│         │                                                                   │
│         │ merchant_id                                                       │
│         ▼                                                                   │
│  ┌──────────────────┐                                                       │
│  │  transactions    │  (현재 상태)                                          │
│  ├──────────────────┤                                                       │
│  │ id               │                                                       │
│  │ original_amount  │                                                       │
│  │ current_amount   │◄──── 이벤트 합계와 일치                                │
│  │ status           │                                                       │
│  └────────┬─────────┘                                                       │
│           │                                                                  │
│           │ transaction_id                                                   │
│           ▼                                                                  │
│  ┌──────────────────┐                                                       │
│  │transaction_events│  (이벤트 이력, 파티셔닝)                                │
│  ├──────────────────┤                                                       │
│  │ id               │                                                       │
│  │ transaction_id   │                                                       │
│  │ event_type       │  APPROVED / CANCELED / PARTIAL_CANCELED               │
│  │ amount           │  +승인 / -취소                                         │
│  │ event_seq        │  1, 2, 3...                                           │
│  └────────┬─────────┘                                                       │
│           │                                                                  │
│           │ transaction_event_id                                             │
│           ▼                                                                  │
│  ┌──────────────────┐                                                       │
│  │   settlements    │  (이벤트별 정산, 복식부기)                              │
│  ├──────────────────┤                                                       │
│  │ id               │                                                       │
│  │ transaction_event_id │◄── 정산의 소스                                     │
│  │ transaction_id   │◄── 조회 편의용                                         │
│  │ entity_type      │  MERCHANT / ORGANIZATION / MASTER                     │
│  │ entry_type       │  CREDIT (승인) / DEBIT (취소)                          │
│  │ amount           │  항상 양수                                             │
│  └──────────────────┘                                                       │
│                                                                              │
│  [주요 관계]                                                                 │
│  • 1 Business : N Merchants (1 사업자 다수 가맹점)                           │
│  • 1 FeePolicy : N Merchants (수수료 정책 공유)                              │
│  • 1 Organization : N Merchants (조직 계층 소속)                             │
│  • 1 Merchant : N PG Mappings (다중 PG/단말기)                               │
│                                                                              │
│  [데이터 흐름]                                                               │
│  PG Webhook → transactions INSERT → transaction_events INSERT               │
│            → settlements INSERT (이벤트 기준 복식부기)                        │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 8. 인덱스 전략

### 8.1 인덱스 유형별 용도

| 인덱스 유형 | 용도 | 적용 컬럼 |
|------------|------|----------|
| B-tree | 일반 조회, 범위 검색 | id, created_at, status |
| GiST | ltree 계층 쿼리 | path, org_path, merchant_path |
| Hash | 동등 비교 (대량) | uuid, pg_tid |
| Partial | 조건부 인덱스 | status = 'ACTIVE' 조건 |

### 8.2 주요 쿼리별 인덱스

| 쿼리 패턴 | 인덱스 |
|----------|--------|
| 하위 조직 조회 | `idx_org_path_gist` (GiST on path) |
| 가맹점 PG 매핑 조회 | `idx_pg_mapping_lookup` (Partial) |
| 거래 내역 조회 | `idx_txn_merchant` + Partition Pruning |
| 정산 집계 | `idx_stl_entity_path` (GiST) |

---

## 9. 마이그레이션 전략

### 9.1 Flyway 설정

```
flyway/
├── public/                    # 공통 스키마
│   ├── V1__create_tenants.sql
│   ├── V2__create_pg_connections.sql
│   └── V3__create_master_data.sql
│
└── tenant/                    # 테넌트 스키마 (템플릿)
    ├── V1__create_organizations.sql
    ├── V2__create_users.sql
    ├── V3__create_businesses.sql           # 사업자
    ├── V4__create_fee_policies.sql         # 수수료 정책
    ├── V5__create_merchants.sql
    ├── V6__create_merchant_pg_mappings.sql
    ├── V7__create_transactions.sql
    ├── V8__create_transaction_events.sql   # 이벤트 이력
    ├── V9__create_settlements.sql          # 이벤트 기준 정산
    └── V10__create_audit_logs.sql
```

### 9.2 테넌트 마이그레이션 실행

```java
@Service
public class TenantMigrationService {

    public void migrateAllTenants() {
        List<Tenant> tenants = tenantRepository.findAll();

        for (Tenant tenant : tenants) {
            Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .schemas(tenant.getSchemaName())
                .locations("classpath:flyway/tenant")
                .load();

            flyway.migrate();
        }
    }
}
```

---

## 변경 이력

| 버전 | 날짜 | 변경 내용 |
|------|------|----------|
| v1.0 | 2026-01-28 | 초안 작성 |
| v2.0 | 2026-01-28 | 하이브리드 이벤트 소싱 방식 적용 - transactions(현재상태) + transaction_events(이력) 분리 |
| v3.0 | 2026-01-29 | KORPAY 연동 필드 추가 (GID/VID 제외), MID-단말기 1:1 매핑 반영 |
| v4.0 | 2026-01-29 | 사업자-가맹점 분리 (businesses 테이블 추가), 수수료 정책 분리 (fee_policies 테이블 추가), merchants 테이블에서 business_id/fee_policy_id FK 참조로 변경 |
