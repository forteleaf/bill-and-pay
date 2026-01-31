-- Add business_entity_id column to organizations table
ALTER TABLE organizations
ADD COLUMN business_entity_id UUID;

-- Add foreign key constraint
ALTER TABLE organizations
ADD CONSTRAINT fk_organizations_business_entity
FOREIGN KEY (business_entity_id) REFERENCES business_entities(id);

-- Add index for better query performance
CREATE INDEX idx_organizations_business_entity_id 
ON organizations(business_entity_id) 
WHERE business_entity_id IS NOT NULL;
