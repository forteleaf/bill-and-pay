-- =============================================================================
-- Bill&Pay Tenant Schema - Test Seed Data
-- =============================================================================
-- 버전: 1.0
-- 설명: 개발/테스트 환경용 샘플 데이터
-- 주의: 프로덕션 환경에서는 제외해야 함
-- =============================================================================

-- =============================================================================
-- Business Entities (사업자 정보)
-- =============================================================================
INSERT INTO business_entities (id, business_type, business_number, corporate_number, business_name, representative_name, open_date, business_address, actual_address, business_category, business_sub_category, main_phone, manager_name, manager_phone, email) VALUES
-- 총판 사업자
('01960000-0000-7000-0000-000000000001', 'CORPORATION', '1234567890', '12345678901234', '(주)코르페이 총판', '김총판', '2020-01-01', '서울시 강남구 테헤란로 100', '서울시 강남구 테헤란로 100', '도소매업', '전자상거래', '0212345678', '이관리', '01012345678', 'master@korpay-dist.com'),
-- 대리점 사업자
('01960000-0000-7000-0000-000000000002', 'CORPORATION', '2345678901', '23456789012345', '(주)서울대리점', '박대리', '2021-03-15', '서울시 서초구 서초대로 200', '서울시 서초구 서초대로 200', '도소매업', '전자상거래', '0223456789', '최영업', '01023456789', 'seoul@agency.com'),
('01960000-0000-7000-0000-000000000003', 'CORPORATION', '3456789012', '34567890123456', '(주)부산대리점', '최대리', '2021-05-20', '부산시 해운대구 해운대로 300', '부산시 해운대구 해운대로 300', '도소매업', '전자상거래', '0513456789', '김영업', '01034567890', 'busan@agency.com'),
-- 딜러 사업자
('01960000-0000-7000-0000-000000000004', 'INDIVIDUAL', '4567890123', NULL, '강남딜러', '정딜러', '2022-01-10', '서울시 강남구 역삼동 100', '서울시 강남구 역삼동 100', '서비스업', '결제대행', '0234567890', NULL, '01045678901', 'gangnam@dealer.com'),
('01960000-0000-7000-0000-000000000005', 'INDIVIDUAL', '5678901234', NULL, '서초딜러', '송딜러', '2022-02-15', '서울시 서초구 반포동 200', '서울시 서초구 반포동 200', '서비스업', '결제대행', '0245678901', NULL, '01056789012', 'seocho@dealer.com'),
-- 판매점 사업자
('01960000-0000-7000-0000-000000000006', 'INDIVIDUAL', '6789012345', NULL, '역삼판매점', '이판매', '2023-01-05', '서울시 강남구 역삼로 50', '서울시 강남구 역삼로 50', '서비스업', '단말기판매', '0256789012', NULL, '01067890123', 'yeoksam@seller.com'),
('01960000-0000-7000-0000-000000000007', 'INDIVIDUAL', '7890123456', NULL, '삼성판매점', '김판매', '2023-02-10', '서울시 강남구 삼성로 60', '서울시 강남구 삼성로 60', '서비스업', '단말기판매', '0267890123', NULL, '01078901234', 'samsung@seller.com'),
-- 가맹점 사업자
('01960000-0000-7000-0000-000000000008', 'INDIVIDUAL', '8901234567', NULL, '맛있는 커피숍', '홍커피', '2024-01-01', '서울시 강남구 역삼동 123', '서울시 강남구 역삼동 123', '음식점업', '커피전문점', '0278901234', NULL, '01089012345', 'coffee@merchant.com'),
('01960000-0000-7000-0000-000000000009', 'INDIVIDUAL', '9012345678', NULL, '행복한 분식점', '박분식', '2024-02-01', '서울시 강남구 삼성동 456', '서울시 강남구 삼성동 456', '음식점업', '한식', '0289012345', NULL, '01090123456', 'bunsik@merchant.com'),
('01960000-0000-7000-0000-000000000010', 'CORPORATION', '0123456789', '01234567890123', '(주)프리미엄마트', '최마트', '2023-06-01', '서울시 서초구 반포대로 789', '서울시 서초구 반포대로 789', '도소매업', '슈퍼마켓', '0290123456', '이매니저', '01001234567', 'mart@merchant.com'),
('01960000-0000-7000-0000-000000000011', 'INDIVIDUAL', '1122334455', NULL, '해운대횟집', '정횟집', '2024-03-01', '부산시 해운대구 해운대해변로 100', '부산시 해운대구 해운대해변로 100', '음식점업', '일식', '0511234567', NULL, '01011223344', 'sushi@merchant.com'),
('01960000-0000-7000-0000-000000000012', 'NON_BUSINESS', NULL, NULL, '김개인', '김개인', NULL, '부산시 해운대구 우동 200', '부산시 해운대구 우동 200', NULL, NULL, '01022334455', NULL, '01022334455', 'kim@personal.com');

