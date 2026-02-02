# Tenant-Aware Webhook URL Implementation (방안1)

## TL;DR

> **Quick Summary**: Add tenantId to webhook URLs for proper multi-tenant isolation. Backend computes full webhook URLs in DTO. Support both old and new URL patterns for backward compatibility.
> 
> **Deliverables**:
> - New webhook endpoint: `POST /webhook/{tenantId}/{pgCode}`
> - Backward compatible old endpoint with deprecation warning
> - `tenantId` column added to `pg_connections` table
> - `generatedWebhookUrl` field in `PgConnectionDto`
> - Frontend displays both old and new webhook URLs with copy buttons
> 
> **Estimated Effort**: Medium (4-6 hours)
> **Parallel Execution**: YES - 3 waves
> **Critical Path**: Task 1 (DB migration) → Task 2 (Entity) → Task 3 (Controller) → Task 5 (Frontend)

---

## Context

### Original Request
Implement tenant-aware webhook URLs (방안1) for multi-tenant Spring Boot application where PG systems send webhook notifications but the system doesn't know which tenant the transaction belongs to.

### Interview Summary
**Key Discussions**:
- Backward compatibility: Dual support (old URL logs deprecation warning)
- URL generation: Backend computes using configurable `app.webhook.base-url`
- webhookSecret: Keep in query param (no breaking changes)
- Testing: Manual only with lsp_diagnostics verification
- Frontend: Display BOTH old and new URLs

**Research Findings**:
- `pg_connections` is in **public schema**, shared across tenants, NO `tenant_id` column exists
- `merchant_pg_mappings` is in **tenant schema**, references `pg_connection_id`
- `TenantContextHolder` has `runInTenant()` with validation pattern `^[a-z][a-z0-9_]{2,49}$`
- `TenantRoutingDataSource` has cached `tenantExistsInDatabase()` method
- For backward compatibility, we MUST add `tenant_id` to `pg_connections` to derive tenant from pgConnectionId

### Critical Discovery During Planning
The original user requirement stated "Old endpoint should REQUIRE pgConnectionId to derive tenant from the PgConnection entity". However, **pg_connections table does NOT have a tenant_id column**. This plan includes adding that column via Flyway migration.

---

## Work Objectives

### Core Objective
Enable multi-tenant webhook processing by embedding tenantId in the webhook URL path, with backward compatibility for existing PG integrations.

### Concrete Deliverables
1. `V3__add_tenant_id_to_pg_connections.sql` - Flyway migration for public schema
2. Modified `PgConnection.java` entity with `tenantId` field
3. Modified `PgConnectionDto.java` with `generatedWebhookUrl` computed field
4. New `WebhookController.java` endpoint `/{tenantId}/{pgCode}`
5. Enhanced `TenantService.java` with `validateTenantExists()` method
6. New `WebhookUrlGenerator.java` service
7. `application.yml` with `app.webhook.base-url` property
8. Modified `PgConnectionDetail.svelte` with webhook URL display
9. Updated `pgConnection.ts` types

### Definition of Done
- [ ] `curl POST /webhook/tenant_001/KORPAY?pgConnectionId=xxx&webhookSecret=yyy` → Processes in tenant_001 context
- [ ] `curl POST /webhook/KORPAY?pgConnectionId=xxx&webhookSecret=yyy` → Logs deprecation warning, processes correctly
- [ ] Frontend displays both webhook URLs with working copy buttons
- [ ] `lsp_diagnostics` shows 0 errors on all modified files

### Must Have
- TenantId validation using existing `^[a-z][a-z0-9_]{2,49}$` pattern
- Tenant existence check using cached `TenantRoutingDataSource` method
- Deprecation warning logged for old endpoint usage
- Copy-to-clipboard functionality for webhook URLs

### Must NOT Have (Guardrails)
- **NO breaking changes** to existing PG integrations
- **NO changes** to webhookSecret handling (keep in query param)
- **NO test infrastructure setup** (manual verification only)
- **NO modification** to `MerchantPgMapping` or tenant-schema tables
- **NO changes** to SecurityConfig (already permits `/webhook/**`)

---

## Verification Strategy (MANDATORY)

### Test Decision
- **Infrastructure exists**: NO
- **User wants tests**: NO (Manual verification)
- **Framework**: N/A
- **QA approach**: Manual verification with curl + lsp_diagnostics

### Manual Verification Procedures

**For each task, verification uses:**
- `lsp_diagnostics` for compile-time errors
- `curl` commands for API testing
- Browser inspection for frontend changes

---

## Execution Strategy

### Parallel Execution Waves

