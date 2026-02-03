# PRD-04: PG 연동 및 알림 시스템

## 1. 개요

Bill&Pay는 외부 PG사(Payment Gateway)로부터 결제 데이터를 수신하여 정산하는 플랫폼입니다. 본 문서에서는 PG 연동 구조와 알림 시스템을 정의합니다.

---

## 2. 전체 데이터 흐름

```
┌─────────────────────────────────────────────────────────────────────┐
│                        External PG Services                          │
│                                                                      │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐            │
│  │ ★KORPAY │  │ 나이스   │  │ KG이니시스│  │ 토스페이 │            │
│  │ (Primary)│  │ 페이먼츠 │  │          │  │  먼츠    │            │
│  └────┬─────┘  └────┬─────┘  └────┬─────┘  └────┬─────┘            │
│       │             │             │             │                   │
└───────┼─────────────┼─────────────┼─────────────┼───────────────────┘
        │             │             │             │
        │  Webhook    │  Webhook    │  Webhook    │  Webhook
        │             │             │             │
        ▼             ▼             ▼             ▼
┌─────────────────────────────────────────────────────────────────────┐
│                         Bill&Pay Platform                            │
│  ┌────────────────────────────────────────────────────────────────┐ │
│  │                    Webhook Gateway                              │ │
│  │  /api/webhook/korpay  /api/webhook/nice  /api/webhook/inicis   │ │
│  └─────────────────────────────┬──────────────────────────────────┘ │
│                                │                                     │
│                                ▼                                     │
│  ┌────────────────────────────────────────────────────────────────┐ │
│  │                  Transaction Processor                          │ │
│  │  1. 서명 검증  2. 가맹점 매핑  3. 원장 저장  4. 정산 계산       │ │
│  └─────────────────────────────┬──────────────────────────────────┘ │
│                                │                                     │
│                                ▼                                     │
│  ┌────────────────────────────────────────────────────────────────┐ │
│  │                  Notification Service                           │ │
│  │  총판: 시스템 알림  /  대리점: 결제 알림                         │ │
│  └────────────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────────┘
```

---

## 3. PG사 연동 설정

### 3.1 pg_connections 테이블

```sql
CREATE TABLE pg_connections (
    id              BIGSERIAL PRIMARY KEY,
    uuid            UUID NOT NULL DEFAULT uuidv7() UNIQUE,

    -- PG사 정보
    pg_code         VARCHAR(20) NOT NULL UNIQUE,  -- NICE, INICIS, TOSS, KSNET
    pg_name         VARCHAR(50) NOT NULL,
    pg_api_version  VARCHAR(20),                  -- API 버전

    -- 연동 인증 정보 (AES-256 암호화 저장)
    merchant_id     VARCHAR(100) NOT NULL,        -- PG사 가맹점 ID
    api_key_enc     BYTEA NOT NULL,               -- 암호화된 API Key
    api_secret_enc  BYTEA NOT NULL,               -- 암호화된 Secret

    -- Webhook 설정
    webhook_path    VARCHAR(100) NOT NULL UNIQUE, -- /api/webhook/nice
    webhook_secret  VARCHAR(100),                 -- Webhook 검증용 시크릿

    -- API 엔드포인트 (결제 조회/취소 등)
    api_base_url    VARCHAR(200),
    api_endpoints   JSONB DEFAULT '{}',

    -- 상태
    status          VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    last_sync_at    TIMESTAMPTZ,
    last_error      TEXT,

    -- 감사
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_pg_status CHECK (status IN ('ACTIVE', 'INACTIVE', 'ERROR'))
);
```

### 3.2 지원 PG사 목록

| PG사 | 코드 | Webhook 경로 (신규) | Webhook 경로 (레거시) | 비고 |
|------|------|---------------------|----------------------|------|
| 코페이 | KORPAY | /api/webhook/{tenantId}/korpay | /api/webhook/korpay | |
| 나이스페이먼츠 | NICE | /api/webhook/{tenantId}/nice | /api/webhook/nice | |
| KG이니시스 | INICIS | /api/webhook/{tenantId}/inicis | /api/webhook/inicis | |
| 토스페이먼츠 | TOSS | /api/webhook/{tenantId}/toss | /api/webhook/toss | |
| KSNET | KSNET | /api/webhook/{tenantId}/ksnet | /api/webhook/ksnet | |
| 세틀뱅크 | SETTLE | /api/webhook/{tenantId}/settle | /api/webhook/settle | 가상계좌 |
| 헥토파이낸셜 | HECTO | /api/webhook/{tenantId}/hecto | /api/webhook/hecto | |

