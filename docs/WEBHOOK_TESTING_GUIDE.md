# Webhook Testing Guide

## Manual Testing with cURL

### 1. Setup Test Data

First, ensure you have:
- A PG connection configured in database
- A merchant PG mapping for the test merchant
- Payment method and card company reference data

```sql
-- Get PG Connection ID
SELECT id, pg_code, webhook_secret FROM pg_connections WHERE pg_code = 'KORPAY';

-- Get Merchant PG Mapping
SELECT id, mid, cat_id FROM merchant_pg_mappings WHERE mid = 'ktest6111m';
```

### 2. Generate HMAC Signature

Use this script to generate the webhook signature:

```bash
#!/bin/bash
# generate_signature.sh

WEBHOOK_SECRET="your-webhook-secret-here"
RAW_BODY="tid=ktest6111m01032304111003000874&mid=ktest6111m&catId=1234567890&amt=1000&cancelYN=N&remainAmt=0&appNo=30059295&ccDnt=&buyerId=&cardNo=12345678****123*&otid=ktest6111m01032304111003000874&tPhone=&ordNm=&connCd=0003&ordNo=12016120230411100300&ediNo=2023041110C1359126&payMethod=CARD&quota=00&appDtm=20230411100300&goodsName=1234567890&appCardCd=02&acqCardCd=02&notiDnt=20230411101512"

SIGNATURE=$(echo -n "$RAW_BODY" | openssl dgst -sha256 -hmac "$WEBHOOK_SECRET" -hex | awk '{print $2}')

echo "Signature: $SIGNATURE"
```

### 3. Test KORPAY Approval Webhook

```bash
curl -X POST 'http://localhost:8080/api/webhook/korpay?pgConnectionId=YOUR-PG-CONNECTION-UUID&webhookSecret=your-webhook-secret' \
  -H 'Content-Type: application/x-www-form-urlencoded' \
  -H 'X-Korpay-Signature: YOUR-COMPUTED-SIGNATURE' \
  --data-urlencode "tid=ktest6111m01032304111003000874" \
  --data-urlencode "otid=ktest6111m01032304111003000874" \
  --data-urlencode "mid=ktest6111m" \
  --data-urlencode "catId=1234567890" \
  --data-urlencode "connCd=0003" \
  --data-urlencode "ediNo=2023041110C1359126" \
  --data-urlencode "ordNo=12016120230411100300" \
  --data-urlencode "amt=1000" \
  --data-urlencode "remainAmt=0" \
  --data-urlencode "payMethod=CARD" \
  --data-urlencode "goodsName=Test Product" \
  --data-urlencode "cardNo=12345678****123*" \
  --data-urlencode "appNo=30059295" \
  --data-urlencode "quota=00" \
  --data-urlencode "appCardCd=02" \
  --data-urlencode "acqCardCd=02" \
  --data-urlencode "fnNm=BC Card" \
  --data-urlencode "ordNm=Hong Gildong" \
  --data-urlencode "buyerId=user123" \
  --data-urlencode "cancelYN=N" \
  --data-urlencode "appDtm=20230411100300" \
  --data-urlencode "ccDnt=" \
  --data-urlencode "notiDnt=20230411101512"
```

Expected Response (200 OK):
```json
{
  "success": true,
  "message": "Webhook processed successfully",
  "transactionId": "TXN-ktest6111m01032304111003000874"
}
```

### 4. Test KORPAY Cancel Webhook

```bash
curl -X POST 'http://localhost:8080/api/webhook/korpay?pgConnectionId=YOUR-PG-CONNECTION-UUID&webhookSecret=your-webhook-secret' \
  -H 'Content-Type: application/x-www-form-urlencoded' \
  -H 'X-Korpay-Signature: YOUR-COMPUTED-SIGNATURE' \
  --data-urlencode "tid=ktest6111m01032304111003000874" \
  --data-urlencode "otid=ktest6111m01032304111003000874" \
  --data-urlencode "mid=ktest6111m" \
  --data-urlencode "catId=1234567890" \
  --data-urlencode "connCd=0003" \
  --data-urlencode "ediNo=2023041110C1359126" \
  --data-urlencode "ordNo=12016120230411100300" \
  --data-urlencode "amt=1000" \
  --data-urlencode "remainAmt=0" \
  --data-urlencode "payMethod=CARD" \
  --data-urlencode "goodsName=Test Product" \
  --data-urlencode "cardNo=12345678****123*" \
  --data-urlencode "appNo=30059295" \
  --data-urlencode "quota=00" \
  --data-urlencode "appCardCd=02" \
  --data-urlencode "acqCardCd=02" \
  --data-urlencode "fnNm=BC Card" \
  --data-urlencode "ordNm=Hong Gildong" \
  --data-urlencode "buyerId=user123" \
  --data-urlencode "cancelYN=Y" \
  --data-urlencode "appDtm=20230411100300" \
  --data-urlencode "ccDnt=20230411102609" \
  --data-urlencode "notiDnt=20230411102637"
```

