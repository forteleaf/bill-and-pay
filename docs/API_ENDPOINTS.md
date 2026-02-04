# Bill&Pay REST API 엔드포인트

## 인증 헤더

모든 API 요청에는 다음 헤더가 필요합니다:

```
X-Tenant-ID: tenant_001
```

## API 기본 정보

- **Base URL**: `http://localhost:8080/api/v1`
- **응답 형식**: JSON
- **문자 인코딩**: UTF-8
- **타임존**: Asia/Seoul

## 공통 응답 형식

### 성공 응답
```json
{
  "success": true,
  "data": { ... },
  "meta": { ... }
}
```

### 에러 응답
```json
{
  "success": false,
  "error": {
    "code": "ERROR_CODE",
    "message": "에러 메시지"
  }
}
```

## 엔드포인트 목록

### 대시보드 API

| 메서드 | 경로 | 설명 | 연동 상태 |
|--------|------|------|-----------|
| GET | `/dashboard/metrics` | 대시보드 메트릭 조회 | ✅ 연동 완료 |
| GET | `/dashboard/top-merchants` | 상위 가맹점 랭킹 | ✅ 연동 완료 |

### 조직 API

| 메서드 | 경로 | 설명 | 연동 상태 |
|--------|------|------|-----------|
| GET | `/organizations` | 조직 목록 조회 | 🔧 백엔드만 |
| GET | `/organizations/{id}` | 조직 상세 조회 | 🔧 백엔드만 |
| POST | `/organizations` | 조직 생성 | 🔧 백엔드만 |
| PUT | `/organizations/{id}` | 조직 수정 | 🔧 백엔드만 |
| GET | `/organizations/{id}/descendants` | 하위 조직 조회 (ltree) | 🔧 백엔드만 |

### 가맹점 API

| 메서드 | 경로 | 설명 | 연동 상태 |
|--------|------|------|-----------|
| GET | `/merchants` | 가맹점 목록 조회 | 🔧 백엔드만 |
| GET | `/merchants/{id}` | 가맹점 상세 조회 | 🔧 백엔드만 |
| POST | `/merchants` | 가맹점 생성 | 🔧 백엔드만 |
| PUT | `/merchants/{id}` | 가맹점 수정 | 🔧 백엔드만 |
| GET | `/merchants/{id}/statistics` | 가맹점 통계 | 🔧 백엔드만 |

### 사업자 정보 API

| 메서드 | 경로 | 설명 | 연동 상태 |
|--------|------|------|-----------|
| GET | `/business-entities` | 사업자 목록 조회 (페이지네이션) | ✅ 연동 완료 |
| GET | `/business-entities/{id}` | 사업자 상세 조회 | ✅ 연동 완료 |
| GET | `/business-entities/search?businessNumber=xxx` | 사업자번호로 검색 | ✅ 연동 완료 |
| GET | `/business-entities/search/name?name=xxx` | 상호명으로 검색 | ✅ 연동 완료 |
| POST | `/business-entities` | 사업자 등록 | ✅ 연동 완료 |
| PUT | `/business-entities/{id}` | 사업자 수정 | ✅ 연동 완료 |

### 거래 API

| 메서드 | 경로 | 설명 | 연동 상태 |
|--------|------|------|-----------|
| GET | `/transactions` | 거래 목록 조회 | ✅ 연동 완료 |
| GET | `/transactions/{id}` | 거래 상세 조회 | 🔧 백엔드만 |
| GET | `/transactions/{id}/events` | 거래 이벤트 이력 | 🔧 백엔드만 |

### 정산 API

| 메서드 | 경로 | 설명 | 연동 상태 |
|--------|------|------|-----------|
| GET | `/settlements` | 정산 목록 조회 | ✅ 연동 완료 |
| GET | `/settlements/summary` | 정산 통계 요약 | ✅ 연동 완료 |
| GET | `/settlements/batch/{date}` | 일별 배치 리포트 | 🔧 백엔드만 |

### 웹훅 API

| 메서드 | 경로 | 설명 | 연동 상태 |
|--------|------|------|-----------|
| POST | `/webhook/{pgCode}` | PG 웹훅 수신 | 🔧 백엔드만 |

## 상세 명세

### GET /dashboard/metrics

**설명**: 대시보드 주요 지표 조회 (오늘/월간 매출, 정산 대기, 거래 건수)

**요청**:
```bash
curl -X GET http://localhost:8080/api/v1/dashboard/metrics \
  -H "X-Tenant-ID: tenant_001"
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

### GET /dashboard/top-merchants

**설명**: 월간 매출 상위 5개 가맹점 조회

**요청**:
```bash
curl -X GET http://localhost:8080/api/v1/dashboard/top-merchants \
  -H "X-Tenant-ID: tenant_001"
```

**응답**:
```json
{
  "success": true,
  "data": [
    {
      "merchantId": "550e8400-e29b-41d4-a716-446655440000",
      "merchantName": "강남 치킨",
      "totalAmount": 15000000,
      "transactionCount": 342
    }
  ]
}
```

### GET /transactions

**설명**: 거래 목록 조회 (페이지네이션, 정렬, 필터링)

**쿼리 파라미터**:
- `page` (int): 페이지 번호 (기본값: 0)
- `size` (int): 페이지 크기 (기본값: 20, 최대: 100)
- `sortBy` (string): 정렬 필드 (기본값: createdAt)
- `direction` (ASC|DESC): 정렬 방향 (기본값: DESC)
- `merchantId` (UUID): 가맹점 ID 필터 (선택)
- `status` (APPROVED|CANCELED|PARTIAL_CANCELED): 상태 필터 (선택)
- `startDate` (ISO 8601): 시작 날짜 (선택)
- `endDate` (ISO 8601): 종료 날짜 (선택)

**요청**:
```bash
curl -X GET "http://localhost:8080/api/v1/transactions?page=0&size=10&sortBy=createdAt&direction=DESC&status=APPROVED" \
  -H "X-Tenant-ID: tenant_001"