> **Note**: 신규 Webhook URL 패턴은 테넌트 식별을 URL 경로에 포함합니다. 레거시 경로는 backward compatibility를 위해 유지되지만, deprecation 로깅이 발생하며 향후 마이그레이션을 권장합니다.

### 3.3 API 엔드포인트 JSONB 구조

```json
{
  "payment_inquiry": "/v1/payments/{tid}",
  "payment_cancel": "/v1/payments/{tid}/cancel",
  "settlement_inquiry": "/v1/settlements",
  "balance_inquiry": "/v1/balance"
}
```

---

## 4. 가맹점-PG 매핑

### 4.1 merchant_pg_mappings 테이블

Bill&Pay 가맹점과 PG사 가맹점 번호를 매핑합니다:

```sql
CREATE TABLE merchant_pg_mappings (
    id              BIGSERIAL PRIMARY KEY,

    -- Bill&Pay 가맹점
    merchant_id     BIGINT NOT NULL REFERENCES merchants(id),

    -- PG 연결
    pg_connection_id BIGINT NOT NULL REFERENCES pg_connections(id),

    -- PG사 가맹점 정보
    pg_merchant_no  VARCHAR(50) NOT NULL,         -- PG사 가맹점 번호
    pg_merchant_key VARCHAR(100),                 -- PG사 가맹점 키 (선택)

    -- 단말기 정보
    terminal_id     VARCHAR(50),                  -- 단말기 TID
    terminal_type   VARCHAR(20),                  -- POS, CAT, ONLINE, MOBILE

    -- 상태
    status          VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',

    -- 감사
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_pg_merchant_mapping UNIQUE (pg_connection_id, pg_merchant_no),
    CONSTRAINT chk_terminal_type CHECK (
        terminal_type IN ('POS', 'CAT', 'ONLINE', 'MOBILE')
    )
);

-- Webhook 수신 시 가맹점 조회용 인덱스
CREATE INDEX idx_pg_mapping_lookup
    ON merchant_pg_mappings (pg_connection_id, pg_merchant_no)
    WHERE status = 'ACTIVE';
```

### 4.2 단말기 유형

| 유형 | 코드 | 설명 |
|------|------|------|
| POS | POS | 카드 단말기 (오프라인) |
| CAT | CAT | 신용카드 조회기 |
| 온라인 | ONLINE | 웹/앱 결제 |
| 모바일 | MOBILE | 모바일 POS |

---

## 5. Webhook 수신 처리

### 5.1 Webhook Controller

#### 5.1.1 테넌트 인식 Webhook URL (권장)

```java
@RestController
@RequestMapping("/api/webhook")
public class WebhookController {

    private final WebhookProcessingService webhookProcessingService;
    private final TenantService tenantService;
    private final PgConnectionRepository pgConnectionRepository;

    /**
     * 테넌트 인식 Webhook 수신 엔드포인트 (권장)
     * URL 패턴: POST /api/webhook/{tenantId}/{pgCode}?pgConnectionId=xxx&webhookSecret=yyy
     */
    @PostMapping("/{tenantId}/{pgCode}")
    public ResponseEntity<WebhookResponse> receiveWebhookWithTenant(
            @PathVariable String tenantId,
            @PathVariable String pgCode,
            @RequestParam Long pgConnectionId,
            @RequestParam String webhookSecret,
            @RequestHeader Map<String, String> headers,
            @RequestBody String rawBody) {

        // 1. 테넌트 존재 여부 검증
        if (!tenantService.validateTenantExists(tenantId)) {
            log.warn("Webhook 수신 실패: 존재하지 않는 테넌트 - tenantId={}", tenantId);
            return ResponseEntity.badRequest()
                .body(WebhookResponse.error("Invalid tenant"));
        }

        // 2. 테넌트 컨텍스트 내에서 처리
        return TenantContextHolder.runInTenant(tenantId, () -> {
            // PG Connection 검증 (tenantId 일치 확인)
            PgConnection pgConnection = pgConnectionRepository
                .findByIdAndTenantId(pgConnectionId, tenantId)
                .orElseThrow(() -> new WebhookValidationException("Invalid PG connection"));

            // Webhook Secret 검증
            if (!webhookSecret.equals(pgConnection.getWebhookSecret())) {
                throw new WebhookValidationException("Invalid webhook secret");
            }

            // 기존 처리 로직 실행
            WebhookResult result = webhookProcessingService.process(pgCode, headers, rawBody);
            return ResponseEntity.ok(result.toResponse());
        });
    }
}
```

