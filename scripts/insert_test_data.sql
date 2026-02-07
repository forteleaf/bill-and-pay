SET search_path TO tenant_001,public;

-- MCH001: 맛있는 커피숍 - 5 more transactions (1 already inserted)
INSERT INTO transactions (id, transaction_id, merchant_id, merchant_pg_mapping_id, pg_connection_id, org_path, payment_method_id, card_company_id, amount, currency, status, pg_transaction_id, approval_number, approved_at, created_at, updated_at)
VALUES
  (gen_random_uuid(), 'TXN-MCH001-T02', '01960000-0000-7000-0003-000000000001', '01960000-0000-7000-0004-000000000001', 1, 'dist_001.agcy_001.deal_001.sell_001.vend_001'::ltree, '019c2bf2-2ce5-7176-bff2-ef736f08896c', '019c2bf2-2ce5-7a04-9fb9-5bf0d526fe3e', 18500, 'KRW', 'APPROVED', 'PG_TEST_MCH001_02', 'APR_T02', '2026-02-01 14:20:00+09', NOW(), NOW()),
  (gen_random_uuid(), 'TXN-MCH001-T03', '01960000-0000-7000-0003-000000000001', '01960000-0000-7000-0004-000000000001', 1, 'dist_001.agcy_001.deal_001.sell_001.vend_001'::ltree, '019c2bf2-2ce4-7c1a-9222-9656ebd60abe', '019c2bf2-2ce5-7a0d-a2ae-77fc65005b5b', 67000, 'KRW', 'APPROVED', 'PG_TEST_MCH001_03', 'APR_T03', '2026-02-03 10:15:00+09', NOW(), NOW()),
  (gen_random_uuid(), 'TXN-MCH001-T04', '01960000-0000-7000-0003-000000000001', '01960000-0000-7000-0004-000000000001', 1, 'dist_001.agcy_001.deal_001.sell_001.vend_001'::ltree, '019c2bf2-2ce4-7c1a-9222-9656ebd60abe', '019c2bf2-2ce5-7a16-9a9f-d0559cd28e54', 95000, 'KRW', 'APPROVED', 'PG_TEST_MCH001_04', 'APR_T04', '2026-02-04 16:45:00+09', NOW(), NOW()),
  (gen_random_uuid(), 'TXN-MCH001-T05', '01960000-0000-7000-0003-000000000001', '01960000-0000-7000-0004-000000000001', 1, 'dist_001.agcy_001.deal_001.sell_001.vend_001'::ltree, '019c2bf2-2ce5-7176-bff2-ef736f08896c', '019c2bf2-2ce5-7a25-b36b-2f7be6b9d4b4', 31000, 'KRW', 'APPROVED', 'PG_TEST_MCH001_05', 'APR_T05', '2026-02-06 11:00:00+09', NOW(), NOW()),
  (gen_random_uuid(), 'TXN-MCH001-T06', '01960000-0000-7000-0003-000000000001', '01960000-0000-7000-0004-000000000001', 1, 'dist_001.agcy_001.deal_001.sell_001.vend_001'::ltree, '019c2bf2-2ce4-7c1a-9222-9656ebd60abe', '019c2bf2-2ce5-7a2d-8c31-a52bf613661a', 53000, 'KRW', 'APPROVED', 'PG_TEST_MCH001_06', 'APR_T06', '2026-02-07 13:30:00+09', NOW(), NOW());

