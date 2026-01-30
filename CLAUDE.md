# Bill&Pay 정산 플랫폼

## 프로젝트 개요
다단계 영업 구조에서 발생하는 복잡한 수수료 체계를 자동화하고, 외부 PG사로부터 수신한 대량 결제 데이터를 원장(Ledger) 기반으로 정확하게 정산하는 플랫폼.

## 기술 스택
| 영역 | 기술 | 버전 |
|------|------|------|
| Database | PostgreSQL | 18 |
| Backend | Spring Boot | 3.5.10 |
| Frontend | Svelte | 5 |
| Runtime | Java | 21 LTS |

### 개발 도구

# do not use npm, use bun

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

## 코딩 규칙

### Java/Spring Boot
- Virtual Threads 활용 (Java 21)
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