#### 5.1.2 레거시 Webhook URL (Deprecated)

기존 연동을 위해 유지되지만, 신규 연동은 테넌트 인식 URL 사용을 권장합니다.

```java
/**
 * 레거시 Webhook 수신 엔드포인트 (Deprecated)
 * URL 패턴: POST /api/webhook/{pgCode}?pgConnectionId=xxx&webhookSecret=yyy
 * 
 * @deprecated 테넌트 인식 엔드포인트 사용 권장: POST /api/webhook/{tenantId}/{pgCode}
 */
@Deprecated
@PostMapping("/{pgCode}")
public ResponseEntity<WebhookResponse> receiveWebhookLegacy(
        @PathVariable String pgCode,
        @RequestParam Long pgConnectionId,
        @RequestParam String webhookSecret,
        @RequestHeader Map<String, String> headers,
        @RequestBody String rawBody) {

    log.warn("DEPRECATION: 레거시 Webhook URL 사용됨 - pgCode={}, pgConnectionId={}. " +
             "테넌트 인식 URL로 마이그레이션 권장: /api/webhook/{tenantId}/{pgCode}",
             pgCode, pgConnectionId);

    // PG Connection에서 tenantId 조회
    PgConnection pgConnection = pgConnectionRepository.findById(pgConnectionId)
        .orElseThrow(() -> new WebhookValidationException("Invalid PG connection"));

    String tenantId = pgConnection.getTenantId();

    // 테넌트 컨텍스트 내에서 처리
    return TenantContextHolder.runInTenant(tenantId, () -> {
        // Webhook Secret 검증 및 처리
        WebhookResult result = webhookProcessingService.process(pgCode, headers, rawBody);
        return ResponseEntity.ok(result.toResponse());
    });
}
```

#### 5.1.3 Webhook URL 생성 서비스

PG 연동 설정 시 Webhook URL을 자동 생성하는 서비스:

```java
@Service
public class WebhookUrlGenerator {

    @Value("${app.webhook.base-url}")
    private String webhookBaseUrl;  // 예: https://api.billpay.com

    /**
     * 테넌트 인식 Webhook URL 생성 (권장)
     */
    public String generateWebhookUrl(String tenantId, PgConnection pgConnection) {
        return UriComponentsBuilder.fromUriString(webhookBaseUrl)
            .path("/api/webhook/{tenantId}/{pgCode}")
            .queryParam("pgConnectionId", pgConnection.getId())
            .queryParam("webhookSecret", pgConnection.getWebhookSecret())
            .buildAndExpand(tenantId, pgConnection.getPgCode())
            .toUriString();
    }

    /**
     * 레거시 Webhook URL 생성 (deprecated)
     */
    @Deprecated
    public String generateLegacyWebhookUrl(PgConnection pgConnection) {
        return UriComponentsBuilder.fromUriString(webhookBaseUrl)
            .path("/api/webhook/{pgCode}")
            .queryParam("pgConnectionId", pgConnection.getId())
            .queryParam("webhookSecret", pgConnection.getWebhookSecret())
            .buildAndExpand(pgConnection.getPgCode())
            .toUriString();
    }
}
```

### 5.2 Webhook 처리 흐름

