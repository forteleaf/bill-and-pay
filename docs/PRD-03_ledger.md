# PRD-03: 원장 설계 및 정산 로직

## 1. 개요

Bill&Pay 시스템의 핵심인 원장(Ledger) 설계와 복식부기 기반 정산 로직을 정의합니다.

---

## 2. 원장 설계 원칙

### 2.1 하이브리드 이벤트 소싱 방식

**transactions**(현재 상태)와 **transaction_events**(이벤트 이력)를 분리하여 관리합니다:

```
┌─────────────────────────────────────────────────────────────────────┐
│  transactions (현재 상태)                                            │
│  ─────────────────────────────────────────────────────────────────  │
│  TXN-001 │ 원금: 100,000 │ 현재: 50,000 │ PARTIAL_CANCELED          │
│  └─ 빠른 조회용, 최종 상태만 저장                                     │
└─────────────────────────────────────────────────────────────────────┘
                              │
                              │ 이력 참조
                              ▼
┌─────────────────────────────────────────────────────────────────────┐
│  transaction_events (이벤트 이력)                                    │
│  ─────────────────────────────────────────────────────────────────  │
│  EVT-001 │ TXN-001 │ APPROVED        │ +100,000 │ seq=1             │
│  EVT-002 │ TXN-001 │ PARTIAL_CANCELED │ -30,000  │ seq=2             │
│  EVT-003 │ TXN-001 │ PARTIAL_CANCELED │ -20,000  │ seq=3             │
│  └─ 모든 이벤트 기록, 정산은 이 테이블 기준으로 처리                   │
└─────────────────────────────────────────────────────────────────────┘
                              │
                              │ 이벤트별 정산
                              ▼
┌─────────────────────────────────────────────────────────────────────┐
│  settlements (정산 원장)                                             │
│  ─────────────────────────────────────────────────────────────────  │
│  EVT-001 기준: 가맹점 +97,000 / 벤더 +500 / ... / 총판 +500          │
│  EVT-002 기준: 가맹점 -29,100 / 벤더 -150 / ... / 총판 -150          │
│  EVT-003 기준: 가맹점 -19,400 / 벤더 -100 / ... / 총판 -100          │
│  └─ 각 이벤트별 복식부기 정산                                         │
└─────────────────────────────────────────────────────────────────────┘
```

### 2.2 설계 원칙

| 원칙 | 설명 |
|------|------|
| **현재 상태 분리** | transactions는 최종 상태만 저장하여 조회 성능 최적화 |
| **이벤트 불변성** | transaction_events는 INSERT만 허용 (UPDATE/DELETE 금지) |
| **이벤트 기반 정산** | 모든 정산은 transaction_events 기준으로 생성 |
| **복식부기 준수** | 각 이벤트별 정산 합계 = 이벤트 금액 |

### 2.3 하이브리드 방식의 장점

| 장점 | 설명 |
|------|------|
| **조회 성능** | 현재 상태 조회 시 transactions만 조회 (JOIN 불필요) |
| **이력 추적** | 모든 변경 이력이 이벤트로 보존 |
| **정산 정확성** | 각 이벤트별 독립 정산으로 추적 용이 |
| **감사 대응** | 이벤트 시퀀스로 전체 흐름 재구성 가능 |

---

## 3. 거래 테이블 (transactions)

### 3.1 역할

- **현재 상태 저장**: 빠른 조회를 위한 최종 상태
- **집계 기준**: 매출 통계, 대시보드 등
- **검색 최적화**: 가맹점별, 기간별 거래 조회

### 3.2 테이블 설계

```sql
CREATE TABLE transactions (
  id UUID PRIMARY KEY DEFAULT uuidv7(),
  transaction_id VARCHAR(100) NOT NULL UNIQUE,
  merchant_id UUID NOT NULL REFERENCES merchants(id),
  merchant_pg_mapping_id UUID NOT NULL REFERENCES merchant_pg_mappings(id),
  pg_connection_id BIGINT NOT NULL,
  org_path public.ltree NOT NULL,
  
  -- 결제수단/카드사 참조
  payment_method_id UUID NOT NULL REFERENCES payment_methods(id),
  card_company_id UUID REFERENCES card_companies(id),
  
  -- 금액
  amount BIGINT NOT NULL,
  currency VARCHAR(3) NOT NULL DEFAULT 'KRW',
  
  -- 상태
  status VARCHAR(20) NOT NULL,
  
  -- PG 응답 정보
  pg_transaction_id VARCHAR(200),
  approval_number VARCHAR(50),
  approved_at TIMESTAMPTZ,
  cancelled_at TIMESTAMPTZ,
  cat_id VARCHAR(50),
  
  -- 메타데이터
  metadata JSONB,
  
  -- 감사
  created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  
  CONSTRAINT transactions_status_check CHECK (
    status IN ('PENDING', 'APPROVED', 'CANCELLED', 'PARTIAL_CANCELLED', 'FAILED')
  ),
  CONSTRAINT transactions_amount_check CHECK (amount > 0)
);

-- 인덱스
CREATE INDEX idx_transactions_org_path_gist ON transactions USING GIST(org_path);
CREATE INDEX idx_transactions_merchant_id ON transactions(merchant_id);
CREATE INDEX idx_transactions_pg_connection_id ON transactions(pg_connection_id);
CREATE INDEX idx_transactions_status ON transactions(status);
CREATE INDEX idx_transactions_cat_id ON transactions(cat_id);

-- PG별 거래고유번호 유니크 인덱스
CREATE UNIQUE INDEX idx_transactions_pg_txn_unique
  ON transactions(pg_connection_id, pg_transaction_id)
  WHERE pg_transaction_id IS NOT NULL;
```

