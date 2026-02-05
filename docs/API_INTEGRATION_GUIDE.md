# Bill&Pay API 연동 가이드

## 개요

프론트엔드(Svelte 5)와 백엔드(Spring Boot) 간 REST API 연동이 완료되었습니다.

## 연동된 API 엔드포인트

### 1. 대시보드 API

#### GET /api/v1/dashboard/metrics
**설명**: 대시보드 메트릭 조회

**헤더**:
```
X-Tenant-ID: tenant_001
```

**응답**:
```json
{
  "success": true,
  "data": {
    "todaySales": 12500000,
    "monthSales": 342000000,
    "pendingSettlements": 156,
    "transactionCount": 1523
  }
}
```

#### GET /api/v1/dashboard/top-merchants
**설명**: 상위 5개 가맹점 랭킹 조회

**응답**:
```json
{
  "success": true,
  "data": [
    {
      "merchantId": "uuid",
      "merchantName": "강남 치킨",
      "totalAmount": 15000000,
      "transactionCount": 342
    }
  ]
}
```

### 2. 거래 내역 API

#### GET /api/v1/transactions
**설명**: 거래 목록 조회 (페이지네이션, 정렬, 필터링)

**쿼리 파라미터**:
- `page`: 페이지 번호 (기본값: 0)
- `size`: 페이지 크기 (기본값: 20, 최대: 100)
- `sortBy`: 정렬 필드 (기본값: createdAt)
- `direction`: 정렬 방향 (ASC/DESC, 기본값: DESC)
- `status`: 거래 상태 필터 (APPROVED, CANCELED, PARTIAL_CANCELED)
- `merchantId`: 가맹점 ID 필터
- `startDate`: 시작 날짜 (ISO 8601)
- `endDate`: 종료 날짜 (ISO 8601)

**응답**:
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": "uuid",
        "transactionId": "TXN-001",
        "merchantId": "uuid",
        "amount": 50000,
        "currency": "KRW",
        "status": "APPROVED",
        "tid": "TID1001",
        "catId": "CAT001",
        "approvedAt": "2026-01-30T12:00:00+09:00",
        "createdAt": "2026-01-30T12:00:00+09:00"
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 1523,
    "totalPages": 77,
    "hasNext": true,
    "hasPrevious": false
  }
}
```

#### GET /api/v1/transactions/{id}
**설명**: 거래 상세 조회

#### GET /api/v1/transactions/{id}/events
**설명**: 거래 이벤트 이력 조회

### 3. 정산 관리 API

#### GET /api/v1/settlements
**설명**: 정산 목록 조회 (페이지네이션, 정렬, 필터링)

**쿼리 파라미터**:
- `page`, `size`, `sortBy`, `direction`: (거래 API와 동일)
- `entityType`: 엔티티 타입 필터 (DISTRIBUTOR, AGENCY, DEALER, SELLER, VENDOR)
- `status`: 정산 상태 필터 (PENDING, APPROVED, PAID, FAILED)
- `startDate`, `endDate`: 날짜 범위 필터

**응답**:
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": "uuid",
        "transactionEventId": "uuid",
        "entityType": "DISTRIBUTOR",
        "entityId": "uuid",
        "entryType": "CREDIT",
        "amount": 50000,
        "feeAmount": 500,
        "netAmount": 49500,
        "currency": "KRW",
        "status": "PENDING",
        "createdAt": "2026-01-30T12:00:00+09:00"
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 156,
    "totalPages": 8,
    "hasNext": true,
    "hasPrevious": false
  }
}
```

#### GET /api/v1/settlements/summary
**설명**: 정산 통계 요약

**응답**:
```json
{
  "success": true,
  "data": {
    "pendingCount": 156,
    "approvedCount": 423,
    "paidCount": 1254
  }
}
```

#### GET /api/v1/settlements/batch/{date}
**설명**: 일별 배치 정산 리포트

## 프론트엔드 구현

### API 클라이언트 사용