```
[Webhook 수신]
       │
       ▼
┌──────────────────────────────────────┐
│ 1. PG 연결 정보 조회                  │
│    - pg_code로 pg_connections 조회    │
│    - ACTIVE 상태 확인                 │
└──────────────────────────────────────┘
       │
       ▼
┌──────────────────────────────────────┐
│ 2. 서명 검증 (Signature Verification) │
│    - HMAC-SHA256 검증                 │
│    - 실패 시 400 반환 + 로깅          │
└──────────────────────────────────────┘
       │
       ▼
┌──────────────────────────────────────┐
│ 3. 데이터 파싱 (PG별 Adapter)         │
│    - JSON → 공통 DTO 변환             │
│    - 필수 필드 검증                   │
└──────────────────────────────────────┘
       │
       ▼
┌──────────────────────────────────────┐
│ 4. 중복 체크 (Idempotency)            │
│    - pg_code + pg_tid 조합으로 확인   │
│    - 중복 시 200 OK + 무시            │
└──────────────────────────────────────┘
       │
       ▼
┌──────────────────────────────────────┐
│ 5. 가맹점 매핑 조회                   │
│    - pg_merchant_no로 매핑 조회       │
│    - 미매핑 시 unmapped 큐 저장       │
└──────────────────────────────────────┘
       │
       ▼
┌──────────────────────────────────────┐
│ 6. Transaction 저장                   │
│    - Transaction Ledger 저장          │
│    - 파티션 테이블에 INSERT           │
└──────────────────────────────────────┘
       │
       ▼
┌──────────────────────────────────────┐
│ 7. Settlement 생성                    │
│    - 수수료 계산 엔진 호출            │
│    - Settlement Ledger 저장           │
│    - Zero-Sum 검증                    │
└──────────────────────────────────────┘
       │
       ▼
┌──────────────────────────────────────┐
│ 8. 알림 발송                          │
│    - 대리점에 결제 알림               │
│    - 비동기 처리 (Virtual Threads)    │
└──────────────────────────────────────┘
       │
       ▼
┌──────────────────────────────────────┐
│ 9. 응답 반환                          │
│    - 200 OK                           │
└──────────────────────────────────────┘
```

### 5.3 PG별 Webhook 데이터 어댑터

```java
public interface PgWebhookAdapter {
    /**
     * PG사 원본 데이터를 공통 DTO로 변환
     */
    TransactionDto parse(String rawBody);

    /**
     * Webhook 서명 검증
     */
    boolean verifySignature(Map<String, String> headers, String rawBody, String secret);
}

// 나이스페이먼츠 구현체
@Component
public class NiceWebhookAdapter implements PgWebhookAdapter {

    @Override
    public TransactionDto parse(String rawBody) {
        NiceWebhookData data = objectMapper.readValue(rawBody, NiceWebhookData.class);

        return TransactionDto.builder()
            .pgTid(data.getTid())
            .pgMerchantNo(data.getMerchantNo())
            .orderId(data.getOrderId())
            .amount(data.getAmount())
            .paymentMethod(mapPaymentMethod(data.getPayMethod()))
            .cardCode(data.getCardCode())
            .cardType(mapCardType(data.getCardType()))
            .approvalNo(data.getApprovalNo())
            .transactedAt(data.getTransactionAt())
            .build();
    }

    @Override
    public boolean verifySignature(Map<String, String> headers, String rawBody, String secret) {
        String signature = headers.get("X-Nice-Signature");
        String computed = HmacUtils.hmacSha256Hex(secret, rawBody);
        return MessageDigest.isEqual(signature.getBytes(), computed.getBytes());
    }
}

// ★ KORPAY 구현체 (Primary PG)
@Component
public class KorpayWebhookAdapter implements PgWebhookAdapter {

    @Override
    public TransactionDto parse(String rawBody) {
        KorpayWebhookData data = objectMapper.readValue(rawBody, KorpayWebhookData.class);

        // 취소 여부 판단
        boolean isCancel = "Y".equals(data.getCancelYN());
        String eventType = isCancel
            ? (data.getRemainAmt() > 0 ? "PARTIAL_CANCELED" : "CANCELED")
            : "APPROVED";

        return TransactionDto.builder()
            // PG 거래 정보
            .pgTid(data.getTid())
            .pgOtid(data.getOtid())                    // 원거래 TID (취소 시)
            .pgMerchantNo(data.getMid())              // MID (가맹점 식별)
            .terminalId(data.getCatId())              // 단말기 ID (MID와 1:1)
            .channelType(data.getConnCd())            // 0003:단말기, 0005:온라인
            .vanTid(data.getEdiNo())                  // VAN 거래고유번호

            // 거래 정보
            .orderId(data.getOrdNo())
            .amount(data.getAmt())
            .remainAmount(data.getRemainAmt())        // 잔액 (부분취소 시)
            .paymentMethod(mapPaymentMethod(data.getPayMethod()))
            .goodsName(data.getGoodsName())

            // 카드 정보
            .cardNoMasked(data.getCardNo())
            .approvalNo(data.getAppNo())
            .installment(parseInstallment(data.getQuota()))
            .issuerCode(data.getAppCardCd())          // 발급사
            .acquirerCode(data.getAcqCardCd())        // 매입사
            .cardCompanyName(data.getFnNm())          // 카드사명

            // 구매자 정보
            .buyerName(data.getOrdNm())
            .buyerId(data.getBuyerId())

            // 상태
            .eventType(eventType)
            .isCancel(isCancel)
            .transactedAt(parseDateTime(data.getAppDtm()))
            .cancelAt(isCancel ? parseDateTime(data.getCcDnt()) : null)
            .build();
    }

    @Override
    public boolean verifySignature(Map<String, String> headers, String rawBody, String secret) {
        // KORPAY 서명 검증 로직
        String signature = headers.get("X-Korpay-Signature");
        String computed = HmacUtils.hmacSha256Hex(secret, rawBody);
        return MessageDigest.isEqual(signature.getBytes(), computed.getBytes());
    }

    private LocalDateTime parseDateTime(String dtm) {
        // yyyyMMddHHmmss → LocalDateTime
        return LocalDateTime.parse(dtm, DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
    }

    private int parseInstallment(String quota) {
        return "00".equals(quota) ? 0 : Integer.parseInt(quota);
    }
}
```

