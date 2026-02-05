# 작업 규칙

- 작업이 끝나고 난 뒤에 commit

# Bill&Pay 정산 플랫폼

## 프로젝트 개요
다단계 영업 구조에서 발생하는 복잡한 수수료 체계를 자동화하고, 외부 PG사로부터 수신한 대량 결제 데이터를 원장(Ledger) 기반으로 정확하게 정산하는 플랫폼.

## 기술 스택
| 영역 | 기술 | 버전 |
|------|------|------|
| Database | PostgreSQL | 18 |
| Backend | Spring Boot | 3.5.10 |
| Frontend | Svelte + svelte-shadcn | 5 |
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

### 5. 저장양식

- 전화번호, 사업자번호, 주민등록번호, 계좌번호 숫자만 저장한다.

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

### 날짜 범위 검색 패턴 (CRITICAL)
날짜 범위 검색 UI는 반드시 아래 패턴을 따른다:

#### ✅ 올바른 패턴: DateRangePicker + 빠른 선택 버튼
```svelte
<script lang="ts">
  import { DateRangePicker } from '$lib/components/ui/date-range-picker';
  import { format } from 'date-fns';

  let startDate = $state('');
  let endDate = $state('');

  function setDateRange(days: number) {
    const end = new Date();
    const start = new Date();
    start.setDate(end.getDate() - days);
    startDate = format(start, 'yyyy/MM/dd');
    endDate = format(end, 'yyyy/MM/dd');
  }
</script>

<div class="flex flex-row items-end gap-3">
  <div class="flex flex-col gap-1.5">
    <Label>기간</Label>
    <DateRangePicker
      startDate={startDate}
      endDate={endDate}
      onchange={(start, end) => { startDate = start; endDate = end; }}
      placeholder="기간 선택"
      class="w-[280px]"
    />
  </div>
  <div class="flex gap-1">
    <Button variant="outline" size="sm" onclick={() => setDateRange(7)}>7일</Button>
    <Button variant="outline" size="sm" onclick={() => setDateRange(30)}>30일</Button>
    <Button variant="outline" size="sm" onclick={() => setDateRange(90)}>90일</Button>
  </div>
</div>
```

#### ❌ 잘못된 패턴: 네이티브 Input type="date"
```svelte
<!-- 사용 금지! UX 불량, 모바일 호환성 문제 -->
<Input type="date" bind:value={startDate} />
<Input type="date" bind:value={endDate} />
```

**핵심 요소**:
- `DateRangePicker` 컴포넌트 사용 (bits-ui RangeCalendar 기반)
- 빠른 선택 버튼 (7일, 30일, 90일) 필수 제공
- 날짜 포맷: `yyyy/MM/dd` (API 전송 시 ISO 8601로 변환)

## ⚠️ 자주 하는 실수 (Common Mistakes)

### 1. $effect 무한 루프/무한 로딩 (CRITICAL)

#### ❌ 잘못된 패턴: $effect 내 API 호출 (가드 없음)
```svelte
<!-- 무한 로딩 발생! entityId 변경 → API 호출 → 상태 변경 → $effect 재실행 -->
$effect(() => {
  loadContacts();  // 매 렌더링마다 API 호출
});
```

#### ✅ 올바른 패턴: 가드 조건 추가
```svelte
$effect(() => {
  if (entityId && !loading) {
    loadContacts();
  }
});
```

#### ❌ 잘못된 패턴: onMount + $effect 중복
```svelte
onMount(() => {
  if (entityId) loadContacts();  // 첫 번째 호출
});

$effect(() => {
  if (entityId) loadContacts();  // 두 번째 호출 (중복!)
});
```

#### ✅ 올바른 패턴: 하나만 사용
```svelte
// 초기 로딩만 필요하면 onMount
onMount(() => {
  if (entityId) loadContacts();
});

// 의존성 변경 감지가 필요하면 $effect만 사용 (onMount 제거)
let prevEntityId = $state<string | null>(null);
$effect(() => {
  if (entityId && entityId !== prevEntityId) {
    prevEntityId = entityId;
    loadContacts();
  }
});
```

