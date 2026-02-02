-- =============================================================================
-- Bill&Pay Public Schema - Add tenant_id to pg_connections
-- =============================================================================
-- Description: Enables tenant-aware webhook URL routing
-- - Adds tenant_id column to pg_connections table
-- - Creates foreign key to tenants table
-- - Adds index for tenant lookup
-- =============================================================================

-- Add tenant_id column (nullable for existing data)
ALTER TABLE public.pg_connections
ADD COLUMN tenant_id VARCHAR(50);

-- Add foreign key constraint to tenants table
ALTER TABLE public.pg_connections
ADD CONSTRAINT fk_pg_connections_tenant
FOREIGN KEY (tenant_id) REFERENCES public.tenants(id)
ON DELETE SET NULL;

-- Create index for tenant-based lookups
CREATE INDEX idx_pg_connections_tenant_id ON public.pg_connections(tenant_id)
WHERE tenant_id IS NOT NULL;

COMMENT ON COLUMN public.pg_connections.tenant_id IS 'Tenant ID for tenant-aware webhook routing. NULL for legacy connections.';
