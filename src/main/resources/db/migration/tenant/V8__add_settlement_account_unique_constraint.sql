-- 기존 중복 데이터 검사 (중복 시 마이그레이션 실패)
DO $$
DECLARE dup_count INTEGER;
BEGIN
  SELECT COUNT(*) INTO dup_count FROM (
    SELECT entity_type, entity_id, bank_code, account_number
    FROM settlement_accounts
    WHERE deleted_at IS NULL
    GROUP BY entity_type, entity_id, bank_code, account_number
    HAVING COUNT(*) > 1
  ) duplicates;

  IF dup_count > 0 THEN
    RAISE EXCEPTION '중복 계좌 데이터 % 건 발견. 수동 정리 후 재실행 필요', dup_count;
  END IF;
END $$;

-- Partial Unique Index (soft delete 고려)
CREATE UNIQUE INDEX idx_settlement_accounts_unique_per_entity
  ON settlement_accounts (entity_type, entity_id, bank_code, account_number)
  WHERE deleted_at IS NULL;