### 5.4 KORPAY 웹훅 데이터 구조

#### 5.4.1 KORPAY 웹훅 필드 매핑

| KORPAY 필드 | 타입 | Bill&Pay 필드 | 설명 |
|------------|------|--------------|------|
| tid | String | pg_tid | 거래 고유번호 |
| otid | String | pg_otid | 원거래 TID (취소 시) |
| mid | String | pg_merchant_no | 가맹점 ID (MID) |
| catId | String | terminal_id | 단말기 ID (MID와 1:1 매핑) |
| connCd | String | channel_type | 거래채널 (0003:단말기, 0005:온라인) |
| ediNo | String | van_tid | VAN 거래고유번호 |
| ordNo | String | order_id | 주문번호 |
| amt | Long | original_amount | 거래금액 |
| remainAmt | Long | remain_amount | 잔액 (부분취소 시) |
| payMethod | String | payment_method | 결제수단 |
| goodsName | String | goods_name | 상품명 |
| cardNo | String | card_no_masked | 마스킹된 카드번호 |
| appNo | String | approval_no | 승인번호 |
| quota | String | installment | 할부개월 (00=일시불) |
| appCardCd | String | issuer_code | 발급카드사 코드 |
| acqCardCd | String | acquirer_code | 매입카드사 코드 |
| fnNm | String | card_company_name | 카드사명 |
| ordNm | String | buyer_name | 구매자명 |
| buyerId | String | buyer_id | 구매자 ID |
| cancelYN | String | is_cancel | 취소여부 (Y/N) |
| appDtm | String | transacted_at | 승인일시 (yyyyMMddHHmmss) |
| ccDnt | String | cancel_at | 취소일시 (yyyyMMddHHmmss) |

#### 5.4.2 KORPAY 제외 필드

다음 필드는 Bill&Pay에서 사용하지 않음:

| 필드 | 사유 |
|------|------|
| gid | 의미 없음 (KORPAY 내부용) |
| vid | 의미 없음 (KORPAY 내부용) |

#### 5.4.3 MID와 단말기(catId) 관계

