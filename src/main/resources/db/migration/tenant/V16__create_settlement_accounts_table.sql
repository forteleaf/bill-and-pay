CREATE TYPE account_status AS ENUM ('ACTIVE', 'INACTIVE', 'PENDING_VERIFICATION');

CREATE TABLE settlement_accounts (
    id UUID PRIMARY KEY DEFAULT uuidv7(),
    bank_code VARCHAR(10) NOT NULL,
    bank_name VARCHAR(50) NOT NULL,
    account_number VARCHAR(50) NOT NULL,
    account_holder VARCHAR(100) NOT NULL,
    entity_type contact_entity_type NOT NULL,
    entity_id UUID NOT NULL,
    is_primary BOOLEAN NOT NULL DEFAULT false,
    status account_status NOT NULL DEFAULT 'PENDING_VERIFICATION',
    verified_at TIMESTAMPTZ,
    memo TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMPTZ
);

CREATE INDEX idx_settlement_accounts_entity ON settlement_accounts(entity_type, entity_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_settlement_accounts_status ON settlement_accounts(status) WHERE deleted_at IS NULL;
CREATE UNIQUE INDEX idx_settlement_accounts_primary ON settlement_accounts(entity_type, entity_id) 
    WHERE is_primary = true AND deleted_at IS NULL;

COMMENT ON TABLE settlement_accounts IS '정산계좌 정보 (사업자/가맹점 공용)';
COMMENT ON COLUMN settlement_accounts.bank_code IS '은행코드 (예: 004, 088)';
COMMENT ON COLUMN settlement_accounts.bank_name IS '은행명 (예: KB국민은행)';
COMMENT ON COLUMN settlement_accounts.account_number IS '계좌번호';
COMMENT ON COLUMN settlement_accounts.account_holder IS '예금주';
COMMENT ON COLUMN settlement_accounts.entity_type IS 'BUSINESS_ENTITY 또는 MERCHANT';
COMMENT ON COLUMN settlement_accounts.entity_id IS 'business_entities.id 또는 merchants.id';
COMMENT ON COLUMN settlement_accounts.is_primary IS '주 정산계좌 여부 (엔티티당 1개만 가능)';
COMMENT ON COLUMN settlement_accounts.status IS 'ACTIVE(사용중), INACTIVE(미사용), PENDING_VERIFICATION(검증대기)';
COMMENT ON COLUMN settlement_accounts.verified_at IS '계좌 검증 완료 시각';
COMMENT ON COLUMN settlement_accounts.memo IS '메모';
