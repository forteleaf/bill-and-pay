# PRD-06: KORPAY PG 연동 명세

## 1. 개요

### 1.1 KORPAY의 역할

Bill&Pay 플랫폼의 **Primary PG(Payment Gateway)**로서 카드결제 거래를 실시간으로 수신합니다.

- **지원 결제 수단**: 신용카드 결제(CARD)
- **지원 채널**: 단말기(오프라인) / 온라인(수기결제)
- **연동 방식**: Webhook(노티) 기반 실시간 거래 데이터 전달

### 1.2 관련 문서

- [PRD-01: 아키텍처](PRD-01_architecture.md) - 전체 시스템 아키텍처, 멀티테넌시
- [PRD-03: 원장/정산](PRD-03_ledger.md) - 정산 로직, Zero-Sum 검증, 복식부기
- [PRD-04: PG 연동](PRD-04_pg_integration.md) - PG 연동 공통 구조, Webhook 처리 흐름, 필드 매핑
- [PRD-05: DB 스키마](PRD-05_database_schema.md) - 데이터베이스 설계

---

## 2. KORPAY 시스템 개요

### 2.1 KORPAY 아키텍처

```
┌─────────────────────────────────────────────────────────────┐
│                    KORPAY PG System                          │
│                                                              │
│  ┌──────────────────────────────────────────────────────┐  │
│  │           결제 처리 (온/오프라인)                       │  │
│  │                                                       │  │
│  │  ┌──────────┐          ┌──────────┐                 │  │
│  │  │ 단말기   │          │ 온라인   │                 │  │
│  │  │ (POS/CAT)│          │ (웹/앱)  │                 │  │
│  │  └────┬─────┘          └────┬─────┘                 │  │
│  │       └──────────┬───────────┘                       │  │
│  │                  │                                   │  │
│  │                  ▼                                   │  │
│  │            승인/취소 처리                              │  │
│  │                  │                                   │  │
│  └──────────────────┼───────────────────────────────────┘  │
│                     │                                       │
│          Webhook (HTTP POST)                                │
│          Content-Type: application/x-www-form-urlencoded  │
│                     │                                       │
└─────────────────────┼───────────────────────────────────────┘
                      │
                      ▼
           Bill&Pay Webhook 수신
           /api/webhook/{tenantId}/korpay
```

### 2.2 KORPAY와 Bill&Pay 통합 포인트

| 항목 | 설명 |
|------|------|
| **결제 수신** | Webhook을 통한 승인/취소 거래 실시간 수신 |
| **가맹점 식별** | MID(가맹점 ID)로 Bill&Pay 가맹점 매핑 |
| **단말기 연결** | catId(단말기 ID)와 MID의 1:1 매핑 관리 |
| **서명 검증** | HMAC-SHA256을 통한 Webhook 데이터 진본성 확인 |
| **거래 유형 판단** | cancelYN, remainAmt 필드로 승인/취소/부분취소 판단 |

---

## 3. Webhook(노티) 연동

### 3.1 연동 방식

**KORPAY → Bill&Pay** 일방향 통신:

- **방식**: POST 방식 폼 데이터
- **Content-Type**: `application/x-www-form-urlencoded`
- **인증**: HMAC-SHA256 서명 검증
- **연동문의**: KORPAY 영업담당자에게 문의

### 3.2 Webhook 수신 URL

```
POST /api/webhook/{tenantId}/korpay?pgConnectionId=xxx&webhookSecret=yyy

예시:
POST /api/webhook/tenant_001/korpay?pgConnectionId=123&webhookSecret=abc123xyz
```

**URL 파라미터**:

| 파라미터 | 설명 | 필수 |
|---------|------|------|
| `tenantId` | Bill&Pay 테넌트 ID | O |
| `pgCode` | PG 코드 (korpay) | O |
| `pgConnectionId` | PG 연결 ID | O |
| `webhookSecret` | Webhook 서명 검증용 시크릿 | O |

---

## 4. Webhook 데이터 필드 명세