```
┌─────────────────────────────────────────────────────────────────┐
│                    KORPAY MID 구조                               │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  MID (mid)                     단말기 (catId)                    │
│  ┌──────────────┐              ┌──────────────┐                 │
│  │ M1234567890  │ ◄──── 1:1 ───► │ 1046347583   │                 │
│  └──────────────┘              └──────────────┘                 │
│                                                                  │
│  특징:                                                           │
│  - MID = 온라인승인 계정                                          │
│  - MID와 단말기번호(catId)는 1:1 매핑                             │
│  - 웹훅 수신 시 mid로 가맹점 식별                                  │
│                                                                  │
│  merchant_pg_mappings 저장 예시:                                  │
│  ┌─────────────────────────────────────────────────────────────┐ │
│  │ pg_merchant_no: M1234567890   (KORPAY MID)                  │ │
│  │ terminal_id:    1046347583    (단말기번호)                   │ │
│  │ terminal_type:  ONLINE        (또는 CAT)                    │ │
│  └─────────────────────────────────────────────────────────────┘ │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

#### 5.4.4 KORPAY 웹훅 수신 예시

```json
{
  "tid": "KORPAY20260129123456",
  "otid": "",
  "mid": "M1234567890",
  "catId": "1046347583",
  "connCd": "0003",
  "ediNo": "VAN20260129001",
  "ordNo": "ORDER-2026012900001",
  "amt": 150000,
  "remainAmt": 0,
  "payMethod": "CARD",
  "goodsName": "테스트상품",
  "cardNo": "9410-****-****-1234",
  "appNo": "12345678",
  "quota": "00",
  "appCardCd": "BC",
  "acqCardCd": "BC",
  "fnNm": "비씨카드",
  "ordNm": "홍길동",
  "buyerId": "user123",
  "cancelYN": "N",
  "appDtm": "20260129143052",
  "ccDnt": ""
}
```

#### 5.4.5 취소 거래 처리

```java
// KORPAY 취소 유형 판단
public String determineEventType(KorpayWebhookData data) {
    if (!"Y".equals(data.getCancelYN())) {
        return "APPROVED";
    }

    // 부분취소: 잔액이 남아있는 경우
    if (data.getRemainAmt() != null && data.getRemainAmt() > 0) {
        return "PARTIAL_CANCELED";
    }

    // 전체취소
    return "CANCELED";
}
```

---

## 6. 미매핑 거래 처리

### 6.1 unmapped_transactions 테이블

PG사에서 수신했지만 Bill&Pay에 등록되지 않은 가맹점의 거래:

```sql
CREATE TABLE unmapped_transactions (
    id              BIGSERIAL PRIMARY KEY,
    uuid            UUID NOT NULL DEFAULT uuidv7() UNIQUE,

    -- PG 정보
    pg_connection_id BIGINT NOT NULL REFERENCES pg_connections(id),
    pg_tid          VARCHAR(100) NOT NULL,
    pg_merchant_no  VARCHAR(50) NOT NULL,

    -- 원본 데이터
    raw_data        JSONB NOT NULL,
    amount          BIGINT NOT NULL,
    transacted_at   TIMESTAMPTZ NOT NULL,

    -- 처리 상태
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    mapped_merchant_id BIGINT REFERENCES merchants(id),
    processed_by    BIGINT,
    processed_at    TIMESTAMPTZ,
    process_note    TEXT,

    -- 감사
    received_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_unmapped_status CHECK (
        status IN ('PENDING', 'MAPPED', 'IGNORED', 'EXPIRED')
    )
);

CREATE INDEX idx_unmapped_status ON unmapped_transactions (status);
CREATE INDEX idx_unmapped_pg ON unmapped_transactions (pg_connection_id, pg_merchant_no);
```

### 6.2 미매핑 거래 처리 플로우

```
[미매핑 거래 발생]
       │
       ▼
┌──────────────────────────────────┐
│ 1. unmapped_transactions 저장    │
│    - status: PENDING             │
└──────────────────────────────────┘
       │
       ▼
┌──────────────────────────────────┐
│ 2. 총판 알림 발송                │
│    - "미매핑 거래 N건 발생"       │
└──────────────────────────────────┘
       │
       ▼
┌──────────────────────────────────┐
│ 3. 관리자 수동 처리              │
│    ├─ 가맹점 매핑 → MAPPED       │
│    ├─ 무시 → IGNORED             │
│    └─ 30일 경과 → EXPIRED        │
└──────────────────────────────────┘
       │ (MAPPED인 경우)
       ▼