#### ❌ 잘못된 패턴: setInterval 메모리 누수
```svelte
$effect(() => {
  // 매 렌더링마다 새 interval 생성 → 메모리 누수
  setInterval(() => {
    tabs = tabStore.getTabs();
  }, 100);
});
```

#### ✅ 올바른 패턴: cleanup 함수 반환
```svelte
$effect(() => {
  const interval = setInterval(() => {
    tabs = tabStore.getTabs();
  }, 100);
  return () => clearInterval(interval);  // cleanup 필수!
});
```

### 2. bits-ui v2 바인딩 패턴 (CRITICAL)

#### ❌ 잘못된 패턴: Select value 단방향 바인딩
```svelte
<!-- 상태 동기화 안됨! -->
<Select.Root type="single" value={selectedValue} onValueChange={(v) => selectedValue = v}>
```

#### ✅ 올바른 패턴: bind:value 사용
```svelte
<Select.Root type="single" bind:value={selectedValue}>
```

#### ❌ 잘못된 패턴: Dialog에서 bind:open + onOpenChange 혼용
```svelte
<!-- 상태 충돌 가능! bind가 업데이트 → onOpenChange도 업데이트 시도 -->
<Dialog.Root bind:open={dialogOpen} onOpenChange={(v) => { if (!v) closeDialog(); }}>
```

#### ✅ 올바른 패턴: 둘 중 하나만 사용
```svelte
<!-- 옵션 1: bind:open만 사용 (단순한 경우) -->
<Dialog.Root bind:open={dialogOpen}>

<!-- 옵션 2: onOpenChange만 사용 (부가 로직 필요 시) -->
<Dialog.Root open={dialogOpen} onOpenChange={(v) => {
  dialogOpen = v;
  if (!v) closeDialog();
}}>
```

#### ❌ 잘못된 패턴: Sheet bind:open 함수 구문 (bits-ui v2 호환 불가)
```svelte
<!-- bits-ui v2에서 지원 안 함! -->
<Sheet.Root bind:open={() => sidebar.openMobile, (v) => sidebar.setOpenMobile(v)}>
```

#### ✅ 올바른 패턴: 단순 변수 바인딩 + onOpenChange
```svelte
<Sheet.Root bind:open={sidebar.openMobile} onOpenChange={(v) => sidebar.setOpenMobile(v)}>
```

### 3. API 호출 패턴

#### ❌ 잘못된 패턴: 에러 메시지 무시
```svelte
} catch (err) {
  error = 'Failed to load data.';  // API 에러 상세 정보 없음
}
```

#### ✅ 올바른 패턴: 에러 메시지 추출
```svelte
} catch (err) {
  error = err instanceof Error ? err.message : 'Failed to load data.';
  console.error('API Error:', err);
}
```

#### ❌ 잘못된 패턴: 로딩 상태 없이 API 호출
```svelte
async function loadData() {
  const response = await api.get('/data');
  data = response.data;
}
```

#### ✅ 올바른 패턴: 로딩/에러 상태 관리
```svelte
let loading = $state(false);
let error = $state<string | null>(null);

async function loadData() {
  loading = true;
  error = null;
  try {
    const response = await api.get('/data');
    if (response.success) {
      data = response.data;
    } else {
      error = response.error?.message || '데이터를 불러올 수 없습니다.';
    }
  } catch (err) {
    error = err instanceof Error ? err.message : '데이터를 불러올 수 없습니다.';
  } finally {
    loading = false;
  }
}
```

### 4. IntersectionObserver 무한 스크롤

#### ❌ 잘못된 패턴: 중복 Observer 생성
```svelte
$effect(() => {
  // sentinelEl, loading, hasMore 등 상태 변경마다 새 Observer 생성
  const observer = new IntersectionObserver((entries) => {
    if (entries[0].isIntersecting) loadMore();
  });
  observer.observe(sentinelEl);
});
```

#### ✅ 올바른 패턴: 가드 + cleanup
```svelte
$effect(() => {
  if (!sentinelEl || initialLoading) return;

  const observer = new IntersectionObserver(
    (entries) => {
      if (entries[0].isIntersecting && !loading && hasMore) {
        loadMore();
      }
    },
    { rootMargin: '100px', threshold: 0 }
  );

  observer.observe(sentinelEl);
  return () => observer.disconnect();  // cleanup 필수!
});
```