-- =============================================================================
-- Organizations (5단계 계층 구조)
-- =============================================================================
-- Level 1: DISTRIBUTOR (총판)
INSERT INTO organizations (id, org_code, name, org_type, path, parent_id, level, status, business_entity_id) VALUES
('01960000-0000-7000-0001-000000000001', 'DIST001', '코르페이 총판', 'DISTRIBUTOR', 'dist_001', NULL, 1, 'ACTIVE', '01960000-0000-7000-0000-000000000001');

-- Level 2: AGENCY (대리점)
INSERT INTO organizations (id, org_code, name, org_type, path, parent_id, level, status, business_entity_id) VALUES
('01960000-0000-7000-0001-000000000002', 'AGCY001', '서울대리점', 'AGENCY', 'dist_001.agcy_001', '01960000-0000-7000-0001-000000000001', 2, 'ACTIVE', '01960000-0000-7000-0000-000000000002'),
('01960000-0000-7000-0001-000000000003', 'AGCY002', '부산대리점', 'AGENCY', 'dist_001.agcy_002', '01960000-0000-7000-0001-000000000001', 2, 'ACTIVE', '01960000-0000-7000-0000-000000000003');

-- Level 3: DEALER (딜러)
INSERT INTO organizations (id, org_code, name, org_type, path, parent_id, level, status, business_entity_id) VALUES
('01960000-0000-7000-0001-000000000004', 'DEAL001', '강남딜러', 'DEALER', 'dist_001.agcy_001.deal_001', '01960000-0000-7000-0001-000000000002', 3, 'ACTIVE', '01960000-0000-7000-0000-000000000004'),
('01960000-0000-7000-0001-000000000005', 'DEAL002', '서초딜러', 'DEALER', 'dist_001.agcy_001.deal_002', '01960000-0000-7000-0001-000000000002', 3, 'ACTIVE', '01960000-0000-7000-0000-000000000005');

-- Level 4: SELLER (판매점)
INSERT INTO organizations (id, org_code, name, org_type, path, parent_id, level, status, business_entity_id) VALUES
('01960000-0000-7000-0001-000000000006', 'SELL001', '역삼판매점', 'SELLER', 'dist_001.agcy_001.deal_001.sell_001', '01960000-0000-7000-0001-000000000004', 4, 'ACTIVE', '01960000-0000-7000-0000-000000000006'),
('01960000-0000-7000-0001-000000000007', 'SELL002', '삼성판매점', 'SELLER', 'dist_001.agcy_001.deal_001.sell_002', '01960000-0000-7000-0001-000000000004', 4, 'ACTIVE', '01960000-0000-7000-0000-000000000007');

-- Level 5: VENDOR (가맹)
INSERT INTO organizations (id, org_code, name, org_type, path, parent_id, level, status, business_entity_id) VALUES
('01960000-0000-7000-0001-000000000008', 'VEND001', '맛있는 커피숍', 'VENDOR', 'dist_001.agcy_001.deal_001.sell_001.vend_001', '01960000-0000-7000-0001-000000000006', 5, 'ACTIVE', '01960000-0000-7000-0000-000000000008'),
('01960000-0000-7000-0001-000000000009', 'VEND002', '행복한 분식점', 'VENDOR', 'dist_001.agcy_001.deal_001.sell_001.vend_002', '01960000-0000-7000-0001-000000000006', 5, 'ACTIVE', '01960000-0000-7000-0000-000000000009'),
('01960000-0000-7000-0001-000000000010', 'VEND003', '프리미엄마트', 'VENDOR', 'dist_001.agcy_001.deal_001.sell_002.vend_003', '01960000-0000-7000-0001-000000000007', 5, 'ACTIVE', '01960000-0000-7000-0000-000000000010');