```
Wave 1 (Start Immediately):
├── Task 1: DB Migration (add tenant_id to pg_connections)
├── Task 4: WebhookUrlGenerator service
└── Task 6: application.yml config

Wave 2 (After Wave 1):
├── Task 2: PgConnection entity + TenantService enhancement
├── Task 3: WebhookController new endpoint
└── Task 7: PgConnectionDto with generatedWebhookUrl

Wave 3 (After Wave 2):
├── Task 5: Frontend PgConnectionDetail.svelte
└── Task 8: Frontend types update

Final: Integration verification
```

### Dependency Matrix

| Task | Depends On | Blocks | Can Parallelize With |
|------|------------|--------|---------------------|
| 1 | None | 2, 3, 7 | 4, 6 |
| 2 | 1 | 3, 7 | 4, 6 |
| 3 | 2 | 5 | 7 |
| 4 | None | 7 | 1, 6 |
| 5 | 3, 7 | None | 8 |
| 6 | None | 4, 7 | 1, 4 |
| 7 | 1, 2, 4, 6 | 5 | 3 |
| 8 | None | 5 | 1-7 |

### Agent Dispatch Summary

| Wave | Tasks | Recommended Approach |
|------|-------|---------------------|
| 1 | 1, 4, 6 | Parallel - independent foundation work |
| 2 | 2, 3, 7 | Sequential within wave (entity → controller → dto) |
| 3 | 5, 8 | Parallel - frontend work |

---

## TODOs

### Task 1: Database Migration - Add tenant_id to pg_connections

**What to do**:
- Create `V3__add_tenant_id_to_pg_connections.sql` in `src/main/resources/db/migration/public/`
- Add `tenant_id VARCHAR(50)` column (nullable initially for existing data)
- Add foreign key constraint to `tenants(id)`
- Add index on `tenant_id`
- Update comment on table

**Must NOT do**:
- Make tenant_id NOT NULL immediately (breaks existing data)
- Modify tenant schema migrations
- Add any seed data

**Recommended Agent Profile**:
- **Category**: `quick`
  - Reason: Single file creation, SQL migration
- **Skills**: [`git-master`]
  - `git-master`: For atomic commit after verification

**Parallelization**:
- **Can Run In Parallel**: YES
- **Parallel Group**: Wave 1 (with Tasks 4, 6)
- **Blocks**: Tasks 2, 3, 7
- **Blocked By**: None

**References**:
- `src/main/resources/db/migration/public/V1__create_public_schema_foundation.sql:77-108` - Existing pg_connections table structure
- `src/main/resources/db/migration/public/V1__create_public_schema_foundation.sql:43-69` - Tenants table (FK target)

**Acceptance Criteria**:

```sql
-- Verification query after migration:
SELECT column_name, data_type, is_nullable 
FROM information_schema.columns 
WHERE table_schema = 'public' AND table_name = 'pg_connections' AND column_name = 'tenant_id';
-- Expected: tenant_id | character varying | YES
```

```bash
# Agent runs Flyway migration check:
cd /Users/forteleaf/works/korpay/bill-and-pay
./gradlew flywayInfo
# Assert: V3__add_tenant_id_to_pg_connections.sql shows as Pending
```

**Commit**: YES
- Message: `feat(db): pg_connections 테이블에 tenant_id 컬럼 추가`
- Files: `src/main/resources/db/migration/public/V3__add_tenant_id_to_pg_connections.sql`
- Pre-commit: `lsp_diagnostics`

---

### Task 2: Update PgConnection Entity + TenantService Enhancement

**What to do**:
1. Add `tenantId` field to `PgConnection.java` entity
2. Add `validateTenantExists(String tenantId)` method to `TenantService.java`
3. Update `PgConnectionRepository.java` with `findByIdAndTenantId()` method

**Must NOT do**:
- Modify PgConnectionStatus enum
- Change existing field mappings
- Add validation annotations (controller handles it)

**Recommended Agent Profile**:
- **Category**: `quick`
  - Reason: Small entity modification
- **Skills**: None needed

**Parallelization**:
- **Can Run In Parallel**: NO
- **Parallel Group**: Wave 2 (sequential after Task 1)
- **Blocks**: Tasks 3, 7
- **Blocked By**: Task 1

**References**:
- `src/main/java/com/korpay/billpay/domain/entity/PgConnection.java:1-93` - Entity to modify
- `src/main/java/com/korpay/billpay/config/tenant/TenantService.java:1-37` - Service to enhance
- `src/main/java/com/korpay/billpay/config/tenant/TenantRoutingDataSource.java:88-110` - Pattern for tenant existence check
- `src/main/java/com/korpay/billpay/repository/PgConnectionRepository.java:1-21` - Repository to extend