Expected Response (200 OK):
```json
{
  "success": true,
  "message": "Webhook processed successfully",
  "transactionId": "TXN-ktest6111m01032304111003000874"
}
```

### 5. Test Duplicate Transaction (Idempotency)

Send the same approval webhook twice:

Expected Response (200 OK):
```json
{
  "success": true,
  "message": "Transaction already processed",
  "transactionId": "TXN-ktest6111m01032304111003000874"
}
```

### 6. Test Invalid Signature

Send webhook with wrong signature:

Expected Response (400 Bad Request):
```json
{
  "success": false,
  "message": "Signature verification failed"
}
```

### 7. Test Unmapped Merchant

Send webhook with unknown mid:

Expected Response (200 OK):
```json
{
  "success": true,
  "message": "Unmapped transaction saved"
}
```

## Verification Queries

### Check Transaction Created
```sql
SELECT 
    transaction_id,
    pg_transaction_id,
    status,
    amount,
    currency,
    approval_number,
    approved_at,
    created_at
FROM transactions
WHERE tid = 'ktest6111m01032304111003000874'
ORDER BY created_at DESC;
```

### Check Transaction Events
```sql
SELECT 
    id,
    event_type,
    event_sequence,
    amount,
    previous_status,
    new_status,
    occurred_at,
    created_at
FROM transaction_events
WHERE tid = 'ktest6111m01032304111003000874'
ORDER BY event_sequence ASC;
```

### Check Settlements Created
```sql
SELECT 
    s.id,
    s.entity_type,
    s.entity_id,
    s.entry_type,
    s.amount,
    s.fee_type,
    s.created_at
FROM settlements s
JOIN transaction_events te ON s.transaction_event_id = te.id
WHERE te.tid = 'ktest6111m01032304111003000874'
ORDER BY s.created_at;
```

## Unit Test Examples

### Test Signature Verification

```java
@SpringBootTest
class WebhookSignatureVerifierTest {

    @Autowired
    private WebhookSignatureVerifier verifier;

    @Test
    void testKorpaySignatureVerification_Success() {
        String rawBody = "tid=test123&mid=ktest6111m&amt=1000";
        String secret = "test-secret";
        Map<String, String> headers = Map.of(
            "X-Korpay-Signature", "computed-hmac-sha256-signature"
        );

        assertDoesNotThrow(() -> 
            verifier.verifyKorpaySignature(rawBody, headers, secret)
        );
    }

    @Test
    void testKorpaySignatureVerification_InvalidSignature() {
        String rawBody = "tid=test123&mid=ktest6111m&amt=1000";
        String secret = "test-secret";
        Map<String, String> headers = Map.of(
            "X-Korpay-Signature", "invalid-signature"
        );

        assertThrows(SignatureVerificationFailedException.class, () ->
            verifier.verifyKorpaySignature(rawBody, headers, secret)
        );
    }

    @Test
    void testKorpaySignatureVerification_MissingHeader() {
        String rawBody = "tid=test123&mid=ktest6111m&amt=1000";
        String secret = "test-secret";
        Map<String, String> headers = Map.of();

        assertThrows(SignatureVerificationFailedException.class, () ->
            verifier.verifyKorpaySignature(rawBody, headers, secret)
        );
    }
}
```

### Test KORPAY Adapter Parsing

