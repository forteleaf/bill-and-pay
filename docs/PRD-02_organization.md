# PRD-02: 조직/영업점 계층 구조 설계

## 1. 개요

Bill&Pay 시스템의 조직 계층 구조와 권한 체계를 정의합니다.

---

## 2. 계층 구조

### 2.1 전체 구조도

```
┌─────────────────────────────────────────────────────────────────────┐
│  [시스템 레벨]                                                        │
│                                                                      │
│  총판 (MASTER)                                                       │
│  - 플랫폼 운영자, 테넌트 소유자                                        │
│  - PG사로부터 결제 데이터 수신                                         │
│  - 전체 시스템 관리 권한                                               │
└─────────────────────────────────────────────────────────────────────┘
                                   │
                                   ▼
┌─────────────────────────────────────────────────────────────────────┐
│  [영업 계층 - 5단계]                                                  │
│                                                                      │
│  Level 1: 대리점 (DISTRIBUTOR)                                       │
│           - 영업 최상위 계층                                          │
│           - 실시간 결제 알림 수신                                      │
│           - 하위 모든 조직/가맹점 관리                                  │
│               │                                                      │
│  Level 2:     └─ 에이전시 (AGENCY)                                   │
│                      │                                               │
│  Level 3:            └─ 딜러 (DEALER)                                │
│                             │                                        │
│  Level 4:                   └─ 셀러 (SELLER)                         │
│                                    │                                 │
│  Level 5:                          └─ 벤더 (VENDOR)                  │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
                                   │
                                   ▼
┌─────────────────────────────────────────────────────────────────────┐
│  [거래 레벨]                                                          │
│                                                                      │
│  가맹점 (MERCHANT)                                                   │
│  - 실제 거래 발생 지점                                                │
│  - 영업 계층 어느 단계에든 소속 가능                                    │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

### 2.2 계층별 역할 정의

| 계층 | 코드 | Depth | 역할 |
|------|------|-------|------|
| 총판 | MASTER | 0 | 플랫폼 운영, PG 연동, 전체 관리 |
| 대리점 | DISTRIBUTOR | 1 | 영업 총괄, 알림 수신, 하위 조직 관리 |
| 에이전시 | AGENCY | 2 | 중간 영업 조직 |
| 딜러 | DEALER | 3 | 중간 영업 조직 |
| 셀러 | SELLER | 4 | 말단 영업 조직 |
| 벤더 | VENDOR | 5 | 최하위 영업 조직 |

### 2.3 가맹점 소속 규칙

가맹점은 **영업 계층 어느 단계에든 직접 소속**될 수 있습니다:

```
대리점A (DISTRIBUTOR)
├─ [가맹점1] ─────────────── 대리점 직속 가맹점
├─ 에이전시B (AGENCY)
│   ├─ [가맹점2] ─────────── 에이전시 직속 가맹점
│   └─ 딜러C (DEALER)
│       ├─ [가맹점3] ─────── 딜러 직속 가맹점
│       └─ 셀러D (SELLER)
│           ├─ [가맹점4] ─── 셀러 직속 가맹점
│           └─ 벤더E (VENDOR)
│               └─ [가맹점5] 벤더 직속 가맹점
```

---

## 3. ltree 기반 계층 관리

### 3.1 경로(Path) 체계

PostgreSQL ltree를 사용하여 조직 계층을 관리합니다:

```
조직 경로 예시:
─────────────────────────────────────────────────────────
대리점A                    → 'dist_001'
  └─ 에이전시B             → 'dist_001.agcy_001'
       └─ 딜러C            → 'dist_001.agcy_001.deal_001'
            └─ 셀러D       → 'dist_001.agcy_001.deal_001.sell_001'
                 └─ 벤더E  → 'dist_001.agcy_001.deal_001.sell_001.vend_001'
```

### 3.2 경로 명명 규칙

| 계층 | 접두사 | 예시 |
|------|--------|------|
| 대리점 | dist_ | dist_001, dist_002 |
| 에이전시 | agcy_ | agcy_001, agcy_002 |
| 딜러 | deal_ | deal_001, deal_002 |
| 셀러 | sell_ | sell_001, sell_002 |
| 벤더 | vend_ | vend_001, vend_002 |

### 3.3 주요 ltree 쿼리

```sql
-- 특정 조직의 모든 하위 조직 조회
SELECT * FROM organizations
WHERE path <@ 'dist_001.agcy_001';

-- 특정 조직의 직계 상위 조직들 조회
SELECT * FROM organizations
WHERE path @> 'dist_001.agcy_001.deal_001';

-- 특정 조직의 하위 가맹점 매출 합계
SELECT SUM(t.amount)
FROM transactions t
JOIN merchants m ON t.merchant_id = m.id
WHERE m.org_path <@ 'dist_001.agcy_001';

-- 같은 레벨의 형제 조직 조회 (본인 제외)
SELECT * FROM organizations
WHERE nlevel(path) = nlevel('dist_001.agcy_001')
  AND subpath(path, 0, -1) = subpath('dist_001.agcy_001', 0, -1)
  AND path != 'dist_001.agcy_001';