**Acceptance Criteria**:

```bash
# lsp_diagnostics verification:
lsp_diagnostics src/main/java/com/korpay/billpay/domain/entity/PgConnection.java
# Assert: 0 errors

lsp_diagnostics src/main/java/com/korpay/billpay/config/tenant/TenantService.java
# Assert: 0 errors

lsp_diagnostics src/main/java/com/korpay/billpay/repository/PgConnectionRepository.java
# Assert: 0 errors
```

**Commit**: YES
- Message: `feat(entity): PgConnection에 tenantId 추가, TenantService에 validateTenantExists 메서드 추가`
- Files: `PgConnection.java`, `TenantService.java`, `PgConnectionRepository.java`
- Pre-commit: `lsp_diagnostics`

---

### Task 3: WebhookController - New Tenant-Aware Endpoint

**What to do**:
1. Add new endpoint `POST /{tenantId}/{pgCode}` to `WebhookController.java`
2. Validate tenantId format using TenantContextHolder pattern
3. Validate tenant exists using `TenantService.validateTenantExists()`
4. Wrap processing in `TenantContextHolder.runInTenant()`
5. Modify old endpoint to:
   - Log deprecation warning
   - Look up PgConnection by pgConnectionId
   - Extract tenantId from PgConnection
   - Delegate to new endpoint logic

**Must NOT do**:
- Change webhookSecret handling (keep in query param)
- Modify WebhookProcessingService
- Add new exception types (use existing)
- Modify SecurityConfig

**Recommended Agent Profile**:
- **Category**: `quick`
  - Reason: Controller modification with clear pattern
- **Skills**: None needed

**Parallelization**:
- **Can Run In Parallel**: NO
- **Parallel Group**: Wave 2
- **Blocks**: Task 5
- **Blocked By**: Task 2

**References**:
- `src/main/java/com/korpay/billpay/controller/WebhookController.java:1-75` - Controller to modify
- `src/main/java/com/korpay/billpay/config/tenant/TenantContextHolder.java:60-63` - runInTenant pattern
- `src/main/java/com/korpay/billpay/config/tenant/TenantContextHolder.java:101-109` - Validation pattern
- `src/main/java/com/korpay/billpay/exception/InvalidTenantException.java:1-69` - Exception for invalid format
- `src/main/java/com/korpay/billpay/exception/TenantNotFoundException.java:1-49` - Exception for missing tenant

**Acceptance Criteria**:

```bash
# lsp_diagnostics verification:
lsp_diagnostics src/main/java/com/korpay/billpay/controller/WebhookController.java
# Assert: 0 errors
```

```bash
# API verification (after app running):
# New endpoint:
curl -X POST "http://localhost:8080/webhook/tenant_001/KORPAY?pgConnectionId=xxx&webhookSecret=yyy" \
  -H "Content-Type: application/json" \
  -d '{"test": "data"}'
# Assert: Returns 200 OK or 400 (signature fail is OK, proves routing works)

# Old endpoint (deprecation):
curl -X POST "http://localhost:8080/webhook/KORPAY?pgConnectionId=xxx&webhookSecret=yyy" \
  -H "Content-Type: application/json" \
  -d '{"test": "data"}'
# Assert: Returns response + logs contain "DEPRECATED"
```

**Commit**: YES
- Message: `feat(webhook): 테넌트 인식 웹훅 엔드포인트 추가 및 기존 엔드포인트 deprecation 처리`
- Files: `WebhookController.java`
- Pre-commit: `lsp_diagnostics`

---

### Task 4: WebhookUrlGenerator Service

**What to do**:
1. Create `WebhookUrlGenerator.java` service in `src/main/java/com/korpay/billpay/service/webhook/`
2. Inject webhook base URL from config (`@Value("${app.webhook.base-url}")`)
3. Provide methods:
   - `generateNewUrl(String tenantId, String pgCode, UUID pgConnectionId, String webhookSecret)` → New format URL
   - `generateLegacyUrl(String pgCode, UUID pgConnectionId, String webhookSecret)` → Old format URL

**Must NOT do**:
- Add URL encoding (keep simple)
- Add validation logic (caller responsibility)
- Modify existing services

**Recommended Agent Profile**:
- **Category**: `quick`
  - Reason: New simple service class
- **Skills**: None needed

**Parallelization**:
- **Can Run In Parallel**: YES
- **Parallel Group**: Wave 1 (with Tasks 1, 6)
- **Blocks**: Task 7
- **Blocked By**: None