-- MCH002: 행복한 분식점 - 6건
INSERT INTO transactions (id, transaction_id, merchant_id, merchant_pg_mapping_id, pg_connection_id, org_path, payment_method_id, card_company_id, amount, currency, status, pg_transaction_id, approval_number, approved_at, created_at, updated_at)
VALUES
  (gen_random_uuid(), 'TXN-MCH002-T01', '01960000-0000-7000-0003-000000000002', '01960000-0000-7000-0004-000000000002', 1, 'dist_001.agcy_001.deal_001.sell_001.vend_002'::ltree, '019c2bf2-2ce4-7c1a-9222-9656ebd60abe', '019c2bf2-2ce5-7a0d-a2ae-77fc65005b5b', 7500, 'KRW', 'APPROVED', 'PG_TEST_MCH002_01', 'APR_M01', '2026-02-01 12:00:00+09', NOW(), NOW()),
  (gen_random_uuid(), 'TXN-MCH002-T02', '01960000-0000-7000-0003-000000000002', '01960000-0000-7000-0004-000000000002', 1, 'dist_001.agcy_001.deal_001.sell_001.vend_002'::ltree, '019c2bf2-2ce5-7176-bff2-ef736f08896c', '019c2bf2-2ce5-79f3-8117-402ac9179c3b', 4500, 'KRW', 'APPROVED', 'PG_TEST_MCH002_02', 'APR_M02', '2026-02-02 13:30:00+09', NOW(), NOW()),
  (gen_random_uuid(), 'TXN-MCH002-T03', '01960000-0000-7000-0003-000000000002', '01960000-0000-7000-0004-000000000002', 1, 'dist_001.agcy_001.deal_001.sell_001.vend_002'::ltree, '019c2bf2-2ce4-7c1a-9222-9656ebd60abe', '019c2bf2-2ce5-7a16-9a9f-d0559cd28e54', 12000, 'KRW', 'APPROVED', 'PG_TEST_MCH002_03', 'APR_M03', '2026-02-03 11:45:00+09', NOW(), NOW()),
  (gen_random_uuid(), 'TXN-MCH002-T04', '01960000-0000-7000-0003-000000000002', '01960000-0000-7000-0004-000000000002', 1, 'dist_001.agcy_001.deal_001.sell_001.vend_002'::ltree, '019c2bf2-2ce4-7c1a-9222-9656ebd60abe', '019c2bf2-2ce5-7a04-9fb9-5bf0d526fe3e', 9000, 'KRW', 'APPROVED', 'PG_TEST_MCH002_04', 'APR_M04', '2026-02-05 15:00:00+09', NOW(), NOW()),
  (gen_random_uuid(), 'TXN-MCH002-T05', '01960000-0000-7000-0003-000000000002', '01960000-0000-7000-0004-000000000002', 1, 'dist_001.agcy_001.deal_001.sell_001.vend_002'::ltree, '019c2bf2-2ce5-7176-bff2-ef736f08896c', '019c2bf2-2ce5-7a2d-8c31-a52bf613661a', 6000, 'KRW', 'APPROVED', 'PG_TEST_MCH002_05', 'APR_M05', '2026-02-06 17:20:00+09', NOW(), NOW()),
  (gen_random_uuid(), 'TXN-MCH002-T06', '01960000-0000-7000-0003-000000000002', '01960000-0000-7000-0004-000000000002', 1, 'dist_001.agcy_001.deal_001.sell_001.vend_002'::ltree, '019c2bf2-2ce4-7c1a-9222-9656ebd60abe', '019c2bf2-2ce5-7a25-b36b-2f7be6b9d4b4', 15000, 'KRW', 'APPROVED', 'PG_TEST_MCH002_06', 'APR_M06', '2026-02-07 09:00:00+09', NOW(), NOW());