-- =============================================================================
-- Users (테스트 사용자) - 비밀번호: password123 (BCrypt)
-- =============================================================================
INSERT INTO users (id, username, email, password_hash, org_id, org_path, full_name, phone, role, status) VALUES
-- 총판 관리자
('01960000-0000-7000-0002-000000000001', 'admin', 'admin@korpay.com', '$2a$10$ECihIpHMvuzkZTrOpqdKC.J9A3.5U.QpIw2RTVKQRgEroZW5nw6su', '01960000-0000-7000-0001-000000000001', 'dist_001', '시스템 관리자', '01011111111', 'SUPER_ADMIN', 'ACTIVE'),
('01960000-0000-7000-0002-000000000002', 'dist_admin', 'master@korpay-dist.com', '$2a$10$ECihIpHMvuzkZTrOpqdKC.J9A3.5U.QpIw2RTVKQRgEroZW5nw6su', '01960000-0000-7000-0001-000000000001', 'dist_001', '김총판', '01012345678', 'DISTRIBUTOR_ADMIN', 'ACTIVE'),
-- 대리점 관리자
('01960000-0000-7000-0002-000000000003', 'seoul_admin', 'seoul@agency.com', '$2a$10$ECihIpHMvuzkZTrOpqdKC.J9A3.5U.QpIw2RTVKQRgEroZW5nw6su', '01960000-0000-7000-0001-000000000002', 'dist_001.agcy_001', '박대리', '01023456789', 'AGENCY_ADMIN', 'ACTIVE'),
('01960000-0000-7000-0002-000000000004', 'busan_admin', 'busan@agency.com', '$2a$10$ECihIpHMvuzkZTrOpqdKC.J9A3.5U.QpIw2RTVKQRgEroZW5nw6su', '01960000-0000-7000-0001-000000000003', 'dist_001.agcy_002', '최대리', '01034567890', 'AGENCY_ADMIN', 'ACTIVE'),
-- 딜러 담당자
('01960000-0000-7000-0002-000000000005', 'dealer1', 'gangnam@dealer.com', '$2a$10$ECihIpHMvuzkZTrOpqdKC.J9A3.5U.QpIw2RTVKQRgEroZW5nw6su', '01960000-0000-7000-0001-000000000004', 'dist_001.agcy_001.deal_001', '정딜러', '01045678901', 'DEALER', 'ACTIVE'),
-- 판매점 담당자
('01960000-0000-7000-0002-000000000006', 'seller1', 'yeoksam@seller.com', '$2a$10$ECihIpHMvuzkZTrOpqdKC.J9A3.5U.QpIw2RTVKQRgEroZW5nw6su', '01960000-0000-7000-0001-000000000006', 'dist_001.agcy_001.deal_001.sell_001', '이판매', '01067890123', 'SELLER', 'ACTIVE'),
-- 가맹점 담당자
('01960000-0000-7000-0002-000000000007', 'merchant1', 'coffee@merchant.com', '$2a$10$ECihIpHMvuzkZTrOpqdKC.J9A3.5U.QpIw2RTVKQRgEroZW5nw6su', '01960000-0000-7000-0001-000000000008', 'dist_001.agcy_001.deal_001.sell_001.vend_001', '홍커피', '01089012345', 'MERCHANT', 'ACTIVE');

-- =============================================================================
-- Merchants (가맹점)
-- =============================================================================
INSERT INTO merchants (id, merchant_code, name, org_id, org_path, business_number, business_type, contact_name, contact_email, contact_phone, address, status) VALUES
('01960000-0000-7000-0003-000000000001', 'MCH001', '맛있는 커피숍', '01960000-0000-7000-0001-000000000008', 'dist_001.agcy_001.deal_001.sell_001.vend_001', '8901234567', 'INDIVIDUAL', '홍커피', 'coffee@merchant.com', '01089012345', '서울시 강남구 역삼동 123', 'ACTIVE'),
('01960000-0000-7000-0003-000000000002', 'MCH002', '행복한 분식점', '01960000-0000-7000-0001-000000000009', 'dist_001.agcy_001.deal_001.sell_001.vend_002', '9012345678', 'INDIVIDUAL', '박분식', 'bunsik@merchant.com', '01090123456', '서울시 강남구 삼성동 456', 'ACTIVE'),
('01960000-0000-7000-0003-000000000003', 'MCH003', '프리미엄마트', '01960000-0000-7000-0001-000000000010', 'dist_001.agcy_001.deal_001.sell_002.vend_003', '0123456789', 'CORPORATION', '최마트', 'mart@merchant.com', '01001234567', '서울시 서초구 반포대로 789', 'ACTIVE');