```

**응답**:
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": "550e8400-e29b-41d4-a716-446655440000",
        "transactionId": "TXN-20260130-001",
        "merchantId": "660e8400-e29b-41d4-a716-446655440000",
        "merchantPath": "dist_001.agcy_001.deal_001.sell_001.vend_001",
        "orgPath": "dist_001",
        "paymentMethodId": "770e8400-e29b-41d4-a716-446655440000",
        "cardCompanyId": "880e8400-e29b-41d4-a716-446655440000",
        "amount": 50000,
        "currency": "KRW",
        "status": "APPROVED",
        "pgTransactionId": "PG-TXN-001",
        "approvalNumber": "12345678",
        "approvedAt": "2026-01-30T12:00:00+09:00",
        "catId": "CAT001",
        "tid": "TID1001",
        "metadata": {},
        "createdAt": "2026-01-30T12:00:00+09:00",
        "updatedAt": "2026-01-30T12:00:00+09:00"
      }
    ],
    "page": 0,
    "size": 10,
    "totalElements": 1523,
    "totalPages": 153,
    "hasNext": true,
    "hasPrevious": false
  }
}
```

### GET /settlements

**설명**: 정산 목록 조회 (페이지네이션, 정렬, 필터링)

**쿼리 파라미터**:
- `page`, `size`, `sortBy`, `direction`: (거래 API와 동일)
- `entityType` (DISTRIBUTOR|AGENCY|DEALER|SELLER|VENDOR): 엔티티 타입 필터 (선택)
- `status` (PENDING|APPROVED|PAID|FAILED): 정산 상태 필터 (선택)
- `startDate`, `endDate`: 날짜 범위 필터 (선택)

**요청**:
```bash
curl -X GET "http://localhost:8080/api/v1/settlements?page=0&size=10&status=PENDING" \
  -H "X-Tenant-ID: tenant_001"
```

**응답**:
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": "550e8400-e29b-41d4-a716-446655440000",
        "settlementBatchId": "660e8400-e29b-41d4-a716-446655440000",
        "transactionEventId": "770e8400-e29b-41d4-a716-446655440000",
        "transactionId": "880e8400-e29b-41d4-a716-446655440000",
        "merchantId": "990e8400-e29b-41d4-a716-446655440000",
        "merchantPath": "dist_001.agcy_001.deal_001.sell_001.vend_001",
        "entityId": "aa0e8400-e29b-41d4-a716-446655440000",
        "entityType": "DISTRIBUTOR",
        "entityPath": "dist_001",
        "entryType": "CREDIT",
        "amount": 50000,
        "feeAmount": 500,
        "netAmount": 49500,
        "currency": "KRW",
        "feeRate": 0.01,
        "feeConfig": {},
        "status": "PENDING",
        "settledAt": null,
        "metadata": {},
        "createdAt": "2026-01-30T12:00:00+09:00",
        "updatedAt": "2026-01-30T12:00:00+09:00"
      }
    ],
    "page": 0,
    "size": 10,
    "totalElements": 156,
    "totalPages": 16,
    "hasNext": true,
    "hasPrevious": false
  }
}
```

### GET /settlements/summary

**설명**: 정산 상태별 통계 요약

**요청**:
```bash
curl -X GET http://localhost:8080/api/v1/settlements/summary \
  -H "X-Tenant-ID: tenant_001"
```

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

## 에러 코드

| 코드 | 메시지 | 설명 |
|------|--------|------|
| `TENANT_NOT_FOUND` | 테넌트를 찾을 수 없습니다 | X-Tenant-ID 헤더가 없거나 잘못됨 |
| `INVALID_TENANT` | 유효하지 않은 테넌트입니다 | 테넌트 검증 실패 |
| `ENTITY_NOT_FOUND` | 엔티티를 찾을 수 없습니다 | 요청한 리소스가 존재하지 않음 |
| `ACCESS_DENIED` | 접근 권한이 없습니다 | ltree 계층 권한 위반 |
| `VALIDATION_ERROR` | 유효성 검증 실패 | 입력값 검증 오류 |
| `ZERO_SUM_VIOLATION` | Zero-Sum 검증 실패 | 정산 금액 합계 불일치 |

## 페이지네이션 응답 형식

모든 목록 조회 API는 다음 형식으로 응답합니다:

```json
{
  "success": true,
  "data": {
    "content": [ ... ],
    "page": 0,
    "size": 20,
    "totalElements": 100,
    "totalPages": 5,
    "hasNext": true,
    "hasPrevious": false
  }
}
```

## 연동 상태 범례

- ✅ **연동 완료**: 프론트엔드-백엔드 완전 연동
- 🔧 **백엔드만**: API는 구현되었으나 프론트엔드 미연동
- ❌ **미구현**: API 자체가 구현되지 않음

## 추가 정보

- [API 통합 가이드](API_INTEGRATION_GUIDE.md)
- [Docker 실행 가이드](DOCKER_GUIDE.md)
- [정산 엔진 문서](SETTLEMENT_ENGINE.md)
- [웹훅 구현 문서](WEBHOOK_IMPLEMENTATION.md)
