ALTER TABLE merchants ADD COLUMN corporate_number VARCHAR(20);
ALTER TABLE merchants ADD COLUMN representative_name VARCHAR(100);

COMMENT ON COLUMN merchants.corporate_number IS '법인등록번호 (숫자만)';
COMMENT ON COLUMN merchants.representative_name IS '대표자명';