### 4.1 공통 필드 (단말기 + 온라인)

| 필드명 | 타입 | 크기 | 필수 | 설명 |
|--------|------|------|------|------|
| `tid` | String | 30 | O | 거래 고유번호 (ex: ktest5561m01032012021713340481) |
| `otid` | String | 30 | - | 원거래 TID (부분취소시만 필수) |
| `mid` | String | 10 | O | 가맹점 MID (KORPAY 제공) |
| `gid` | String | 10 | - | 가맹점 GID (Bill&Pay 미사용) |
| `vid` | String | 10 | - | 총판 VID (Bill&Pay 미사용) |
| `payMethod` | String | - | O | 결제수단 (CARD: 고정) |
| `appCardCd` | String | - | - | 발급사 코드 |
| `acqCardCd` | String | - | - | 매입사 코드 |
| `fnNm` | String | - | - | 카드사/은행명 |
| `cancelYN` | String | 1 | O | 취소 구분 (Y: 취소, N: 승인) |
| `ediNo` | String | - | O | VAN 거래 고유번호 |
| `appDtm` | String | 14 | O | 승인 일시 (yyyyMMddHHmmss) |
| `ccDnt` | String | 14 | - | 취소 일시 (cancelYN==Y일 때만) (yyyyMMddHHmmss) |
| `amt` | String | - | O | 거래금액 (amt > 0) |
| `buyerId` | String | - | - | 구매자 ID |
| `ordNm` | String | - | - | 구매자명 |
| `ordNo` | String | - | O | 주문번호 |
| `goodsName` | String | - | - | 상품명 |
| `appNo` | String | - | O | 카드사 승인번호 |
| `quota` | String | 2 | - | 할부개월 (00: 일시불, 01-12: 할부) |
| `notiDnt` | String | - | O | 알림 통보 시각 (milliseconds) |
| `cardNo` | String | - | O | 카드번호 (마스킹됨, ex: 12345678****1234) |
| `catId` | String | - | O | 단말기 ID (오프라인) / MID (온라인) |
| `connCd` | String | 4 | O | 거래 채널 (0003: 오프라인, 0005: 수기결제/온라인) |

### 4.2 단말기 전용 필드 (connCd=0003)

| 필드명 | 타입 | 설명 |
|--------|------|------|
| `tPhone` | String | 입력된 전화번호 |
| `remainAmt` | String | 잔액 (승인/전체취소: 0, 부분취소: 잔액금액) |

### 4.3 온라인 전용 필드 (connCd=0005)

| 필드명 | 타입 | 설명 |
|--------|------|------|
| `charSet` | String | 1 고정 |
| `hashStr` | String | 해쉬키 (SHA256(mid + ediDate + amt + mKey)) |
| `ediDate` | String | 알림 통보 일시 (milliseconds) |
| `lmtDay` | String | 예약필드 (미사용) |
| `resultCd` | String | 수기결제 상태값 (3001: 승인, 2001: 취소) |
| `usePointAmt` | String | 카드 포인트 사용액 |
| `vacntNo` | String | 가상계좌번호 |
| `socHpNo` | String | 구매자 휴대폰번호 |
| `cashCrctFlg` | String | 현금영수증 발행여부 (미사용) |

### 4.4 미사용 필드

다음 필드는 KORPAY에서 제공하지만 Bill&Pay에서 사용하지 않음:

| 필드 | 사유 |
|------|------|
| `gid` | KORPAY 내부용 (Group ID) |
| `vid` | KORPAY 내부용 (Vendor ID) |

---

## 5. MID / 단말기(catId) 관계

### 5.1 매핑 구조

KORPAY에서 모든 거래는 **MID**(가맹점 ID)로 식별되며, 단말기(catId)와 1:1 매핑 관계를 가집니다:

