-- =============================================================================
-- Bill&Pay Tenant Schema - Seed Data
-- =============================================================================
-- 버전: 1.0 (마이그레이션 통합)
-- 설명: 초기 참조 데이터
-- =============================================================================

-- =============================================================================
-- Seed: Payment Methods
-- =============================================================================
INSERT INTO payment_methods (id, method_code, name, category, config, display_order, status) VALUES
(uuidv7(), 'CREDIT', '신용카드', 'CARD',
 '{"requires_auth": true, "settlement_delay_days": 3, "supports_installment": true}'::jsonb,
 1, 'ACTIVE'),
(uuidv7(), 'DEBIT', '체크카드', 'CARD',
 '{"requires_auth": true, "settlement_delay_days": 1, "supports_installment": false}'::jsonb,
 2, 'ACTIVE'),
(uuidv7(), 'OVERSEAS', '해외카드', 'CARD',
 '{"requires_auth": true, "settlement_delay_days": 5, "supports_installment": false, "requires_currency_conversion": true}'::jsonb,
 3, 'ACTIVE'),
(uuidv7(), 'TRANSFER', '계좌이체', 'BANK',
 '{"requires_auth": true, "settlement_delay_days": 1, "real_time_verification": true}'::jsonb,
 4, 'ACTIVE'),
(uuidv7(), 'VIRTUAL', '가상계좌', 'VIRTUAL',
 '{"requires_auth": false, "settlement_delay_days": 1, "requires_deposit_confirmation": true}'::jsonb,
 5, 'ACTIVE');

-- =============================================================================
-- Seed: Card Companies (Korean Major Issuers)
-- =============================================================================
INSERT INTO card_companies (id, company_code, company_name, config, display_order, status) VALUES
(uuidv7(), 'BC', 'BC카드',
 '{"full_name": "비씨카드", "logo_url": "/logos/bc.png", "bin_ranges": ["94", "54", "34"]}'::jsonb,
 1, 'ACTIVE'),
(uuidv7(), 'KB', 'KB국민카드',
 '{"full_name": "케이비국민카드", "logo_url": "/logos/kb.png", "bin_ranges": ["44", "98"]}'::jsonb,
 2, 'ACTIVE'),
(uuidv7(), 'SS', '삼성카드',
 '{"full_name": "삼성카드", "logo_url": "/logos/ss.png", "bin_ranges": ["51", "52", "53"]}'::jsonb,
 3, 'ACTIVE'),
(uuidv7(), 'SH', '신한카드',
 '{"full_name": "신한카드", "logo_url": "/logos/sh.png", "bin_ranges": ["40", "41", "42"]}'::jsonb,
 4, 'ACTIVE'),
(uuidv7(), 'HD', '현대카드',
 '{"full_name": "현대카드", "logo_url": "/logos/hd.png", "bin_ranges": ["43", "95"]}'::jsonb,
 5, 'ACTIVE'),
(uuidv7(), 'LT', '롯데카드',
 '{"full_name": "롯데카드", "logo_url": "/logos/lt.png", "bin_ranges": ["47", "48"]}'::jsonb,
 6, 'ACTIVE'),
(uuidv7(), 'HN', '하나카드',
 '{"full_name": "하나카드", "logo_url": "/logos/hn.png", "bin_ranges": ["45"]}'::jsonb,
 7, 'ACTIVE'),
(uuidv7(), 'WR', '우리카드',
 '{"full_name": "우리카드", "logo_url": "/logos/wr.png", "bin_ranges": ["46"]}'::jsonb,
 8, 'ACTIVE'),
(uuidv7(), 'NH', 'NH농협카드',
 '{"full_name": "엔에이치농협카드", "logo_url": "/logos/nh.png", "bin_ranges": ["91", "92"]}'::jsonb,
 9, 'ACTIVE');

COMMENT ON TABLE payment_methods IS 'Seeded with 5 standard Korean payment methods';
COMMENT ON TABLE card_companies IS 'Seeded with 9 major Korean card companies';
