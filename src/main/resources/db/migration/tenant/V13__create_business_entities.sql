-- =============================================================================
-- Bill&Pay Tenant Schema - Business Entities
-- =============================================================================
-- Separates business registration info from organizations
-- Same business (123-45-67890) can be DISTRIBUTOR, DEALER, etc.
-- =============================================================================

-- =============================================================================
-- Business Entities Table
-- =============================================================================
CREATE TABLE business_entities (
  id UUID PRIMARY KEY DEFAULT uuidv7(),
  
  -- Business Type
  business_type VARCHAR(20) NOT NULL,
  
  -- Business Registration
  business_number VARCHAR(12),
  corporate_number VARCHAR(14),
  
  -- Business Info
  business_name VARCHAR(200) NOT NULL,
  representative_name VARCHAR(100) NOT NULL,
  open_date DATE,
  
  -- Address
  business_address TEXT,
  actual_address TEXT,
  
  -- Business Category
  business_category VARCHAR(100),
  business_sub_category VARCHAR(100),
  
  -- Contact
  main_phone VARCHAR(20),
  manager_name VARCHAR(100),
  manager_phone VARCHAR(20),
  email VARCHAR(255),
  
  -- Audit
  created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted_at TIMESTAMPTZ,
  
  CONSTRAINT business_entities_type_check 
    CHECK (business_type IN ('CORPORATION', 'INDIVIDUAL', 'NON_BUSINESS')),
  CONSTRAINT business_entities_number_unique 
    UNIQUE NULLS NOT DISTINCT (business_number),
  CONSTRAINT business_entities_corp_check
    CHECK (
      (business_type = 'NON_BUSINESS' AND business_number IS NULL) OR
      (business_type != 'NON_BUSINESS' AND business_number IS NOT NULL)
    ),
  CONSTRAINT business_entities_corp_number_check
    CHECK (
      (business_type = 'CORPORATION' AND corporate_number IS NOT NULL) OR
      (business_type != 'CORPORATION')
    )
);

CREATE INDEX idx_business_entities_business_number ON business_entities(business_number) WHERE business_number IS NOT NULL;
CREATE INDEX idx_business_entities_business_type ON business_entities(business_type);
CREATE INDEX idx_business_entities_business_name ON business_entities(business_name);

COMMENT ON TABLE business_entities IS 'Business registration info (separated from organizations)';
COMMENT ON COLUMN business_entities.business_type IS 'CORPORATION, INDIVIDUAL, NON_BUSINESS';
COMMENT ON COLUMN business_entities.business_number IS 'Format: 000-00-00000 (10 digits)';
COMMENT ON COLUMN business_entities.corporate_number IS 'Format: 000000-0000000 (13 digits), required for CORPORATION';

-- =============================================================================
-- Add business_entity_id to organizations
-- =============================================================================
ALTER TABLE organizations 
  ADD COLUMN business_entity_id UUID;

ALTER TABLE organizations
  ADD CONSTRAINT organizations_business_entity_fk 
  FOREIGN KEY (business_entity_id) REFERENCES business_entities(id);

CREATE INDEX idx_organizations_business_entity_id ON organizations(business_entity_id) WHERE business_entity_id IS NOT NULL;

COMMENT ON COLUMN organizations.business_entity_id IS 'Reference to business_entities table';

-- =============================================================================
-- Migrate existing data (if any)
-- =============================================================================
INSERT INTO business_entities (
  id,
  business_type,
  business_number,
  business_name,
  representative_name,
  created_at,
  updated_at
)
SELECT 
  gen_random_uuid(),
  'INDIVIDUAL',
  o.business_number,
  COALESCE(o.business_name, o.name),
  COALESCE(o.representative_name, 'N/A'),
  o.created_at,
  o.updated_at
FROM organizations o
WHERE o.business_number IS NOT NULL
  AND o.business_entity_id IS NULL
ON CONFLICT (business_number) DO NOTHING;

UPDATE organizations o
SET business_entity_id = be.id
FROM business_entities be
WHERE o.business_number = be.business_number
  AND o.business_entity_id IS NULL;
