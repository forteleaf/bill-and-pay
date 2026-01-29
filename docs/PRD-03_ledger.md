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
    id              BIGSERIAL PRIMARY KEY,
    uuid            UUID NOT NULL DEFAULT uuidv7() UNIQUE,

    -- PG 거래 정보
    pg_code         VARCHAR(20) NOT NULL,
    pg_tid          VARCHAR(100) NOT NULL,        -- 최초 승인 TID
    pg_merchant_no  VARCHAR(50) NOT NULL,

    -- Bill&Pay 매핑
    merchant_id     BIGINT NOT NULL,
    merchant_path   LTREE NOT NULL,

    -- 거래 정보
    order_id        VARCHAR(40),
    original_amount BIGINT NOT NULL,              -- 원거래 금액 (불변)
    current_amount  BIGINT NOT NULL,              -- 현재 유효 금액 (취소 반영)
    payment_method  VARCHAR(20) NOT NULL,

    -- 카드 정보
    card_code       VARCHAR(10),
    card_type       VARCHAR(10),
    card_no_masked  VARCHAR(20),
    approval_no     VARCHAR(20),
    installment     SMALLINT DEFAULT 0,

    -- 상태 (최종 상태)
    status          VARCHAR(20) NOT NULL,
    event_count     SMALLINT NOT NULL DEFAULT 1,  -- 이벤트 개수

    -- 시간
    transacted_at   TIMESTAMPTZ NOT NULL,         -- 최초 승인 시간
    last_event_at   TIMESTAMPTZ NOT NULL,         -- 마지막 이벤트 시간
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_pg_tid UNIQUE (pg_code, pg_tid),
    CONSTRAINT chk_status CHECK (
        status IN ('APPROVED', 'CANCELED', 'PARTIAL_CANCELED')
    ),
    CONSTRAINT chk_current_amount CHECK (current_amount >= 0)
);
```

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
    id              BIGSERIAL,
    uuid            UUID NOT NULL DEFAULT uuidv7(),

    -- 거래 참조
    transaction_id  BIGINT NOT NULL REFERENCES transactions(id),

    -- 이벤트 정보
    event_type      VARCHAR(20) NOT NULL,
    event_seq       SMALLINT NOT NULL,            -- 순서 (1, 2, 3...)

    -- 금액 (부호 포함)
    amount          BIGINT NOT NULL,              -- +승인, -취소

    -- PG 응답 정보
    pg_tid          VARCHAR(100),                 -- 취소 시 별도 TID
    pg_response     JSONB,                        -- PG 원본 응답

    -- 시간
    event_at        TIMESTAMPTZ NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    -- 복합 PK (파티셔닝용)
    PRIMARY KEY (id, created_at),

    CONSTRAINT uq_txn_event_seq UNIQUE (transaction_id, event_seq),
    CONSTRAINT chk_event_type CHECK (
        event_type IN ('APPROVED', 'CANCELED', 'PARTIAL_CANCELED')
    )
) PARTITION BY RANGE (created_at);
```

### 4.3 이벤트 유형

| 이벤트 | 코드 | amount 부호 | 설명 |
|--------|------|------------|------|
| 승인 | APPROVED | + (양수) | 최초 결제 승인 |
| 전액취소 | CANCELED | - (음수) | 전액 취소 (원금 전액) |
| 부분취소 | PARTIAL_CANCELED | - (음수) | 일부 금액 취소 |

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
    id              BIGSERIAL PRIMARY KEY,
    uuid            UUID NOT NULL DEFAULT uuidv7(),

    -- 이벤트 참조 (정산의 소스)
    transaction_event_id BIGINT NOT NULL,
    transaction_event_at TIMESTAMPTZ NOT NULL,

    -- 거래 참조 (조회 편의용)
    transaction_id  BIGINT NOT NULL,

    -- 수취인
    entity_type     VARCHAR(20) NOT NULL,
    entity_id       BIGINT NOT NULL,
    entity_path     LTREE,

    -- 금액
    entry_type      VARCHAR(10) NOT NULL,
    amount          BIGINT NOT NULL,              -- 항상 양수
    fee_rate        DECIMAL(5,4),
    description     VARCHAR(100),

    -- 정산 상태
    settlement_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    settlement_date   DATE,
    settled_at        TIMESTAMPTZ,

    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_entry_type CHECK (entry_type IN ('CREDIT', 'DEBIT')),
    CONSTRAINT chk_entity_type CHECK (
        entity_type IN ('MERCHANT', 'ORGANIZATION', 'MASTER')
    ),
    CONSTRAINT chk_settlement_status CHECK (
        settlement_status IN ('PENDING', 'CONFIRMED', 'PAID', 'HELD')
    )
);
```

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
}
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

## 변경 이력

| 버전 | 날짜 | 변경 내용 |
|------|------|----------|
| v1.0 | 2026-01-28 | 초안 작성 |
| v2.0 | 2026-01-28 | 하이브리드 이벤트 소싱 방식으로 변경 |
