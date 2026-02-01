# Bill&Pay 정산 플랫폼

## 프로젝트 개요
다단계 영업 구조에서 발생하는 복잡한 수수료 체계를 자동화하고, 외부 PG사로부터 수신한 대량 결제 데이터를 원장(Ledger) 기반으로 정확하게 정산하는 플랫폼.

## 기술 스택
| 영역 | 기술 | 버전 |
|------|------|------|
| Database | PostgreSQL | 18 |
| Backend | Spring Boot | 3.5.10 |
| Frontend | Svelte | 5 |
| Runtime | Java | 25 LTS |

### 개발 도구

- do not use npm, use bun


## 핵심 아키텍처 원칙

### 1. 복식부기 정산 (PRD-03)
- 모든 정산은 `transaction_events` 기준으로 생성
- **Zero-Sum 원칙**: `|이벤트 금액| = SUM(정산 amount)`
- 승인: 모든 entity에 CREDIT
- 취소: 모든 entity에 DEBIT (역분개)

### 2. ltree 계층 구조 (PRD-02)
```
5단계 계층: DISTRIBUTOR > AGENCY > DEALER > SELLER > VENDOR
경로 예시: dist_001.agcy_001.deal_001.sell_001.vend_001
```

**권한 규칙**:
- 상위 → 하위: 조회 가능
- 하위 → 상위: 조회 불가
- 형제 간: 조회 불가

### 3. 멀티테넌시 (PRD-05)
- Schema-per-Tenant 방식
- `public`: 공통 데이터 (tenants, pg_connections, holidays)
- `tenant_xxx`: 테넌트별 데이터 (organizations, merchants, transactions, settlements)

### 4. 하이브리드 이벤트 소싱 (PRD-03)
- `transactions`: 현재 상태 (빠른 조회)
- `transaction_events`: 모든 이벤트 이력 (불변, 파티셔닝)
- `settlements`: 복식부기 정산 원장

## 문서 참조
- [PRD-01: 아키텍처](docs/PRD-01_architecture.md)
- [PRD-02: 조직 구조](docs/PRD-02_organization.md)
- [PRD-03: 원장/정산](docs/PRD-03_ledger.md)
- [PRD-04: PG 연동](docs/PRD-04_pg_integration.md)
- [PRD-05: DB 스키마](docs/PRD-05_database_schema.md)
- [PRD-06: KORPAY](docs/PRD-06_korpay.md)
- [PRD-07: UI 화면 설계](docs/PRD-07_ui_screens.md)

## 코딩 규칙

### Java/Spring Boot
- Virtual Threads 활용 (Java 25)
- ScopedValue로 테넌트 컨텍스트 전파
- 모든 금액 계산은 BigDecimal 사용

### 정산 로직
- Zero-Sum 검증 필수: `validateZeroSum()` 호출
- 부분취소 시 비례 계산 적용
- 단수 차이는 총판(MASTER)에 흡수

### 데이터베이스
- ltree 인덱스는 GiST 타입 사용
- transaction_events는 일별 파티션
- 민감 정보는 AES-256 암호화

### 보안
- API 인증: JWT + Refresh Token
- Webhook 검증: HMAC-SHA256
- 총판 계정: 2FA 필수

### Frontend (Svelte 5 + shadcn-svelte)
- Tailwind 유틸리티 클래스만 사용 (scoped `<style>` 금지)
- shadcn 컴포넌트 import: `$lib/components/ui`
  ```typescript
  import { Button } from '$lib/components/ui/button';
  import { Card, CardHeader, CardTitle, CardContent } from '$lib/components/ui/card';
  import { Badge } from '$lib/components/ui/badge';
  ```
- cn() 함수로 조건부 클래스 병합: `cn("base-class", condition && "conditional-class")`
- CSS 변수 활용: `text-foreground`, `bg-muted`, `border-border`
- 반응형: `grid-cols-1 md:grid-cols-2 lg:grid-cols-4`

## 커스텀 에이전트
프로젝트 개발을 위한 전문 에이전트가 `.claude/agents.md`에 정의되어 있습니다:
- `db-schema-agent`: DB 스키마, Flyway, ltree
- `settlement-engine-agent`: 정산 로직, Zero-Sum
- `pg-integration-agent`: PG 연동, Webhook
- `api-design-agent`: REST API, 권한
- `test-generator-agent`: 테스트 생성
- `code-review-agent`: PRD 준수, 코드 품질

## 개발환경

### DATABASE

- postgres 18
- host: localhost
- port: 5432
- POSTGRES_USER: postgres
- POSTGRES_PASSWORD: postgres

## 구현 현황

