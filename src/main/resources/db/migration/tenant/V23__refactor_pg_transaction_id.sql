-- =============================================================================
-- V23: PG사별 거래고유번호 중복 방지 + 스키마 개선
-- =============================================================================
-- 변경 내용:
-- 1. transactions 테이블에서 tid 컬럼 제거 (pg_transaction_id로 통합)
-- 2. transaction_events 테이블에서 tid 컬럼 제거
-- 3. PG별 거래고유번호 유니크 인덱스 추가
-- 4. Covering 인덱스 추가 (Index-Only Scan 지원)
-- =============================================================================

-- =============================================================================
-- 1. transactions 테이블
-- =============================================================================

-- 기존 tid 인덱스 제거
DROP INDEX IF EXISTS idx_transactions_tid;

-- tid 컬럼 제거
ALTER TABLE transactions DROP COLUMN IF EXISTS tid;

-- PG별 거래고유번호 유니크 인덱스 (중복 방지)
-- 같은 PG 연결 내에서 pg_transaction_id는 유일해야 함
CREATE UNIQUE INDEX idx_transactions_pg_txn_unique
  ON transactions(pg_connection_id, pg_transaction_id)
  WHERE pg_transaction_id IS NOT NULL;

-- Covering 인덱스 (Index-Only Scan 지원)
-- 웹훅 중복 체크 시 테이블 접근 없이 인덱스만으로 조회 가능
DROP INDEX IF EXISTS idx_transactions_pg_transaction_id;
CREATE INDEX idx_transactions_pg_txn_lookup
  ON transactions(pg_connection_id, pg_transaction_id)
  INCLUDE (id, status, amount, merchant_id)
  WHERE pg_transaction_id IS NOT NULL;

-- =============================================================================
-- 2. transaction_events 테이블 (파티션)
-- =============================================================================

-- tid 컬럼 제거 (파티션 테이블)
ALTER TABLE transaction_events DROP COLUMN IF EXISTS tid;

-- PG별 거래 조회 인덱스
CREATE INDEX idx_transaction_events_pg_txn
  ON transaction_events(pg_connection_id, pg_transaction_id, created_at DESC)
  WHERE pg_transaction_id IS NOT NULL;

-- =============================================================================
-- 코멘트 업데이트
-- =============================================================================
COMMENT ON COLUMN transactions.pg_transaction_id IS 'PG사 거래고유번호 - PG별 유니크 (기존 tid 통합)';
COMMENT ON INDEX idx_transactions_pg_txn_unique IS 'PG별 거래고유번호 중복 방지';
COMMENT ON INDEX idx_transactions_pg_txn_lookup IS '웹훅 중복 체크 Index-Only Scan';