### 5. 기타 공통 실수

| 실수 | 올바른 방법 |
|------|------------|
| `npm install` 사용 | `bun install` 사용 |
| 하드코딩된 API URL | 환경변수 사용: `import.meta.env.VITE_API_BASE_URL` |
| `Math.random()` 목업 데이터 | 실제 API 연동 또는 테스트 fixture 사용 |
| scoped `<style>` 사용 | Tailwind 유틸리티 클래스만 사용 |
| 스켈레톤 로더 없이 "Loading..." 텍스트 | shadcn Skeleton 컴포넌트 사용 |
| fetch timeout 없음 | AbortController + setTimeout 사용 |

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

- docker 로 설치
- container name: postgres-postgres-1
- postgres 18
- host: localhost
- port: 5432
- POSTGRES_USER: postgres
- POSTGRES_PASSWORD: postgres

## 구현 현황

### 백엔드 (Spring Boot)

#### 핵심 통계
| 항목 | 개수 |
|------|------|
| Controller | 16개 |
| Entity | 20개 |
| Repository | 20개 |
| Service | 33개 |
| DTO | 52개 |
| Enum | 22개 |
| Flyway 마이그레이션 | 6개 (public 3, tenant 3) |

#### 아키텍처
- ✅ 멀티테넌트 아키텍처 (ScopedValue, TenantRoutingDataSource)
- ✅ PostgreSQL ltree 기반 5단계 조직 계층
- ✅ 복식부기 정산 엔진 (Zero-Sum 검증, 부분취소 비례 계산)
- ✅ CORS 설정
- ✅ JWT 인증 (Spring Security + JWT)
  - JwtTokenProvider, JwtAuthenticationFilter, CustomUserDetailsService
  - AuthService, AuthController

#### API Controllers (16개)
| Controller | 기능 |
|------------|------|
| AuthController | 로그인, 토큰 리프레시, 로그아웃 |
| BusinessEntityController | 사업자 정보 CRUD + 검색 |
| ContactController | 담당자 관리 CRUD |
| DashboardController | 대시보드 매출 지표 |
| DemoRequestController | 데모 요청 관리 |
| MerchantController | 가맹점 CRUD + 이동/거래/정산 조회 |
| MerchantPgMappingController | 가맹점-PG 매핑 관리 |
| OrganizationController | 영업점 계층 관리 |
| PgConnectionController | PG 연결 설정 관리 |
| PreferentialBusinessController | 우대 사업자 조회 |
| SettlementAccountController | 정산 계좌 관리 |
| SettlementController | 정산 내역 조회 |
| TerminalController | 단말기 CRUD |
| TransactionController | 거래 내역 조회 |
| UserController | 사용자 CRUD + 비밀번호 변경 |
| WebhookController | KORPAY 웹훅 처리 |

#### Entities (20개)
AuthUser, BusinessEntity, CardCompany, Contact, DemoRequest, FeeConfiguration, Merchant, MerchantOrgHistory, MerchantPgMapping, Organization, PaymentMethod, PgConnection, Settlement, SettlementAccount, SettlementBatch, Terminal, Transaction, TransactionEvent, User, WebhookLog

#### 정산 엔진
- ✅ FeeConfiguration 엔티티 (fee_configurations 테이블)
- ✅ FeeConfigResolver (수수료율 조회)
- ✅ FeeCalculationService (계층별 마진 계산)
- ✅ SettlementCreationService (이벤트 기반 정산 생성)
- ✅ ZeroSumValidator (검증)
- ✅ PartialCancelCalculator (부분취소 비례 계산)
- ✅ WebhookProcessingService (Settlement 자동 생성 연동)

#### Webhook 처리
- ✅ KORPAY PG 웹훅 처리 (HMAC-SHA256, 중복 방지)
- ✅ KorpayWebhookAdapter, WebhookSignatureVerifier
- ✅ WebhookLoggingService, WebhookRetryService

### 프론트엔드 (Svelte 5)

