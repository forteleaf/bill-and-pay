# Bill&Pay 정산 관리 시스템 PRD v2.0

## 1. 프로젝트 개요

### 1.1 제품 정보

| 항목 | 내용 |
|------|------|
| **제품명** | Bill&Pay (빌앤페이) 정산 플랫폼 |
| **버전** | v2.0 |
| **최종 수정일** | 2026-01-28 |

### 1.2 배경 및 목적

다단계 영업 구조에서 발생하는 복잡한 수수료 체계를 자동화하고, 외부 PG사로부터 수신한 일일 10만 건 이상의 대량 결제 데이터를 원장(Ledger) 기반으로 정확하게 정산하기 위한 플랫폼입니다.

### 1.3 핵심 가치

- **정확성**: 복식부기 기반 원장으로 1원 단위 오차 없는 정산
- **투명성**: 계층별 수수료 배분 내역 실시간 추적
- **확장성**: JSONB 기반 유연한 수수료 정책, 신규 PG사 즉시 연동
- **성능**: 일 10만건 이상 처리, 1초 이내 대시보드 렌더링

---

## 2. 기술 스택

### 2.1 Core Stack

| 영역 | 기술 | 버전 | 선정 이유 |
|------|------|------|----------|
| **Database** | PostgreSQL | 18 | ltree 계층 쿼리, 파티셔닝, JSONB |
| **Backend** | Spring Boot | 3.5.10 | Java 21 Virtual Threads 지원 |
| **Frontend** | Svelte | 5 | Runes 기반 반응형, 경량 번들 |
| **Runtime** | Java | 21 LTS | Virtual Threads, ScopedValue |

### 2.2 주요 라이브러리

| 영역 | 라이브러리 | 용도 |
|------|-----------|------|
| Frontend | TanStack Table v5 | 대용량 데이터 테이블 + 가상 스크롤 |
| Frontend | shadcn-svelte | UI 컴포넌트 시스템 (Tailwind CSS 기반) |
| Frontend | Tailwind CSS v4 | 유틸리티 기반 CSS 프레임워크 |
| Frontend | bits-ui | Headless UI 컴포넌트 (Collapsible, DropdownMenu 등) |
| Backend | HikariCP | 커넥션 풀 관리 |
| Backend | Flyway | 스키마 마이그레이션 |
| Infra | PgBouncer | 커넥션 풀링 (선택) |

### 2.3 Frontend UI 구조

```
frontend/src/lib/
├── components/ui/          # shadcn-svelte UI 컴포넌트
│   ├── button/            # Button 컴포넌트
│   ├── input/             # Input 컴포넌트
│   ├── card/              # Card 컴포넌트 세트
│   ├── badge/             # Badge 컴포넌트
│   ├── label/             # Label 컴포넌트
│   └── table/             # Table 컴포넌트 세트
└── utils.ts               # cn() 유틸리티 (clsx + tailwind-merge)
```

**스타일링 원칙**:
- CSS-in-component 대신 Tailwind 유틸리티 클래스 사용
- `cn()` 함수로 조건부 클래스 병합
- shadcn 색상 변수 (bg-background, text-foreground 등) 활용

### 2.4 아키텍처 개요

```
┌─────────────────────────────────────────────────────────────────────┐
│                        External PG Services                          │
│              (나이스페이, KG이니시스, 토스페이먼츠 등)                   │
└─────────────────────────────────────────────────────────────────────┘
                                   │ Webhook
                                   ▼
┌─────────────────────────────────────────────────────────────────────┐
│                         Bill&Pay Platform                            │
├─────────────────────────────────────────────────────────────────────┤
│  ┌─────────────┐    ┌─────────────┐    ┌─────────────┐             │
│  │   Svelte 5  │◄──►│ Spring Boot │◄──►│ PostgreSQL  │             │
│  │  Frontend   │    │   Backend   │    │     18      │             │
│  └─────────────┘    └─────────────┘    └─────────────┘             │
│        │                  │                   │                     │
│        │                  │                   │                     │
│  ┌─────▼─────┐      ┌─────▼─────┐      ┌─────▼─────┐               │
│  │ Dashboard │      │ Fee Engine│      │  Ledger   │               │
│  │  Runes    │      │ Virtual   │      │ Partition │               │
│  │ TanStack  │      │ Threads   │      │  ltree    │               │
│  └───────────┘      └───────────┘      └───────────┘               │
└─────────────────────────────────────────────────────────────────────┘
```

