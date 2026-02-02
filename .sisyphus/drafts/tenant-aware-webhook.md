# Draft: Tenant-Aware Webhook URL Implementation (방안1)

## Requirements (confirmed)

- **Core**: Add tenantId to webhook URL path for multi-tenant identification
- **New URL pattern**: `POST /webhook/{tenantId}/{pgCode}?pgConnectionId=xxx&webhookSecret=yyy`
- **Backward compatibility**: Support old URL pattern during transition
- **Frontend**: Display generated webhook URL with copy-to-clipboard

## Technical Decisions

### Backend Architecture (discovered from codebase)

1. **TenantContextHolder** (Java 25 ScopedValue)
   - Already has `runInTenant(String tenantId, Supplier<R> operation)` method
   - Has tenant ID validation pattern: `^[a-z][a-z0-9_]{2,49}$`
   - Throws `InvalidTenantException` for bad format

2. **TenantRoutingDataSource**
   - Has `tenantExistsInDatabase(String tenantId)` method (cached)
   - Checks `public.tenants` table for ACTIVE status
   - Throws `TenantNotFoundException` if not found

3. **Current Webhook Flow**
   - WebhookController receives POST at `/webhook/{pgCode}`
   - No tenant context is set (PROBLEM)
   - WebhookProcessingService processes in default context

4. **Tenant Context Flow for Other Endpoints**
   - TenantInterceptor extracts tenant from JWT or X-Tenant-ID header
   - TenantRequestFilter wraps request in `runInTenant()`
   - This flow bypasses webhooks (no JWT from PG systems)

### Frontend Architecture (discovered)

1. **PgConnectionDetail.svelte**
   - Has `webhookBaseUrl` field (user-configurable)
   - Does NOT generate/display full webhook URL
   - Location: lines 346-360 for webhook section

2. **pgConnection.ts types**
   - `webhookBaseUrl?: string` in PgConnectionDto
   - Need to add computed `generatedWebhookUrl` field

## Research Findings

- **TenantService.java**: Currently only handles cache eviction, no validation method
- **SecurityConfig**: `/webhook/**` already permitted (no JWT required)
- **Existing exceptions**: `InvalidTenantException`, `TenantNotFoundException` ready for use

## Decisions Made (User Confirmed)

1. **Backward compatibility**: DUAL SUPPORT
   - Keep old `/webhook/{pgCode}` working with deprecation warning
   - Old endpoint REQUIRES pgConnectionId to derive tenant (lookup in public schema)
   - New `/webhook/{tenantId}/{pgCode}` is recommended going forward

2. **Webhook URL Generation**: BACKEND COMPUTES
   - Add `generatedWebhookUrl` to `PgConnectionDto`
   - Use configurable `app.webhook.base-url` property in application.yml

3. **webhookSecret Location**: KEEP QUERY PARAM
   - No breaking changes to PG integrations
   - Keep `?webhookSecret=xxx` format

4. **Testing Strategy**: MANUAL ONLY
   - No test infrastructure exists
   - Verify with lsp_diagnostics
   - curl commands for API verification

5. **Frontend Display**: SHOW BOTH URLs
   - Old URL for existing integrations
   - New URL (with tenant) for fresh setups

## Scope Boundaries

### INCLUDE
- New webhook endpoint: `POST /webhook/{tenantId}/{pgCode}`
- Tenant validation in webhook controller
- TenantService enhancement with `validateTenantExists()` method
- Frontend webhook URL display with copy button
- Backward compatibility wrapper for old endpoint

### EXCLUDE
- Migration script for existing PG connections (out of scope)
- PG system notification about URL change (manual process)
- Webhook URL rotation/regeneration feature

## Implementation Approach

### Backend Changes

1. **TenantService.java** - Add `validateTenantExists(String tenantId)` method
   - Reuse `TenantRoutingDataSource.tenantExistsInDatabase()` logic
   - Throw `TenantNotFoundException` if not found

2. **WebhookController.java** - New endpoint with tenantId
   - Add `/{tenantId}/{pgCode}` mapping
   - Validate tenant, wrap in `runInTenant()`
   - Keep old endpoint for backward compatibility (delegate to new)

3. **PgConnectionDto.java** - Add computed webhook URL field
   - `generatedWebhookUrl` built from config + tenantId + pgCode + id + secret

### Frontend Changes

4. **PgConnectionDetail.svelte** - Display webhook URL
   - New Card section showing generated URL
   - Copy-to-clipboard button
   - Only visible in view mode (not edit)

5. **pgConnection.ts** - Add type field
   - `generatedWebhookUrl?: string`