#### 핵심 통계
| 항목 | 개수 |
|------|------|
| 라우트/페이지 | 28개 |
| 비즈니스 컴포넌트 | 20개 |
| UI 컴포넌트 | 95개 (19개 카테고리) |
| API 클라이언트 | 12개 |
| 타입 정의 | 8개 |

#### UI 시스템 (shadcn-svelte)
- ✅ Tailwind CSS v4 기반 유틸리티 클래스
- ✅ 19개 UI 컴포넌트 카테고리:
  - Alert, Badge, Button, Calendar, Card
  - ContextMenu, DatePicker, DateRangePicker, Input, Label
  - Popover, Select, Separator, Sheet, Sidebar
  - Skeleton, Sonner (toast), Table, Tooltip
- ✅ cn() 유틸리티 (clsx + tailwind-merge)
- ✅ bits-ui 기반 컴포넌트

#### 인증
- ✅ Auth Store (localStorage 기반 토큰 관리)
- ✅ API 클라이언트 (토큰 자동 포함, 401 자동 리프레시)
- ✅ 로그인 화면, 인증 가드

#### 페이지 (28개)
| 카테고리 | 페이지 |
|----------|--------|
| 메인 | Dashboard, Landing, Login |
| 거래/정산 | Transactions, Settlements, SettlementSummary, SettlementBatches |
| 가맹점 | MerchantList, MerchantDetail, MerchantRegistration, MerchantTransactions, MerchantSettlements, MerchantManagement |
| 영업점 | BranchList, BranchDetail, BranchRegistration, BranchOrganization, BranchSettlement |
| 단말기 | TerminalList, TerminalDetail |
| 사용자 | UserList, UserDetail, UserRegistration |
| PG연결 | PgConnectionList, PgConnectionDetail |
| 기타 | Organizations, DemoRequest, PreferentialBusinessInquiry |

#### 비즈니스 컴포넌트 (20개)
ConfirmModal, ContactManager, Header, Layout, MerchantPgMappingManager, NewHeader, NewLayout, NewSidebar, OrgFlowNode, OrgForm, OrgTree, OrgTreeNode, OrganizationPickerDialog, OrganizationSettlementDetailModal, ParentSelector, SettlementAccountManager, SettlementDetailModal, Sidebar, StatusBar, TabBar

#### API 클라이언트 (12개)
api.ts, authStore.ts, branchApi.ts, contactApi.ts, merchantApi.ts, merchantPgMappingApi.ts, pgConnectionApi.ts, settlementAccountApi.ts, settlementApi.ts, terminalApi.ts, transactionApi.ts, userApi.ts

### 인프라 (Docker)
- ✅ PostgreSQL 18 + ltree 확장
- ✅ Spring Boot 멀티스테이지 빌드
- ✅ Svelte Vite 개발 서버
- ✅ compose.yaml (로컬), compose.nixos.yaml (NixOS), compose.prod.yaml (운영)
- ✅ 환경변수 관리 (.env, .env.production)

### 미구현 항목
- ⏳ 통합 테스트 (JUnit + MockMvc)
- ⏳ 로깅 및 모니터링 (Logback, Micrometer)
- ⏳ 2FA (TOTP)

## 실행 가이드

### 개발 환경

```bash
cp .env.example .env
docker compose up -d

# 접속
# Frontend: http://localhost:5173
# Backend API: http://localhost:8080/api
# PostgreSQL: localhost:5432
```

### 운영 환경 (Nginx Reverse Proxy)

```bash
docker compose -f compose.prod.yaml up -d

# 접속 (단일 포트)
# All requests: http://localhost
#   - /api/* → Backend (8080)
#   - /* → Frontend (80)
# PostgreSQL: localhost:5432
```

상세 내용은 다음 문서 참조:
- [DOCKER_GUIDE.md](docs/DOCKER_GUIDE.md) - Docker 실행 가이드
- [API_INTEGRATION_GUIDE.md](docs/API_INTEGRATION_GUIDE.md) - API 연동 가이드
- [AUTH_TESTING_GUIDE.md](docs/AUTH_TESTING_GUIDE.md) - JWT 인증 테스트 가이드