```
┌─────────────────────────────────────────────────────────┐
│            KORPAY MID 관리 체계                          │
├─────────────────────────────────────────────────────────┤
│                                                         │
│  MID (온라인승인 계정)                                   │
│  ┌──────────────┐                                      │
│  │ M1234567890  │ ◄─── 1:1 Mapping ───► ┌──────────────┐│
│  └──────────────┘                       │ 1046347583   ││
│                                         │ (단말기 ID)   ││
│                                         └──────────────┘│
│                                                         │
│  특징:                                                  │
│  - MID = 가맹점 식별자 (고정)                            │
│  - 모든 거래는 MID로 가맹점 식별                         │
│  - catId는 단말기 또는 온라인 채널 구분                 │
│                                                         │
└─────────────────────────────────────────────────────────┘
```

### 5.2 Bill&Pay 매핑 저장

**merchant_pg_mappings 테이블**:

```sql
INSERT INTO merchant_pg_mappings (
    merchant_id,
    pg_connection_id,
    pg_merchant_no,        -- KORPAY MID (M1234567890)
    terminal_id,           -- 단말기 catId (1046347583)
    terminal_type,         -- POS, CAT, ONLINE
    status
) VALUES (
    123,
    1,
    'M1234567890',
    '1046347583',
    'ONLINE',
    'ACTIVE'
);
```

**Webhook 수신 시 매핑 조회**:

```
Webhook 수신 → mid 필드로 merchant_pg_mappings 검색
    ↓
pg_merchant_no = mid 인 매핑 찾기
    ↓
merchant_id로 Bill&Pay 가맹점 식별
```

---

## 6. 거래 유형 판단 로직

### 6.1 승인 / 취소 / 부분취소 판단

**KORPAY Webhook 필드를 기반으로 거래 유형 결정**:

```java
public String determineEventType(KorpayWebhookData data) {
    // 1. cancelYN = N → 승인
    if (!"Y".equals(data.getCancelYN())) {
        return "APPROVED";
    }

    // 2. cancelYN = Y 이고 remainAmt > 0 → 부분취소
    if (data.getRemainAmt() != null && data.getRemainAmt() > 0) {
        return "PARTIAL_CANCELED";
    }

    // 3. cancelYN = Y 이고 remainAmt = 0 → 전체취소
    return "CANCELED";
}
```

| cancelYN | remainAmt | 거래유형 | 정산처리 |
|----------|-----------|---------|---------|
| N | 0 | 승인 | CREDIT (전액) |
| Y | 0 | 전체취소 | DEBIT (전액) |
| Y | > 0 | 부분취소 | DEBIT (취소액만) |

### 6.2 원거래 추적 (otid)

**부분/전체 취소 시 원거래 식별**:

- **otid 필드**: 원거래의 tid값
- **사용 사례**:
  1. 취소 거래 수신 시 otid로 원거래 Transaction 검색
  2. 원거래의 settlement 레코드를 역분개 처리
  3. Zero-Sum 검증 적용

```sql
-- 취소 거래 처리 예시
SELECT * FROM transactions
WHERE pg_tid = ?오거래의otid값?;

-- 해당 거래의 settlement 레코드를 DEBIT로 역분개
```

### 6.3 채널 구분 (connCd)

| connCd | 설명 | 단말기 정보 |
|--------|------|-----------|
| 0003 | 오프라인 (POS/CAT) | remainAmt 필드 포함 |
| 0005 | 온라인 (수기결제/웹/앱) | hashStr 필드 포함 |

---

## 7. Bill&Pay 필드 매핑

**KORPAY Webhook → TransactionDto / Transaction Entity**

