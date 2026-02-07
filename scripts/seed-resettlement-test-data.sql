-- =============================================================================
-- 재정산 기능 테스트용 시드 데이터
-- =============================================================================
-- 목적: FAILED, PENDING_REVIEW 상태의 정산 데이터를 생성하여
--       수동 재정산 기능(POST /v1/settlements/resettle)을 테스트할 수 있도록 함
--
-- 사용법:
--   docker exec -i postgres-18 psql -U postgres -d billpay \
--     < scripts/seed-resettlement-test-data.sql
--
-- 또는 실행 스크립트:
--   ./scripts/run-seed-resettlement.sh
--
-- 전제조건: V3__seed_test_data.sql이 먼저 적용되어 있어야 함
-- =============================================================================

-- 스키마 설정 (tenant_001은 실제 테넌트 스키마)
SET search_path TO tenant_001, public;

DO $$
DECLARE
  v_credit_method_id UUID;
  v_debit_method_id UUID;
  v_kb_card_id UUID;
  v_sh_card_id UUID;
  v_ss_card_id UUID;
  v_now TIMESTAMPTZ := CURRENT_TIMESTAMP;
  v_exists BOOLEAN;

  -- 거래 ID (FAILED 상태용)
  v_txn_id_f1 UUID := '01960000-0000-7000-0009-000000000011';
  v_txn_id_f2 UUID := '01960000-0000-7000-0009-000000000012';
  -- 거래 ID (PENDING_REVIEW 상태용)
  v_txn_id_pr1 UUID := '01960000-0000-7000-0009-000000000021';
  v_txn_id_pr2 UUID := '01960000-0000-7000-0009-000000000022';
  -- 거래 ID (혼합 상태 - 일부 COMPLETED, 일부 FAILED)
  v_txn_id_mix UUID := '01960000-0000-7000-0009-000000000031';

  -- 이벤트 ID
  v_event_id_f1 UUID := '01960000-0000-7000-000A-000000000011';
  v_event_id_f2 UUID := '01960000-0000-7000-000A-000000000012';
  v_event_id_pr1 UUID := '01960000-0000-7000-000A-000000000021';
  v_event_id_pr2 UUID := '01960000-0000-7000-000A-000000000022';
  v_event_id_mix UUID := '01960000-0000-7000-000A-000000000031';