-- =============================================================================
-- Merchant PG Mappings
-- =============================================================================
INSERT INTO merchant_pg_mappings (id, merchant_id, pg_connection_id, mid, terminal_id, cat_id, status) VALUES
('01960000-0000-7000-0004-000000000001', '01960000-0000-7000-0003-000000000001', 1, 'KORPAY_MCH001', 'TID001', 'CAT001', 'ACTIVE'),
('01960000-0000-7000-0004-000000000002', '01960000-0000-7000-0003-000000000002', 1, 'KORPAY_MCH002', 'TID002', 'CAT002', 'ACTIVE'),
('01960000-0000-7000-0004-000000000003', '01960000-0000-7000-0003-000000000003', 1, 'KORPAY_MCH003', 'TID003', 'CAT003', 'ACTIVE');

-- =============================================================================
-- Terminals (단말기)
-- =============================================================================
INSERT INTO terminals (id, cat_id, terminal_type, merchant_id, organization_id, serial_number, model, manufacturer, install_address, install_date, status) VALUES
('01960000-0000-7000-0005-000000000001', 'CAT001', 'CAT', '01960000-0000-7000-0003-000000000001', '01960000-0000-7000-0001-000000000008', 'SN001', 'KP-100', 'KORPAY', '서울시 강남구 역삼동 123', '2024-01-15', 'ACTIVE'),
('01960000-0000-7000-0005-000000000002', 'CAT002', 'POS', '01960000-0000-7000-0003-000000000002', '01960000-0000-7000-0001-000000000009', 'SN002', 'KP-200', 'KORPAY', '서울시 강남구 삼성동 456', '2024-02-10', 'ACTIVE'),
('01960000-0000-7000-0005-000000000003', 'CAT003', 'KIOSK', '01960000-0000-7000-0003-000000000003', '01960000-0000-7000-0001-000000000010', 'SN003', 'KP-300', 'KORPAY', '서울시 서초구 반포대로 789', '2024-03-05', 'ACTIVE'),
('01960000-0000-7000-0005-000000000004', 'CAT004', 'ONLINE', '01960000-0000-7000-0003-000000000003', '01960000-0000-7000-0001-000000000010', 'SN004', 'ONLINE', 'KORPAY', NULL, '2024-03-05', 'ACTIVE');

