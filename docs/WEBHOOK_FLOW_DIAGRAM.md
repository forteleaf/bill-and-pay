# Webhook Processing Flow Diagram

## Complete Flow

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              KORPAY PG System                                │
│                                                                              │
│  Transaction Occurs → Generate Webhook → Sign with HMAC-SHA256              │
└────────────────────────────────┬────────────────────────────────────────────┘
                                 │
                                 │ POST /api/webhook/korpay
                                 │ Content-Type: application/x-www-form-urlencoded
                                 │ X-Korpay-Signature: {hmac}
                                 │
                                 ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                           WebhookController                                  │
│  POST /api/webhook/{pgCode}?pgConnectionId={uuid}&webhookSecret={secret}    │
└────────────────────────────────┬────────────────────────────────────────────┘
                                 │
                                 │ 1. Extract raw body (for signature)
                                 │ 2. Extract all headers as Map
                                 │ 3. Validate required params
                                 │
                                 ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                       WebhookProcessingService                               │
│                         @Transactional                                       │
└────────────────────────────────┬────────────────────────────────────────────┘
                                 │
                                 ├──────► Get PgWebhookAdapter by pgCode
                                 │        (KORPAY → KorpayWebhookAdapter)
                                 │
                                 ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                    Step 1: Signature Verification                            │
│                     WebhookSignatureVerifier                                 │
└────────────────────────────────┬────────────────────────────────────────────┘
                                 │
                                 ├─ Extract X-Korpay-Signature from headers
                                 ├─ Compute HMAC-SHA256(rawBody, secret)
                                 ├─ Compare with timing-attack resistance
                                 │
                                 ├─ [INVALID] → SignatureVerificationFailedException
                                 │             → 400 Bad Request (NO RETRY)
                                 │
                                 ▼ [VALID]
┌─────────────────────────────────────────────────────────────────────────────┐
│                      Step 2: Parse Webhook Data                              │
│                      KorpayWebhookAdapter.parse()                            │
└────────────────────────────────┬────────────────────────────────────────────┘
                                 │
                                 ├─ Parse form-encoded body
                                 ├─ Map KORPAY fields → TransactionDto
                                 ├─ Determine event type:
                                 │  • cancelYN='N' → APPROVAL
                                 │  • cancelYN='Y' + remainAmt=0 → CANCEL
                                 │  • cancelYN='Y' + remainAmt>0 → PARTIAL_CANCEL
                                 ├─ Parse datetime (yyyyMMddHHmmss, Asia/Seoul)
                                 │
                                 ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                   Step 3: Merchant Mapping Lookup                            │
│                      MerchantMappingService                                  │
│                         @Cacheable                                           │
└────────────────────────────────┬────────────────────────────────────────────┘
                                 │
                                 ├─ Find by pgConnectionId + mid
                                 ├─ Filter by status = ACTIVE
                                 ├─ Cache result (10 min TTL)
                                 │
                                 ├─ [NOT FOUND] → MerchantMappingNotFoundException
                                 │                → Save to unmapped_transactions
                                 │                → 200 OK "Unmapped transaction saved"
                                 │
                                 ▼ [FOUND]