| KORPAY 필드 | 타입 | Bill&Pay 필드 | Entity 매핑 |
|------------|------|-------------|-----------|
| tid | String | pg_tid | transaction.pg_tid |
| otid | String | pg_otid | transaction.pg_otid |
| mid | String | pg_merchant_no | transaction.pg_merchant_no / merchant_pg_mappings.pg_merchant_no |
| catId | String | terminal_id | transaction.terminal_id / merchant_pg_mappings.terminal_id |
| connCd | String | channel_type | transaction.channel_type (0003/0005) |
| ediNo | String | van_tid | transaction.van_tid |
| ordNo | String | order_id | transaction.order_id |
| amt | Long | original_amount | transaction.amount |
| remainAmt | Long | remain_amount | transaction.remain_amount |
| payMethod | String | payment_method | transaction.payment_method (CARD) |
| goodsName | String | goods_name | transaction.goods_name |
| cardNo | String | card_no_masked | transaction.card_no_masked |
| appNo | String | approval_no | transaction.approval_no |
| quota | String | installment | transaction.installment (0=일시불, 1-12=할부) |
| appCardCd | String | issuer_code | transaction.issuer_code |
| acqCardCd | String | acquirer_code | transaction.acquirer_code |
| fnNm | String | card_company_name | transaction.card_company_name |
| ordNm | String | buyer_name | transaction.buyer_name |
| buyerId | String | buyer_id | transaction.buyer_id |
| cancelYN | String | is_cancel | transaction.event_type 파생 (Y → CANCELED/PARTIAL_CANCELED) |
| appDtm | String | transacted_at | transaction.transacted_at (LocalDateTime 변환) |
| ccDnt | String | cancel_at | transaction.cancel_at (LocalDateTime 변환) |

**날짜/시간 변환**:

```java
// yyyyMMddHHmmss → LocalDateTime
LocalDateTime parseDateTime(String dtm) {
    return LocalDateTime.parse(dtm, DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
}

// Installment 변환
int parseInstallment(String quota) {
    return "00".equals(quota) ? 0 : Integer.parseInt(quota);
}
```

---

## 8. 서명 검증

### 8.1 HMAC-SHA256 검증 (Webhook 인증)

**모든 Webhook 요청에 대해 서명을 검증**:

```java
public boolean verifySignature(Map<String, String> headers, String rawBody, String secret) {
    // 1. Request 헤더에서 서명 추출
    String signature = headers.get("X-Korpay-Signature");

    // 2. 서명 재계산
    String computed = HmacUtils.hmacSha256Hex(secret, rawBody);

    // 3. 타이밍 공격 방지로 MessageDigest.isEqual 사용
    return MessageDigest.isEqual(signature.getBytes(), computed.getBytes());
}
```

**검증 실패 처리**:
- HTTP 400 Bad Request 반환
- 로깅: `"Webhook signature verification failed"`
- PG는 재시도 하지 않음 (클라이언트 오류)

### 8.2 온라인 전용: hashStr 검증

온라인 결제(connCd=0005)에서 추가 검증:

```
hashStr = SHA256(mid + ediDate + amt + mKey)

mKey: KORPAY에서 제공하는 마스터키
```

**검증 로직**:

```java
public boolean verifyOnlineHash(KorpayWebhookData data, String mKey) {
    // hashStr 재계산
    String input = data.getMid() + data.getEdiDate() + data.getAmt() + mKey;
    String computed = DigestUtils.sha256Hex(input);

    // 전송받은 hashStr과 비교
    return data.getHashStr().equalsIgnoreCase(computed);
}
```

---

## 9. 에러 처리

### 9.1 Webhook 수신 에러 시나리오

| 에러 유형 | HTTP 코드 | 로깅 | PG 재시도 | 처리 |
|----------|----------|------|----------|------|
| 서명 검증 실패 | 400 | ✓ | No | 무시 |
| 데이터 파싱 실패 (필드 누락) | 400 | ✓ | No | 무시 + 알림 |
| 필드 타입 불일치 | 400 | ✓ | No | 무시 + 알림 |
| 가맹점 미매핑 | 200 | ✓ | No | unmapped_transactions 저장 + 알림 |
| DB 저장 실패 (Unique 제약) | 409 | ✓ | Yes | 중복 처리 또는 재시도 |
| 타임아웃 | 504 | ✓ | Yes | PG 자동 재시도 |

### 9.2 필드 검증

**필수 필드 누락 감지**:

```java
List<String> requiredFields = List.of(
    "tid", "mid", "ordNo", "amt", "payMethod",
    "appDtm", "cancelYN", "catId", "connCd", "ediNo"
);

for (String field : requiredFields) {
    if (rawData.get(field) == null || rawData.get(field).toString().isEmpty()) {
        log.error("Required field missing: {}", field);
        return ResponseEntity.badRequest().body("Missing field: " + field);
    }
}
```

**타입 변환 오류 처리**:

```java
try {
    long amount = Long.parseLong(data.getAmt());
    int installment = Integer.parseInt(data.getQuota());
} catch (NumberFormatException e) {
    log.error("Type conversion failed: {}", e.getMessage());
    return ResponseEntity.badRequest().body("Invalid data format");
}
```

### 9.3 중복 거래 감지

**Webhook 멱등성 보장**:

```java
// 1단계: 중복 확인
Optional<Transaction> existing = transactionRepository.findByPgTid(pgTid);

if (existing.isPresent()) {
    log.info("Duplicate webhook received: tid={}", pgTid);
    // 이미 처리된 거래이므로 200 OK 반환
    return ResponseEntity.ok(WebhookResponse.success("Already processed"));
}

// 2단계: 신규 거래 저장
Transaction transaction = new Transaction();
// ... 저장 로직
```

---

## 10. 샘플 데이터

### 10.1 단말기 승인 샘플 (connCd=0003, cancelYN=N)

```bash
curl --location --request POST '수신받을 노티주소' \
    --header 'Content-Type: application/x-www-form-urlencoded' \
    --data-urlencode "gid=test11110g" \
    --data-urlencode "remainAmt=0" \
    --data-urlencode "cancelYN=N" \
    --data-urlencode "mid=ktest6111m" \
    --data-urlencode "amt=1000" \
    --data-urlencode "appNo=30059295" \
    --data-urlencode "ccDnt=" \
    --data-urlencode "buyerId=" \
    --data-urlencode "cardNo=12345678****123*" \
    --data-urlencode "tid=ktest6111m01032304111003000874" \
    --data-urlencode "otid=ktest6111m01032304111003000874" \
    --data-urlencode "vid=ctest0001a" \
    --data-urlencode "tPhone=" \
    --data-urlencode "ordNm=" \
    --data-urlencode "catId=1234567890" \
    --data-urlencode "connCd=0003" \
    --data-urlencode "ordNo=12016120230411100300" \
    --data-urlencode "ediNo=2023041110C1359126" \
    --data-urlencode "payMethod=CARD" \
    --data-urlencode "quota=00" \
    --data-urlencode "appDtm=20230411100300" \
    --data-urlencode "goodsName=1234567890" \
    --data-urlencode "appCardCd=02" \
    --data-urlencode "acqCardCd=02" \
    --data-urlencode "notiDnt=20230411101512"
```

### 10.2 온라인 승인 샘플 (connCd=0005, cancelYN=N, hashStr 포함)

```bash
curl --location --request POST '수신받을 노티주소' \
    --header 'Content-Type: application/x-www-form-urlencoded' \
    --data-urlencode "cashCrctFlg=" \
    --data-urlencode "gid=test11110g" \
    --data-urlencode "acqCardCd=02" \
    --data-urlencode "lmtDay=" \
    --data-urlencode "mid=ktest5599m" \
    --data-urlencode "amt=1000" \
    --data-urlencode "appNo=30039792" \
    --data-urlencode "ccDnt=" \
    --data-urlencode "cardNo=12345678****1234" \
    --data-urlencode "tid=ktest5599m01012304111010250264" \
    --data-urlencode "otid=ktest5599m01012304111010250264" \
    --data-urlencode "hashStr=1cac0a74c232eae141dc7abc6ab04393c9594123b68416e05c3cd19281805118" \
    --data-urlencode "vid=ctest0001a" \
    --data-urlencode "ordNm=홍길*" \
    --data-urlencode "connCd=0005" \
    --data-urlencode "ordNo=552440430050704" \
    --data-urlencode "payMethod=CARD" \
    --data-urlencode "fnNm=국민" \
    --data-urlencode "quota=00" \
    --data-urlencode "appDtm=20230411101055" \
    --data-urlencode "ediDate=20230411101211" \
    --data-urlencode "usePointAmt=0" \
    --data-urlencode "authType=01" \
    --data-urlencode "goodsName=테스트상품" \
    --data-urlencode "appCardCd=02" \
    --data-urlencode "charSet=1" \
    --data-urlencode "resultCd=3001" \
    --data-urlencode "cancelYN=N" \
    --data-urlencode "cardType=01" \
    --data-urlencode "resultMsg=카드 결제 성공" \
    --data-urlencode "catId=ktest5599m" \
    --data-urlencode "vacntNo=" \
    --data-urlencode "ediNo=3040229491" \
    --data-urlencode "socHpNo=" \
    --data-urlencode "notiDnt=20230411101211"
```

