-- =============================================================================
-- Bill&Pay Platform Admin Tables
-- =============================================================================
-- 설명: 플랫폼 관리자(Super Master) 계정, 감사 로그, 공지사항
-- =============================================================================

-- 1. 플랫폼 관리자 테이블
CREATE TABLE public.platform_admins (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  username VARCHAR(50) NOT NULL UNIQUE,
  password VARCHAR(255) NOT NULL,
  email VARCHAR(255) NOT NULL UNIQUE,
  full_name VARCHAR(200) NOT NULL,
  phone VARCHAR(20),
  role VARCHAR(30) NOT NULL,
  two_factor_enabled BOOLEAN NOT NULL DEFAULT FALSE,
  last_login_at TIMESTAMPTZ,
  failed_login_count INTEGER NOT NULL DEFAULT 0,
  locked_until TIMESTAMPTZ,
  status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT platform_admins_role_check CHECK (role IN ('SUPER_ADMIN', 'PLATFORM_OPERATOR', 'PLATFORM_VIEWER')),
  CONSTRAINT platform_admins_status_check CHECK (status IN ('ACTIVE', 'SUSPENDED', 'DELETED'))
);

CREATE INDEX idx_platform_admins_username ON public.platform_admins(username);
CREATE INDEX idx_platform_admins_status ON public.platform_admins(status) WHERE status = 'ACTIVE';

COMMENT ON TABLE public.platform_admins IS '플랫폼 관리자 (Super Master)';
COMMENT ON COLUMN public.platform_admins.role IS '역할 (SUPER_ADMIN, PLATFORM_OPERATOR, PLATFORM_VIEWER)';
COMMENT ON COLUMN public.platform_admins.status IS '상태 (ACTIVE, SUSPENDED, DELETED)';

-- 2. 플랫폼 감사 로그
CREATE TABLE public.platform_audit_logs (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  admin_id UUID NOT NULL REFERENCES public.platform_admins(id),
  admin_username VARCHAR(50) NOT NULL,
  action VARCHAR(50) NOT NULL,
  resource_type VARCHAR(100) NOT NULL,
  resource_id VARCHAR(200),
  target_tenant_id VARCHAR(50),
  details JSONB,
  ip_address INET,
  created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_audit_logs_admin_id ON public.platform_audit_logs(admin_id);
CREATE INDEX idx_audit_logs_action ON public.platform_audit_logs(action);
CREATE INDEX idx_audit_logs_created_at ON public.platform_audit_logs(created_at DESC);
CREATE INDEX idx_audit_logs_target_tenant ON public.platform_audit_logs(target_tenant_id) WHERE target_tenant_id IS NOT NULL;

COMMENT ON TABLE public.platform_audit_logs IS '플랫폼 관리 감사 로그 (불변)';

-- 3. 공지사항
CREATE TABLE public.platform_announcements (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  title VARCHAR(500) NOT NULL,
  content TEXT NOT NULL,
  announcement_type VARCHAR(30) NOT NULL,
  severity VARCHAR(20) NOT NULL DEFAULT 'INFO',
  target_type VARCHAR(20) NOT NULL DEFAULT 'ALL',
  target_tenant_ids JSONB,
  status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
  publish_at TIMESTAMPTZ,
  expire_at TIMESTAMPTZ,
  created_by UUID REFERENCES public.platform_admins(id),
  created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT announcements_type_check CHECK (announcement_type IN ('MAINTENANCE', 'POLICY', 'URGENT', 'GENERAL')),
  CONSTRAINT announcements_severity_check CHECK (severity IN ('INFO', 'WARNING', 'CRITICAL')),
  CONSTRAINT announcements_target_check CHECK (target_type IN ('ALL', 'SPECIFIC')),
  CONSTRAINT announcements_status_check CHECK (status IN ('DRAFT', 'PUBLISHED', 'ARCHIVED'))
);

CREATE INDEX idx_announcements_status ON public.platform_announcements(status) WHERE status = 'PUBLISHED';
CREATE INDEX idx_announcements_publish_at ON public.platform_announcements(publish_at);

COMMENT ON TABLE public.platform_announcements IS '플랫폼 공지사항';

-- 4. tenants 테이블 status에 PROVISIONING 추가
ALTER TABLE public.tenants DROP CONSTRAINT IF EXISTS tenants_status_check;
ALTER TABLE public.tenants ADD CONSTRAINT tenants_status_check
  CHECK (status IN ('PROVISIONING', 'ACTIVE', 'SUSPENDED', 'DELETED'));

-- 5. 초기 Super Admin 시드 (비밀번호: admin123!)
INSERT INTO public.platform_admins (username, password, email, full_name, role, status)
VALUES ('superadmin', '$2a$10$ECihIpHMvuzkZTrOpqdKC.J9A3.5U.QpIw2RTVKQRgEroZW5nw6su',
        'admin@billpay.co.kr', '시스템 관리자', 'SUPER_ADMIN', 'ACTIVE');
