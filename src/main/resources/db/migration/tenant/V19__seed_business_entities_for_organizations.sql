-- V19: Create business_entities from existing organizations
-- This migration creates business_entities for organizations that don't have one linked

DO $$
DECLARE
    org_record RECORD;
    new_be_id UUID;
BEGIN
    FOR org_record IN 
        SELECT id, name, business_number, business_name, representative_name, 
               address, phone, email, created_at
        FROM organizations 
        WHERE business_entity_id IS NULL
    LOOP
        new_be_id := gen_random_uuid();
        
        INSERT INTO business_entities (
            id, business_type, business_number, business_name, representative_name,
            business_address, main_phone, email, created_at, updated_at
        ) VALUES (
            new_be_id,
            CASE WHEN org_record.business_number IS NOT NULL THEN 'INDIVIDUAL' ELSE 'NON_BUSINESS' END,
            org_record.business_number,
            COALESCE(org_record.business_name, org_record.name),
            COALESCE(org_record.representative_name, '대표자'),
            org_record.address,
            org_record.phone,
            org_record.email,
            org_record.created_at,
            CURRENT_TIMESTAMP
        );
        
        UPDATE organizations SET business_entity_id = new_be_id WHERE id = org_record.id;
    END LOOP;
END $$;
