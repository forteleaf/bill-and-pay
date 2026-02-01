-- =============================================================================
-- V18: Migrate organization business data to business_entities
-- =============================================================================
-- 1. Create business_entities records from existing organizations (if not linked)
-- 2. Update organizations.business_entity_id to point to created records
-- 3. Drop deprecated columns (business_number, business_name, representative_name)
-- =============================================================================

-- Step 1: Insert business_entities from organizations that don't have business_entity_id set
-- Skip if business_number is null (will use default/empty business entity)
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
    uuidv7(),
    'INDIVIDUAL'::VARCHAR(20), -- Default to INDIVIDUAL
    o.business_number,
    COALESCE(o.business_name, o.name), -- Use org name if business_name is null
    COALESCE(o.representative_name, 'N/A'), -- Default representative
    o.created_at,
    CURRENT_TIMESTAMP
FROM organizations o
WHERE o.business_entity_id IS NULL
  AND o.business_number IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM business_entities be 
      WHERE be.business_number = o.business_number
  );

-- Step 2: Update organizations to link to existing or newly created business_entities
UPDATE organizations o
SET business_entity_id = be.id
FROM business_entities be
WHERE o.business_entity_id IS NULL
  AND o.business_number IS NOT NULL
  AND o.business_number = be.business_number;

-- Step 3: For organizations without business_number, create a placeholder business_entity
-- (NON_BUSINESS type, no business_number)
INSERT INTO business_entities (
    id,
    business_type,
    business_name,
    representative_name,
    created_at,
    updated_at
)
SELECT 
    uuidv7(),
    'NON_BUSINESS'::VARCHAR(20),
    o.name,
    COALESCE(o.representative_name, 'N/A'),
    o.created_at,
    CURRENT_TIMESTAMP
FROM organizations o
WHERE o.business_entity_id IS NULL
  AND o.business_number IS NULL;

-- Step 4: Link organizations without business_number to their placeholder business_entities
-- Using name matching for organizations created in step 3
UPDATE organizations o
SET business_entity_id = (
    SELECT be.id 
    FROM business_entities be 
    WHERE be.business_name = o.name 
      AND be.business_type = 'NON_BUSINESS'
      AND be.business_number IS NULL
    ORDER BY be.created_at DESC
    LIMIT 1
)
WHERE o.business_entity_id IS NULL;

-- Step 5: Drop deprecated columns
ALTER TABLE organizations DROP COLUMN IF EXISTS business_number;
ALTER TABLE organizations DROP COLUMN IF EXISTS business_name;
ALTER TABLE organizations DROP COLUMN IF EXISTS representative_name;

-- Step 6: Add comment
COMMENT ON COLUMN organizations.business_entity_id IS 'Reference to business_entities table for business information';
