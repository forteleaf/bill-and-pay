-- =============================================================================
-- V4: 가맹점 정산 주기 필드 추가
-- =============================================================================

ALTER TABLE merchants ADD COLUMN settlement_cycle VARCHAR(20) NOT NULL DEFAULT 'D_PLUS_1';

ALTER TABLE merchants ADD CONSTRAINT merchants_settlement_cycle_check
    CHECK (settlement_cycle IN ('D_PLUS_1', 'D_PLUS_3', 'REALTIME'));

COMMENT ON COLUMN merchants.settlement_cycle IS '정산 주기 (D_PLUS_1: 익영업일, D_PLUS_3: 3영업일, REALTIME: 당일정산)';