```

---

## 4. 권한 및 가시성

### 4.1 데이터 접근 권한 매트릭스

| 조회 주체 | 총판 | 대리점 | 에이전시 | 딜러 | 셀러 | 벤더 | 가맹점 |
|-----------|:----:|:------:|:--------:|:----:|:----:|:----:|:------:|
| **총판** | - | 전체 | 전체 | 전체 | 전체 | 전체 | 전체 |
| **대리점** | ❌ | 본인 | 하위 | 하위 | 하위 | 하위 | 하위 전체 |
| **에이전시** | ❌ | ❌ | 본인 | 하위 | 하위 | 하위 | 하위만 |
| **딜러** | ❌ | ❌ | ❌ | 본인 | 하위 | 하위 | 하위만 |
| **셀러** | ❌ | ❌ | ❌ | ❌ | 본인 | 하위 | 하위만 |
| **벤더** | ❌ | ❌ | ❌ | ❌ | ❌ | 본인 | 하위만 |

### 4.2 핵심 규칙

1. **상위 → 하위**: 모든 하위 데이터 조회 가능
2. **하위 → 상위**: 상위 조직 정보 조회 **불가**
3. **형제 간**: 형제 조직 정보 조회 **불가**
4. **가맹점**: 소속된 조직 계보(Ancestors)에서만 조회 가능

### 4.3 권한 검증 로직

```java
public class OrganizationAccessValidator {

    /**
     * 현재 사용자가 대상 조직에 접근 가능한지 검증
     */
    public boolean canAccess(Organization currentOrg, Organization targetOrg) {
        // 총판은 모든 접근 허용
        if (currentOrg.isMaster()) {
            return true;
        }

        // 대상이 현재 조직의 하위인지 확인 (ltree 포함 관계)
        return targetOrg.getPath().startsWith(currentOrg.getPath());
    }

    /**
     * 현재 사용자가 대상 가맹점에 접근 가능한지 검증
     */
    public boolean canAccessMerchant(Organization currentOrg, Merchant merchant) {
        if (currentOrg.isMaster()) {
            return true;
        }

        // 가맹점 소속 조직이 현재 조직의 하위인지 확인
        return merchant.getOrgPath().startsWith(currentOrg.getPath());
    }
}
```

---

## 5. 계정 관리

### 5.1 계정 생성 권한

| 생성 주체 | 생성 가능 대상 |
|-----------|---------------|
| 총판 | 대리점, 에이전시, 딜러, 셀러, 벤더 (전체) |
| 대리점 | 에이전시, 딜러, 셀러, 벤더 (하위만) |
| 에이전시 | 딜러, 셀러, 벤더 (하위만) |
| 딜러 | 셀러, 벤더 (하위만) |
| 셀러 | 벤더 (하위만) |
| 벤더 | 생성 불가 |

### 5.2 계정 생성 플로우

```
[1] 상위 조직에서 계정 생성 요청
    └─ 이메일 주소 입력
    └─ 조직 정보 입력
    └─ 수수료율 설정
           │
           ▼
[2] 시스템에서 인증 메일 발송
    └─ 이메일 인증 링크 포함
    └─ 유효 기간: 24시간
           │
           ▼
[3] 사용자 이메일 인증 완료
    └─ 비밀번호 설정
    └─ 추가 정보 입력
           │
           ▼
[4] 계정 활성화
    └─ 상태: PENDING → ACTIVE
    └─ 로그인 가능
```

### 5.3 사용자 역할

| 역할 | 코드 | 설명 |
|------|------|------|
| 마스터 관리자 | MASTER_ADMIN | 총판 최고 관리자 |
| 조직 관리자 | ORG_ADMIN | 조직 내 관리 권한 |
| 일반 사용자 | USER | 조회 권한만 |

---

## 6. 수수료 구조

### 6.1 마진 배분 원리

수수료는 **가맹점 수수료율에서 상위 조직 수수료율을 차감**한 마진으로 배분됩니다:

```
거래 금액: 1,000,000원
가맹점 수수료율: 3.0% (30,000원)