┌──────────────────────────────────┐
│ 4. Transaction/Settlement 생성   │
│    - 정상 처리 플로우 재수행      │
└──────────────────────────────────┘
```

---

## 7. 알림 시스템

### 7.1 알림 수신 대상

| 대상 | 알림 유형 | 설명 |
|------|----------|------|
| **총판** | 시스템 알림 | PG 연동 오류, 미매핑 거래, 배치 실패, 일일 리포트 |
| **대리점** | 비즈니스 알림 | 결제 성공/취소, 정산 완료 |

### 7.2 notification_settings 테이블

```sql
CREATE TABLE notification_settings (
    id              BIGSERIAL PRIMARY KEY,

    -- 수신 대상
    target_type     VARCHAR(20) NOT NULL,         -- MASTER, ORGANIZATION
    target_id       BIGINT,                       -- org_id (ORGANIZATION인 경우)

    -- 총판용 알림 설정
    pg_error_enabled        BOOLEAN DEFAULT TRUE,  -- PG 연동 오류
    unmapped_txn_enabled    BOOLEAN DEFAULT TRUE,  -- 미매핑 거래
    batch_error_enabled     BOOLEAN DEFAULT TRUE,  -- 배치 실패
    daily_report_enabled    BOOLEAN DEFAULT TRUE,  -- 일일 리포트

    -- 대리점용 알림 설정
    payment_success_enabled BOOLEAN DEFAULT TRUE,  -- 결제 성공
    payment_cancel_enabled  BOOLEAN DEFAULT TRUE,  -- 결제 취소
    settlement_enabled      BOOLEAN DEFAULT TRUE,  -- 정산 완료

    -- 수신 채널
    push_enabled    BOOLEAN DEFAULT TRUE,
    email_enabled   BOOLEAN DEFAULT FALSE,
    sms_enabled     BOOLEAN DEFAULT FALSE,

    -- Webhook 설정 (외부 시스템 연동)
    webhook_url     VARCHAR(500),
    webhook_secret  VARCHAR(100),
    webhook_enabled BOOLEAN DEFAULT FALSE,

    -- 감사
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_notification_target UNIQUE (target_type, target_id),
    CONSTRAINT chk_target_type CHECK (target_type IN ('MASTER', 'ORGANIZATION'))
);
```

### 7.3 알림 유형별 상세

#### 7.3.1 결제 성공 알림 (대리점)

```json
{
  "type": "PAYMENT_SUCCESS",
  "timestamp": "2026-01-28T18:32:15+09:00",
  "data": {
    "transaction_id": "TXN-20260128-001234",
    "merchant_name": "강남 치킨집",
    "amount": 150000,
    "payment_method": "CREDIT",
    "card_company": "신한카드",
    "approval_no": "12345678"
  }
}
```

#### 7.3.2 정산 완료 알림 (대리점)

```json
{
  "type": "SETTLEMENT_COMPLETE",
  "timestamp": "2026-01-29T00:05:00+09:00",
  "data": {
    "settlement_date": "2026-01-28",
    "total_transactions": 1523,
    "total_amount": 125000000,
    "total_fee": 3750000,
    "net_settlement": 121250000
  }
}
```

#### 7.3.3 미매핑 거래 알림 (총판)

```json
{
  "type": "UNMAPPED_TRANSACTION",
  "timestamp": "2026-01-28T18:35:00+09:00",
  "data": {
    "pg_code": "NICE",
    "pg_merchant_no": "UNKNOWN_001",
    "count": 5,
    "total_amount": 750000,
    "message": "미등록 가맹점에서 5건의 거래가 발생했습니다."
  }
}
```

### 7.4 알림 발송 서비스

```java
@Service
public class NotificationService {

    private final NotificationSettingsRepository settingsRepository;
    private final PushService pushService;
    private final EmailService emailService;
    private final WebhookService webhookService;

    /**
     * 결제 알림 발송 (대리점)
     */
    @Async
    public void sendPaymentNotification(Transaction txn, Merchant merchant) {
        // 대리점 조회 (가맹점 소속 계보에서 대리점 찾기)
        Organization distributor = findDistributor(merchant.getOrgPath());

        NotificationSettings settings = settingsRepository
            .findByTargetTypeAndTargetId("ORGANIZATION", distributor.getId())
            .orElse(NotificationSettings.defaultSettings());

        if (!settings.isPaymentSuccessEnabled()) {
            return;
        }

        NotificationPayload payload = NotificationPayload.builder()
            .type(NotificationType.PAYMENT_SUCCESS)
            .data(buildPaymentData(txn, merchant))
            .build();

        // 채널별 발송
        if (settings.isPushEnabled()) {
            pushService.send(distributor.getId(), payload);
        }
        if (settings.isEmailEnabled()) {
            emailService.send(distributor.getEmail(), payload);
        }
        if (settings.isWebhookEnabled()) {
            webhookService.send(settings.getWebhookUrl(), settings.getWebhookSecret(), payload);
        }
    }

