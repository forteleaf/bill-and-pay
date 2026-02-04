-- =============================================================================
-- Demo Requests Table
-- =============================================================================
-- 데모 신청 정보를 저장하는 테이블 (public 스키마 - 인증 불필요)
-- =============================================================================

CREATE TYPE demo_request_status AS ENUM ('PENDING', 'CONTACTED', 'SCHEDULED', 'COMPLETED', 'CANCELLED');

CREATE TABLE public.demo_requests (
    id UUID PRIMARY KEY DEFAULT uuidv7(),
    company_name VARCHAR(100) NOT NULL,
    contact_name VARCHAR(50) NOT NULL,
    email VARCHAR(100) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    position VARCHAR(50),
    employee_count VARCHAR(20),
    monthly_volume VARCHAR(20),
    message TEXT,
    status demo_request_status NOT NULL DEFAULT 'PENDING',
    ip_address VARCHAR(45),
    user_agent TEXT,
    notes TEXT,
    contacted_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_demo_requests_status ON public.demo_requests(status);
CREATE INDEX idx_demo_requests_email ON public.demo_requests(email);
CREATE INDEX idx_demo_requests_created_at ON public.demo_requests(created_at DESC);

COMMENT ON TABLE public.demo_requests IS '데모 신청 정보';
COMMENT ON COLUMN public.demo_requests.employee_count IS '임직원 수 범위 (1-10, 11-50, 51-200, 201-500, 500+)';
COMMENT ON COLUMN public.demo_requests.monthly_volume IS '월 거래액 범위';
COMMENT ON COLUMN public.demo_requests.ip_address IS '신청자 IP 주소 (Rate Limiting 용)';