```java
@SpringBootTest
class KorpayWebhookAdapterTest {

    @Autowired
    private KorpayWebhookAdapter adapter;

    @Test
    void testParseApprovalWebhook() {
        String rawBody = "tid=ktest6111m01032304111003000874" +
            "&mid=ktest6111m" +
            "&catId=1234567890" +
            "&amt=1000" +
            "&cancelYN=N" +
            "&appDtm=20230411100300";
        Map<String, String> headers = Map.of();

        TransactionDto dto = adapter.parse(rawBody, headers);

        assertEquals("ktest6111m01032304111003000874", dto.getPgTid());
        assertEquals("ktest6111m", dto.getPgMerchantNo());
        assertEquals("1234567890", dto.getTerminalId());
        assertEquals(1000L, dto.getAmount());
        assertEquals(EventType.APPROVAL, dto.getEventType());
        assertFalse(dto.getIsCancel());
    }

    @Test
    void testParseCancelWebhook() {
        String rawBody = "tid=ktest6111m01032304111003000874" +
            "&mid=ktest6111m" +
            "&catId=1234567890" +
            "&amt=1000" +
            "&remainAmt=0" +
            "&cancelYN=Y" +
            "&appDtm=20230411100300" +
            "&ccDnt=20230411102609";
        Map<String, String> headers = Map.of();

        TransactionDto dto = adapter.parse(rawBody, headers);

        assertEquals(EventType.CANCEL, dto.getEventType());
        assertTrue(dto.getIsCancel());
        assertEquals(0L, dto.getRemainAmount());
        assertNotNull(dto.getCanceledAt());
    }

    @Test
    void testParsePartialCancelWebhook() {
        String rawBody = "tid=ktest6111m01032304111003000874" +
            "&mid=ktest6111m" +
            "&catId=1234567890" +
            "&amt=1000" +
            "&remainAmt=600" +
            "&cancelYN=Y" +
            "&appDtm=20230411100300" +
            "&ccDnt=20230411102609";
        Map<String, String> headers = Map.of();

        TransactionDto dto = adapter.parse(rawBody, headers);

        assertEquals(EventType.PARTIAL_CANCEL, dto.getEventType());
        assertTrue(dto.getIsCancel());
        assertEquals(600L, dto.getRemainAmount());
    }
}
```

### Test Transaction Service

```java
@SpringBootTest
@Transactional
class TransactionServiceTest {

    @Autowired
    private TransactionService transactionService;

    @MockBean
    private PaymentMethodRepository paymentMethodRepository;

    @MockBean
    private CardCompanyRepository cardCompanyRepository;

    @Test
    void testCreateTransactionFromWebhook() {
        // Arrange
        MerchantPgMapping mapping = createTestMerchantPgMapping();
        TransactionDto dto = createTestApprovalDto();

        when(paymentMethodRepository.findByMethodCode("CARD"))
            .thenReturn(Optional.of(createTestPaymentMethod()));
        when(cardCompanyRepository.findByCompanyCode("02"))
            .thenReturn(Optional.of(createTestCardCompany()));

        // Act
        Transaction transaction = transactionService.createOrUpdateFromWebhook(dto, mapping);

        // Assert
        assertNotNull(transaction.getId());
        assertEquals("TXN-" + dto.getPgTid(), transaction.getTransactionId());
        assertEquals(TransactionStatus.APPROVED, transaction.getStatus());
        assertEquals(dto.getAmount(), transaction.getAmount());
    }

    @Test
    void testDuplicateApprovalThrowsException() {
        // Arrange
        MerchantPgMapping mapping = createTestMerchantPgMapping();
        TransactionDto dto = createTestApprovalDto();

        // Create first transaction
        transactionService.createOrUpdateFromWebhook(dto, mapping);

        // Act & Assert - Second attempt should throw
        assertThrows(DuplicateTransactionException.class, () ->
            transactionService.createOrUpdateFromWebhook(dto, mapping)
        );
    }

    @Test
    void testCancelTransaction() {
        // Arrange - Create approval first
        MerchantPgMapping mapping = createTestMerchantPgMapping();
        TransactionDto approvalDto = createTestApprovalDto();
        Transaction transaction = transactionService.createOrUpdateFromWebhook(approvalDto, mapping);

        // Act - Cancel transaction
        TransactionDto cancelDto = createTestCancelDto();
        Transaction updated = transactionService.createOrUpdateFromWebhook(cancelDto, mapping);

        // Assert
        assertEquals(TransactionStatus.CANCELLED, updated.getStatus());
        assertEquals(0L, updated.getAmount());
        assertNotNull(updated.getCancelledAt());
    }
}
```