**주요 변경사항 (v2.0)**:
- `id`: BIGSERIAL → UUID
- `transaction_id`: 내부 거래번호 (유니크)
- `merchant_pg_mapping_id`, `pg_connection_id` 참조 추가
- `payment_method_id`, `card_company_id` FK 참조로 변경
- `org_path`: 가맹점 경로 복사본 (조회 최적화)
- `pg_transaction_id`: PG 측 거래 ID
- `cat_id`: 단말기 ID
- `metadata`: 추가 정보 JSONB

### 3.3 상태 정의

| 상태 | 코드 | 조건 |
|------|------|------|
| 승인 | APPROVED | current_amount = original_amount |
| 전액취소 | CANCELED | current_amount = 0 |
| 부분취소 | PARTIAL_CANCELED | 0 < current_amount < original_amount |

---

## 4. 이벤트 테이블 (transaction_events)

### 4.1 역할

- **이벤트 저장**: 모든 거래 이벤트(승인/취소/부분취소) 개별 저장
- **정산 기준**: 정산 생성의 소스 테이블
- **이력 추적**: 거래의 전체 라이프사이클 기록

### 4.2 테이블 설계

```sql
CREATE TABLE transaction_events (
  id UUID NOT NULL DEFAULT uuidv7(),
  event_type VARCHAR(20) NOT NULL,
  event_sequence INTEGER NOT NULL,
  
  -- 거래/가맹점 참조
  transaction_id UUID NOT NULL,
  merchant_id UUID NOT NULL,
  merchant_pg_mapping_id UUID NOT NULL,
  pg_connection_id BIGINT NOT NULL,
  org_path public.ltree NOT NULL,
  
  -- 결제수단/카드사 참조
  payment_method_id UUID NOT NULL,
  card_company_id UUID,
  
  -- 금액 (부호 포함)
  amount BIGINT NOT NULL,
  currency VARCHAR(3) NOT NULL DEFAULT 'KRW',
  
  -- 상태 변경
  previous_status VARCHAR(20),
  new_status VARCHAR(20) NOT NULL,
  
  -- PG 응답 정보
  pg_transaction_id VARCHAR(200),
  approval_number VARCHAR(50),
  cat_id VARCHAR(50),
  metadata JSONB,
  
  -- 시간
  occurred_at TIMESTAMPTZ NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  
  -- 복합 PK (파티셔닝용)
  CONSTRAINT transaction_events_pk PRIMARY KEY (id, created_at),
  
  -- 제약조건
  CONSTRAINT transaction_events_event_type_check CHECK (
    event_type IN ('APPROVAL', 'CANCEL', 'PARTIAL_CANCEL', 'REFUND')
  ),
  CONSTRAINT transaction_events_amount_check CHECK (amount != 0),
  CONSTRAINT transaction_events_sequence_positive CHECK (event_sequence > 0),
  CONSTRAINT transaction_events_amount_sign_matches_type CHECK (
    (event_type = 'APPROVAL' AND amount > 0) OR
    (event_type IN ('CANCEL', 'PARTIAL_CANCEL', 'REFUND') AND amount < 0)
  ),
  CONSTRAINT transaction_events_occurred_at_not_future CHECK (occurred_at <= CURRENT_TIMESTAMP)
) PARTITION BY RANGE (created_at);

-- 인덱스
CREATE INDEX idx_transaction_events_org_path_gist ON transaction_events USING GIST(org_path);
CREATE INDEX idx_transaction_events_transaction_id ON transaction_events(transaction_id, event_sequence);
CREATE INDEX idx_transaction_events_merchant_id ON transaction_events(merchant_id, created_at DESC);
CREATE INDEX idx_transaction_events_event_type ON transaction_events(event_type, created_at DESC);
CREATE INDEX idx_transaction_events_occurred_at ON transaction_events(occurred_at DESC);
```

**주요 변경사항 (v2.0)**:
- `id`: BIGSERIAL → UUID
- `event_seq` → `event_sequence`: 컬럼명 변경
- `merchant_id`, `merchant_pg_mapping_id`, `pg_connection_id`, `org_path` 추가 (비정규화)
- `payment_method_id`, `card_company_id` FK 참조 추가
- `previous_status`, `new_status` 상태 변경 추적
- `occurred_at`: 이벤트 실제 발생 시점
- 제약조건 추가: amount 부호 검증, occurred_at 미래 검증