### 백엔드 (Spring Boot)
- ✅ 멀티테넌트 아키텍처 (ScopedValue, TenantRoutingDataSource)
- ✅ PostgreSQL ltree 기반 5단계 조직 계층
- ✅ 복식부기 정산 엔진 (Zero-Sum 검증, 부분취소 비례 계산)
- ✅ KORPAY PG 웹훅 처리 (HMAC-SHA256, 중복 방지)
- ✅ REST API 24개 엔드포인트 (조직/가맹점/거래/정산/대시보드/사업자)
- ✅ Flyway 마이그레이션 (public 2개, tenant 13개)
- ✅ JPA 엔티티 11개, Repository 11개
- ✅ CORS 설정
- ✅ JWT 인증 (Spring Security + JWT)
  - JwtTokenProvider (토큰 생성/검증)
  - JwtAuthenticationFilter (Bearer 토큰 처리)
  - CustomUserDetailsService (사용자 조회)
  - AuthService (로그인/리프레시)
  - AuthController (인증 API 3개)
- ✅ **사업자 정보 분리** (BusinessEntity)
  - BusinessEntity 엔티티 (법인/개인/비사업자 구분)
  - BusinessEntityController (CRUD + 검색 API 6개)
  - 영업점(Organization)과 1:N 관계

### 프론트엔드 (Svelte 5)
- ✅ Runes API ($state, $derived, $effect)
- ✅ 대시보드 (매출 지표, 캘린더, 상위 가맹점 랭킹) - API 연동 완료
- ✅ 거래 내역 조회 (필터링, 정렬, 페이지네이션) - API 연동 완료
- ✅ 정산 관리 (목록, 통계, 상태별 필터) - API 연동 완료
- ✅ 에러 핸들링 및 로딩 상태 표시
- ✅ 반응형 디자인
- ✅ JWT 인증 통합
  - Auth Store (localStorage 기반 토큰 관리)
  - API 클라이언트 (토큰 자동 포함, 401 자동 리프레시)
  - 로그인 화면 (Login.svelte)
  - 로그아웃 기능 (Header.svelte)
  - 인증 가드 (App.svelte)
- ✅ **shadcn-svelte UI 시스템** (전면 적용 완료)
  - Tailwind CSS v4 기반 유틸리티 클래스
  - 모든 컴포넌트에서 scoped `<style>` 제거, Tailwind 유틸리티만 사용
  - UI 컴포넌트 (`$lib/components/ui`):
    - Button (default, destructive, outline, secondary, ghost, link 변형)
    - Card, CardHeader, CardTitle, CardContent, CardFooter
    - Badge (default, secondary, destructive, outline, success, warning 변형)
    - Input, Label
    - Table, TableHeader, TableBody, TableRow, TableHead, TableCell
    - Separator
  - CSS 변수 기반 테마 (`app.css`):
    - 라이트 모드: background, foreground, primary, secondary, muted, accent, destructive
    - 사이드바 전용: sidebar-background, sidebar-foreground, sidebar-primary, sidebar-accent, sidebar-border
  - cn() 유틸리티 (clsx + tailwind-merge)
  - bits-ui: Collapsible, DropdownMenu
- ✅ **영업점 관리** (Branch Management)
  - 영업점 목록 조회 (BranchList.svelte)
  - 영업점 등록 위자드 (BranchRegistration.svelte)
  - 영업점 상세/수정 (BranchDetail.svelte)
  - 사업자 정보 검색/선택 기능 연동

### 인프라 (Finch/Docker)
- ✅ PostgreSQL 18 + ltree 확장
- ✅ Spring Boot 멀티스테이지 빌드
- ✅ Svelte Vite 개발 서버
- ✅ compose.yaml 통합 환경
- ✅ 환경변수 관리 (.env)

### 미구현 항목
- ⏳ 통합 테스트 (JUnit + MockMvc)
- ⏳ Seed 데이터 (Flyway)
- ⏳ 로깅 및 모니터링 (Logback, Micrometer)
- ⏳ 2FA (TOTP)
- ⏳ 사용자 관리 UI

## 실행 가이드

### Finch로 전체 스택 실행

```bash
cp .env.example .env
finch compose up -d

# 접속
# Frontend: http://localhost:5173
# Backend API: http://localhost:8080/api
# PostgreSQL: localhost:5432
```

상세 내용은 다음 문서 참조:
- [DOCKER_GUIDE.md](DOCKER_GUIDE.md) - Docker/Finch 실행 가이드
- [API_INTEGRATION_GUIDE.md](API_INTEGRATION_GUIDE.md) - API 연동 가이드
- [AUTH_TESTING_GUIDE.md](AUTH_TESTING_GUIDE.md) - JWT 인증 테스트 가이드