┌────────────────────────────────────────────────────────────┐
│ 계층        │ 설정 수수료율 │ 마진율      │ 마진 금액     │
├────────────────────────────────────────────────────────────┤
│ 가맹점      │ 3.0%         │ (기준점)    │ -            │
│ 벤더        │ 2.8%         │ 0.2%        │ 2,000원      │
│ 셀러        │ 2.5%         │ 0.3%        │ 3,000원      │
│ 딜러        │ 2.0%         │ 0.5%        │ 5,000원      │
│ 에이전시    │ 1.5%         │ 0.5%        │ 5,000원      │
│ 대리점      │ 1.0%         │ 0.5%        │ 5,000원      │
│ 총판        │ 0.0%         │ 1.0%        │ 10,000원     │
├────────────────────────────────────────────────────────────┤
│ 합계                       │ 3.0%        │ 30,000원     │
└────────────────────────────────────────────────────────────┘
```

### 6.2 수수료 정책 JSONB 구조

```json
{
  "version": "2026-01-28",
  "applied_at": "2026-01-28T00:00:00+09:00",
  "rates": {
    "credit_card": {
      "micro": 1.5,
      "small1": 2.0,
      "small2": 2.3,
      "small3": 2.5,
      "normal": 3.0
    },
    "debit_card": {
      "micro": 1.0,
      "small1": 1.5,
      "small2": 1.8,
      "small3": 2.0,
      "normal": 2.5
    },
    "bank_transfer": {
      "type": "fixed",
      "amount": 300
    },
    "virtual_account": {
      "type": "fixed",
      "amount": 200
    }
  }
}
```

### 6.3 수수료 계산 규칙

1. **정률 수수료**: 거래 금액 × 수수료율
2. **정액 수수료**: 건당 고정 금액
3. **소수점 처리**: 원 단위 절사 (floor)
4. **버전 관리**: 변경 시 스냅샷 저장, 과거 정산 내역 조회 시 당시 정책 적용

---

## 7. 조직 상태 관리

### 7.1 조직 상태

| 상태 | 코드 | 설명 |
|------|------|------|
| 활성 | ACTIVE | 정상 운영 중 |
| 정지 | SUSPENDED | 일시 정지 (로그인 불가, 정산 중단) |
| 해지 | TERMINATED | 계약 해지 (데이터 보존) |

### 7.2 상태 변경 영향

```
조직 상태가 SUSPENDED/TERMINATED로 변경되면:
├─ 해당 조직 로그인 차단
├─ 하위 모든 조직 로그인 차단
├─ 신규 거래 정산 중단
└─ 기존 정산 내역은 보존
```

---

## 8. 데이터베이스 스키마

### 8.1 organizations 테이블

```sql
CREATE TABLE organizations (
    id              BIGSERIAL PRIMARY KEY,
    uuid            UUID NOT NULL DEFAULT uuidv7() UNIQUE,

    -- 계층 정보
    path            LTREE NOT NULL,
    depth           SMALLINT NOT NULL,
    org_type        VARCHAR(20) NOT NULL,

    -- 기본 정보
    code            VARCHAR(20) NOT NULL UNIQUE,  -- 조직 코드
    name            VARCHAR(100) NOT NULL,
    business_no     VARCHAR(20),
    representative  VARCHAR(50),

    -- 연락처
    phone           VARCHAR(20),
    email           VARCHAR(255),
    address         TEXT,

    -- 상위 조직
    parent_id       BIGINT REFERENCES organizations(id),
    root_id         BIGINT REFERENCES organizations(id),  -- 대리점 ID

    -- 수수료 정책
    fee_policy      JSONB NOT NULL DEFAULT '{}',

    -- 상태
    status          VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',

    -- 감사
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by      BIGINT NOT NULL,

    CONSTRAINT chk_org_type CHECK (
        org_type IN ('DISTRIBUTOR', 'AGENCY', 'DEALER', 'SELLER', 'VENDOR')
    ),
    CONSTRAINT chk_depth CHECK (depth BETWEEN 1 AND 5),
    CONSTRAINT chk_status CHECK (
        status IN ('ACTIVE', 'SUSPENDED', 'TERMINATED')
    )
);

-- 인덱스
CREATE INDEX idx_org_path_gist ON organizations USING GIST (path);
CREATE INDEX idx_org_parent ON organizations (parent_id);
CREATE INDEX idx_org_root ON organizations (root_id);
CREATE INDEX idx_org_type ON organizations (org_type);
CREATE INDEX idx_org_status ON organizations (status);
```

### 8.2 users 테이블

```sql
CREATE TABLE users (
    id              BIGSERIAL PRIMARY KEY,
    uuid            UUID NOT NULL DEFAULT uuidv7() UNIQUE,

    -- 인증 정보
    email           VARCHAR(255) NOT NULL UNIQUE,
    password_hash   VARCHAR(255),

    -- 소속
    org_id          BIGINT REFERENCES organizations(id),
    role            VARCHAR(20) NOT NULL,

    -- 프로필
    name            VARCHAR(50) NOT NULL,
    phone           VARCHAR(20),

    -- 상태
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    email_verified  BOOLEAN NOT NULL DEFAULT FALSE,
    mfa_enabled     BOOLEAN NOT NULL DEFAULT FALSE,

    -- 감사
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    last_login_at   TIMESTAMPTZ,

    CONSTRAINT chk_role CHECK (role IN ('MASTER_ADMIN', 'ORG_ADMIN', 'USER')),
    CONSTRAINT chk_user_status CHECK (
        status IN ('PENDING', 'ACTIVE', 'SUSPENDED', 'TERMINATED')
    )
);

CREATE INDEX idx_user_org ON users (org_id);
CREATE INDEX idx_user_email ON users (email);
CREATE INDEX idx_user_status ON users (status);
```

---

## 변경 이력

| 버전 | 날짜 | 변경 내용 |
|------|------|----------|
| v1.0 | 2026-01-28 | 초안 작성 |