```typescript
import { apiClient } from '../lib/api';

apiClient.setTenantId('tenant_001');

const response = await apiClient.get<PagedResponse<Transaction>>('/transactions?page=0&size=20');

if (response.success && response.data) {
  const transactions = response.data.content;
  console.log(transactions);
}
```

### 화면별 연동 현황

| 화면 | 파일 | API 호출 | 상태 |
|------|------|---------|------|
| 대시보드 | `Dashboard.svelte` | `/dashboard/metrics`, `/dashboard/top-merchants` | ✅ |
| 거래 내역 | `Transactions.svelte` | `/transactions` | ✅ |
| 정산 관리 | `Settlements.svelte` | `/settlements`, `/settlements/summary` | ✅ |

### 에러 핸들링

모든 화면에서 다음을 구현:
- 로딩 상태 표시 (`loading` 변수)
- 에러 메시지 표시 (`error` 변수)
- API 실패 시 사용자 친화적 메시지

```svelte
{#if loading}
  <div class="loading">데이터를 불러오는 중...</div>
{:else if error}
  <div class="error">{error}</div>
{:else}
  <!-- 데이터 표시 -->
{/if}
```

## 로컬 테스트 방법

### 1. Docker로 전체 스택 실행

```bash
# 프로젝트 루트에서
cp .env.example .env
docker compose up -d

# 로그 확인
docker compose logs -f backend
docker compose logs -f frontend
```

### 2. 서비스 접속

- **프론트엔드**: http://localhost:5173
- **백엔드 API**: http://localhost:8080/api
- **데이터베이스**: localhost:5432

### 3. 테넌트 설정

프론트엔드에서 테넌트 ID 설정:
```typescript
// src/lib/stores.ts에서 기본값 변경
export const tenantStore = {
  current: 'tenant_001'
};
```

### 4. 데이터 확인

```bash
# PostgreSQL 접속
docker compose exec postgres psql -U postgres -d billpay

# 테넌트 스키마 확인
\dn

# 데이터 조회
SET search_path TO tenant_001;
SELECT * FROM transactions LIMIT 10;
SELECT * FROM settlements LIMIT 10;
```

## API 테스트 (curl)

### 대시보드 메트릭

```bash
curl -X GET http://localhost:8080/api/v1/dashboard/metrics \
  -H "X-Tenant-ID: tenant_001"
```

### 거래 목록

```bash
curl -X GET "http://localhost:8080/api/v1/transactions?page=0&size=10&sortBy=createdAt&direction=DESC" \
  -H "X-Tenant-ID: tenant_001"
```

### 정산 목록

```bash
curl -X GET "http://localhost:8080/api/v1/settlements?page=0&size=10&status=PENDING" \
  -H "X-Tenant-ID: tenant_001"
```

## CORS 설정

백엔드에서 다음 오리진 허용:
- `http://localhost:5173` (Vite 개발 서버)
- `http://localhost:3000` (예비)

허용 메서드: GET, POST, PUT, DELETE, PATCH, OPTIONS

## 알려진 제한사항

1. **인증 미구현**: 현재 JWT 인증이 없음 (X-Tenant-ID 헤더만 검증)
2. **Mock 사용자**: UserContextHolder에서 임시 사용자 반환 (실제 인증 필요)
3. **데이터 부족**: 초기 데이터가 없어 빈 목록 표시 가능 (Seed 데이터 필요)

## 다음 단계

1. **JWT 인증 추가**
   - Spring Security 설정
   - 로그인 API (`/api/v1/auth/login`)
   - JWT 토큰 발급 및 검증
   - 프론트엔드 로그인 화면

2. **Seed 데이터 추가**
   - 샘플 조직, 가맹점, 거래 데이터
   - Flyway 마이그레이션 또는 SQL 스크립트

3. **통합 테스트 작성**
   - API 엔드포인트 테스트
   - 정산 엔진 테스트
   - 웹훅 처리 테스트

4. **프로덕션 준비**
   - 로깅 (Logback, ELK)
   - 모니터링 (Actuator, Prometheus)
   - 환경 분리 (dev, staging, prod)
