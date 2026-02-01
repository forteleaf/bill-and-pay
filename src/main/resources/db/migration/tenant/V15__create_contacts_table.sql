CREATE TYPE contact_role AS ENUM ('PRIMARY', 'SECONDARY', 'SETTLEMENT', 'TECHNICAL');
CREATE TYPE contact_entity_type AS ENUM ('BUSINESS_ENTITY', 'MERCHANT');

CREATE TABLE contacts (
    id UUID PRIMARY KEY DEFAULT uuidv7(),
    name VARCHAR(100) NOT NULL,
    phone VARCHAR(20),
    email VARCHAR(255),
    role contact_role NOT NULL DEFAULT 'PRIMARY',
    entity_type contact_entity_type NOT NULL,
    entity_id UUID NOT NULL,
    is_primary BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMPTZ
);

CREATE INDEX idx_contacts_entity ON contacts(entity_type, entity_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_contacts_role ON contacts(role) WHERE deleted_at IS NULL;
CREATE UNIQUE INDEX idx_contacts_primary_unique ON contacts(entity_type, entity_id) 
    WHERE is_primary = true AND deleted_at IS NULL;

COMMENT ON TABLE contacts IS '담당자 정보 (사업자/가맹점 공용)';
COMMENT ON COLUMN contacts.name IS '담당자 이름';
COMMENT ON COLUMN contacts.phone IS '담당자 연락처';
COMMENT ON COLUMN contacts.email IS '담당자 이메일';
COMMENT ON COLUMN contacts.role IS 'PRIMARY(주담당자), SECONDARY(부담당자), SETTLEMENT(정산담당), TECHNICAL(기술담당)';
COMMENT ON COLUMN contacts.entity_type IS 'BUSINESS_ENTITY 또는 MERCHANT';
COMMENT ON COLUMN contacts.entity_id IS 'business_entities.id 또는 merchants.id';
COMMENT ON COLUMN contacts.is_primary IS '주 담당자 여부 (엔티티당 1명만 가능)';

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
    be.manager_name,
    be.manager_phone,
    be.email,
    'PRIMARY'::contact_role,
    'BUSINESS_ENTITY'::contact_entity_type,
    be.id,
    true,
    be.created_at,
    be.updated_at
FROM business_entities be
WHERE be.manager_name IS NOT NULL
  AND be.deleted_at IS NULL;