-- =============================================================================
-- Fee Configurations (수수료 설정)
-- =============================================================================
-- 결제수단 ID 조회 (서브쿼리)
INSERT INTO fee_configurations (id, entity_id, entity_type, entity_path, payment_method_id, card_company_id, fee_type, fee_rate, fixed_fee, priority, status) VALUES
-- 총판 기본 수수료 (카드결제 2.5%)
('01960000-0000-7000-0006-000000000001', '01960000-0000-7000-0001-000000000001', 'DISTRIBUTOR', 'dist_001', (SELECT id FROM payment_methods WHERE method_code = 'CREDIT'), NULL, 'PERCENTAGE', 0.025000, NULL, 100, 'ACTIVE'),
('01960000-0000-7000-0006-000000000002', '01960000-0000-7000-0001-000000000001', 'DISTRIBUTOR', 'dist_001', (SELECT id FROM payment_methods WHERE method_code = 'DEBIT'), NULL, 'PERCENTAGE', 0.020000, NULL, 100, 'ACTIVE'),
-- 대리점 수수료 (신용 2.8%, 체크 2.3%)
('01960000-0000-7000-0006-000000000003', '01960000-0000-7000-0001-000000000002', 'AGENCY', 'dist_001.agcy_001', (SELECT id FROM payment_methods WHERE method_code = 'CREDIT'), NULL, 'PERCENTAGE', 0.028000, NULL, 90, 'ACTIVE'),
('01960000-0000-7000-0006-000000000004', '01960000-0000-7000-0001-000000000002', 'AGENCY', 'dist_001.agcy_001', (SELECT id FROM payment_methods WHERE method_code = 'DEBIT'), NULL, 'PERCENTAGE', 0.023000, NULL, 90, 'ACTIVE'),
-- 딜러 수수료 (신용 3.0%, 체크 2.5%)
('01960000-0000-7000-0006-000000000005', '01960000-0000-7000-0001-000000000004', 'DEALER', 'dist_001.agcy_001.deal_001', (SELECT id FROM payment_methods WHERE method_code = 'CREDIT'), NULL, 'PERCENTAGE', 0.030000, NULL, 80, 'ACTIVE'),
('01960000-0000-7000-0006-000000000006', '01960000-0000-7000-0001-000000000004', 'DEALER', 'dist_001.agcy_001.deal_001', (SELECT id FROM payment_methods WHERE method_code = 'DEBIT'), NULL, 'PERCENTAGE', 0.025000, NULL, 80, 'ACTIVE'),
-- 판매점 수수료 (신용 3.2%, 체크 2.7%)
('01960000-0000-7000-0006-000000000007', '01960000-0000-7000-0001-000000000006', 'SELLER', 'dist_001.agcy_001.deal_001.sell_001', (SELECT id FROM payment_methods WHERE method_code = 'CREDIT'), NULL, 'PERCENTAGE', 0.032000, NULL, 70, 'ACTIVE'),
('01960000-0000-7000-0006-000000000008', '01960000-0000-7000-0001-000000000006', 'SELLER', 'dist_001.agcy_001.deal_001.sell_001', (SELECT id FROM payment_methods WHERE method_code = 'DEBIT'), NULL, 'PERCENTAGE', 0.027000, NULL, 70, 'ACTIVE'),
-- 가맹점 수수료 (신용 3.5%, 체크 3.0%)
('01960000-0000-7000-0006-000000000009', '01960000-0000-7000-0001-000000000008', 'VENDOR', 'dist_001.agcy_001.deal_001.sell_001.vend_001', (SELECT id FROM payment_methods WHERE method_code = 'CREDIT'), NULL, 'PERCENTAGE', 0.035000, NULL, 60, 'ACTIVE'),
('01960000-0000-7000-0006-000000000010', '01960000-0000-7000-0001-000000000008', 'VENDOR', 'dist_001.agcy_001.deal_001.sell_001.vend_001', (SELECT id FROM payment_methods WHERE method_code = 'DEBIT'), NULL, 'PERCENTAGE', 0.030000, NULL, 60, 'ACTIVE');

-- =============================================================================
-- Contacts (담당자)
-- =============================================================================
INSERT INTO contacts (id, name, phone, email, role, entity_type, entity_id, is_primary) VALUES
-- 가맹점 담당자
('01960000-0000-7000-0007-000000000001', '홍커피', '01089012345', 'coffee@merchant.com', 'PRIMARY', 'MERCHANT', '01960000-0000-7000-0003-000000000001', true),
('01960000-0000-7000-0007-000000000002', '박분식', '01090123456', 'bunsik@merchant.com', 'PRIMARY', 'MERCHANT', '01960000-0000-7000-0003-000000000002', true),
('01960000-0000-7000-0007-000000000003', '최마트', '01001234567', 'mart@merchant.com', 'PRIMARY', 'MERCHANT', '01960000-0000-7000-0003-000000000003', true),
('01960000-0000-7000-0007-000000000004', '이매니저', '01011112222', 'manager@mart.com', 'SETTLEMENT', 'MERCHANT', '01960000-0000-7000-0003-000000000003', false),
-- 사업자 담당자
('01960000-0000-7000-0007-000000000005', '김총판', '01012345678', 'master@korpay-dist.com', 'PRIMARY', 'BUSINESS_ENTITY', '01960000-0000-7000-0000-000000000001', true),
('01960000-0000-7000-0007-000000000006', '이관리', '01012345679', 'admin@korpay-dist.com', 'SECONDARY', 'BUSINESS_ENTITY', '01960000-0000-7000-0000-000000000001', false);