**References**:
- `src/main/java/com/korpay/billpay/service/webhook/WebhookProcessingService.java:1-30` - Package structure pattern
- `src/main/java/com/korpay/billpay/controller/WebhookController.java:25-31` - URL parameter structure

**Acceptance Criteria**:

```bash
# lsp_diagnostics verification:
lsp_diagnostics src/main/java/com/korpay/billpay/service/webhook/WebhookUrlGenerator.java
# Assert: 0 errors
```

**Commit**: YES
- Message: `feat(service): WebhookUrlGenerator 서비스 추가`
- Files: `WebhookUrlGenerator.java`
- Pre-commit: `lsp_diagnostics`

---

### Task 5: Frontend - PgConnectionDetail.svelte Webhook URL Display

**What to do**:
1. Add new Card section after "등록 정보" card (around line 487)
2. Display "Webhook URL 정보" section with:
   - New URL (recommended): `{generatedWebhookUrl}` from DTO
   - Legacy URL: computed from base + pgCode + id + webhookSecret
3. Add copy-to-clipboard buttons for each URL
4. Only show in view mode (not edit mode), when connection exists
5. Use shadcn-svelte Button, Card, Input components
6. Add helper text explaining old vs new URL

**Must NOT do**:
- Modify edit form fields
- Add new API calls
- Change existing form validation
- Use scoped `<style>` (Tailwind only)

**Recommended Agent Profile**:
- **Category**: `visual-engineering`
  - Reason: Frontend UI component work
- **Skills**: [`frontend-ui-ux`]
  - `frontend-ui-ux`: For proper shadcn-svelte patterns

**Parallelization**:
- **Can Run In Parallel**: YES
- **Parallel Group**: Wave 3 (with Task 8)
- **Blocks**: None
- **Blocked By**: Tasks 3, 7

**References**:
- `frontend/src/routes/pgConnection/PgConnectionDetail.svelte:469-487` - Location to add new section
- `frontend/src/routes/pgConnection/PgConnectionDetail.svelte:282-382` - Card component pattern
- `frontend/src/lib/components/ui/button/index.ts` - Button import
- `frontend/src/lib/components/ui/card/index.ts` - Card component imports

**Acceptance Criteria**:

```bash
# lsp_diagnostics verification:
lsp_diagnostics frontend/src/routes/pgConnection/PgConnectionDetail.svelte
# Assert: 0 errors (may have pre-existing errors in other files)
```

```
# Browser verification (via playwright):
1. Navigate to: http://localhost:5173
2. Login and go to PG Connection detail page
3. Assert: "Webhook URL 정보" card visible in view mode
4. Assert: Two URL input fields (read-only) with copy buttons
5. Click copy button
6. Assert: Toast shows "복사되었습니다" or similar
```

**Commit**: YES
- Message: `feat(frontend): PG 연결 상세에 Webhook URL 표시 및 복사 기능 추가`
- Files: `PgConnectionDetail.svelte`
- Pre-commit: `lsp_diagnostics`

---

### Task 6: application.yml - Webhook Base URL Configuration

**What to do**:
1. Add `app.webhook.base-url` property to `application.yml`
2. Add environment variable fallback `${WEBHOOK_BASE_URL:http://localhost:8080}`
3. Add comment explaining the property