### 10.3 단말기 취소 샘플 (connCd=0003, cancelYN=Y, remainAmt=0)

```bash
curl --location --request POST '수신받을 노티주소' \
    --header 'Content-Type: application/x-www-form-urlencoded' \
    --data-urlencode "gid=test11110g" \
    --data-urlencode "remainAmt=0" \
    --data-urlencode "cancelYN=Y" \
    --data-urlencode "mid=ktest6111m" \
    --data-urlencode "amt=1000" \
    --data-urlencode "appNo=30059295" \
    --data-urlencode "ccDnt=20230411102609" \
    --data-urlencode "buyerId=" \
    --data-urlencode "cardNo=12345678****123*" \
    --data-urlencode "tid=ktest6111m01032304111003000874" \
    --data-urlencode "otid=ktest6111m01032304111003000874" \
    --data-urlencode "vid=ctest0001a" \
    --data-urlencode "tPhone=" \
    --data-urlencode "ordNm=" \
    --data-urlencode "catId=1234567890" \
    --data-urlencode "connCd=0003" \
    --data-urlencode "ordNo=12016120230411100300" \
    --data-urlencode "ediNo=2023041110C1359126" \
    --data-urlencode "payMethod=CARD" \
    --data-urlencode "quota=00" \
    --data-urlencode "appDtm=20230411100300" \
    --data-urlencode "goodsName=1234567890" \
    --data-urlencode "appCardCd=02" \
    --data-urlencode "acqCardCd=02" \
    --data-urlencode "notiDnt=20230411102637"
```

### 10.4 온라인 취소 샘플 (connCd=0005, cancelYN=Y)

```bash
curl --location --request POST '수신받을 노티주소' \
    --header 'Content-Type: application/x-www-form-urlencoded' \
    --data-urlencode "cashCrctFlg=" \
    --data-urlencode "gid=test11110g" \
    --data-urlencode "acqCardCd=02" \
    --data-urlencode "lmtDay=" \
    --data-urlencode "mid=ktest5599m" \
    --data-urlencode "amt=1000" \
    --data-urlencode "appNo=30039792" \
    --data-urlencode "ccDnt=20230411101521" \
    --data-urlencode "cardNo=12345678****1234" \
    --data-urlencode "tid=ktest5599m01012304111010250264" \
    --data-urlencode "otid=ktest5599m01012304111010250264" \
    --data-urlencode "hashStr=8eab4a185ae61d0ecf63dfc548913134d2eb0c49d2ad83732e2ba26d1785bc54" \
    --data-urlencode "vid=ctest0001a" \
    --data-urlencode "ordNm=홍길*" \
    --data-urlencode "connCd=0005" \
    --data-urlencode "ordNo=552440430050704" \
    --data-urlencode "payMethod=CARD" \
    --data-urlencode "fnNm=국민" \
    --data-urlencode "quota=00" \
    --data-urlencode "appDtm=20230411101055" \
    --data-urlencode "ediDate=20230411101637" \
    --data-urlencode "usePointAmt=0" \
    --data-urlencode "authType=01" \
    --data-urlencode "goodsName=테스트상품" \
    --data-urlencode "appCardCd=02" \
    --data-urlencode "charSet=1" \
    --data-urlencode "resultCd=2001" \
    --data-urlencode "cancelYN=Y" \
    --data-urlencode "cardType=01" \
    --data-urlencode "resultMsg=취소 성공" \
    --data-urlencode "catId=ktest5599m" \
    --data-urlencode "vacntNo=" \
    --data-urlencode "ediNo=3040229491" \
    --data-urlencode "socHpNo=" \
    --data-urlencode "notiDnt=20230411101637"
```