-- =============================================================================
-- Settlement Accounts (정산계좌)
-- =============================================================================
INSERT INTO settlement_accounts (id, bank_code, bank_name, account_number, account_holder, entity_type, entity_id, is_primary, status, verified_at) VALUES
-- 가맹점 정산계좌
('01960000-0000-7000-0008-000000000001', '004', 'KB국민은행', '12345678901234', '홍커피', 'MERCHANT', '01960000-0000-7000-0003-000000000001', true, 'ACTIVE', CURRENT_TIMESTAMP),
('01960000-0000-7000-0008-000000000002', '088', '신한은행', '23456789012345', '박분식', 'MERCHANT', '01960000-0000-7000-0003-000000000002', true, 'ACTIVE', CURRENT_TIMESTAMP),
('01960000-0000-7000-0008-000000000003', '020', '우리은행', '34567890123456', '(주)프리미엄마트', 'MERCHANT', '01960000-0000-7000-0003-000000000003', true, 'ACTIVE', CURRENT_TIMESTAMP),
-- 사업자 정산계좌
('01960000-0000-7000-0008-000000000004', '003', '기업은행', '45678901234567', '(주)코르페이 총판', 'BUSINESS_ENTITY', '01960000-0000-7000-0000-000000000001', true, 'ACTIVE', CURRENT_TIMESTAMP);

-- =============================================================================
-- Transactions (샘플 거래 - 최근 30일)
-- =============================================================================
-- 주의: transaction_events는 파티셔닝되어 있으므로 날짜에 맞는 파티션이 있어야 함

DO $$
DECLARE
  v_credit_method_id UUID;
  v_debit_method_id UUID;
  v_kb_card_id UUID;
  v_sh_card_id UUID;
  v_ss_card_id UUID;
  v_txn_id_1 UUID := '01960000-0000-7000-0009-000000000001';
  v_txn_id_2 UUID := '01960000-0000-7000-0009-000000000002';
  v_txn_id_3 UUID := '01960000-0000-7000-0009-000000000003';
  v_event_id_1 UUID := '01960000-0000-7000-000A-000000000001';
  v_event_id_2 UUID := '01960000-0000-7000-000A-000000000002';
  v_event_id_3 UUID := '01960000-0000-7000-000A-000000000003';
  v_now TIMESTAMPTZ := CURRENT_TIMESTAMP;
  v_exists BOOLEAN;