### 4.3 이벤트 유형

| 이벤트 | 코드 | amount 부호 | 설명 |
|--------|------|------------|------|
| 승인 | APPROVAL | + (양수) | 최초 결제 승인 |
| 전액취소 | CANCEL | - (음수) | 전액 취소 (원금 전액) |
| 부분취소 | PARTIAL_CANCEL | - (음수) | 일부 금액 취소 |
| 환불 | REFUND | - (음수) | 환불 처리 |

### 4.4 이벤트 시퀀스 예시

```
거래 TXN-001: 원금 100,000원

┌─────┬──────────────────┬───────────┬─────────────────────────┐
│ seq │ event_type       │ amount    │ 설명                    │
├─────┼──────────────────┼───────────┼─────────────────────────┤
│  1  │ APPROVED         │ +100,000  │ 최초 승인               │
│  2  │ PARTIAL_CANCELED │ -30,000   │ 1차 부분취소            │
│  3  │ PARTIAL_CANCELED │ -20,000   │ 2차 부분취소            │
├─────┼──────────────────┼───────────┼─────────────────────────┤
│     │ 합계             │ +50,000   │ = transactions.current_amount │
└─────┴──────────────────┴───────────┴─────────────────────────┘
```

---

## 5. 정산 원장 (settlements)

### 5.1 역할

- **이벤트별 정산**: 각 transaction_event에 대한 수수료 분배 기록
- **복식부기**: 이벤트 금액 = 정산 합계 (Zero-Sum)
- **정산 추적**: 각 계층별 수익 기록

### 5.2 테이블 설계

```sql
CREATE TABLE settlements (
  id UUID PRIMARY KEY DEFAULT uuidv7(),
  settlement_batch_id UUID,
  
  -- 이벤트/거래 참조
  transaction_event_id UUID NOT NULL,
  transaction_id UUID NOT NULL,
  merchant_id UUID NOT NULL REFERENCES merchants(id),
  org_path public.ltree NOT NULL,
  
  -- 수취인
  entity_id UUID NOT NULL,
  entity_type VARCHAR(20) NOT NULL,
  entity_path public.ltree NOT NULL,
  
  -- 금액 (부호 포함)
  entry_type VARCHAR(10) NOT NULL,
  amount BIGINT NOT NULL,                         -- CREDIT: +, DEBIT: -
  fee_amount BIGINT NOT NULL DEFAULT 0,
  net_amount BIGINT NOT NULL,
  currency VARCHAR(3) NOT NULL DEFAULT 'KRW',
  
  -- 수수료 설정
  fee_rate NUMERIC(10,6),
  fee_config JSONB,
  
  -- 정산 상태
  status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
  settled_at TIMESTAMPTZ,
  metadata JSONB,
  
  created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  
  CONSTRAINT settlements_entity_type_check CHECK (
    entity_type IN ('DISTRIBUTOR', 'AGENCY', 'DEALER', 'SELLER', 'VENDOR')
  ),
  CONSTRAINT settlements_entry_type_check CHECK (entry_type IN ('CREDIT', 'DEBIT')),
  CONSTRAINT settlements_status_check CHECK (
    status IN ('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED', 'CANCELLED')
  ),
  CONSTRAINT settlements_amount_sign_check CHECK (
    (entry_type = 'CREDIT' AND amount > 0) OR (entry_type = 'DEBIT' AND amount < 0)
  ),
  CONSTRAINT settlements_fee_amount_non_negative CHECK (fee_amount >= 0),
  CONSTRAINT settlements_net_amount_calculation CHECK (
    (entry_type = 'CREDIT' AND net_amount = amount - fee_amount) OR
    (entry_type = 'DEBIT' AND net_amount = amount + fee_amount)
  ),
  CONSTRAINT settlements_settled_at_required CHECK (
    (status = 'COMPLETED' AND settled_at IS NOT NULL) OR status <> 'COMPLETED'
  )
);

-- 인덱스
CREATE INDEX idx_settlements_org_path_gist ON settlements USING GIST(org_path);
CREATE INDEX idx_settlements_entity_path_gist ON settlements USING GIST(entity_path);
CREATE INDEX idx_settlements_transaction_id ON settlements(transaction_id);
CREATE INDEX idx_settlements_entity_id ON settlements(entity_id, entity_type);
CREATE INDEX idx_settlements_status ON settlements(status);
CREATE INDEX idx_settlements_batch_id ON settlements(settlement_batch_id) WHERE settlement_batch_id IS NOT NULL;
```

**주요 변경사항 (v2.0)**:
- `id`: BIGSERIAL → UUID
- `settlement_batch_id`: 정산 배치 참조 추가
- `entity_type`: MERCHANT/ORGANIZATION/MASTER → DISTRIBUTOR/AGENCY/DEALER/SELLER/VENDOR
- `amount`: 양수 → 부호 포함 (CREDIT: +, DEBIT: -)
- `fee_amount`, `net_amount` 추가
- `fee_config` JSONB 추가
- `settlement_status` → `status`
- 제약조건 추가: amount 부호 검증, net_amount 계산 검증

