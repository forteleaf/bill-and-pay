-- =============================================================================
-- Bill&Pay Tenant Schema - Seed Organization Data
-- =============================================================================
-- Description: Test organization hierarchy (5 levels)
-- Purpose: Testing organization management features
-- =============================================================================

-- Level 1: DISTRIBUTOR (Root)
INSERT INTO organizations (
    id, org_code, name, org_type, path, parent_id, level,
    email, phone, address, status, created_at, updated_at
) VALUES (
    'a0000000-0000-0000-0000-000000000001'::uuid,
    'dist_001',
    'Korea Payment Distributor',
    'DISTRIBUTOR',
    'dist_001'::ltree,
    NULL,
    1,
    'contact@kpdist.com',
    '02-1234-5678',
    'Seoul, Korea',
    'ACTIVE',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

-- Level 2: AGENCY (under DISTRIBUTOR)
INSERT INTO organizations (
    id, org_code, name, org_type, path, parent_id, level,
    email, phone, address, status, created_at, updated_at
) VALUES 
(
    'a0000000-0000-0000-0000-000000000002'::uuid,
    'agcy_001',
    'Seoul Central Agency',
    'AGENCY',
    'dist_001.agcy_001'::ltree,
    'a0000000-0000-0000-0000-000000000001'::uuid,
    2,
    'seoul@agency.com',
    '02-2234-5678',
    'Gangnam, Seoul',
    'ACTIVE',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
),
(
    'a0000000-0000-0000-0000-000000000003'::uuid,
    'agcy_002',
    'Busan Regional Agency',
    'AGENCY',
    'dist_001.agcy_002'::ltree,
    'a0000000-0000-0000-0000-000000000001'::uuid,
    2,
    'busan@agency.com',
    '051-123-4567',
    'Haeundae, Busan',
    'ACTIVE',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

-- Level 3: DEALER (under AGENCY)
INSERT INTO organizations (
    id, org_code, name, org_type, path, parent_id, level,
    email, phone, address, status, created_at, updated_at
) VALUES 
(
    'a0000000-0000-0000-0000-000000000004'::uuid,
    'deal_001',
    'Gangnam Premium Dealer',
    'DEALER',
    'dist_001.agcy_001.deal_001'::ltree,
    'a0000000-0000-0000-0000-000000000002'::uuid,
    3,
    'gangnam@dealer.com',
    '02-3234-5678',
    'Gangnam Station, Seoul',
    'ACTIVE',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
),
(
    'a0000000-0000-0000-0000-000000000005'::uuid,
    'deal_002',
    'Hongdae Shopping Dealer',
    'DEALER',
    'dist_001.agcy_001.deal_002'::ltree,
    'a0000000-0000-0000-0000-000000000002'::uuid,
    3,
    'hongdae@dealer.com',
    '02-3334-5678',
    'Hongdae, Seoul',
    'SUSPENDED',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
),
(
    'a0000000-0000-0000-0000-000000000006'::uuid,
    'deal_003',
    'Busan Harbor Dealer',
    'DEALER',
    'dist_001.agcy_002.deal_003'::ltree,
    'a0000000-0000-0000-0000-000000000003'::uuid,
    3,
    'harbor@dealer.com',
    '051-223-4567',
    'Busan Harbor',
    'ACTIVE',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

-- Level 4: SELLER (under DEALER)
INSERT INTO organizations (
    id, org_code, name, org_type, path, parent_id, level,
    email, phone, address, status, created_at, updated_at
) VALUES 
(
    'a0000000-0000-0000-0000-000000000007'::uuid,
    'sell_001',
    'Fashion Mall Seller',
    'SELLER',
    'dist_001.agcy_001.deal_001.sell_001'::ltree,
    'a0000000-0000-0000-0000-000000000004'::uuid,
    4,
    'fashion@seller.com',
    '02-4234-5678',
    'Coex Mall, Seoul',
    'ACTIVE',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
),
(
    'a0000000-0000-0000-0000-000000000008'::uuid,
    'sell_002',
    'Electronics Store Seller',
    'SELLER',
    'dist_001.agcy_001.deal_001.sell_002'::ltree,
    'a0000000-0000-0000-0000-000000000004'::uuid,
    4,
    'electronics@seller.com',
    '02-4334-5678',
    'Yongsan, Seoul',
    'ACTIVE',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
),
(
    'a0000000-0000-0000-0000-000000000009'::uuid,
    'sell_003',
    'Cafe Chain Seller',
    'SELLER',
    'dist_001.agcy_001.deal_002.sell_003'::ltree,
    'a0000000-0000-0000-0000-000000000005'::uuid,
    4,
    'cafe@seller.com',
    '02-4434-5678',
    'Hongdae Street, Seoul',
    'ACTIVE',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

-- Level 5: VENDOR (under SELLER - Leaf nodes)
INSERT INTO organizations (
    id, org_code, name, org_type, path, parent_id, level,
    email, phone, address, status, created_at, updated_at
) VALUES 
(
    'a0000000-0000-0000-0000-00000000000a'::uuid,
    'vend_001',
    'Fashion Store #1',
    'VENDOR',
    'dist_001.agcy_001.deal_001.sell_001.vend_001'::ltree,
    'a0000000-0000-0000-0000-000000000007'::uuid,
    5,
    'store1@fashion.com',
    '02-5234-5678',
    'Coex Mall 1F',
    'ACTIVE',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
),
(
    'a0000000-0000-0000-0000-00000000000b'::uuid,
    'vend_002',
    'Fashion Store #2',
    'VENDOR',
    'dist_001.agcy_001.deal_001.sell_001.vend_002'::ltree,
    'a0000000-0000-0000-0000-000000000007'::uuid,
    5,
    'store2@fashion.com',
    '02-5334-5678',
    'Coex Mall 2F',
    'ACTIVE',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
),
(
    'a0000000-0000-0000-0000-00000000000c'::uuid,
    'vend_003',
    'Electronics Shop',
    'VENDOR',
    'dist_001.agcy_001.deal_001.sell_002.vend_003'::ltree,
    'a0000000-0000-0000-0000-000000000008'::uuid,
    5,
    'shop@electronics.com',
    '02-5434-5678',
    'Yongsan Market',
    'ACTIVE',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
),
(
    'a0000000-0000-0000-0000-00000000000d'::uuid,
    'vend_004',
    'Cafe Branch A',
    'VENDOR',
    'dist_001.agcy_001.deal_002.sell_003.vend_004'::ltree,
    'a0000000-0000-0000-0000-000000000009'::uuid,
    5,
    'brancha@cafe.com',
    '02-5534-5678',
    'Hongdae Main Street',
    'TERMINATED',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

-- =============================================================================
-- Summary
-- =============================================================================
-- Total: 13 organizations
-- DISTRIBUTOR: 1
-- AGENCY: 2
-- DEALER: 3
-- SELLER: 3
-- VENDOR: 4
--
-- Status distribution:
-- ACTIVE: 11
-- SUSPENDED: 1
-- TERMINATED: 1
-- =============================================================================

COMMENT ON TABLE organizations IS 'Seeded with 13 test organizations in 5-level hierarchy';