BEGIN
  -- 결제수단/카드사 ID 조회
  SELECT id INTO v_credit_method_id FROM payment_methods WHERE method_code = 'CREDIT';
  SELECT id INTO v_debit_method_id FROM payment_methods WHERE method_code = 'DEBIT';
  SELECT id INTO v_kb_card_id FROM card_companies WHERE company_code = 'KB';
  SELECT id INTO v_sh_card_id FROM card_companies WHERE company_code = 'SH';
  SELECT id INTO v_ss_card_id FROM card_companies WHERE company_code = 'SS';

  -- =========================================================================
  -- 1. FAILED 상태 정산 - 커피숍 거래 (신용카드 25,000원)
  -- =========================================================================
  SELECT EXISTS(SELECT 1 FROM transactions WHERE id = v_txn_id_f1) INTO v_exists;
  IF NOT v_exists THEN
    INSERT INTO transactions (id, transaction_id, merchant_id, merchant_pg_mapping_id, pg_connection_id, org_path, payment_method_id, card_company_id, amount, status, pg_transaction_id, approval_number, approved_at, cat_id, created_at)
    VALUES (v_txn_id_f1, 'TXN' || to_char(CURRENT_DATE, 'YYYYMMDD') || 'F01', '01960000-0000-7000-0003-000000000001', '01960000-0000-7000-0004-000000000001', 1, 'dist_001.agcy_001.deal_001.sell_001.vend_001', v_credit_method_id, v_kb_card_id, 25000, 'APPROVED', 'PG_TXN_F01', 'APRF01', v_now - INTERVAL '3 days', 'CAT001', v_now - INTERVAL '3 days');

    INSERT INTO transaction_events (id, event_type, event_sequence, transaction_id, merchant_id, merchant_pg_mapping_id, pg_connection_id, org_path, payment_method_id, card_company_id, amount, previous_status, new_status, pg_transaction_id, approval_number, cat_id, occurred_at, created_at)
    VALUES (v_event_id_f1, 'APPROVAL', 1, v_txn_id_f1, '01960000-0000-7000-0003-000000000001', '01960000-0000-7000-0004-000000000001', 1, 'dist_001.agcy_001.deal_001.sell_001.vend_001', v_credit_method_id, v_kb_card_id, 25000, 'PENDING', 'APPROVED', 'PG_TXN_F01', 'APRF01', 'CAT001', v_now - INTERVAL '3 days', v_now - INTERVAL '3 days');

    -- 정산 (FAILED) - 5단계 계층
    INSERT INTO settlements (id, transaction_event_id, transaction_id, merchant_id, org_path, entity_id, entity_type, entity_path, entry_type, amount, fee_amount, net_amount, fee_rate, status, created_at, updated_at) VALUES
    ('01960000-0000-7000-000B-000000000101', v_event_id_f1, v_txn_id_f1, '01960000-0000-7000-0003-000000000001', 'dist_001.agcy_001.deal_001.sell_001.vend_001', '01960000-0000-7000-0001-000000000008', 'VENDOR', 'dist_001.agcy_001.deal_001.sell_001.vend_001', 'CREDIT', 24125, 875, 23250, 0.035000, 'FAILED', v_now - INTERVAL '3 days', v_now - INTERVAL '2 days'),
    ('01960000-0000-7000-000B-000000000102', v_event_id_f1, v_txn_id_f1, '01960000-0000-7000-0003-000000000001', 'dist_001.agcy_001.deal_001.sell_001.vend_001', '01960000-0000-7000-0001-000000000006', 'SELLER', 'dist_001.agcy_001.deal_001.sell_001', 'CREDIT', 75, 0, 75, 0.003000, 'FAILED', v_now - INTERVAL '3 days', v_now - INTERVAL '2 days'),
    ('01960000-0000-7000-000B-000000000103', v_event_id_f1, v_txn_id_f1, '01960000-0000-7000-0003-000000000001', 'dist_001.agcy_001.deal_001.sell_001.vend_001', '01960000-0000-7000-0001-000000000004', 'DEALER', 'dist_001.agcy_001.deal_001', 'CREDIT', 50, 0, 50, 0.002000, 'FAILED', v_now - INTERVAL '3 days', v_now - INTERVAL '2 days'),
    ('01960000-0000-7000-000B-000000000104', v_event_id_f1, v_txn_id_f1, '01960000-0000-7000-0003-000000000001', 'dist_001.agcy_001.deal_001.sell_001.vend_001', '01960000-0000-7000-0001-000000000002', 'AGENCY', 'dist_001.agcy_001', 'CREDIT', 75, 0, 75, 0.003000, 'FAILED', v_now - INTERVAL '3 days', v_now - INTERVAL '2 days'),
    ('01960000-0000-7000-000B-000000000105', v_event_id_f1, v_txn_id_f1, '01960000-0000-7000-0003-000000000001', 'dist_001.agcy_001.deal_001.sell_001.vend_001', '01960000-0000-7000-0001-000000000001', 'DISTRIBUTOR', 'dist_001', 'CREDIT', 675, 625, 50, 0.027000, 'FAILED', v_now - INTERVAL '3 days', v_now - INTERVAL '2 days');

    RAISE NOTICE '✅ FAILED 정산 #1 생성 완료 (커피숍 25,000원)';
  END IF;

  -- =========================================================================
  -- 2. FAILED 상태 정산 - 분식점 거래 (체크카드 12,000원)
  -- =========================================================================
  SELECT EXISTS(SELECT 1 FROM transactions WHERE id = v_txn_id_f2) INTO v_exists;
  IF NOT v_exists THEN
    INSERT INTO transactions (id, transaction_id, merchant_id, merchant_pg_mapping_id, pg_connection_id, org_path, payment_method_id, card_company_id, amount, status, pg_transaction_id, approval_number, approved_at, cat_id, created_at)
    VALUES (v_txn_id_f2, 'TXN' || to_char(CURRENT_DATE, 'YYYYMMDD') || 'F02', '01960000-0000-7000-0003-000000000002', '01960000-0000-7000-0004-000000000002', 1, 'dist_001.agcy_001.deal_001.sell_001.vend_002', v_debit_method_id, v_sh_card_id, 12000, 'APPROVED', 'PG_TXN_F02', 'APRF02', v_now - INTERVAL '2 days', 'CAT002', v_now - INTERVAL '2 days');

    INSERT INTO transaction_events (id, event_type, event_sequence, transaction_id, merchant_id, merchant_pg_mapping_id, pg_connection_id, org_path, payment_method_id, card_company_id, amount, previous_status, new_status, pg_transaction_id, approval_number, cat_id, occurred_at, created_at)
    VALUES (v_event_id_f2, 'APPROVAL', 1, v_txn_id_f2, '01960000-0000-7000-0003-000000000002', '01960000-0000-7000-0004-000000000002', 1, 'dist_001.agcy_001.deal_001.sell_001.vend_002', v_debit_method_id, v_sh_card_id, 12000, 'PENDING', 'APPROVED', 'PG_TXN_F02', 'APRF02', 'CAT002', v_now - INTERVAL '2 days', v_now - INTERVAL '2 days');

    -- 정산 (FAILED) - 5단계 계층
    INSERT INTO settlements (id, transaction_event_id, transaction_id, merchant_id, org_path, entity_id, entity_type, entity_path, entry_type, amount, fee_amount, net_amount, fee_rate, status, created_at, updated_at) VALUES
    ('01960000-0000-7000-000B-000000000111', v_event_id_f2, v_txn_id_f2, '01960000-0000-7000-0003-000000000002', 'dist_001.agcy_001.deal_001.sell_001.vend_002', '01960000-0000-7000-0001-000000000009', 'VENDOR', 'dist_001.agcy_001.deal_001.sell_001.vend_002', 'CREDIT', 11640, 360, 11280, 0.030000, 'FAILED', v_now - INTERVAL '2 days', v_now - INTERVAL '1 day'),
    ('01960000-0000-7000-000B-000000000112', v_event_id_f2, v_txn_id_f2, '01960000-0000-7000-0003-000000000002', 'dist_001.agcy_001.deal_001.sell_001.vend_002', '01960000-0000-7000-0001-000000000006', 'SELLER', 'dist_001.agcy_001.deal_001.sell_001', 'CREDIT', 24, 0, 24, 0.002000, 'FAILED', v_now - INTERVAL '2 days', v_now - INTERVAL '1 day'),
    ('01960000-0000-7000-000B-000000000113', v_event_id_f2, v_txn_id_f2, '01960000-0000-7000-0003-000000000002', 'dist_001.agcy_001.deal_001.sell_001.vend_002', '01960000-0000-7000-0001-000000000004', 'DEALER', 'dist_001.agcy_001.deal_001', 'CREDIT', 24, 0, 24, 0.002000, 'FAILED', v_now - INTERVAL '2 days', v_now - INTERVAL '1 day'),
    ('01960000-0000-7000-000B-000000000114', v_event_id_f2, v_txn_id_f2, '01960000-0000-7000-0003-000000000002', 'dist_001.agcy_001.deal_001.sell_001.vend_002', '01960000-0000-7000-0001-000000000002', 'AGENCY', 'dist_001.agcy_001', 'CREDIT', 36, 0, 36, 0.003000, 'FAILED', v_now - INTERVAL '2 days', v_now - INTERVAL '1 day'),
    ('01960000-0000-7000-000B-000000000115', v_event_id_f2, v_txn_id_f2, '01960000-0000-7000-0003-000000000002', 'dist_001.agcy_001.deal_001.sell_001.vend_002', '01960000-0000-7000-0001-000000000001', 'DISTRIBUTOR', 'dist_001', 'CREDIT', 276, 240, 36, 0.023000, 'FAILED', v_now - INTERVAL '2 days', v_now - INTERVAL '1 day');

    RAISE NOTICE '✅ FAILED 정산 #2 생성 완료 (분식점 12,000원)';
  END IF;

  -- =========================================================================
  -- 3. PENDING_REVIEW 상태 정산 - 커피숍 대형 거래 (신용카드 500,000원)
  --    Zero-Sum 검증 실패로 PENDING_REVIEW 상태
  -- =========================================================================
  SELECT EXISTS(SELECT 1 FROM transactions WHERE id = v_txn_id_pr1) INTO v_exists;
  IF NOT v_exists THEN
    INSERT INTO transactions (id, transaction_id, merchant_id, merchant_pg_mapping_id, pg_connection_id, org_path, payment_method_id, card_company_id, amount, status, pg_transaction_id, approval_number, approved_at, cat_id, created_at)
    VALUES (v_txn_id_pr1, 'TXN' || to_char(CURRENT_DATE, 'YYYYMMDD') || 'PR1', '01960000-0000-7000-0003-000000000001', '01960000-0000-7000-0004-000000000001', 1, 'dist_001.agcy_001.deal_001.sell_001.vend_001', v_credit_method_id, v_ss_card_id, 500000, 'APPROVED', 'PG_TXN_PR1', 'APRPR1', v_now - INTERVAL '1 day', 'CAT001', v_now - INTERVAL '1 day');

    INSERT INTO transaction_events (id, event_type, event_sequence, transaction_id, merchant_id, merchant_pg_mapping_id, pg_connection_id, org_path, payment_method_id, card_company_id, amount, previous_status, new_status, pg_transaction_id, approval_number, cat_id, occurred_at, created_at)
    VALUES (v_event_id_pr1, 'APPROVAL', 1, v_txn_id_pr1, '01960000-0000-7000-0003-000000000001', '01960000-0000-7000-0004-000000000001', 1, 'dist_001.agcy_001.deal_001.sell_001.vend_001', v_credit_method_id, v_ss_card_id, 500000, 'PENDING', 'APPROVED', 'PG_TXN_PR1', 'APRPR1', 'CAT001', v_now - INTERVAL '1 day', v_now - INTERVAL '1 day');

    -- 정산 (PENDING_REVIEW) - Zero-Sum 검증 실패
    INSERT INTO settlements (id, transaction_event_id, transaction_id, merchant_id, org_path, entity_id, entity_type, entity_path, entry_type, amount, fee_amount, net_amount, fee_rate, status, created_at, updated_at) VALUES
    ('01960000-0000-7000-000B-000000000201', v_event_id_pr1, v_txn_id_pr1, '01960000-0000-7000-0003-000000000001', 'dist_001.agcy_001.deal_001.sell_001.vend_001', '01960000-0000-7000-0001-000000000008', 'VENDOR', 'dist_001.agcy_001.deal_001.sell_001.vend_001', 'CREDIT', 482500, 17500, 465000, 0.035000, 'PENDING_REVIEW', v_now - INTERVAL '1 day', v_now - INTERVAL '1 day'),
    ('01960000-0000-7000-000B-000000000202', v_event_id_pr1, v_txn_id_pr1, '01960000-0000-7000-0003-000000000001', 'dist_001.agcy_001.deal_001.sell_001.vend_001', '01960000-0000-7000-0001-000000000006', 'SELLER', 'dist_001.agcy_001.deal_001.sell_001', 'CREDIT', 1500, 0, 1500, 0.003000, 'PENDING_REVIEW', v_now - INTERVAL '1 day', v_now - INTERVAL '1 day'),
    ('01960000-0000-7000-000B-000000000203', v_event_id_pr1, v_txn_id_pr1, '01960000-0000-7000-0003-000000000001', 'dist_001.agcy_001.deal_001.sell_001.vend_001', '01960000-0000-7000-0001-000000000004', 'DEALER', 'dist_001.agcy_001.deal_001', 'CREDIT', 1000, 0, 1000, 0.002000, 'PENDING_REVIEW', v_now - INTERVAL '1 day', v_now - INTERVAL '1 day'),
    ('01960000-0000-7000-000B-000000000204', v_event_id_pr1, v_txn_id_pr1, '01960000-0000-7000-0003-000000000001', 'dist_001.agcy_001.deal_001.sell_001.vend_001', '01960000-0000-7000-0001-000000000002', 'AGENCY', 'dist_001.agcy_001', 'CREDIT', 1500, 0, 1500, 0.003000, 'PENDING_REVIEW', v_now - INTERVAL '1 day', v_now - INTERVAL '1 day'),
    ('01960000-0000-7000-000B-000000000205', v_event_id_pr1, v_txn_id_pr1, '01960000-0000-7000-0003-000000000001', 'dist_001.agcy_001.deal_001.sell_001.vend_001', '01960000-0000-7000-0001-000000000001', 'DISTRIBUTOR', 'dist_001', 'CREDIT', 13500, 12500, 1000, 0.027000, 'PENDING_REVIEW', v_now - INTERVAL '1 day', v_now - INTERVAL '1 day');

    RAISE NOTICE '✅ PENDING_REVIEW 정산 #1 생성 완료 (커피숍 500,000원)';
  END IF;

  -- =========================================================================
  -- 4. PENDING_REVIEW 상태 정산 - 프리미엄마트 거래 (신용카드 1,200,000원)
  -- =========================================================================
  SELECT EXISTS(SELECT 1 FROM transactions WHERE id = v_txn_id_pr2) INTO v_exists;
  IF NOT v_exists THEN
    INSERT INTO transactions (id, transaction_id, merchant_id, merchant_pg_mapping_id, pg_connection_id, org_path, payment_method_id, card_company_id, amount, status, pg_transaction_id, approval_number, approved_at, cat_id, created_at)
    VALUES (v_txn_id_pr2, 'TXN' || to_char(CURRENT_DATE, 'YYYYMMDD') || 'PR2', '01960000-0000-7000-0003-000000000003', '01960000-0000-7000-0004-000000000003', 1, 'dist_001.agcy_001.deal_001.sell_002.vend_003', v_credit_method_id, v_kb_card_id, 1200000, 'APPROVED', 'PG_TXN_PR2', 'APRPR2', v_now - INTERVAL '12 hours', 'CAT003', v_now - INTERVAL '12 hours');

    INSERT INTO transaction_events (id, event_type, event_sequence, transaction_id, merchant_id, merchant_pg_mapping_id, pg_connection_id, org_path, payment_method_id, card_company_id, amount, previous_status, new_status, pg_transaction_id, approval_number, cat_id, occurred_at, created_at)
    VALUES (v_event_id_pr2, 'APPROVAL', 1, v_txn_id_pr2, '01960000-0000-7000-0003-000000000003', '01960000-0000-7000-0004-000000000003', 1, 'dist_001.agcy_001.deal_001.sell_002.vend_003', v_credit_method_id, v_kb_card_id, 1200000, 'PENDING', 'APPROVED', 'PG_TXN_PR2', 'APRPR2', 'CAT003', v_now - INTERVAL '12 hours', v_now - INTERVAL '12 hours');

    -- 정산 (PENDING_REVIEW) - 프리미엄마트는 sell_002 → vend_003 경로
    -- fee config: VENDOR 3.5%, SELLER 3.2%, DEALER 3.0%, AGENCY 2.8%, DISTRIBUTOR 2.5%
    INSERT INTO settlements (id, transaction_event_id, transaction_id, merchant_id, org_path, entity_id, entity_type, entity_path, entry_type, amount, fee_amount, net_amount, fee_rate, status, created_at, updated_at) VALUES
    ('01960000-0000-7000-000B-000000000211', v_event_id_pr2, v_txn_id_pr2, '01960000-0000-7000-0003-000000000003', 'dist_001.agcy_001.deal_001.sell_002.vend_003', '01960000-0000-7000-0001-000000000010', 'VENDOR', 'dist_001.agcy_001.deal_001.sell_002.vend_003', 'CREDIT', 1158000, 42000, 1116000, 0.035000, 'PENDING_REVIEW', v_now - INTERVAL '12 hours', v_now - INTERVAL '12 hours'),
    ('01960000-0000-7000-000B-000000000212', v_event_id_pr2, v_txn_id_pr2, '01960000-0000-7000-0003-000000000003', 'dist_001.agcy_001.deal_001.sell_002.vend_003', '01960000-0000-7000-0001-000000000007', 'SELLER', 'dist_001.agcy_001.deal_001.sell_002', 'CREDIT', 3600, 0, 3600, 0.003000, 'PENDING_REVIEW', v_now - INTERVAL '12 hours', v_now - INTERVAL '12 hours'),
    ('01960000-0000-7000-000B-000000000213', v_event_id_pr2, v_txn_id_pr2, '01960000-0000-7000-0003-000000000003', 'dist_001.agcy_001.deal_001.sell_002.vend_003', '01960000-0000-7000-0001-000000000004', 'DEALER', 'dist_001.agcy_001.deal_001', 'CREDIT', 2400, 0, 2400, 0.002000, 'PENDING_REVIEW', v_now - INTERVAL '12 hours', v_now - INTERVAL '12 hours'),
    ('01960000-0000-7000-000B-000000000214', v_event_id_pr2, v_txn_id_pr2, '01960000-0000-7000-0003-000000000003', 'dist_001.agcy_001.deal_001.sell_002.vend_003', '01960000-0000-7000-0001-000000000002', 'AGENCY', 'dist_001.agcy_001', 'CREDIT', 3600, 0, 3600, 0.003000, 'PENDING_REVIEW', v_now - INTERVAL '12 hours', v_now - INTERVAL '12 hours'),
    ('01960000-0000-7000-000B-000000000215', v_event_id_pr2, v_txn_id_pr2, '01960000-0000-7000-0003-000000000003', 'dist_001.agcy_001.deal_001.sell_002.vend_003', '01960000-0000-7000-0001-000000000001', 'DISTRIBUTOR', 'dist_001', 'CREDIT', 32400, 30000, 2400, 0.027000, 'PENDING_REVIEW', v_now - INTERVAL '12 hours', v_now - INTERVAL '12 hours');

    RAISE NOTICE '✅ PENDING_REVIEW 정산 #2 생성 완료 (프리미엄마트 1,200,000원)';
  END IF;

  -- =========================================================================
  -- 5. COMPLETED 상태 정산 (재정산 불가 확인용) - 분식점 거래 (체크카드 5,000원)
  -- =========================================================================
  SELECT EXISTS(SELECT 1 FROM transactions WHERE id = v_txn_id_mix) INTO v_exists;
  IF NOT v_exists THEN
    INSERT INTO transactions (id, transaction_id, merchant_id, merchant_pg_mapping_id, pg_connection_id, org_path, payment_method_id, card_company_id, amount, status, pg_transaction_id, approval_number, approved_at, cat_id, created_at)
    VALUES (v_txn_id_mix, 'TXN' || to_char(CURRENT_DATE, 'YYYYMMDD') || 'MIX', '01960000-0000-7000-0003-000000000002', '01960000-0000-7000-0004-000000000002', 1, 'dist_001.agcy_001.deal_001.sell_001.vend_002', v_debit_method_id, v_kb_card_id, 5000, 'APPROVED', 'PG_TXN_MIX', 'APRMIX', v_now - INTERVAL '5 days', 'CAT002', v_now - INTERVAL '5 days');

    INSERT INTO transaction_events (id, event_type, event_sequence, transaction_id, merchant_id, merchant_pg_mapping_id, pg_connection_id, org_path, payment_method_id, card_company_id, amount, previous_status, new_status, pg_transaction_id, approval_number, cat_id, occurred_at, created_at)
    VALUES (v_event_id_mix, 'APPROVAL', 1, v_txn_id_mix, '01960000-0000-7000-0003-000000000002', '01960000-0000-7000-0004-000000000002', 1, 'dist_001.agcy_001.deal_001.sell_001.vend_002', v_debit_method_id, v_kb_card_id, 5000, 'PENDING', 'APPROVED', 'PG_TXN_MIX', 'APRMIX', 'CAT002', v_now - INTERVAL '5 days', v_now - INTERVAL '5 days');

    -- 정산 (COMPLETED - 재정산 불가)
    INSERT INTO settlements (id, transaction_event_id, transaction_id, merchant_id, org_path, entity_id, entity_type, entity_path, entry_type, amount, fee_amount, net_amount, fee_rate, status, settled_at, created_at, updated_at) VALUES
    ('01960000-0000-7000-000B-000000000301', v_event_id_mix, v_txn_id_mix, '01960000-0000-7000-0003-000000000002', 'dist_001.agcy_001.deal_001.sell_001.vend_002', '01960000-0000-7000-0001-000000000009', 'VENDOR', 'dist_001.agcy_001.deal_001.sell_001.vend_002', 'CREDIT', 4850, 150, 4700, 0.030000, 'COMPLETED', v_now - INTERVAL '4 days', v_now - INTERVAL '5 days', v_now - INTERVAL '4 days'),
    ('01960000-0000-7000-000B-000000000302', v_event_id_mix, v_txn_id_mix, '01960000-0000-7000-0003-000000000002', 'dist_001.agcy_001.deal_001.sell_001.vend_002', '01960000-0000-7000-0001-000000000006', 'SELLER', 'dist_001.agcy_001.deal_001.sell_001', 'CREDIT', 10, 0, 10, 0.002000, 'COMPLETED', v_now - INTERVAL '4 days', v_now - INTERVAL '5 days', v_now - INTERVAL '4 days'),
    ('01960000-0000-7000-000B-000000000303', v_event_id_mix, v_txn_id_mix, '01960000-0000-7000-0003-000000000002', 'dist_001.agcy_001.deal_001.sell_001.vend_002', '01960000-0000-7000-0001-000000000004', 'DEALER', 'dist_001.agcy_001.deal_001', 'CREDIT', 10, 0, 10, 0.002000, 'COMPLETED', v_now - INTERVAL '4 days', v_now - INTERVAL '5 days', v_now - INTERVAL '4 days'),
    ('01960000-0000-7000-000B-000000000304', v_event_id_mix, v_txn_id_mix, '01960000-0000-7000-0003-000000000002', 'dist_001.agcy_001.deal_001.sell_001.vend_002', '01960000-0000-7000-0001-000000000002', 'AGENCY', 'dist_001.agcy_001', 'CREDIT', 15, 0, 15, 0.003000, 'COMPLETED', v_now - INTERVAL '4 days', v_now - INTERVAL '5 days', v_now - INTERVAL '4 days'),
    ('01960000-0000-7000-000B-000000000305', v_event_id_mix, v_txn_id_mix, '01960000-0000-7000-0003-000000000002', 'dist_001.agcy_001.deal_001.sell_001.vend_002', '01960000-0000-7000-0001-000000000001', 'DISTRIBUTOR', 'dist_001', 'CREDIT', 115, 100, 15, 0.023000, 'COMPLETED', v_now - INTERVAL '4 days', v_now - INTERVAL '5 days', v_now - INTERVAL '4 days');

    RAISE NOTICE '✅ COMPLETED 정산 생성 완료 (분식점 5,000원 - 재정산 불가 확인용)';
  END IF;

  RAISE NOTICE '';
  RAISE NOTICE '========================================';
  RAISE NOTICE '테스트 데이터 생성 완료!';
  RAISE NOTICE '----------------------------------------';
  RAISE NOTICE '  FAILED 정산: 2건 (거래 이벤트 2개)';
  RAISE NOTICE '  PENDING_REVIEW 정산: 2건 (거래 이벤트 2개)';
  RAISE NOTICE '  COMPLETED 정산: 1건 (재정산 불가 확인용)';
  RAISE NOTICE '========================================';
  RAISE NOTICE '';
  RAISE NOTICE '재정산 테스트 방법:';
  RAISE NOTICE '  1. POST /api/v1/settlements/resettle';
  RAISE NOTICE '     Body: {"transactionEventId": "01960000-0000-7000-000a-000000000011"}  -- FAILED';
  RAISE NOTICE '     Body: {"transactionEventId": "01960000-0000-7000-000a-000000000012"}  -- FAILED';
  RAISE NOTICE '     Body: {"transactionEventId": "01960000-0000-7000-000a-000000000021"}  -- PENDING_REVIEW';
  RAISE NOTICE '     Body: {"transactionEventId": "01960000-0000-7000-000a-000000000022"}  -- PENDING_REVIEW';
  RAISE NOTICE '  2. COMPLETED 거래는 재정산 불가 (에러 반환 확인)';
  RAISE NOTICE '     Body: {"transactionEventId": "01960000-0000-7000-000a-000000000031"}  -- COMPLETED (거부됨)';
END $$;
