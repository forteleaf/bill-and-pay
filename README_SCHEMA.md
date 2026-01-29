# Bill&Pay Database Schema

## Overview

This directory contains Flyway migration scripts for the Bill&Pay multi-tenant settlement platform. The schema implements a **Schema-per-Tenant** multi-tenancy model with shared public infrastructure and isolated tenant data.

## Directory Structure

```
src/main/resources/db/migration/
├── public/          # Shared infrastructure (all tenants)
│   ├── V1__create_public_schema_foundation.sql
│   └── V2__seed_public_data.sql
└── tenant/          # Tenant-specific schemas (template)
    ├── V1__create_tenant_schema_foundation.sql
    ├── V2__create_tenant_payment_reference.sql
    ├── V3__create_tenant_transactions.sql
    ├── V4__create_tenant_settlements.sql
    ├── V5__create_tenant_fee_configurations.sql
    ├── V6__seed_tenant_reference_data.sql
    ├── V7__create_tenant_additional_tables.sql
    ├── V8__create_tenant_indexes_optimization.sql
    └── V9__create_tenant_constraints_validation.sql
```

## Database Architecture

### Multi-Tenancy Model

- **Public Schema**: Shared across all tenants
  - `tenants`: Tenant registry
  - `pg_connections`: PG company configurations
  - `holidays`: Business day calendar

- **Tenant Schemas**: Isolated per tenant (e.g., `tenant_korpay`, `tenant_acme`)
  - `organizations`: 5-level ltree hierarchy
  - `merchants`: Payment acceptance points
  - `transactions`: Current transaction state
  - `transaction_events`: Immutable event log (partitioned)
  - `settlements`: Double-entry ledger

### Key Technologies

| Feature | Technology | Purpose |
|---------|-----------|---------|
| Hierarchical Data | ltree | 5-level organization hierarchy with path queries |
| Time-ordered IDs | UUID v7 | Sequential UUIDs for better B-tree indexing |
| Event Sourcing | Partitioned Tables | Daily partitions on transaction_events |
| Double-Entry | settlements table | CREDIT for approval, DEBIT for cancel |
| Search | GiST, GIN | ltree path queries, JSONB searches |

## Schema Design Principles

### 1. ltree Hierarchy (PRD-02)

5-level organization structure:
```
DISTRIBUTOR (level 1)
  └─ AGENCY (level 2)
      └─ DEALER (level 3)
          └─ SELLER (level 4)
              └─ VENDOR (level 5)
```

**Path Format**: `dist_001.agcy_001.deal_001.sell_001.vend_001`

**Access Control**:
- Parent can view descendants: `path <@ 'dist_001'`
- Cannot view ancestors or siblings

### 2. Hybrid Event Sourcing (PRD-03)

**transactions** (Current State):
- Mutable, fast queries
- Updated on each status change
- Indexed for real-time lookups

**transaction_events** (Immutable Log):
- Partitioned by `created_at` (daily)
- Source of truth for settlements
- Never updated, only appended

### 3. Double-Entry Ledger (PRD-03)

**settlements** table follows double-entry bookkeeping:

| Event Type | Entry Type | Amount Sign |
|------------|-----------|-------------|
| APPROVAL | CREDIT | Positive |
| CANCEL | DEBIT | Negative |
| PARTIAL_CANCEL | DEBIT | Negative |

**Zero-Sum Rule**: For each `transaction_event`:
```sql
|event.amount| = SUM(settlement.amount)
```

Validated by `settlement_zero_sum_validation` view.

### 4. KORPAY Integration (PRD-04, PRD-06)

Per PRD-04 and PRD-06:
- **MID** (mid): 1:1 mapping with terminal ID
- **catId**: KORPAY terminal ID (stored in `merchant_pg_mappings.cat_id`)
- **tid**: Transaction ID (stored in `transactions.tid`)
- **No GID/VID fields**: Not used per PRD-04

## Index Strategy

### GiST Indexes (ltree)
```sql
CREATE INDEX idx_organizations_path_gist ON organizations USING GIST(path);
```
Used for:
- Ancestor queries: `path <@ 'dist_001'`
- Descendant queries: `path @> 'dist_001.agcy_001'`
- LCA queries: `lca(path1, path2)`

### Partial Indexes (Status Filters)
```sql
CREATE INDEX idx_organizations_status ON organizations(status) WHERE status = 'ACTIVE';
```
Reduces index size, improves write performance.

### Covering Indexes (Index-Only Scans)
```sql
CREATE INDEX idx_transactions_amount_report ON transactions(created_at, status) 
  INCLUDE (amount, currency, merchant_id);
```
Enables index-only scans without table access.

### GIN Indexes (JSONB)
```sql
CREATE INDEX idx_webhook_logs_payload_gin ON webhook_logs USING GIN(payload);
```
For searching within JSONB columns.

## Partitioning Strategy

### transaction_events Partitioning

**Type**: RANGE partitioning by `created_at`  
**Interval**: Daily partitions  
**Naming**: `transaction_events_YYYYMMDD`

**Initial Partitions**: 7 days (today + 6 days ahead)

**Production Setup**:
1. Automate partition creation with `pg_cron`:
```sql
SELECT create_transaction_events_partition(CURRENT_DATE + INTERVAL '7 days');
```

2. Automate partition dropping (retention policy):
```sql
DROP TABLE IF EXISTS transaction_events_20250101; -- After 2+ years
```