### 5.3 Entry Type 규칙

| 이벤트 | 가맹점 정산금 | 계층별 수수료 |
|--------|-------------|-------------|
| APPROVED (+) | CREDIT | CREDIT |
| CANCELED (-) | DEBIT | DEBIT |
| PARTIAL_CANCELED (-) | DEBIT | DEBIT |

---

## 6. 복식부기 (Double-Entry)

### 6.1 승인 이벤트 정산

```
[이벤트] EVT-001: APPROVED, +100,000원

┌─────────────────────────────────────────────────────────────────┐
│  Settlement Ledger                                               │
├─────────────────────────────────────────────────────────────────┤
│  entity_type  │ entity_id │ entry_type │ amount   │ description │
├─────────────────────────────────────────────────────────────────┤
│  MERCHANT     │ 1001      │ CREDIT     │ 97,000   │ 정산금      │
│  ORGANIZATION │ 501 (벤더)│ CREDIT     │ 500      │ 수수료      │
│  ORGANIZATION │ 401 (셀러)│ CREDIT     │ 500      │ 수수료      │
│  ORGANIZATION │ 301 (딜러)│ CREDIT     │ 500      │ 수수료      │
│  ORGANIZATION │ 201 (에이전시)│ CREDIT  │ 500      │ 수수료      │
│  ORGANIZATION │ 101 (대리점)│ CREDIT   │ 500      │ 수수료      │
│  MASTER       │ 1         │ CREDIT     │ 500      │ 수수료      │
├─────────────────────────────────────────────────────────────────┤
│  합계 (CREDIT)                          │ 100,000  │ = 이벤트 금액 │
└─────────────────────────────────────────────────────────────────┘
```

### 6.2 취소 이벤트 정산 (역분개)

```
[이벤트] EVT-002: CANCELED, -100,000원

┌─────────────────────────────────────────────────────────────────┐
│  Settlement Ledger (역분개)                                      │
├─────────────────────────────────────────────────────────────────┤
│  entity_type  │ entity_id │ entry_type │ amount   │ description │
├─────────────────────────────────────────────────────────────────┤
│  MERCHANT     │ 1001      │ DEBIT      │ 97,000   │ 취소-정산금 │
│  ORGANIZATION │ 501       │ DEBIT      │ 500      │ 취소-수수료 │
│  ORGANIZATION │ 401       │ DEBIT      │ 500      │ 취소-수수료 │
│  ORGANIZATION │ 301       │ DEBIT      │ 500      │ 취소-수수료 │
│  ORGANIZATION │ 201       │ DEBIT      │ 500      │ 취소-수수료 │
│  ORGANIZATION │ 101       │ DEBIT      │ 500      │ 취소-수수료 │
│  MASTER       │ 1         │ DEBIT      │ 500      │ 취소-수수료 │
├─────────────────────────────────────────────────────────────────┤
│  합계 (DEBIT)                           │ 100,000  │ = |이벤트 금액| │
└─────────────────────────────────────────────────────────────────┘
```

### 6.3 부분취소 정산

```
[이벤트] EVT-002: PARTIAL_CANCELED, -30,000원

취소 비율: 30,000 / 100,000 = 30%
각 계층별 정산금의 30%를 역분개

┌─────────────────────────────────────────────────────────────────┐
│  entity_type  │ 원정산    │ 취소비율 │ entry_type │ 취소금액   │
├─────────────────────────────────────────────────────────────────┤
│  MERCHANT     │ 97,000    │ 30%      │ DEBIT      │ 29,100     │
│  ORGANIZATION │ 500       │ 30%      │ DEBIT      │ 150        │
│  ...          │ ...       │ 30%      │ DEBIT      │ ...        │
├─────────────────────────────────────────────────────────────────┤
│  합계                                  │ DEBIT      │ 30,000     │
└─────────────────────────────────────────────────────────────────┘
```

---

## 7. 이벤트 처리 흐름

### 7.1 승인 처리

```
[PG Webhook - 승인]
       │
       ▼
┌──────────────────────────────────────────┐
│ 1. transactions INSERT                    │
│    original_amount = 100,000              │
│    current_amount = 100,000               │
│    status = 'APPROVED'                    │
│    event_count = 1                        │
└──────────────────────────────────────────┘
       │
       ▼
┌──────────────────────────────────────────┐
│ 2. transaction_events INSERT              │
│    event_type = 'APPROVED'                │
│    amount = +100,000                      │
│    event_seq = 1                          │
└──────────────────────────────────────────┘
       │
       ▼
┌──────────────────────────────────────────┐
│ 3. settlements INSERT (이벤트 기준)       │
│    가맹점: CREDIT +97,000                 │
│    계층별: CREDIT +수수료                 │
└──────────────────────────────────────────┘
       │
       ▼
┌──────────────────────────────────────────┐
│ 4. 알림 발송 (대리점)                     │
└──────────────────────────────────────────┘
```

