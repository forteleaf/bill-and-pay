# Webhook Deployment Checklist

## Pre-Deployment Checklist

### 1. Code Review ✓
- [x] All exception classes created and documented
- [x] DTO classes follow project conventions
- [x] Service classes use proper transaction boundaries
- [x] Controller has proper error handling
- [x] Signature verification uses timing-attack resistant comparison
- [x] No sensitive data in logs
- [x] All KORPAY field mappings per PRD-06
- [x] Idempotency implemented correctly

### 2. Database Preparation
- [ ] Run migration scripts
- [ ] Add missing method to TransactionEventRepository
- [ ] Create indexes on (cat_id, tid) for transactions table
- [ ] Verify pg_connections table has KORPAY entry
- [ ] Verify merchant_pg_mappings has test data
- [ ] Verify payment_methods has CARD entry
- [ ] Verify card_companies has issuer codes

### 3. Configuration
- [ ] Add cache configuration to application.yml
- [ ] Configure HMAC secret in environment variables
- [ ] Set up logging levels (DEBUG for initial deployment)
- [ ] Configure database connection pool
- [ ] Enable JPA auditing (@EnableJpaAuditing)
- [ ] Configure timezone handling (Asia/Seoul)

### 4. Security Review
- [ ] HTTPS/TLS enabled in production
- [ ] Webhook secret stored securely (not in code)
- [ ] Rate limiting configured on /api/webhook/* endpoints
- [ ] CORS policy reviewed
- [ ] Authentication/authorization bypassed for webhook endpoints
- [ ] SQL injection prevention verified
- [ ] XSS prevention verified

### 5. Testing
- [ ] Unit tests for WebhookSignatureVerifier
- [ ] Unit tests for KorpayWebhookAdapter
- [ ] Unit tests for TransactionService
- [ ] Integration test for full webhook flow
- [ ] Test with KORPAY sandbox environment
- [ ] Test signature verification with real secret
- [ ] Test duplicate handling (send same webhook twice)
- [ ] Test cancel and partial cancel flows
- [ ] Test unmapped merchant handling
- [ ] Test all error scenarios

### 6. Monitoring Setup
- [ ] Application metrics configured
- [ ] Log aggregation configured
- [ ] Alerting rules created
- [ ] Dashboard created for webhook metrics
- [ ] Error tracking configured (Sentry/similar)
- [ ] Database query monitoring enabled

## Deployment Steps

### Step 1: Database Migration

```sql
-- 1. Verify TransactionEventRepository method exists in code
-- 2. Run any pending Flyway migrations

-- 3. Verify indexes
CREATE INDEX IF NOT EXISTS idx_transactions_catid_tid 
ON transactions (cat_id, tid);

CREATE INDEX IF NOT EXISTS idx_merchant_pg_mappings_active 
ON merchant_pg_mappings (pg_connection_id, mid, status) 
WHERE status = 'ACTIVE';

-- 4. Insert test data (if not exists)
INSERT INTO pg_connections (
    pg_code, pg_name, webhook_path, webhook_secret, status
) VALUES (
    'KORPAY', 
    'KORPAY Payment Gateway', 
    '/api/webhook/tenant_001/KORPAY', 
    'YOUR_WEBHOOK_SECRET_HERE',
    'ACTIVE'
) ON CONFLICT (pg_code) DO NOTHING;

-- 5. Verify payment methods exist
SELECT * FROM payment_methods WHERE method_code = 'CARD';

-- 6. Verify card companies exist
SELECT * FROM card_companies WHERE company_code IN ('02', '06', '11');
```

### Step 2: Application Configuration

```yaml
# application.yml
spring:
  # Cache configuration
  cache:
    type: caffeine
    cache-names: merchantPgMappings
    caffeine:
      spec: maximumSize=1000,expireAfterWrite=10m
  
  # JPA configuration
  jpa:
    properties:
      hibernate:
        jdbc:
          time_zone: Asia/Seoul

# Logging
logging:
  level:
    com.korpay.billpay.controller.WebhookController: INFO
    com.korpay.billpay.service.webhook: INFO
    com.korpay.billpay.service.transaction: INFO
    # DEBUG for troubleshooting:
    # com.korpay.billpay.service.webhook.adapter: DEBUG
    # com.korpay.billpay.service.webhook.verifier: DEBUG

# Management endpoints
management:
  endpoints:
    web:
      exposure:
        include: health,metrics,prometheus
  metrics:
    tags:
      application: billpay
      environment: production
```

### Step 3: Build and Deploy

```bash
# 1. Build application
./gradlew clean build -x test

# 2. Run tests
./gradlew test

# 3. Deploy to staging
# (Your deployment process here)

# 4. Smoke test on staging
curl -X POST "http://staging.example.com/api/webhook/tenant_001/KORPAY?pgConnectionId=xxx&webhookSecret=yyy" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -H "X-Korpay-Signature: test-signature" \
  -d "tid=test123&mid=test&catId=test&amt=1000&cancelYN=N&appDtm=20240130120000"

# Expected: 400 (signature verification failed) or 200 (if mapping not found)

# 5. Deploy to production
# (Your deployment process here)
```

### Step 4: KORPAY Integration Setup

```bash
# 1. Register webhook URL with KORPAY
# Contact KORPAY support to register:
# URL: https://your-domain.com/api/webhook/tenant_001/KORPAY?pgConnectionId={UUID}&webhookSecret={SECRET}
# Method: POST
# Content-Type: application/x-www-form-urlencoded

# 2. Get webhook secret from KORPAY

# 3. Update pg_connections with actual secret
UPDATE pg_connections 
SET webhook_secret = 'ACTUAL_SECRET_FROM_KORPAY'
WHERE pg_code = 'KORPAY';

# 4. Test with KORPAY sandbox
# (KORPAY will provide test transaction flow)
```

### Step 5: Verification

```bash
# 1. Check application logs
tail -f logs/application.log | grep -i webhook

# 2. Send test webhook from KORPAY sandbox

# 3. Verify transaction created
psql -d billpay -c "
SELECT 
    transaction_id, 
    status, 
    amount, 
    pg_transaction_id,
    created_at 
FROM transactions 
ORDER BY created_at DESC 
LIMIT 5;
"

# 4. Verify transaction event created
psql -d billpay -c "
SELECT 
    event_type, 
    event_sequence, 
    amount, 
    new_status,
    created_at 
FROM transaction_events 
ORDER BY created_at DESC 
LIMIT 5;
"

# 5. Check metrics
curl http://localhost:8100/actuator/metrics/http.server.requests | jq '.measurements'
```

## Post-Deployment Monitoring

### First 24 Hours

#### Hour 1-4: Critical Monitoring
- [ ] Monitor webhook request rate
- [ ] Check error logs every 30 minutes
- [ ] Verify signature verification success rate > 95%
- [ ] Check response time < 500ms (p95)
- [ ] Monitor database connection pool
- [ ] Verify transaction creation rate matches webhook rate

#### Hour 4-24: Regular Monitoring
- [ ] Check error logs every 2 hours
- [ ] Monitor duplicate transaction rate
- [ ] Check unmapped merchant rate
- [ ] Verify no memory leaks
- [ ] Monitor database query performance
- [ ] Check cache hit rate

### Week 1: Stability Period
- [ ] Daily error log review
- [ ] Weekly performance report
- [ ] Monitor signature verification failures
- [ ] Check for any pattern in unmapped merchants
- [ ] Review database growth rate
- [ ] Optimize slow queries if any

### Metrics to Track

```sql
-- Daily webhook summary
SELECT 
    DATE(created_at) as date,
    COUNT(*) as total_transactions,
    COUNT(*) FILTER (WHERE status = 'APPROVED') as approvals,
    COUNT(*) FILTER (WHERE status = 'CANCELLED') as cancels,
    COUNT(*) FILTER (WHERE status = 'PARTIAL_CANCELLED') as partial_cancels,
    SUM(amount) as total_amount
FROM transactions
WHERE created_at >= NOW() - INTERVAL '7 days'
GROUP BY DATE(created_at)
ORDER BY date DESC;

-- Event sequence verification (should be continuous)
SELECT 
    transaction_id,
    array_agg(event_sequence ORDER BY event_sequence) as sequences,
    COUNT(*) as event_count
FROM transaction_events
WHERE created_at >= NOW() - INTERVAL '1 day'
GROUP BY transaction_id
HAVING array_length(array_agg(event_sequence ORDER BY event_sequence), 1) != MAX(event_sequence);
-- Should return 0 rows (all sequences are continuous)

-- Performance check
SELECT 
    pg_transaction_id,
    created_at,
    updated_at,
    EXTRACT(EPOCH FROM (updated_at - created_at)) as processing_seconds
FROM transactions
WHERE created_at >= NOW() - INTERVAL '1 hour'
ORDER BY processing_seconds DESC
LIMIT 10;
```

## Rollback Plan

### If Critical Issues Occur

```bash
# 1. Immediate: Disable webhook processing
# Option A: Stop application
systemctl stop billpay-application

# Option B: Route webhook traffic to maintenance page
# (Configure in load balancer/API gateway)

# 2. Notify KORPAY to pause webhook delivery
# Contact: support@korpay.com
# Message: "Please pause webhook delivery to {your-url} due to technical issues"

# 3. Investigate issues
# - Check application logs
# - Check database errors
# - Review recent transactions

# 4. Rollback if needed
git checkout previous-stable-version
./gradlew clean build
# Deploy previous version

# 5. Resume service
systemctl start billpay-application

# 6. Notify KORPAY to resume webhook delivery

# 7. Process missed webhooks
# Option A: Request replay from KORPAY
# Option B: Pull transactions via KORPAY API (if available)
```

## Known Issues and Workarounds

### Issue 1: High Database Load
**Symptom**: Slow webhook response time during peak hours

**Solution**:
- Increase database connection pool size
- Add read replica for queries
- Consider async settlement processing

### Issue 2: Signature Verification Intermittent Failures
**Symptom**: Random signature verification failures

**Solution**:
- Check character encoding (UTF-8)
- Verify no middleware modifying request body
- Check load balancer pass-through configuration

### Issue 3: Duplicate Transactions Not Detected
**Symptom**: Same webhook creates multiple transactions

**Solution**:
- Verify database index on (cat_id, tid)
- Check transaction isolation level
- Review findByCatIdAndTid query

## Support Contacts

### Internal Team
- Backend Team Lead: [Name] - [Email]
- Database Admin: [Name] - [Email]
- DevOps: [Name] - [Email]
- On-Call: [Phone Number]

### External Partners
- KORPAY Technical Support: support@korpay.com
- KORPAY Account Manager: [Name] - [Email]
- KORPAY Emergency: [Phone Number]

## Documentation Links

- [PRD-04: PG Integration](docs/PRD-04_pg_integration.md)
- [PRD-06: KORPAY Specs](docs/PRD-06_korpay.md)
- [Webhook Implementation](WEBHOOK_IMPLEMENTATION.md)
- [Testing Guide](WEBHOOK_TESTING_GUIDE.md)
- [Flow Diagram](WEBHOOK_FLOW_DIAGRAM.md)
- [API Documentation](http://localhost:8100/swagger-ui.html)

## Sign-Off

### Pre-Deployment Approval
- [ ] Technical Lead: _________________ Date: _______
- [ ] Product Owner: _________________ Date: _______
- [ ] Security Team: _________________ Date: _______
- [ ] DevOps Team: __________________ Date: _______

### Post-Deployment Verification
- [ ] All smoke tests passed: _________________ Date: _______
- [ ] 24-hour monitoring completed: ___________ Date: _______
- [ ] Week 1 review completed: _______________ Date: _______

### Production Ready
- [ ] System stable and performing within SLA
- [ ] All critical issues resolved
- [ ] Monitoring and alerting operational
- [ ] Documentation complete and up-to-date

**Final Approval**: _________________ Date: _______