## Integration Test Example

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class WebhookControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private TransactionEventRepository transactionEventRepository;

    @Test
    void testKorpayWebhookEndToEnd() throws Exception {
        // Arrange
        String pgConnectionId = "test-pg-connection-id";
        String webhookSecret = "test-secret";
        String rawBody = buildKorpayApprovalBody();
        String signature = computeSignature(rawBody, webhookSecret);

        // Act
        mockMvc.perform(post("/api/webhook/korpay")
                .param("pgConnectionId", pgConnectionId)
                .param("webhookSecret", webhookSecret)
                .header("X-Korpay-Signature", signature)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .content(rawBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.transactionId").exists());

        // Assert
        Optional<Transaction> transaction = transactionRepository
            .findByCatIdAndTid("1234567890", "ktest6111m01032304111003000874");
        assertTrue(transaction.isPresent());
        assertEquals(TransactionStatus.APPROVED, transaction.get().getStatus());

        List<TransactionEvent> events = transactionEventRepository
            .findByTransactionIdOrderByEventSequenceAsc(transaction.get().getId());
        assertEquals(1, events.size());
        assertEquals(EventType.APPROVAL, events.get(0).getEventType());
    }

    private String buildKorpayApprovalBody() {
        return "tid=ktest6111m01032304111003000874" +
               "&mid=ktest6111m" +
               "&catId=1234567890" +
               "&amt=1000" +
               "&cancelYN=N" +
               "&appDtm=20230411100300";
    }

    private String computeSignature(String data, String secret) {
        // HMAC-SHA256 computation
        // ...
    }
}
```

## Monitoring and Debugging

### Enable Debug Logging

```yaml
logging:
  level:
    com.korpay.billpay.service.webhook: DEBUG
    com.korpay.billpay.controller.WebhookController: DEBUG
```

### Check Application Logs

```bash
# Tail logs in real-time
tail -f logs/application.log | grep -i webhook

# Search for specific transaction
grep "ktest6111m01032304111003000874" logs/application.log

# Check for errors
grep -i "error\|exception" logs/application.log | grep webhook
```

### Webhook Processing Metrics

Monitor these metrics:
- Webhook requests per minute
- Success rate (2xx responses)
- Signature verification failures (400)
- Processing errors (500)
- Average processing time
- Duplicate transaction rate

## Troubleshooting

### Issue: Signature Verification Failed

**Symptoms**: 400 Bad Request with "Signature verification failed"

**Possible Causes**:
1. Incorrect webhook secret
2. Body modified before signature verification
3. Character encoding mismatch
4. Header name case sensitivity

**Solution**:
```bash
# Verify signature computation
echo -n "$RAW_BODY" | openssl dgst -sha256 -hmac "$SECRET" -hex

# Check header name (case-sensitive)
curl -v ... | grep -i signature
```

### Issue: Merchant Mapping Not Found

**Symptoms**: 200 OK with "Unmapped transaction saved"

**Possible Causes**:
1. Merchant not registered in merchant_pg_mappings
2. Status not ACTIVE
3. Wrong mid value

**Solution**:
```sql
SELECT * FROM merchant_pg_mappings 
WHERE mid = 'ktest6111m' 
  AND pg_connection_id = 'your-uuid';
```

### Issue: Original Transaction Not Found (Cancel)

**Symptoms**: 500 Internal Server Error

**Possible Causes**:
1. Cancel webhook received before approval
2. Wrong tid in cancel webhook
3. Transaction deleted from database

**Solution**:
```sql
SELECT * FROM transactions 
WHERE cat_id = '1234567890' 
  AND tid = 'ktest6111m01032304111003000874';
```

## Performance Testing

### Load Test with Apache Bench

```bash
# 100 requests, 10 concurrent
ab -n 100 -c 10 \
   -p webhook_body.txt \
   -T "application/x-www-form-urlencoded" \
   -H "X-Korpay-Signature: your-signature" \
   "http://localhost:8080/api/webhook/korpay?pgConnectionId=xxx&webhookSecret=yyy"
```

### Expected Performance

- Response time: < 500ms (95th percentile)
- Throughput: > 100 requests/second
- Error rate: < 0.1%
- Database connections: Monitor pool utilization

## Security Testing

### Test Invalid Signature
```bash
curl -X POST ... -H 'X-Korpay-Signature: invalid-sig'
# Expected: 400 Bad Request
```

### Test Missing Signature
```bash
curl -X POST ... (without X-Korpay-Signature header)
# Expected: 400 Bad Request
```

### Test SQL Injection
```bash
curl -X POST ... --data-urlencode "mid='; DROP TABLE transactions; --"
# Expected: 200 OK or merchant not found, but no SQL error
```

### Test XSS
```bash
curl -X POST ... --data-urlencode "goodsName=<script>alert('xss')</script>"
# Expected: 200 OK, no script execution
```
