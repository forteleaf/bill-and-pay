-- =============================================================================
-- V5: Webhook 멱등성(Idempotency) + 미매핑 거래(Unmapped Transactions) 관리
-- =============================================================================
-- 목적:
--  1. webhook_idempotency_keys: Webhook 중복 처리 방지 (멱등성)
--  2. unmapped_transactions: PG에서 수신했으나 가맹점 매핑 실패한 거래 관리
--  3. transaction_events DEFAULT 파티션: 범위 밖 데이터 안전망
--  4. settlements 상태에 'PENDING_REVIEW' 추가: Zero-Sum 검증 실패 시 처리
--
-- 참고: PRD-04 (PG 연동), PRD-05 (DB 스키마)
-- =============================================================================

-- =============================================================================
-- 1. webhook_idempotency_keys 테이블
-- =============================================================================
-- 목적: 동일한 pg_tid가 중복으로 수신될 경우 멱등성 보장
-- 사용: Webhook 수신 시 먼저 이 테이블에서 pg_tid 확인
--   - PROCESSING: 처리 중 → 응답 대기
--   - COMPLETED: 완료 → 중복 이벤트 무시
--   - FAILED: 실패 → 재시도 가능
CREATE TABLE webhook_idempotency_keys (
    pg_connection_id BIGINT NOT NULL,
    pg_tid VARCHAR(200) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PROCESSING',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (pg_connection_id, pg_tid),
    CONSTRAINT webhook_idempotency_status_check CHECK (
        status IN ('PROCESSING', 'COMPLETED', 'FAILED')
    )
);

-- 오래된 키 정리용 인덱스 (배치 정리 작업에서 사용)
CREATE INDEX idx_webhook_idempotency_created ON webhook_idempotency_keys (created_at);

COMMENT ON TABLE webhook_idempotency_keys IS '웹훅 멱등성 관리 (중복 처리 방지)';
COMMENT ON COLUMN webhook_idempotency_keys.pg_connection_id IS 'PG 연결 ID';
COMMENT ON COLUMN webhook_idempotency_keys.pg_tid IS 'PG사 거래 ID (고유)';
COMMENT ON COLUMN webhook_idempotency_keys.status IS '처리 상태 (PROCESSING, COMPLETED, FAILED)';
COMMENT ON COLUMN webhook_idempotency_keys.created_at IS '키 생성일시';
COMMENT ON COLUMN webhook_idempotency_keys.updated_at IS '상태 변경일시';

-- =============================================================================
-- 2. unmapped_transactions 테이블
-- =============================================================================
-- 목적: PG에서 수신한 거래 중 가맹점 매핑 실패한 거래 관리
-- 상태:
--   - PENDING: 수신 직후, 매핑 시도 전
--   - MAPPED: 관리자가 수동 매핑 완료 → settlement 자동 생성
--   - IGNORED: 중복/오류/테스트 거래로 판단, 처리 안 함
--   - HELD: Zero-Sum 검증 실패로 PENDING_REVIEW 상태 정산 생성 (PRD-03)
CREATE TABLE unmapped_transactions (
    id UUID PRIMARY KEY DEFAULT uuidv7(),
    pg_connection_id BIGINT NOT NULL,
    pg_tid VARCHAR(200) NOT NULL,
    pg_merchant_no VARCHAR(50) NOT NULL,
    raw_data JSONB NOT NULL,
    amount BIGINT NOT NULL,
    transacted_at TIMESTAMPTZ NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    mapped_merchant_id UUID,
    processed_by UUID,
    processed_at TIMESTAMPTZ,
    process_note TEXT,
    received_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_unmapped_status CHECK (
        status IN ('PENDING', 'MAPPED', 'IGNORED', 'HELD')
    )
);

CREATE INDEX idx_unmapped_status ON unmapped_transactions (status);
CREATE INDEX idx_unmapped_pg ON unmapped_transactions (pg_connection_id, pg_merchant_no);
CREATE INDEX idx_unmapped_received ON unmapped_transactions (received_at);

COMMENT ON TABLE unmapped_transactions IS '미매핑 거래 관리 (가맹점 매핑 실패 거래)';
COMMENT ON COLUMN unmapped_transactions.id IS '미매핑 거래 고유 ID (UUID v7)';
COMMENT ON COLUMN unmapped_transactions.pg_connection_id IS 'PG 연결 ID';
COMMENT ON COLUMN unmapped_transactions.pg_tid IS 'PG사 거래 ID';
COMMENT ON COLUMN unmapped_transactions.pg_merchant_no IS 'PG사 가맹점 번호';
COMMENT ON COLUMN unmapped_transactions.raw_data IS '수신한 원본 거래 데이터 (JSONB)';
COMMENT ON COLUMN unmapped_transactions.amount IS '거래 금액 (단위: 원)';
COMMENT ON COLUMN unmapped_transactions.transacted_at IS 'PG사 거래 발생 시각';
COMMENT ON COLUMN unmapped_transactions.status IS '처리 상태 (PENDING, MAPPED, IGNORED, HELD)';
COMMENT ON COLUMN unmapped_transactions.mapped_merchant_id IS '수동 매핑 후 할당된 가맹점 ID';
COMMENT ON COLUMN unmapped_transactions.processed_by IS '처리 담당자 사용자 ID';
COMMENT ON COLUMN unmapped_transactions.processed_at IS '처리 완료일시';
COMMENT ON COLUMN unmapped_transactions.process_note IS '처리 메모/사유';
COMMENT ON COLUMN unmapped_transactions.received_at IS '수신일시';

-- =============================================================================
-- 3. transaction_events DEFAULT 파티션
-- =============================================================================
-- 목적: transaction_events에 대한 범위 파티션의 안전망
-- 사용: 정의되지 않은 날짜 범위의 데이터를 여기에 저장
-- 주의: 파티션 추가 시 마다 유지보수 필수
CREATE TABLE IF NOT EXISTS transaction_events_default
    PARTITION OF transaction_events DEFAULT;

COMMENT ON TABLE transaction_events_default IS '거래 이벤트 DEFAULT 파티션 (범위 밖 데이터 안전망)';

-- =============================================================================
-- 4. settlements 상태 CHECK 제약 업데이트
-- =============================================================================
-- 목적: 'PENDING_REVIEW' 상태 추가
--   - Zero-Sum 검증 실패로 정산 생성 후 수동 검토 대기 상태
-- 참고: PRD-03 하이브리드 이벤트 소싱 - Zero-Sum 검증 실패 처리

-- 기존 CHECK 제약 제거
ALTER TABLE settlements DROP CONSTRAINT IF EXISTS settlements_status_check;

-- 새로운 CHECK 제약 추가 ('PENDING_REVIEW' 포함)
ALTER TABLE settlements ADD CONSTRAINT settlements_status_check CHECK (
    status IN ('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED', 'CANCELLED', 'PENDING_REVIEW')
);

COMMENT ON COLUMN settlements.status IS '정산 상태 (PENDING, PROCESSING, COMPLETED, FAILED, CANCELLED, PENDING_REVIEW)';