### 7.2 취소 처리

```
[PG Webhook - 취소]
       │
       ▼
┌──────────────────────────────────────────┐
│ 1. transactions UPDATE                    │
│    current_amount = 0                     │
│    status = 'CANCELED'                    │
│    event_count = event_count + 1          │
│    last_event_at = NOW()                  │
└──────────────────────────────────────────┘
       │
       ▼
┌──────────────────────────────────────────┐
│ 2. transaction_events INSERT              │
│    event_type = 'CANCELED'                │
│    amount = -100,000                      │
│    event_seq = 2                          │
└──────────────────────────────────────────┘
       │
       ▼
┌──────────────────────────────────────────┐
│ 3. settlements INSERT (역분개)            │
│    가맹점: DEBIT -97,000                  │
│    계층별: DEBIT -수수료                  │
└──────────────────────────────────────────┘
       │
       ▼
┌──────────────────────────────────────────┐
│ 4. 알림 발송 (대리점)                     │
└──────────────────────────────────────────┘
```

### 7.3 부분취소 처리

```
[PG Webhook - 부분취소 30,000원]
       │
       ▼
┌──────────────────────────────────────────┐
│ 1. transactions UPDATE                    │
│    current_amount = current_amount - 30,000│
│    status = 'PARTIAL_CANCELED'            │
│    event_count = event_count + 1          │
└──────────────────────────────────────────┘
       │
       ▼
┌──────────────────────────────────────────┐
│ 2. transaction_events INSERT              │
│    event_type = 'PARTIAL_CANCELED'        │
│    amount = -30,000                       │
│    event_seq = (다음 순번)                 │
└──────────────────────────────────────────┘
       │
       ▼
┌──────────────────────────────────────────┐
│ 3. settlements INSERT (비례 역분개)       │
│    가맹점: DEBIT -29,100 (30%)            │
│    계층별: DEBIT -수수료 (30%)            │
└──────────────────────────────────────────┘
```

---

## 8. 수수료 계산 엔진

### 8.1 이벤트 기반 계산기

```java
@Service
public class FeeCalculator {

    /**
     * 이벤트 기반 정산 생성
     */
    public List<Settlement> calculate(TransactionEvent event, Merchant merchant) {
        List<Settlement> settlements = new ArrayList<>();

        long eventAmount = event.getAmount();  // 부호 포함
        boolean isCredit = eventAmount > 0;
        long absAmount = Math.abs(eventAmount);

        // 1. 가맹점 수수료 계산
        FeePolicy policy = merchant.getFeePolicy();
        BigDecimal feeRate = getFeeRate(policy, event.getTransaction());
        long totalFee = calculateFee(absAmount, feeRate);
        long merchantSettlement = absAmount - totalFee;

        // 2. 가맹점 정산
        settlements.add(Settlement.builder()
            .transactionEventId(event.getId())
            .transactionId(event.getTransactionId())
            .entityType(EntityType.MERCHANT)
            .entityId(merchant.getId())
            .entryType(isCredit ? EntryType.CREDIT : EntryType.DEBIT)
            .amount(merchantSettlement)
            .description(isCredit ? "정산금" : "취소-정산금")
            .settlementDate(calculateSettlementDate(merchant, event))
            .build());

        // 3. 계층별 마진 계산
        List<Organization> ancestors = getAncestors(merchant.getOrgId());
        BigDecimal prevRate = feeRate;

        for (Organization org : ancestors) {
            BigDecimal orgRate = getFeeRate(org.getFeePolicy(), event.getTransaction());
            BigDecimal marginRate = prevRate.subtract(orgRate);
            long marginAmount = calculateFee(absAmount, marginRate);

            if (marginAmount > 0) {
                settlements.add(Settlement.builder()
                    .transactionEventId(event.getId())
                    .transactionId(event.getTransactionId())
                    .entityType(EntityType.ORGANIZATION)
                    .entityId(org.getId())
                    .entityPath(org.getPath())
                    .entryType(isCredit ? EntryType.CREDIT : EntryType.DEBIT)
                    .amount(marginAmount)
                    .feeRate(marginRate)
                    .description(isCredit ? "수수료" : "취소-수수료")
                    .build());
            }

            prevRate = orgRate;
        }

        // 4. 총판 마진
        long orgTotal = settlements.stream().mapToLong(Settlement::getAmount).sum();
        long masterMargin = absAmount - orgTotal;

        if (masterMargin > 0) {
            settlements.add(Settlement.builder()
                .transactionEventId(event.getId())
                .transactionId(event.getTransactionId())
                .entityType(EntityType.MASTER)
                .entityId(1L)
                .entryType(isCredit ? EntryType.CREDIT : EntryType.DEBIT)
                .amount(masterMargin)
                .description(isCredit ? "수수료" : "취소-수수료")
                .build());
        }

        // 5. Zero-Sum 검증
        validateZeroSum(absAmount, settlements);

        return settlements;
    }

    private void validateZeroSum(long eventAmount, List<Settlement> settlements) {
        long total = settlements.stream().mapToLong(Settlement::getAmount).sum();
        if (total != eventAmount) {
            throw new SettlementException(
                "Zero-Sum 검증 실패: 이벤트=" + eventAmount + ", 정산합=" + total
            );
        }
    }
}
```