### 10.5 부분취소 샘플 (cancelYN=Y, remainAmt > 0)

```bash
# 단말기 부분취소: 원래 1000원, 500원만 취소하면 remainAmt=500
curl --location --request POST '수신받을 노티주소' \
    --header 'Content-Type: application/x-www-form-urlencoded' \
    --data-urlencode "gid=test11110g" \
    --data-urlencode "remainAmt=500" \
    --data-urlencode "cancelYN=Y" \
    --data-urlencode "mid=ktest6111m" \
    --data-urlencode "amt=500" \
    --data-urlencode "appNo=30059295" \
    --data-urlencode "ccDnt=20230411102609" \
    --data-urlencode "cardNo=12345678****123*" \
    --data-urlencode "tid=ktest6111m01032304111003000875" \
    --data-urlencode "otid=ktest6111m01032304111003000874" \
    --data-urlencode "catId=1234567890" \
    --data-urlencode "connCd=0003" \
    --data-urlencode "ordNo=12016120230411100301" \
    --data-urlencode "ediNo=2023041110C1359127" \
    --data-urlencode "payMethod=CARD" \
    --data-urlencode "quota=00" \
    --data-urlencode "appDtm=20230411100300" \
    --data-urlencode "goodsName=1234567890" \
    --data-urlencode "appCardCd=02" \
    --data-urlencode "acqCardCd=02" \
    --data-urlencode "notiDnt=20230411102637"
```

---

## 11. Bill&Pay 정산 연동

### 11.1 Webhook 수신 → Settlement 자동 생성

```
[KORPAY Webhook 수신]
       │
       ▼
┌─────────────────────────┐
│ 1. 서명 검증            │
└────────┬────────────────┘
         │
         ▼
┌─────────────────────────┐
│ 2. 거래 파싱            │
│    (APPROVED/CANCELED/  │
│     PARTIAL_CANCELED)   │
└────────┬────────────────┘
         │
         ▼
┌─────────────────────────┐
│ 3. Transaction 저장     │
│    (transaction_events) │
└────────┬────────────────┘
         │
         ▼
┌─────────────────────────┐
│ 4. Settlement 생성      │
│    (Zero-Sum 검증)      │
│    (복식부기 기록)      │
└────────┬────────────────┘
         │
         ▼
┌─────────────────────────┐
│ 5. 알림 발송            │
│    (대리점/총판)        │
└─────────────────────────┘
```

### 11.2 Zero-Sum 검증

모든 거래는 PRD-03(원장/정산)의 Zero-Sum 원칙을 따릅니다:

```
|거래금액| = SUM(정산계좌의 CREDIT/DEBIT)

예시:
- 승인 1,000원 → 모든 entity에 CREDIT 1,000원
- 취소 1,000원 → 모든 entity에 DEBIT 1,000원 (역분개)
```

---

## 변경 이력

| 버전 | 날짜 | 변경 내용 |
|------|------|----------|
| v1.0 | 2026-01-28 | KORPAY API 명세 원본 |
| v2.0 | 2026-02-07 | Bill&Pay 아키텍처 관점으로 재구성 |
| | | - Webhook 수신 구조 명확화 |
| | | - 거래 유형 판단 로직 추가 |
| | | - MID/단말기 매핑 관계 상세 설명 |
| | | - 필드 매핑 테이블 추가 |
| | | - 서명 검증 방법 명시 |
| | | - 에러 처리 시나리오 정의 |
| | | - 정산 연동 흐름 추가 |