┌─────────────────────────────────────────────────────────────────────────────┐
│                  Step 4: Transaction Create/Update                           │
│                          TransactionService                                  │
└────────────────────────────────┬────────────────────────────────────────────┘
                                 │
                                 ├─ Check if transaction exists (catId + tid)
                                 │
                                 ├─── [EXISTS + APPROVAL event]
                                 │    → DuplicateTransactionException
                                 │    → 200 OK "Transaction already processed"
                                 │    (IDEMPOTENT - PG won't retry)
                                 │
                                 ├─── [EXISTS + CANCEL/PARTIAL_CANCEL]
                                 │    → Update transaction:
                                 │       • status → CANCELLED / PARTIAL_CANCELLED
                                 │       • amount → 0 / remainAmount
                                 │       • cancelledAt → now
                                 │
                                 └─── [NOT EXISTS + APPROVAL]
                                      → Create new transaction:
                                         • transactionId = "TXN-{pgTid}"
                                         • status = APPROVED
                                         • Link merchant, pg_mapping, payment_method
                                         • Store all fields
                                 │
                                 ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                    Step 5: Transaction Event Creation                        │
│                          TransactionService                                  │
└────────────────────────────────┬────────────────────────────────────────────┘
                                 │
                                 ├─ Calculate next event_sequence
                                 ├─ Create TransactionEvent (immutable):
                                 │  • id = transaction.id (UUID)
                                 │  • created_at = now
                                 │  • event_type = APPROVAL/CANCEL/PARTIAL_CANCEL
                                 │  • event_sequence = auto-increment
                                 │  • previous_status / new_status
                                 │  • Copy all transaction fields
                                 │  • Store metadata as JSONB
                                 │
                                 ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                    Step 6: Settlement Processing                             │
│                         SettlementService                                    │
│                       (TODO - Not Implemented)                               │
└────────────────────────────────┬────────────────────────────────────────────┘
                                 │
                                 ├─ Calculate fees for each entity:
                                 │  • DISTRIBUTOR (master)
                                 │  • AGENCY
                                 │  • DEALER
                                 │  • SELLER
                                 │  • VENDOR
                                 │
                                 ├─ Create Settlement entries (复式簿記):
                                 │  • CREDIT for each entity
                                 │  • Validate Zero-Sum
                                 │
                                 ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                         Step 7: Success Response                             │
│                              200 OK                                          │
└─────────────────────────────────────────────────────────────────────────────┘
                                 │
                                 ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│  {                                                                           │
│    "success": true,                                                          │
│    "message": "Webhook processed successfully",                              │
│    "transactionId": "TXN-ktest6111m01032304111003000874"                     │
│  }                                                                           │
└─────────────────────────────────────────────────────────────────────────────┘
```

## Error Handling Flow

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                            Error Scenarios                                   │
└─────────────────────────────────────────────────────────────────────────────┘

1. Signature Verification Failed
   └─► SignatureVerificationFailedException
       └─► 400 Bad Request
           └─► { "success": false, "message": "Signature verification failed" }
           └─► PG: NO RETRY (validation error)

2. Merchant Mapping Not Found
   └─► MerchantMappingNotFoundException
       └─► Catch → Save to unmapped_transactions
           └─► 200 OK
               └─► { "success": true, "message": "Unmapped transaction saved" }
               └─► PG: Success (no retry)

3. Duplicate Transaction (Approval)
   └─► DuplicateTransactionException
       └─► 200 OK (Idempotent)
           └─► { "success": true, "message": "Transaction already processed", "transactionId": "..." }
           └─► PG: Success (no retry)

4. Database Error / Processing Error
   └─► WebhookProcessingException
       └─► 500 Internal Server Error
           └─► { "success": false, "message": "Internal processing error" }
           └─► PG: RETRY (transient error)

5. Validation Error (Missing Required Field)
   └─► WebhookProcessingException
       └─► 400 Bad Request
           └─► { "success": false, "message": "Invalid webhook data" }
           └─► PG: NO RETRY (validation error)
```

## Database State Changes

```
[APPROVAL Event]
┌─────────────────────────────────────────────────────────────────────────────┐
│ transactions                                                                 │
├─────────────────────────────────────────────────────────────────────────────┤
│ INSERT:                                                                      │
│   id: UUID (generated)                                                       │
│   transaction_id: "TXN-{pgTid}"                                              │
│   status: APPROVED                                                           │
│   amount: 1000                                                               │
│   merchant_id: {from mapping}                                                │
│   merchant_path: {from merchant}                                             │
│   org_path: {from merchant}                                                  │
│   pg_transaction_id: {pgTid}                                                 │
│   approval_number: {appNo}                                                   │
│   approved_at: {transactedAt}                                                │
│   cat_id: {catId}                                                            │
│   tid: {pgTid}                                                               │
│   metadata: {JSONB}                                                          │
│   created_at: now()                                                          │
└─────────────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────────────┐
│ transaction_events                                                           │
├─────────────────────────────────────────────────────────────────────────────┤
│ INSERT:                                                                      │
│   id: {transaction.id}                                                       │
│   created_at: now()                                                          │
│   event_type: APPROVAL                                                       │
│   event_sequence: 1                                                          │
│   transaction_id: {transaction.id}                                           │
│   merchant_id: {merchant.id}                                                 │
│   merchant_path: {merchant path}                                             │
│   org_path: {org path}                                                       │
│   amount: 1000                                                               │
│   previous_status: NULL                                                      │
│   new_status: "APPROVED"                                                     │
│   occurred_at: {transactedAt}                                                │
│   ... (all transaction fields copied)                                        │
└─────────────────────────────────────────────────────────────────────────────┘

[CANCEL Event]
┌─────────────────────────────────────────────────────────────────────────────┐
│ transactions                                                                 │
├─────────────────────────────────────────────────────────────────────────────┤
│ UPDATE:                                                                      │
│   status: APPROVED → CANCELLED                                               │
│   amount: 1000 → 0                                                           │
│   cancelled_at: {canceledAt}                                                 │
│   updated_at: now()                                                          │
└─────────────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────────────┐
│ transaction_events                                                           │
├─────────────────────────────────────────────────────────────────────────────┤
│ INSERT:                                                                      │
│   id: {transaction.id}                                                       │
│   created_at: now()                                                          │
│   event_type: CANCEL                                                         │
│   event_sequence: 2                                                          │
│   amount: 1000                                                               │
│   previous_status: "APPROVED"                                                │
│   new_status: "CANCELLED"                                                    │
│   occurred_at: {canceledAt}                                                  │
│   ... (all transaction fields copied)                                        │
└─────────────────────────────────────────────────────────────────────────────┘

[PARTIAL_CANCEL Event]
┌─────────────────────────────────────────────────────────────────────────────┐
│ transactions                                                                 │
├─────────────────────────────────────────────────────────────────────────────┤
│ UPDATE:                                                                      │
│   status: APPROVED → PARTIAL_CANCELLED                                       │
│   amount: 1000 → 600 (remainAmount)                                          │
│   cancelled_at: {canceledAt}                                                 │
│   updated_at: now()                                                          │
└─────────────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────────────┐
│ transaction_events                                                           │
├─────────────────────────────────────────────────────────────────────────────┤
│ INSERT:                                                                      │
│   id: {transaction.id}                                                       │
│   created_at: now()                                                          │
│   event_type: PARTIAL_CANCEL                                                 │
│   event_sequence: 2                                                          │
│   amount: 400 (cancelled amount)                                             │
│   previous_status: "APPROVED"                                                │
│   new_status: "PARTIAL_CANCELLED"                                            │
│   occurred_at: {canceledAt}                                                  │
│   ... (all transaction fields copied)                                        │
└─────────────────────────────────────────────────────────────────────────────┘
```

## Sequence Diagram

```
┌──────┐          ┌────────┐          ┌────────┐          ┌────────┐          ┌────────┐
│KORPAY│          │Webhook │          │Webhook │          │Merchant│          │Transaction
│  PG  │          │Controller         │Processing│        │Mapping │          │Service │
└──┬───┘          └───┬────┘          └───┬────┘          └───┬────┘          └───┬────┘
   │                  │                   │                   │                   │
   │ POST /webhook    │                   │                   │                   │
   │ X-Korpay-Sig     │                   │                   │                   │
   ├─────────────────►│                   │                   │                   │
   │                  │                   │                   │                   │
   │                  │ processWebhook()  │                   │                   │
   │                  ├──────────────────►│                   │                   │
   │                  │                   │                   │                   │
   │                  │                   │ verifySignature() │                   │
   │                  │                   ├───────────────────┤                   │
   │                  │                   │                   │                   │
   │                  │                   │ parse()           │                   │
   │                  │                   ├───────────────────┤                   │
   │                  │                   │                   │                   │
   │                  │                   │ findByPgCodeAndMerchantNo()          │
   │                  │                   ├──────────────────►│                   │
   │                  │                   │                   │                   │
   │                  │                   │ MerchantPgMapping │                   │
   │                  │                   │◄──────────────────┤                   │
   │                  │                   │                   │                   │
   │                  │                   │ createOrUpdateFromWebhook()          │
   │                  │                   ├──────────────────────────────────────►│
   │                  │                   │                                       │
   │                  │                   │                      Transaction      │
   │                  │                   │◄──────────────────────────────────────┤
   │                  │                   │                                       │
   │                  │                   │ createTransactionEvent()              │
   │                  │                   ├──────────────────────────────────────►│
   │                  │                   │                                       │
   │                  │                   │                      TransactionEvent │
   │                  │                   │◄──────────────────────────────────────┤
   │                  │                   │                                       │
   │                  │  WebhookResponse  │                                       │
   │                  │◄──────────────────┤                                       │
   │                  │                   │                                       │
   │  200 OK          │                   │                                       │
   │  { success:true }│                   │                                       │
   │◄─────────────────┤                   │                                       │
   │                  │                   │                                       │
```

## Component Dependencies

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                            Component Architecture                            │
└─────────────────────────────────────────────────────────────────────────────┘

WebhookController
    │
    └──► WebhookProcessingService
            │
            ├──► PgWebhookAdapter (interface)
            │       └──► KorpayWebhookAdapter (impl)
            │               └──► WebhookSignatureVerifier
            │
            ├──► MerchantMappingService
            │       └──► MerchantPgMappingRepository
            │
            └──► TransactionService
                    ├──► TransactionRepository
                    ├──► TransactionEventRepository
                    ├──► PaymentMethodRepository
                    └──► CardCompanyRepository

Exception Hierarchy:

WebhookException (base)
    ├──► SignatureVerificationFailedException
    ├──► MerchantMappingNotFoundException
    ├──► DuplicateTransactionException
    └──► WebhookProcessingException

DTO Classes:

TransactionDto (common format for all PGs)
WebhookResponse (standard response)
KorpayWebhookData (KORPAY-specific parsing)
```

## Configuration Flow

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                       Configuration Requirements                             │
└─────────────────────────────────────────────────────────────────────────────┘

1. Database Setup
   ├─ Create pg_connections record
   │  └─ pg_code: KORPAY
   │     webhook_path: /api/webhook/korpay
   │     webhook_secret: {HMAC secret}
   │     status: ACTIVE
   │
   ├─ Create merchant_pg_mappings records
   │  └─ For each merchant:
   │     mid: {KORPAY MID}
   │     cat_id: {Terminal ID}
   │     pg_connection_id: {from pg_connections}
   │     status: ACTIVE
   │
   └─ Create reference data
      ├─ payment_methods (CARD, etc.)
      └─ card_companies (BC, NH, etc.)

2. Application Configuration
   ├─ application.yml
   │  └─ Cache configuration (Caffeine)
   │
   └─ KORPAY PG Configuration
      └─ Register webhook URL with KORPAY:
         https://your-domain.com/api/webhook/korpay?pgConnectionId={uuid}&webhookSecret={secret}

3. Security Configuration
   ├─ HTTPS/TLS required in production
   ├─ Rate limiting on webhook endpoint
   ├─ IP whitelist (optional)
   └─ Monitor signature verification failures
```

## Monitoring Points

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         Monitoring & Observability                           │
└─────────────────────────────────────────────────────────────────────────────┘

Metrics to Track:
├─ Webhook Request Rate (req/min)
├─ Success Rate (%)
├─ Response Time (p50, p95, p99)
├─ Signature Verification Failure Rate
├─ Duplicate Transaction Rate
├─ Unmapped Merchant Rate
├─ Processing Error Rate
└─ Database Query Performance

Log Points (with transaction ID):
├─ INFO: Webhook received
├─ INFO: Signature verified
├─ INFO: Transaction parsed
├─ INFO: Merchant mapping found
├─ INFO: Transaction created/updated
├─ INFO: Transaction event created
├─ WARN: Signature verification failed
├─ WARN: Merchant mapping not found
├─ ERROR: Processing error
└─ ERROR: Database error

Alerts:
├─ Signature failure rate > 5%
├─ Processing error rate > 1%
├─ Response time p95 > 1s
├─ Unmapped merchant rate > 10%
└─ Zero webhooks received for > 5 min (during business hours)
```

## Data Consistency Guarantees

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                          ACID Guarantees                                     │
└─────────────────────────────────────────────────────────────────────────────┘

@Transactional Boundary:
┌─────────────────────────────────────────────────────────────────────────────┐
│  BEGIN TRANSACTION                                                           │
│                                                                              │
│  1. Verify signature                                                         │
│  2. Parse webhook data                                                       │
│  3. Lookup merchant mapping (cached)                                         │
│  4. Create/Update transaction                                                │
│  5. Create transaction event                                                 │
│  6. [TODO] Create settlements                                                │
│                                                                              │
│  COMMIT  (all or nothing)                                                    │
└─────────────────────────────────────────────────────────────────────────────┘

Idempotency:
├─ Same approval webhook → DuplicateTransactionException → 200 OK
├─ Same cancel webhook → Update same transaction → 200 OK
└─ Guaranteed by (cat_id, tid) uniqueness

Event Sourcing:
├─ Transaction: Current state (mutable)
├─ TransactionEvent: Event history (immutable)
└─ Event sequence: Auto-incrementing, never reused

Consistency Checks:
├─ Transaction.amount matches sum of events
├─ Transaction.status matches last event.new_status
└─ Event sequence is continuous (1, 2, 3, ...)
```