### 8.2 부분취소 계산기

```java
@Service
public class PartialCancelCalculator {

    /**
     * 부분취소 정산 생성 (비례 계산)
     */
    public List<Settlement> calculatePartialCancel(
            TransactionEvent cancelEvent,
            TransactionEvent originalApprovalEvent) {

        // 원승인 정산 조회
        List<Settlement> originalSettlements = settlementRepository
            .findByTransactionEventId(originalApprovalEvent.getId());

        // 취소 비율 계산
        BigDecimal cancelRatio = BigDecimal.valueOf(Math.abs(cancelEvent.getAmount()))
            .divide(BigDecimal.valueOf(originalApprovalEvent.getAmount()), 10, RoundingMode.HALF_UP);

        List<Settlement> cancelSettlements = new ArrayList<>();

        for (Settlement original : originalSettlements) {
            long cancelAmount = BigDecimal.valueOf(original.getAmount())
                .multiply(cancelRatio)
                .setScale(0, RoundingMode.FLOOR)
                .longValue();

            cancelSettlements.add(Settlement.builder()
                .transactionEventId(cancelEvent.getId())
                .transactionId(cancelEvent.getTransactionId())
                .entityType(original.getEntityType())
                .entityId(original.getEntityId())
                .entityPath(original.getEntityPath())
                .entryType(EntryType.DEBIT)
                .amount(cancelAmount)
                .feeRate(original.getFeeRate())
                .description("취소-" + original.getDescription())
                .build());
        }

        // 단수 차이 조정 (총판에 흡수)
        adjustRoundingDifference(cancelEvent, cancelSettlements);

        return cancelSettlements;
    }

    /**
     * 단수 차이 조정 (총판에 흡수)
     *
     * 절사(FLOOR)로 인해 발생하는 단수 차이를 총판(MASTER) 정산에 흡수합니다.
     * Zero-Sum 원칙: |취소금액| = SUM(정산금액) 을 보장합니다.
     */
    private void adjustRoundingDifference(
            TransactionEvent cancelEvent,
            List<Settlement> cancelSettlements) {

        long cancelAbsAmount = Math.abs(cancelEvent.getAmount());
        long currentTotal = cancelSettlements.stream()
            .mapToLong(Settlement::getAmount)
            .sum();

        long difference = cancelAbsAmount - currentTotal;

        if (difference == 0) {
            return;  // 단수 차이 없음
        }

        // 총판(MASTER) 정산 찾기
        Settlement masterSettlement = cancelSettlements.stream()
            .filter(s -> s.getEntityType() == EntityType.MASTER)
            .findFirst()
            .orElseThrow(() -> new SettlementException("총판 정산이 없습니다."));

        // 단수 차이를 총판에 흡수
        masterSettlement.setAmount(masterSettlement.getAmount() + difference);

        // Zero-Sum 검증
        long finalTotal = cancelSettlements.stream()
            .mapToLong(Settlement::getAmount)
            .sum();

        if (finalTotal != cancelAbsAmount) {
            throw new SettlementException(
                String.format("Zero-Sum 검증 실패: 취소금액=%d, 정산합=%d",
                    cancelAbsAmount, finalTotal)
            );
        }
    }
}
```

### 8.3 단수처리 규칙

#### 8.3.1 단수 발생 원인

부분취소 시 취소비율을 적용하면 소수점 이하가 발생할 수 있습니다:

```
원금: 100,000원, 취소금: 33,333원
취소비율: 33,333 / 100,000 = 0.33333

가맹점 원정산: 97,000원
가맹점 취소금: 97,000 × 0.33333 = 32,332.01원
→ 절사 적용: 32,332원
```

각 entity별로 절사하면 합계가 취소금액에 미달하는 **단수 차이**가 발생합니다.

#### 8.3.2 단수처리 원칙

| 원칙 | 설명 |
|------|------|
| **절사 방식** | 모든 금액 계산은 `RoundingMode.FLOOR` (내림) 적용 |
| **총판 흡수** | 단수 차이는 총판(MASTER)의 정산금에 흡수 |
| **Zero-Sum 보장** | 최종 정산 합계는 반드시 이벤트 금액과 일치 |

#### 8.3.3 단수처리 예시

