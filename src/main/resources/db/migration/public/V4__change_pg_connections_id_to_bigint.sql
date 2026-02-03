-- =============================================================================
-- Bill&Pay Public Schema - Change pg_connections.id from UUID to BIGINT
-- =============================================================================
-- Description: Changes pg_connections primary key from UUID to BIGINT SERIAL
-- This is a breaking change - existing data will be recreated
-- =============================================================================

DROP TABLE IF EXISTS public.pg_connections CASCADE;

-- Recreate pg_connections with BIGINT id
CREATE TABLE public.pg_connections (
  id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  pg_code VARCHAR(20) NOT NULL UNIQUE,
  pg_name VARCHAR(100) NOT NULL,
  
  -- Connection details
  api_base_url VARCHAR(500) NOT NULL,
  webhook_base_url VARCHAR(500),
  
  -- Tenant association (for multi-tenant webhook routing)
  tenant_id VARCHAR(50) REFERENCES public.tenants(id) ON DELETE SET NULL,
  
  -- Credentials (encrypt in application layer)
  credentials JSONB NOT NULL,
  
  -- Configuration
  config JSONB,
  
  -- Status
  status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  
  -- Audit
  created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  
  CONSTRAINT pg_connections_status_check CHECK (status IN ('ACTIVE', 'INACTIVE', 'MAINTENANCE'))
);

CREATE INDEX idx_pg_connections_code ON public.pg_connections(pg_code);
CREATE INDEX idx_pg_connections_status ON public.pg_connections(status) WHERE status = 'ACTIVE';
CREATE INDEX idx_pg_connections_tenant_id ON public.pg_connections(tenant_id) WHERE tenant_id IS NOT NULL;

COMMENT ON TABLE public.pg_connections IS 'Payment Gateway connection configurations (KORPAY, NICE, etc)';
COMMENT ON COLUMN public.pg_connections.id IS 'Auto-incremented BIGINT primary key';
COMMENT ON COLUMN public.pg_connections.pg_code IS 'Unique PG identifier (e.g., KORPAY, NICE_PAY)';
COMMENT ON COLUMN public.pg_connections.credentials IS 'Encrypted API keys, secrets: {api_key, secret_key, merchant_id}';
COMMENT ON COLUMN public.pg_connections.config IS 'PG-specific settings: {timeout_ms, retry_count, webhook_secret}';

-- Re-seed KORPAY connection
INSERT INTO public.pg_connections (pg_code, pg_name, api_base_url, webhook_base_url, credentials, config, status)
VALUES (
  'KORPAY',
  'KORPAY Payment Gateway',
  'https://api.korpay.com',
  'https://api.korpay.com/webhook',
  '{"api_key": "SAMPLE_KEY", "secret_key": "SAMPLE_SECRET", "merchant_id": "SAMPLE_MID"}'::jsonb,
  '{"timeout_ms": 30000, "retry_count": 3, "webhook_secret": "SAMPLE_WEBHOOK_SECRET"}'::jsonb,
  'ACTIVE'
);