BEGIN
  SELECT id INTO v_credit_method_id FROM payment_methods WHERE method_code = 'CREDIT';
  SELECT id INTO v_debit_method_id FROM payment_methods WHERE method_code = 'DEBIT';
  SELECT id INTO v_kb_card_id FROM card_companies WHERE company_code = 'KB';
  SELECT id INTO v_sh_card_id FROM card_companies WHERE company_code = 'SH';
  SELECT id INTO v_ss_card_id FROM card_companies WHERE company_code = 'SS';

  SELECT EXISTS(SELECT 1 FROM transactions WHERE id = v_txn_id_1) INTO v_exists;
  IF NOT v_exists THEN
    INSERT INTO transactions (id, transaction_id, merchant_id, merchant_pg_mapping_id, pg_connection_id, merchant_path, org_path, payment_method_id, card_company_id, amount, status, pg_transaction_id, approval_number, approved_at, cat_id, created_at)
    VALUES (v_txn_id_1, 'TXN' || to_char(CURRENT_DATE, 'YYYYMMDD') || '001', '01960000-0000-7000-0003-000000000001', '01960000-0000-7000-0004-000000000001', 1, 'dist_001.agcy_001.deal_001.sell_001.vend_001', 'dist_001.agcy_001.deal_001.sell_001.vend_001', v_credit_method_id, v_kb_card_id, 15000, 'APPROVED', 'PG_TXN_001', 'APR001', v_now - INTERVAL '2 hours', 'CAT001', v_now - INTERVAL '2 hours');

    INSERT INTO transaction_events (id, event_type, event_sequence, transaction_id, merchant_id, merchant_pg_mapping_id, pg_connection_id, merchant_path, org_path, payment_method_id, card_company_id, amount, previous_status, new_status, pg_transaction_id, approval_number, cat_id, occurred_at, created_at)
    VALUES (v_event_id_1, 'APPROVAL', 1, v_txn_id_1, '01960000-0000-7000-0003-000000000001', '01960000-0000-7000-0004-000000000001', 1, 'dist_001.agcy_001.deal_001.sell_001.vend_001', 'dist_001.agcy_001.deal_001.sell_001.vend_001', v_credit_method_id, v_kb_card_id, 15000, 'PENDING', 'APPROVED', 'PG_TXN_001', 'APR001', 'CAT001', v_now - INTERVAL '2 hours', v_now - INTERVAL '2 hours');
  END IF;

  SELECT EXISTS(SELECT 1 FROM transactions WHERE id = v_txn_id_2) INTO v_exists;
  IF NOT v_exists THEN
    INSERT INTO transactions (id, transaction_id, merchant_id, merchant_pg_mapping_id, pg_connection_id, merchant_path, org_path, payment_method_id, card_company_id, amount, status, pg_transaction_id, approval_number, approved_at, cat_id, created_at)
    VALUES (v_txn_id_2, 'TXN' || to_char(CURRENT_DATE, 'YYYYMMDD') || '002', '01960000-0000-7000-0003-000000000002', '01960000-0000-7000-0004-000000000002', 1, 'dist_001.agcy_001.deal_001.sell_001.vend_002', 'dist_001.agcy_001.deal_001.sell_001.vend_002', v_debit_method_id, v_sh_card_id, 8500, 'APPROVED', 'PG_TXN_002', 'APR002', v_now - INTERVAL '1 hour', 'CAT002', v_now - INTERVAL '1 hour');

    INSERT INTO transaction_events (id, event_type, event_sequence, transaction_id, merchant_id, merchant_pg_mapping_id, pg_connection_id, merchant_path, org_path, payment_method_id, card_company_id, amount, previous_status, new_status, pg_transaction_id, approval_number, cat_id, occurred_at, created_at)
    VALUES (v_event_id_2, 'APPROVAL', 1, v_txn_id_2, '01960000-0000-7000-0003-000000000002', '01960000-0000-7000-0004-000000000002', 1, 'dist_001.agcy_001.deal_001.sell_001.vend_002', 'dist_001.agcy_001.deal_001.sell_001.vend_002', v_debit_method_id, v_sh_card_id, 8500, 'PENDING', 'APPROVED', 'PG_TXN_002', 'APR002', 'CAT002', v_now - INTERVAL '1 hour', v_now - INTERVAL '1 hour');
  END IF;

  SELECT EXISTS(SELECT 1 FROM transactions WHERE id = v_txn_id_3) INTO v_exists;
  IF NOT v_exists THEN
    INSERT INTO transactions (id, transaction_id, merchant_id, merchant_pg_mapping_id, pg_connection_id, merchant_path, org_path, payment_method_id, card_company_id, amount, status, pg_transaction_id, approval_number, approved_at, cat_id, created_at)
    VALUES (v_txn_id_3, 'TXN' || to_char(CURRENT_DATE, 'YYYYMMDD') || '003', '01960000-0000-7000-0003-000000000003', '01960000-0000-7000-0004-000000000003', 1, 'dist_001.agcy_001.deal_001.sell_002.vend_003', 'dist_001.agcy_001.deal_001.sell_002.vend_003', v_credit_method_id, v_ss_card_id, 125000, 'APPROVED', 'PG_TXN_003', 'APR003', v_now - INTERVAL '30 minutes', 'CAT003', v_now - INTERVAL '30 minutes');

    INSERT INTO transaction_events (id, event_type, event_sequence, transaction_id, merchant_id, merchant_pg_mapping_id, pg_connection_id, merchant_path, org_path, payment_method_id, card_company_id, amount, previous_status, new_status, pg_transaction_id, approval_number, cat_id, occurred_at, created_at)
    VALUES (v_event_id_3, 'APPROVAL', 1, v_txn_id_3, '01960000-0000-7000-0003-000000000003', '01960000-0000-7000-0004-000000000003', 1, 'dist_001.agcy_001.deal_001.sell_002.vend_003', 'dist_001.agcy_001.deal_001.sell_002.vend_003', v_credit_method_id, v_ss_card_id, 125000, 'PENDING', 'APPROVED', 'PG_TXN_003', 'APR003', 'CAT003', v_now - INTERVAL '30 minutes', v_now - INTERVAL '30 minutes');
  END IF;
END $$;