```
[부분취소] 원금 100,000원 중 33,333원 취소 (33.333%)

┌─────────────┬───────────┬───────────┬───────────┬───────────┐
│ entity      │ 원정산    │ × 비율    │ 절사 후   │ 최종      │
├─────────────┼───────────┼───────────┼───────────┼───────────┤
│ 가맹점      │ 97,000    │ 32,332.01 │ 32,332    │ 32,332    │
│ 벤더        │ 500       │ 166.665   │ 166       │ 166       │
│ 셀러        │ 500       │ 166.665   │ 166       │ 166       │
│ 딜러        │ 500       │ 166.665   │ 166       │ 166       │
│ 에이전시    │ 500       │ 166.665   │ 166       │ 166       │
│ 대리점      │ 500       │ 166.665   │ 166       │ 166       │
│ 총판        │ 500       │ 166.665   │ 166       │ 171 (+5)  │
├─────────────┼───────────┼───────────┼───────────┼───────────┤
│ 합계        │ 100,000   │           │ 33,328    │ 33,333 ✅ │
└─────────────┴───────────┴───────────┴───────────┴───────────┘

단수 차이: 33,333 - 33,328 = 5원 → 총판이 흡수
```

#### 8.3.4 구현 시 주의사항

```java
// ✅ 올바른 방식: BigDecimal 사용
BigDecimal cancelAmount = BigDecimal.valueOf(original.getAmount())
    .multiply(cancelRatio)
    .setScale(0, RoundingMode.FLOOR);  // 명시적 절사

// ❌ 피해야 할 방식: double 사용 (부동소수점 오차)
double cancelAmount = original.getAmount() * ratio;

// ✅ 단수 조정은 마지막에 한 번만
long difference = targetAmount - currentTotal;
masterSettlement.setAmount(masterSettlement.getAmount() + difference);
```

---

## 9. 정산 주기 (D+N)

### 9.1 정산 예정일 계산

정산 예정일은 **이벤트 발생일** 기준으로 계산합니다:

```java
public LocalDate calculateSettlementDate(Merchant merchant, TransactionEvent event) {
    String cycle = merchant.getSettlementCycle();  // "D+1", "D+2" 등
    LocalDate eventDate = event.getEventAt().toLocalDate();

    int daysToAdd = parseCycle(cycle);
    return addBusinessDays(eventDate, daysToAdd);
}
```

### 9.2 정산 상태

| 상태 | 코드 | 설명 |
|------|------|------|
| 대기 | PENDING | 정산 예정일 미도래 |
| 확정 | CONFIRMED | 정산 예정일 도래, 확정됨 |
| 지급완료 | PAID | 실제 송금 완료 |
| 보류 | HELD | 정산 보류 (이슈 발생) |

### 9.3 정산 확정 배치

```sql
-- 매일 자정 실행
UPDATE settlements
SET settlement_status = 'CONFIRMED',
    updated_at = NOW()
WHERE settlement_status = 'PENDING'
  AND settlement_date <= CURRENT_DATE;
```

---

## 10. 무결성 검증

### 10.1 이벤트-거래 정합성 검증

```sql
-- 이벤트 합계 = 거래 현재 금액 검증
SELECT
    t.id AS transaction_id,
    t.current_amount,
    COALESCE(SUM(e.amount), 0) AS event_total,
    t.current_amount - COALESCE(SUM(e.amount), 0) AS difference
FROM transactions t
LEFT JOIN transaction_events e ON t.id = e.transaction_id
WHERE t.created_at >= CURRENT_DATE - INTERVAL '1 day'
GROUP BY t.id, t.current_amount
HAVING t.current_amount != COALESCE(SUM(e.amount), 0);
```

### 10.2 이벤트-정산 Zero-Sum 검증

```sql
-- 각 이벤트의 정산 합계 = 이벤트 금액 검증
SELECT
    e.id AS event_id,
    e.amount AS event_amount,
    COALESCE(SUM(s.amount), 0) AS settlement_total,
    ABS(e.amount) - COALESCE(SUM(s.amount), 0) AS difference
FROM transaction_events e
LEFT JOIN settlements s ON e.id = s.transaction_event_id
WHERE e.created_at >= CURRENT_DATE - INTERVAL '1 day'
GROUP BY e.id, e.amount
HAVING ABS(e.amount) != COALESCE(SUM(s.amount), 0);
```

### 10.3 검증 스케줄러

```java
@Scheduled(cron = "0 0 1 * * *")  // 매일 01:00
public void verifyDataIntegrity() {
    // 1. 이벤트-거래 정합성
    List<Long> txnMismatches = verifyTransactionEventConsistency();

    // 2. 이벤트-정산 Zero-Sum
    List<Long> eventMismatches = verifyEventSettlementZeroSum();

    if (!txnMismatches.isEmpty() || !eventMismatches.isEmpty()) {
        // 알림 발송 + 정산 보류 처리
        notificationService.sendSystemAlert(
            NotificationType.DATA_INTEGRITY_ERROR,
            Map.of(
                "transaction_mismatches", txnMismatches,
                "event_mismatches", eventMismatches
            )
        );
    }
}
```

---

## 11. 조회 API

### 11.1 거래 현재 상태 조회 (빠름)