---

## 3. 멀티테넌시 설계

### 3.1 Schema-per-Tenant 방식

총판(Master)별로 스키마를 분리하여 데이터 격리를 보장합니다.

```
PostgreSQL Instance
├── public (공통 스키마)
│   ├── system_configs      -- 시스템 설정
│   ├── payment_methods     -- 결제 수단 코드
│   └── card_companies      -- 카드사 정보
│
├── tenant_master001 (총판A 스키마)
│   ├── organizations       -- 조직 구조
│   ├── merchants           -- 가맹점
│   ├── transactions        -- 거래 원장
│   └── settlements         -- 정산 원장
│
└── tenant_master002 (총판B 스키마)
    └── ... (동일 구조)
```

### 3.2 테넌트 라우팅

```java
// Spring Boot - ScopedValue 기반 테넌트 컨텍스트
public class TenantContextHolder {
    private static final ScopedValue<String> TENANT_ID = ScopedValue.newInstance();

    public static String getCurrentTenant() {
        return TENANT_ID.get();
    }
}

// AbstractRoutingDataSource로 동적 스키마 전환
// X-Tenant-ID 헤더 기반 런타임 라우팅
```

### 3.3 운영 고려사항

- **마이그레이션**: Flyway로 모든 테넌트 스키마 동시 버전 관리
- **커넥션 관리**: PgBouncer 도입으로 테넌트 급증 시 커넥션 효율화
- **백업**: 테넌트별 독립 백업/복원 가능

---

## 4. 비기능 요구사항

### 4.1 성능

| 항목 | 목표 |
|------|------|
| 일일 거래 처리량 | 10만 건 이상 |
| 대시보드 로딩 | 1초 이내 |
| Webhook 응답 | 500ms 이내 |
| 정산 배치 | 10만 건/10분 이내 |

### 4.2 보안

| 항목 | 요구사항 |
|------|----------|
| 개인정보 암호화 | AES-256 (계좌번호, 주민번호 등) |
| 2FA 인증 | 총판 계정 필수 |
| API 인증 | JWT + Refresh Token |
| Webhook 검증 | HMAC-SHA256 서명 |

### 4.3 확장성

| 항목 | 대응 방안 |
|------|----------|
| 신규 결제 수단 | JSONB 필드로 스키마 변경 없이 대응 |
| 신규 PG사 | pg_connections 테이블에 설정 추가 |
| 수수료 정책 변경 | 버전 관리된 스냅샷 저장 |

---

## 5. 화면 구성

> **Note**: 화면 설계 및 UI 명세는 [PRD-07: UI 화면 설계](PRD-07_ui_screens.md)를 참조하세요.

---

## 6. 문서 구성

PRD 문서는 다음과 같이 분리하여 관리합니다:

| 문서 | 내용 |
|------|------|
| **PRD-01** (본 문서) | 아키텍처 개요, 기술 스택, 비기능 요구사항 |
| **PRD-02** | 조직/영업점 계층 구조 설계 |
| **PRD-03** | 원장 설계 및 정산 로직 |
| **PRD-04** | PG 연동 및 알림 시스템 |
| **PRD-05** | 데이터베이스 스키마 |
| **PRD-06** | KORPAY 명세 |
| **PRD-07** | UI 화면 설계 |

---

## 변경 이력

| 버전 | 날짜 | 변경 내용 |
|------|------|----------|
| v1.0 | 2026-01-15 | 초안 작성 |
| v1.2 | 2026-01-20 | 고도화 전략 추가 |
| v2.0 | 2026-01-28 | 문서 구조 개편, PG 연동 구조 추가 |