    /**
     * 시스템 알림 발송 (총판)
     */
    @Async
    public void sendSystemNotification(NotificationType type, Object data) {
        NotificationSettings settings = settingsRepository
            .findByTargetType("MASTER")
            .orElse(NotificationSettings.defaultSettings());

        NotificationPayload payload = NotificationPayload.builder()
            .type(type)
            .data(data)
            .build();

        // 총판 관리자들에게 발송
        if (settings.isPushEnabled()) {
            pushService.sendToMasterAdmins(payload);
        }
        if (settings.isEmailEnabled()) {
            emailService.sendToMasterAdmins(payload);
        }
    }
}
```

### 7.5 대리점 Webhook 연동

대리점이 자체 시스템과 연동하기 위한 Webhook:

```
[Bill&Pay] ──────► [대리점 시스템]
             │
             │  POST https://대리점.com/webhook/billpay
             │  Headers:
             │    X-Signature: HMAC-SHA256(body, secret)
             │    X-Timestamp: 1706434335
             │  Body:
             │    { "type": "PAYMENT_SUCCESS", ... }
             │
             ▼
        [대리점 자체 처리]
        - 자체 DB 저장
        - 사장님 알림 발송
        - 재고 연동 등
```

---

## 8. 에러 처리 및 재시도

### 8.1 Webhook 수신 에러

| 에러 유형 | HTTP 코드 | PG 재시도 | Bill&Pay 처리 |
|----------|-----------|----------|---------------|
| 서명 검증 실패 | 400 | No | 로깅 + 무시 |
| 데이터 파싱 실패 | 400 | No | 로깅 + 알림 |
| 가맹점 미매핑 | 200 | No | unmapped 저장 + 알림 |
| DB 저장 실패 | 500 | Yes | 로깅 + PG 재시도 유도 |
| 타임아웃 | 504 | Yes | 로깅 + PG 재시도 유도 |

### 8.2 알림 발송 재시도

```java
@Service
public class NotificationRetryService {

    private static final int MAX_RETRY = 3;
    private static final int[] RETRY_DELAYS = {1000, 5000, 30000}; // ms

    @Retryable(
        value = NotificationException.class,
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 5)
    )
    public void sendWithRetry(NotificationPayload payload, String webhookUrl) {
        webhookService.send(webhookUrl, payload);
    }

    @Recover
    public void recover(NotificationException e, NotificationPayload payload) {
        // 최종 실패 시 DB에 저장 후 배치로 재처리
        failedNotificationRepository.save(FailedNotification.builder()
            .payload(payload)
            .error(e.getMessage())
            .retryCount(MAX_RETRY)
            .build());

        // 총판에 알림 실패 알림 (메타 알림)
        notificationService.sendSystemNotification(
            NotificationType.NOTIFICATION_FAILED,
            Map.of("target", payload.getTargetId(), "type", payload.getType())
        );
    }
}
```

---

## 9. 모니터링

### 9.1 PG 연동 상태 모니터링

```sql
-- PG별 최근 1시간 수신 현황
SELECT
    pc.pg_code,
    pc.pg_name,
    pc.status,
    COUNT(t.id) AS txn_count,
    SUM(t.amount) AS total_amount,
    MAX(t.created_at) AS last_received_at
FROM pg_connections pc
LEFT JOIN transactions t ON t.pg_code = pc.pg_code
    AND t.created_at >= NOW() - INTERVAL '1 hour'
GROUP BY pc.pg_code, pc.pg_name, pc.status
ORDER BY pc.pg_code;
```

### 9.2 알림 발송 현황

```sql
-- 최근 24시간 알림 발송 현황
SELECT
    notification_type,
    channel,
    status,
    COUNT(*) AS count
FROM notification_logs
WHERE created_at >= NOW() - INTERVAL '24 hours'
GROUP BY notification_type, channel, status
ORDER BY notification_type, channel;
```

---

## 변경 이력

| 버전 | 날짜 | 변경 내용 |
|------|------|----------|
| v1.0 | 2026-01-28 | 초안 작성 |
| v2.0 | 2026-01-29 | KORPAY 연동 추가 - Webhook Adapter, 필드 매핑, MID/단말기 관계 설명 |
| v3.0 | 2026-02-03 | 테넌트 인식 Webhook URL 구현 - URL 경로에 tenantId 포함, 레거시 URL deprecation |