**Performance Notes**:
- Indexes are created on the parent table
- Query planner automatically prunes irrelevant partitions
- Partitions can be archived to separate tablespace

## Data Types

### Amount Storage
**Type**: `BIGINT`  
**Unit**: Smallest currency unit (KRW = won, no decimals)

Example:
- ₩10,000 → `10000`
- $10.50 → `1050` (cents)

**Why BIGINT?**
- Exact precision (no floating-point errors)
- Fast arithmetic operations
- Standard practice for financial systems

### Decimal Precision
**Type**: `DECIMAL(10, 6)` for fee rates

Example:
- 3% → `0.030000`
- 2.5% → `0.025000`

### JSONB Usage

Used for flexible, schema-less data:
- `config`: Feature flags, settings
- `metadata`: Additional context, PG responses
- `tier_config`: Tiered fee structures
- `scopes`: Array of permissions

## Migration Execution

### Public Schema (One-Time)
```bash
flyway -schemas=public -locations=filesystem:src/main/resources/db/migration/public migrate
```

### Tenant Schema (Per Tenant)
```bash
flyway -schemas=tenant_korpay -locations=filesystem:src/main/resources/db/migration/tenant migrate
```

### Application-Level Tenant Creation
```java
public void createTenant(String tenantId) {
    // 1. Create schema
    jdbcTemplate.execute("CREATE SCHEMA tenant_" + tenantId);
    
    // 2. Run Flyway migrations
    Flyway flyway = Flyway.configure()
        .dataSource(dataSource)
        .schemas("tenant_" + tenantId)
        .locations("filesystem:src/main/resources/db/migration/tenant")
        .load();
    flyway.migrate();
    
    // 3. Register tenant
    tenantRepository.save(new Tenant(tenantId, "tenant_" + tenantId));
}
```

## Critical Validations

### 1. Zero-Sum Validation (PRD-03)
```sql
SELECT * FROM settlement_zero_sum_validation WHERE is_zero_sum_valid = FALSE;
```
Should return **zero rows** in production.

### 2. Orphaned Settlements
```sql
SELECT * FROM settlements WHERE transaction_event_id NOT IN (SELECT id FROM transaction_events);
```
Should return **zero rows** (referential integrity).

### 3. Missing ltree Paths
```sql
SELECT * FROM organizations WHERE path IS NULL OR path = '';
```
Should return **zero rows** (ltree path required).

### 4. KORPAY catId Validation
```sql
SELECT * FROM merchant_pg_mappings 
WHERE pg_connection_id IN (SELECT id FROM public.pg_connections WHERE pg_code = 'KORPAY')
  AND cat_id IS NULL;
```
Should return **zero rows** (catId required for KORPAY).

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

## Maintenance Tasks

### Daily
1. Create tomorrow's partition (automated via pg_cron)
2. Monitor webhook processing backlog
3. Check Zero-Sum validation

### Weekly
1. Analyze table statistics: `ANALYZE;`
2. Review slow query log
3. Check settlement batch completion

### Monthly
1. Vacuum old partitions: `VACUUM transaction_events_202601XX;`
2. Archive old partitions to separate tablespace
3. Review index usage and drop unused indexes

## Security Considerations

### Row-Level Security (Future Enhancement)
```sql
ALTER TABLE merchants ENABLE ROW LEVEL SECURITY;

CREATE POLICY merchant_access ON merchants
  USING (org_path <@ current_setting('app.user_org_path')::ltree);
```

### Sensitive Data
- **Encrypt at Application Layer**: 
  - `users.password_hash`: bcrypt
  - `users.two_factor_secret`: AES-256
  - `pg_connections.credentials`: AES-256
  - `api_keys.key_hash`: SHA-256

- **Never Store Plaintext**:
  - Passwords
  - API secrets
  - Webhook secrets

## Troubleshooting

### ltree Queries Not Working
**Problem**: `ERROR: operator does not exist: ltree <@ ltree`

**Solution**: Ensure ltree extension is enabled:
```sql
CREATE EXTENSION IF NOT EXISTS ltree;
```

### Partition Not Found
**Problem**: `ERROR: no partition of relation "transaction_events" found for row`

**Solution**: Create missing partition:
```sql
CREATE TABLE transaction_events_20260131 PARTITION OF transaction_events
  FOR VALUES FROM ('2026-01-31') TO ('2026-02-01');
```

### Zero-Sum Violation
**Problem**: `SELECT * FROM settlement_zero_sum_validation WHERE is_zero_sum_valid = FALSE;` returns rows

**Root Causes**:
1. Partial cancellation proration error
2. Rounding discrepancy
3. Missing settlement entries

**Fix**: Investigate specific transaction_event, adjust settlements, document in metadata.

## References

- [PRD-05: Database Schema](../../docs/PRD-05_database_schema.md)
- [PRD-02: Organization Structure](../../docs/PRD-02_organization.md)
- [PRD-03: Ledger & Settlement](../../docs/PRD-03_ledger.md)
- [PRD-04: PG Integration](../../docs/PRD-04_pg_integration.md)
- [PostgreSQL ltree Documentation](https://www.postgresql.org/docs/current/ltree.html)
- [Flyway Documentation](https://flywaydb.org/documentation/)

---

**Version**: 1.0  
**Last Updated**: 2026-01-30  
**Maintainer**: Bill&Pay Platform Team
