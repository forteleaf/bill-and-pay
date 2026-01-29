-- =============================================================================
-- Bill&Pay Public Schema - Seed Data
-- =============================================================================
-- Description: Initial data for public schema
-- - Korean holidays (2024-2026)
-- - KORPAY PG connection (sample)
-- =============================================================================

-- =============================================================================
-- Seed: Korean Holidays (2024-2026)
-- =============================================================================
INSERT INTO public.holidays (id, holiday_date, name, country_code, is_recurring) VALUES
-- 2024
(uuidv7(), '2024-01-01', '신정', 'KR', true),
(uuidv7(), '2024-02-09', '설날 연휴', 'KR', false),
(uuidv7(), '2024-02-10', '설날', 'KR', false),
(uuidv7(), '2024-02-11', '설날 연휴', 'KR', false),
(uuidv7(), '2024-03-01', '삼일절', 'KR', true),
(uuidv7(), '2024-04-10', '국회의원 선거일', 'KR', false),
(uuidv7(), '2024-05-05', '어린이날', 'KR', true),
(uuidv7(), '2024-05-06', '대체공휴일', 'KR', false),
(uuidv7(), '2024-05-15', '부처님오신날', 'KR', false),
(uuidv7(), '2024-06-06', '현충일', 'KR', true),
(uuidv7(), '2024-08-15', '광복절', 'KR', true),
(uuidv7(), '2024-09-16', '추석 연휴', 'KR', false),
(uuidv7(), '2024-09-17', '추석', 'KR', false),
(uuidv7(), '2024-09-18', '추석 연휴', 'KR', false),
(uuidv7(), '2024-10-03', '개천절', 'KR', true),
(uuidv7(), '2024-10-09', '한글날', 'KR', true),
(uuidv7(), '2024-12-25', '크리스마스', 'KR', true),

-- 2025
(uuidv7(), '2025-01-01', '신정', 'KR', true),
(uuidv7(), '2025-01-28', '설날 연휴', 'KR', false),
(uuidv7(), '2025-01-29', '설날', 'KR', false),
(uuidv7(), '2025-01-30', '설날 연휴', 'KR', false),
(uuidv7(), '2025-03-01', '삼일절', 'KR', true),
(uuidv7(), '2025-03-03', '대체공휴일', 'KR', false),
(uuidv7(), '2025-05-05', '어린이날', 'KR', true),
(uuidv7(), '2025-05-06', '부처님오신날', 'KR', false),
(uuidv7(), '2025-06-06', '현충일', 'KR', true),
(uuidv7(), '2025-08-15', '광복절', 'KR', true),
(uuidv7(), '2025-10-03', '개천절', 'KR', true),
(uuidv7(), '2025-10-05', '추석 연휴', 'KR', false),
(uuidv7(), '2025-10-06', '추석', 'KR', false),
(uuidv7(), '2025-10-07', '추석 연휴', 'KR', false),
(uuidv7(), '2025-10-08', '대체공휴일', 'KR', false),
(uuidv7(), '2025-10-09', '한글날', 'KR', true),
(uuidv7(), '2025-12-25', '크리스마스', 'KR', true),

-- 2026
(uuidv7(), '2026-01-01', '신정', 'KR', true),
(uuidv7(), '2026-02-16', '설날 연휴', 'KR', false),
(uuidv7(), '2026-02-17', '설날', 'KR', false),
(uuidv7(), '2026-02-18', '설날 연휴', 'KR', false),
(uuidv7(), '2026-03-01', '삼일절', 'KR', true),
(uuidv7(), '2026-05-05', '어린이날', 'KR', true),
(uuidv7(), '2026-05-25', '부처님오신날', 'KR', false),
(uuidv7(), '2026-06-06', '현충일', 'KR', true),
(uuidv7(), '2026-08-15', '광복절', 'KR', true),
(uuidv7(), '2026-09-24', '추석 연휴', 'KR', false),
(uuidv7(), '2026-09-25', '추석', 'KR', false),
(uuidv7(), '2026-09-26', '추석 연휴', 'KR', false),
(uuidv7(), '2026-10-03', '개천절', 'KR', true),
(uuidv7(), '2026-10-05', '대체공휴일', 'KR', false),
(uuidv7(), '2026-10-09', '한글날', 'KR', true),
(uuidv7(), '2026-12-25', '크리스마스', 'KR', true);

-- =============================================================================
-- Seed: KORPAY PG Connection (Sample)
-- =============================================================================
-- Note: Credentials should be encrypted at application layer
INSERT INTO public.pg_connections (id, pg_code, pg_name, api_base_url, webhook_base_url, credentials, config, status)
VALUES (
  uuidv7(),
  'KORPAY',
  'KORPAY Payment Gateway',
  'https://api.korpay.com',
  'https://api.korpay.com/webhook',
  '{"api_key": "SAMPLE_KEY", "secret_key": "SAMPLE_SECRET", "merchant_id": "SAMPLE_MID"}'::jsonb,
  '{"timeout_ms": 30000, "retry_count": 3, "webhook_secret": "SAMPLE_WEBHOOK_SECRET"}'::jsonb,
  'ACTIVE'
);

COMMENT ON TABLE public.pg_connections IS 'Sample KORPAY connection - update credentials before production use';