```sql
-- 가맹점별 당일 거래 현황
SELECT
    status,
    COUNT(*) AS count,
    SUM(original_amount) AS total_original,
    SUM(current_amount) AS total_current
FROM transactions
WHERE merchant_id = :merchantId
  AND transacted_at >= CURRENT_DATE
GROUP BY status;
```

### 11.2 거래 이벤트 이력 조회

```sql
-- 특정 거래의 전체 이력
SELECT
    e.event_seq,
    e.event_type,
    e.amount,
    e.event_at,
    e.pg_tid
FROM transaction_events e
WHERE e.transaction_id = :transactionId
ORDER BY e.event_seq;
```

### 11.3 정산 집계 조회

```sql
-- 조직별 정산 집계 (이벤트 기준)
SELECT
    DATE(e.event_at) AS event_date,
    s.entity_type,
    s.entity_id,
    SUM(CASE WHEN s.entry_type = 'CREDIT' THEN s.amount ELSE 0 END) AS credit_total,
    SUM(CASE WHEN s.entry_type = 'DEBIT' THEN s.amount ELSE 0 END) AS debit_total,
    SUM(CASE WHEN s.entry_type = 'CREDIT' THEN s.amount ELSE -s.amount END) AS net_total
FROM settlements s
JOIN transaction_events e ON s.transaction_event_id = e.id
WHERE s.entity_path <@ :orgPath
  AND e.event_at BETWEEN :startDate AND :endDate
GROUP BY DATE(e.event_at), s.entity_type, s.entity_id;
```

---

## 12. 구현 현황

### 12.1 수수료 조회 (FeeConfigResolver)

수수료율은 `fee_configurations` 테이블에서 조회합니다:

```java
@Service
public class FeeConfigResolver {
    private final FeeConfigurationRepository feeConfigurationRepository;
    private final PaymentMethodRepository paymentMethodRepository;

    public BigDecimal resolveMerchantFeeRate(Merchant merchant, String paymentMethodCode) {
        Organization vendorOrg = merchant.getOrganization();
        PaymentMethod paymentMethod = paymentMethodRepository.findByMethodCode(paymentMethodCode);
        
        List<FeeConfiguration> configs = feeConfigurationRepository.findActiveByEntityAndPaymentMethod(
            vendorOrg.getId(),
            vendorOrg.getOrgType(),
            paymentMethod.getId(),
            FeeConfigStatus.ACTIVE,
            OffsetDateTime.now()
        );
        
        return configs.getFirst().getFeeRate();
    }
}
```

### 12.2 정산 생성 흐름

```
[Webhook 수신]
     │
     ▼
[WebhookProcessingService]
     │ processWebhook()
     ▼
[TransactionService]
     │ createOrUpdateFromWebhook()
     │ createTransactionEvent()
     ▼
[SettlementService]
     │ processTransactionEvent()
     ▼
[SettlementCreationService]
     │ createSettlements()
     ▼
[FeeCalculationService]
     │ calculateFees()
     │  ├─ resolveMerchantFeeRate() → 가맹점 정산금
     │  ├─ resolveOrganizationFeeRate() × N → 계층별 마진
     │  └─ buildMasterSettlement() → DISTRIBUTOR 잔여금
     ▼
[ZeroSumValidator]
     │ validate() → 이벤트 금액 = SUM(정산금)
     ▼
[SettlementRepository.saveAll()]
```

### 12.3 Zero-Sum 검증 예시

```
이벤트: APPROVAL +50,000원

┌─────────────┬──────────────────────────────────────────────┬────────┐
│ Entity Type │ Entity Path                                   │ Amount │
├─────────────┼──────────────────────────────────────────────┼────────┤
│ VENDOR      │ dist_001.agcy_001.deal_001.sell_001.vend_001 │ 48,250 │
│ SELLER      │ dist_001.agcy_001.deal_001.sell_001          │    150 │
│ DEALER      │ dist_001.agcy_001.deal_001                   │    100 │
│ AGENCY      │ dist_001.agcy_001                            │    100 │
│ DISTRIBUTOR │ dist_001 (margin)                            │    150 │
│ DISTRIBUTOR │ dist_001 (master residual)                   │  1,250 │
├─────────────┼──────────────────────────────────────────────┼────────┤
│ 합계        │                                              │ 50,000 │
└─────────────┴──────────────────────────────────────────────┴────────┘

✅ Zero-Sum 검증 통과: 이벤트 금액(50,000) = Settlement 합계(50,000)
```

---

## 변경 이력

| 버전 | 날짜 | 변경 내용 |
|------|------|----------|
| v1.0 | 2026-01-28 | 초안 작성 |
| v2.0 | 2026-01-28 | 하이브리드 이벤트 소싱 방식으로 변경 |
| v3.0 | 2026-02-05 | 실제 마이그레이션 기준 스키마 업데이트 (transactions, transaction_events, settlements) |
| v3.1 | 2026-02-06 | 정산 엔진 구현 완료 (FeeConfigResolver, FeeCalculationService, Zero-Sum 검증) |