INSERT INTO settlements (id, transaction_event_id, transaction_id, merchant_id, merchant_path, entity_id, entity_type, entity_path, entry_type, amount, fee_amount, net_amount, fee_rate, status)
SELECT * FROM (VALUES
  ('01960000-0000-7000-000B-000000000001'::UUID, '01960000-0000-7000-000A-000000000001'::UUID, '01960000-0000-7000-0009-000000000001'::UUID, '01960000-0000-7000-0003-000000000001'::UUID, 'dist_001.agcy_001.deal_001.sell_001.vend_001'::public.ltree, '01960000-0000-7000-0001-000000000008'::UUID, 'VENDOR'::VARCHAR, 'dist_001.agcy_001.deal_001.sell_001.vend_001'::public.ltree, 'CREDIT'::VARCHAR, 14475::BIGINT, 525::BIGINT, 13950::BIGINT, 0.035000::NUMERIC, 'PENDING'::VARCHAR),
  ('01960000-0000-7000-000B-000000000002'::UUID, '01960000-0000-7000-000A-000000000001'::UUID, '01960000-0000-7000-0009-000000000001'::UUID, '01960000-0000-7000-0003-000000000001'::UUID, 'dist_001.agcy_001.deal_001.sell_001.vend_001'::public.ltree, '01960000-0000-7000-0001-000000000006'::UUID, 'SELLER'::VARCHAR, 'dist_001.agcy_001.deal_001.sell_001'::public.ltree, 'CREDIT'::VARCHAR, 45::BIGINT, 0::BIGINT, 45::BIGINT, 0.003000::NUMERIC, 'PENDING'::VARCHAR),
  ('01960000-0000-7000-000B-000000000003'::UUID, '01960000-0000-7000-000A-000000000001'::UUID, '01960000-0000-7000-0009-000000000001'::UUID, '01960000-0000-7000-0003-000000000001'::UUID, 'dist_001.agcy_001.deal_001.sell_001.vend_001'::public.ltree, '01960000-0000-7000-0001-000000000004'::UUID, 'DEALER'::VARCHAR, 'dist_001.agcy_001.deal_001.001'::public.ltree, 'CREDIT'::VARCHAR, 30::BIGINT, 0::BIGINT, 30::BIGINT, 0.002000::NUMERIC, 'PENDING'::VARCHAR),
  ('01960000-0000-7000-000B-000000000004'::UUID, '01960000-0000-7000-000A-000000000001'::UUID, '01960000-0000-7000-0009-000000000001'::UUID, '01960000-0000-7000-0003-000000000001'::UUID, 'dist_001.agcy_001.deal_001.sell_001.vend_001'::public.ltree, '01960000-0000-7000-0001-000000000002'::UUID, 'AGENCY'::VARCHAR, 'dist_001.agcy_001'::public.ltree, 'CREDIT'::VARCHAR, 30::BIGINT, 0::BIGINT, 30::BIGINT, 0.002000::NUMERIC, 'PENDING'::VARCHAR),
  ('01960000-0000-7000-000B-000000000005'::UUID, '01960000-0000-7000-000A-000000000001'::UUID, '01960000-0000-7000-0009-000000000001'::UUID, '01960000-0000-7000-0003-000000000001'::UUID, 'dist_001.agcy_001.deal_001.sell_001.vend_001'::public.ltree, '01960000-0000-7000-0001-000000000001'::UUID, 'DISTRIBUTOR'::VARCHAR, 'dist_001'::public.ltree, 'CREDIT'::VARCHAR, 420::BIGINT, 375::BIGINT, 45::BIGINT, 0.028000::NUMERIC, 'PENDING'::VARCHAR)
) AS v(id, transaction_event_id, transaction_id, merchant_id, merchant_path, entity_id, entity_type, entity_path, entry_type, amount, fee_amount, net_amount, fee_rate, status)
WHERE NOT EXISTS (SELECT 1 FROM settlements WHERE id = '01960000-0000-7000-000B-000000000001')

COMMENT ON TABLE business_entities IS 'Test seed: 12 business entities for development';
COMMENT ON TABLE organizations IS 'Test seed: 10 organizations (5-level hierarchy) for development';
COMMENT ON TABLE users IS 'Test seed: 7 users (password: password123) for development';
COMMENT ON TABLE merchants IS 'Test seed: 3 merchants for development';
COMMENT ON TABLE terminals IS 'Test seed: 4 terminals for development';
COMMENT ON TABLE fee_configurations IS 'Test seed: 10 fee configurations for development';
COMMENT ON TABLE transactions IS 'Test seed: 3 transactions for development';
COMMENT ON TABLE settlements IS 'Test seed: 5 settlements for development';
