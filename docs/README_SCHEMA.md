# Bill&Pay Database Schema - Operations Guide

## Overview

Flyway 마이그레이션 실행 및 운영 가이드. 스키마 설계 상세는 [PRD-05: Database Schema](./PRD-05_database_schema.md) 참조.

## Directory Structure

```
src/main/resources/db/migration/
├── public/                              # 공유 인프라 (전체 테넌트 공통)
│   ├── V1__init_public_schema.sql       # tenants, pg_connections, holidays, organizations, users
│   ├── V2__seed_data.sql                # 초기 데이터 (테넌트, PG 연결 정보)
│   └── V3__demo_requests_table.sql      # demo_requests 테이블
└── tenant/                              # 테넌트별 스키마 (템플릿)
    ├── V1__init_tenant_schema.sql       # 21개 테이블 전체 생성
    ├── V2__seed_data.sql                # 참조 데이터 (payment_methods, card_issuers 등)
    └── V3__seed_test_data.sql           # 테스트 데이터
```

## Migration Execution

### Public Schema (최초 1회)

```bash
flyway -schemas=public \
  -locations=filesystem:src/main/resources/db/migration/public \
  migrate
```

### Tenant Schema (테넌트 생성 시마다)

```bash
flyway -schemas=tenant_korpay \
  -locations=filesystem:src/main/resources/db/migration/tenant \
  migrate
```

### Application-Level Tenant Creation

```java
public void createTenant(String tenantId) {
    // 1. 스키마 생성
    jdbcTemplate.execute("CREATE SCHEMA tenant_" + tenantId);
    
    // 2. Flyway 마이그레이션 실행
    Flyway flyway = Flyway.configure()
        .dataSource(dataSource)
        .schemas("tenant_" + tenantId)
        .locations("filesystem:src/main/resources/db/migration/tenant")
        .load();
    flyway.migrate();
    
    // 3. 테넌트 등록
    tenantRepository.save(new Tenant(tenantId, "tenant_" + tenantId));
}
```

## Critical Validations

운영 환경에서 반드시 확인해야 할 데이터 무결성 검증 쿼리.

### 1. Zero-Sum Validation (PRD-03)

```sql
SELECT * FROM settlement_zero_sum_validation WHERE is_zero_sum_valid = FALSE;
```
**Expected**: 0 rows

### 2. Orphaned Settlements

```sql
SELECT * FROM settlements 
WHERE transaction_event_id NOT IN (SELECT id FROM transaction_events);
```
**Expected**: 0 rows

### 3. Missing ltree Paths

```sql
SELECT * FROM organizations WHERE path IS NULL OR path = '';
```
**Expected**: 0 rows

### 4. KORPAY catId Validation

```sql
SELECT * FROM merchant_pg_mappings 
WHERE pg_connection_id IN (SELECT id FROM public.pg_connections WHERE pg_code = 'KORPAY')
  AND cat_id IS NULL;
```
**Expected**: 0 rows

## Performance Monitoring

### Index Usage

```sql
SELECT 
  schemaname, tablename, indexname, 
  idx_scan, idx_tup_read, idx_tup_fetch
FROM pg_stat_user_indexes
ORDER BY idx_scan DESC;
```

### Table Sizes

```sql
SELECT 
  schemaname, tablename,
  pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) AS size
FROM pg_tables
WHERE schemaname LIKE 'tenant_%'
ORDER BY pg_total_relation_size(schemaname||'.'||tablename) DESC;
```

### Partition Status

```sql
SELECT 
  parent.relname AS parent_table,
  child.relname AS partition_name,
  pg_size_pretty(pg_total_relation_size(child.oid)) AS size
FROM pg_inherits
JOIN pg_class parent ON pg_inherits.inhparent = parent.oid
JOIN pg_class child ON pg_inherits.inhrelid = child.oid
WHERE parent.relname = 'transaction_events'
ORDER BY child.relname;
```

## Partitioning Operations

### transaction_events 파티션 관리

**Type**: RANGE partitioning by `created_at` (daily)

#### 파티션 생성 (운영)

```sql
-- 내일 파티션 생성
SELECT create_transaction_events_partition(CURRENT_DATE + INTERVAL '1 day');

-- pg_cron 자동화 (권장)
SELECT cron.schedule('create_partition', '0 0 * * *', 
  $$SELECT create_transaction_events_partition(CURRENT_DATE + INTERVAL '7 days')$$);
```

#### 파티션 삭제 (2년 이상 보관 후)

```sql
DROP TABLE IF EXISTS transaction_events_20240101;
```

## Maintenance Tasks

### Daily
- [ ] 다음 날 파티션 생성 확인 (자동화)
- [ ] webhook_logs 처리 백로그 모니터링
- [ ] Zero-Sum validation 확인

### Weekly
- [ ] `ANALYZE;` 실행 (통계 업데이트)
- [ ] 슬로우 쿼리 로그 리뷰
- [ ] settlement_batches 완료 상태 확인

### Monthly
- [ ] 오래된 파티션 VACUUM: `VACUUM transaction_events_202601XX;`
- [ ] 오래된 파티션 아카이브 (별도 테이블스페이스)
- [ ] 미사용 인덱스 검토 및 제거

## Troubleshooting

### ltree Queries Not Working

**Error**: `operator does not exist: ltree <@ ltree`

**Solution**:
```sql
CREATE EXTENSION IF NOT EXISTS ltree;
```

### Partition Not Found

**Error**: `no partition of relation "transaction_events" found for row`

**Solution**:
```sql
CREATE TABLE transaction_events_20260131 PARTITION OF transaction_events
  FOR VALUES FROM ('2026-01-31') TO ('2026-02-01');
```

### Zero-Sum Violation

**Problem**: validation query returns rows

**Root Causes**:
1. 부분취소 비례 계산 오류
2. 반올림 차이
3. 정산 엔트리 누락

**Fix**: 해당 transaction_event 조사 → settlements 조정 → metadata에 문서화

## Security Notes

### Sensitive Data Encryption

| Field | Algorithm |
|-------|-----------|
| `users.password_hash` | bcrypt |
| `users.two_factor_secret` | AES-256 |
| `pg_connections.credentials` | AES-256 |
| `api_keys.key_hash` | SHA-256 |

### Row-Level Security (Future)

```sql
ALTER TABLE merchants ENABLE ROW LEVEL SECURITY;

CREATE POLICY merchant_access ON merchants
  USING (org_path <@ current_setting('app.user_org_path')::ltree);
```

## References

- [PRD-05: Database Schema](./PRD-05_database_schema.md) - 스키마 설계 상세
- [PRD-02: Organization Structure](./PRD-02_organization.md) - ltree 계층 구조
- [PRD-03: Ledger & Settlement](./PRD-03_ledger.md) - 복식부기 정산
- [PostgreSQL ltree](https://www.postgresql.org/docs/current/ltree.html)
- [Flyway Documentation](https://flywaydb.org/documentation/)

---

**Version**: 2.0  
**Last Updated**: 2026-02-05  
**Maintainer**: Bill&Pay Platform Team
