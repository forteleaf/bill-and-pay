-- V10: 가맹점 조직 이동 이력 테이블
-- 가맹점이 영업조직을 이동할 때마다 이력을 기록

CREATE TABLE merchant_org_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    merchant_id UUID NOT NULL,
    from_org_id UUID NOT NULL,
    from_org_path LTREE NOT NULL,
    to_org_id UUID NOT NULL,
    to_org_path LTREE NOT NULL,
    moved_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    moved_by VARCHAR(100), -- 이동 처리한 사용자 ID
    reason TEXT, -- 이동 사유
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_merchant_org_history_merchant 
        FOREIGN KEY (merchant_id) REFERENCES merchants(id) ON DELETE CASCADE,
    CONSTRAINT fk_merchant_org_history_from_org 
        FOREIGN KEY (from_org_id) REFERENCES organizations(id) ON DELETE RESTRICT,
    CONSTRAINT fk_merchant_org_history_to_org 
        FOREIGN KEY (to_org_id) REFERENCES organizations(id) ON DELETE RESTRICT
);

-- 인덱스
CREATE INDEX idx_merchant_org_history_merchant_id ON merchant_org_history(merchant_id);
CREATE INDEX idx_merchant_org_history_moved_at ON merchant_org_history(moved_at DESC);
CREATE INDEX idx_merchant_org_history_from_org ON merchant_org_history(from_org_id);
CREATE INDEX idx_merchant_org_history_to_org ON merchant_org_history(to_org_id);
CREATE INDEX idx_merchant_org_history_from_path ON merchant_org_history USING GIST(from_org_path);
CREATE INDEX idx_merchant_org_history_to_path ON merchant_org_history USING GIST(to_org_path);

-- 주석
COMMENT ON TABLE merchant_org_history IS '가맹점 조직 이동 이력';
COMMENT ON COLUMN merchant_org_history.merchant_id IS '가맹점 ID';
COMMENT ON COLUMN merchant_org_history.from_org_id IS '이동 전 조직 ID';
COMMENT ON COLUMN merchant_org_history.from_org_path IS '이동 전 조직 경로';
COMMENT ON COLUMN merchant_org_history.to_org_id IS '이동 후 조직 ID';
COMMENT ON COLUMN merchant_org_history.to_org_path IS '이동 후 조직 경로';
COMMENT ON COLUMN merchant_org_history.moved_at IS '이동 일시';
COMMENT ON COLUMN merchant_org_history.moved_by IS '이동 처리자';
COMMENT ON COLUMN merchant_org_history.reason IS '이동 사유';