**Must NOT do**:
- Add to application-prod.yml (doesn't exist)
- Modify existing properties
- Add validation

**Recommended Agent Profile**:
- **Category**: `quick`
  - Reason: Simple config addition
- **Skills**: None needed

**Parallelization**:
- **Can Run In Parallel**: YES
- **Parallel Group**: Wave 1 (with Tasks 1, 4)
- **Blocks**: Task 7
- **Blocked By**: None

**References**:
- `src/main/resources/application.yml` - Config file to modify

**Acceptance Criteria**:

```bash
# Verify config syntax:
cat src/main/resources/application.yml | grep -A2 "webhook"
# Assert: Shows app.webhook.base-url property
```

**Commit**: YES (group with Task 4)
- Message: `feat(config): Webhook base URL 설정 추가`
- Files: `application.yml`
- Pre-commit: YAML syntax check

---

### Task 7: PgConnectionDto - Add generatedWebhookUrl Field

**What to do**:
1. Add `generatedWebhookUrl` field to `PgConnectionDto.java`
2. Add `legacyWebhookUrl` field to `PgConnectionDto.java`
3. Modify `PgConnectionService.java` to inject `WebhookUrlGenerator` and compute URLs
4. Update `from(PgConnection entity)` to call generator

**Must NOT do**:
- Modify PgConnectionController
- Add tenant validation (service responsibility)
- Change existing DTO fields

**Recommended Agent Profile**:
- **Category**: `quick`
  - Reason: DTO and service modification
- **Skills**: None needed

**Parallelization**:
- **Can Run In Parallel**: YES
- **Parallel Group**: Wave 2 (with Task 3)
- **Blocks**: Task 5
- **Blocked By**: Tasks 1, 2, 4, 6

**References**:
- `src/main/java/com/korpay/billpay/dto/response/PgConnectionDto.java:1-45` - DTO to modify
- `src/main/java/com/korpay/billpay/service/pg/PgConnectionService.java` - Service to modify

**Acceptance Criteria**:

```bash
# lsp_diagnostics verification:
lsp_diagnostics src/main/java/com/korpay/billpay/dto/response/PgConnectionDto.java
# Assert: 0 errors

lsp_diagnostics src/main/java/com/korpay/billpay/service/pg/PgConnectionService.java
# Assert: 0 errors
```

```bash
# API verification:
curl -X GET "http://localhost:8080/api/v1/pg-connections" \
  -H "Authorization: Bearer xxx" \
  -H "X-Tenant-ID: tenant_001"
# Assert: Response contains "generatedWebhookUrl" and "legacyWebhookUrl" fields
```

**Commit**: YES
- Message: `feat(dto): PgConnectionDto에 generatedWebhookUrl, legacyWebhookUrl 필드 추가`
- Files: `PgConnectionDto.java`, `PgConnectionService.java`
- Pre-commit: `lsp_diagnostics`

---

### Task 8: Frontend Types - Update pgConnection.ts

**What to do**:
1. Add `generatedWebhookUrl?: string` to `PgConnectionDto` interface
2. Add `legacyWebhookUrl?: string` to `PgConnectionDto` interface

**Must NOT do**:
- Modify request types
- Add new exports
- Change existing type definitions

**Recommended Agent Profile**:
- **Category**: `quick`
  - Reason: Simple type addition
- **Skills**: None needed

**Parallelization**:
- **Can Run In Parallel**: YES
- **Parallel Group**: Wave 3 (with Task 5) or anytime
- **Blocks**: Task 5
- **Blocked By**: None

**References**:
- `frontend/src/types/pgConnection.ts:13-23` - Interface to modify

**Acceptance Criteria**:

```bash
# lsp_diagnostics verification:
lsp_diagnostics frontend/src/types/pgConnection.ts
# Assert: 0 errors
```

**Commit**: YES (group with Task 5)
- Message: `feat(types): PgConnectionDto에 webhook URL 필드 타입 추가`
- Files: `pgConnection.ts`
- Pre-commit: `lsp_diagnostics`

---

## Commit Strategy

| After Task(s) | Message | Files | Verification |
|---------------|---------|-------|--------------|
| 1 | `feat(db): pg_connections 테이블에 tenant_id 컬럼 추가` | V3__*.sql | flywayInfo |
| 2 | `feat(entity): PgConnection에 tenantId 추가, TenantService 확장` | PgConnection.java, TenantService.java, PgConnectionRepository.java | lsp_diagnostics |
| 3 | `feat(webhook): 테넌트 인식 웹훅 엔드포인트 추가` | WebhookController.java | lsp_diagnostics |
| 4, 6 | `feat(service): WebhookUrlGenerator 서비스 및 설정 추가` | WebhookUrlGenerator.java, application.yml | lsp_diagnostics |
| 7 | `feat(dto): PgConnectionDto에 webhook URL 필드 추가` | PgConnectionDto.java, PgConnectionService.java | lsp_diagnostics |
| 5, 8 | `feat(frontend): PG 연결 상세에 Webhook URL 표시 기능 추가` | PgConnectionDetail.svelte, pgConnection.ts | lsp_diagnostics |

---

## Success Criteria

### Verification Commands

```bash
# Backend build check:
./gradlew compileJava
# Expected: BUILD SUCCESSFUL

# Frontend type check:
cd frontend && bun run check
# Expected: No errors

# Full backend startup:
./gradlew bootRun
# Expected: Application starts without errors
```

### Final Checklist
- [ ] New endpoint `POST /webhook/{tenantId}/{pgCode}` processes webhooks in correct tenant context
- [ ] Old endpoint `POST /webhook/{pgCode}` logs deprecation warning and still works
- [ ] `pg_connections.tenant_id` column exists in database
- [ ] `PgConnectionDto` contains `generatedWebhookUrl` and `legacyWebhookUrl`
- [ ] Frontend displays both URLs with copy buttons
- [ ] All "Must NOT Have" guardrails respected (no breaking changes, no new test infra)
