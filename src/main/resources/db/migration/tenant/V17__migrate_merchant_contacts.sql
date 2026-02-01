-- =============================================================================
-- V17: Migrate merchants contact data to contacts table
-- =============================================================================
-- 기존 merchants 테이블의 contact_name, contact_email, contact_phone 데이터를
-- contacts 테이블로 이관하고, 해당 컬럼을 삭제합니다.
-- =============================================================================

-- Step 1: Migrate existing merchant contacts to contacts table
INSERT INTO contacts (
    id,
    name,
    phone,
    email,
    role,
    entity_type,
    entity_id,
    is_primary,
    created_at,
    updated_at
)
SELECT 
    gen_random_uuid(),
    m.contact_name,
    m.contact_phone,
    m.contact_email,
    'PRIMARY'::contact_role,
    'MERCHANT'::contact_entity_type,
    m.id,
    true,
    m.created_at,
    m.updated_at
FROM merchants m
WHERE m.contact_name IS NOT NULL
  AND m.deleted_at IS NULL
  AND NOT EXISTS (
    SELECT 1 FROM contacts c 
    WHERE c.entity_type = 'MERCHANT' 
      AND c.entity_id = m.id 
      AND c.is_primary = true
      AND c.deleted_at IS NULL
  );

-- Step 2: Drop contact columns from merchants table
ALTER TABLE merchants DROP COLUMN IF EXISTS contact_name;
ALTER TABLE merchants DROP COLUMN IF EXISTS contact_email;
ALTER TABLE merchants DROP COLUMN IF EXISTS contact_phone;

-- Add comment
COMMENT ON TABLE contacts IS '담당자 정보 (사업자/가맹점 공용) - V17에서 merchants 연락처 통합';