-- MCH003: 프리미엄마트 - 6건
INSERT INTO transactions (id, transaction_id, merchant_id, merchant_pg_mapping_id, pg_connection_id, org_path, payment_method_id, card_company_id, amount, currency, status, pg_transaction_id, approval_number, approved_at, created_at, updated_at)
VALUES
  (gen_random_uuid(), 'TXN-MCH003-T01', '01960000-0000-7000-0003-000000000003', '01960000-0000-7000-0004-000000000003', 1, 'dist_001.agcy_001.deal_001.sell_002.vend_003'::ltree, '019c2bf2-2ce4-7c1a-9222-9656ebd60abe', '019c2bf2-2ce5-79f3-8117-402ac9179c3b', 250000, 'KRW', 'APPROVED', 'PG_TEST_MCH003_01', 'APR_P01', '2026-02-01 10:00:00+09', NOW(), NOW()),
  (gen_random_uuid(), 'TXN-MCH003-T02', '01960000-0000-7000-0003-000000000003', '01960000-0000-7000-0004-000000000003', 1, 'dist_001.agcy_001.deal_001.sell_002.vend_003'::ltree, '019c2bf2-2ce4-7c1a-9222-9656ebd60abe', '019c2bf2-2ce5-7a04-9fb9-5bf0d526fe3e', 180000, 'KRW', 'APPROVED', 'PG_TEST_MCH003_02', 'APR_P02', '2026-02-02 11:30:00+09', NOW(), NOW()),
  (gen_random_uuid(), 'TXN-MCH003-T03', '01960000-0000-7000-0003-000000000003', '01960000-0000-7000-0004-000000000003', 1, 'dist_001.agcy_001.deal_001.sell_002.vend_003'::ltree, '019c2bf2-2ce5-7176-bff2-ef736f08896c', '019c2bf2-2ce5-7a0d-a2ae-77fc65005b5b', 75000, 'KRW', 'APPROVED', 'PG_TEST_MCH003_03', 'APR_P03', '2026-02-03 14:00:00+09', NOW(), NOW()),
  (gen_random_uuid(), 'TXN-MCH003-T04', '01960000-0000-7000-0003-000000000003', '01960000-0000-7000-0004-000000000003', 1, 'dist_001.agcy_001.deal_001.sell_002.vend_003'::ltree, '019c2bf2-2ce4-7c1a-9222-9656ebd60abe', '019c2bf2-2ce5-7a16-9a9f-d0559cd28e54', 320000, 'KRW', 'APPROVED', 'PG_TEST_MCH003_04', 'APR_P04', '2026-02-04 09:15:00+09', NOW(), NOW()),
  (gen_random_uuid(), 'TXN-MCH003-T05', '01960000-0000-7000-0003-000000000003', '01960000-0000-7000-0004-000000000003', 1, 'dist_001.agcy_001.deal_001.sell_002.vend_003'::ltree, '019c2bf2-2ce4-7c1a-9222-9656ebd60abe', '019c2bf2-2ce5-7a25-b36b-2f7be6b9d4b4', 156000, 'KRW', 'APPROVED', 'PG_TEST_MCH003_05', 'APR_P05', '2026-02-05 16:30:00+09', NOW(), NOW()),
  (gen_random_uuid(), 'TXN-MCH003-T06', '01960000-0000-7000-0003-000000000003', '01960000-0000-7000-0004-000000000003', 1, 'dist_001.agcy_001.deal_001.sell_002.vend_003'::ltree, '019c2bf2-2ce5-7176-bff2-ef736f08896c', '019c2bf2-2ce5-7a2d-8c31-a52bf613661a', 88000, 'KRW', 'APPROVED', 'PG_TEST_MCH003_06', 'APR_P06', '2026-02-07 10:45:00+09', NOW(), NOW());

-- Create transaction_events for new transactions that don't have events yet
INSERT INTO transaction_events (id, event_type, event_sequence, transaction_id, merchant_id, merchant_pg_mapping_id, pg_connection_id, org_path, payment_method_id, card_company_id, amount, currency, previous_status, new_status, pg_transaction_id, approval_number, occurred_at, created_at)
SELECT gen_random_uuid(), 'APPROVAL', 1, t.id, t.merchant_id, t.merchant_pg_mapping_id, t.pg_connection_id, t.org_path, t.payment_method_id, t.card_company_id, t.amount, t.currency, 'PENDING', 'APPROVED', t.pg_transaction_id, t.approval_number, t.approved_at, t.approved_at
FROM transactions t
WHERE t.pg_transaction_id LIKE 'PG_TEST_%'
AND NOT EXISTS (
  SELECT 1 FROM transaction_events te WHERE te.transaction_id = t.id
);
