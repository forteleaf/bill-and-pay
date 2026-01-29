# Bill&Pay REST API Implementation Summary

## Overview
Implemented complete REST API controllers with organization hierarchy access control based on PRD-02 authorization rules using ltree path-based permissions.

## Implementation Date
2026-01-30

---

## 1. Access Control Service

### File: `service/auth/AccessControlService.java`

**Purpose**: Enforce ltree path-based authorization per PRD-02 rules

**Key Methods**:
- `validateOrganizationAccess(User, String)` - Validates org path access, throws AccessDeniedException
- `validateMerchantAccess(User, Merchant)` - Validates merchant access
- `hasAccessToOrganization(User, String)` - Boolean check for org access
- `hasAccessToMerchant(User, Merchant)` - Boolean check for merchant access
- `canCreateOrganization(User, String)` - Validates creation permission
- `isMasterAdmin(User)` - Checks for master admin role

**Authorization Rules**:
- Master Admin (`MASTER_ADMIN` role): Full access to everything
- Regular Users: Can only access paths where `targetPath.startsWith(userPath)`
- Implements: `targetPath <@ userPath` (ltree descendant check)

---

## 2. Exception Handling

### New Exception Classes
- `AccessDeniedException` - HTTP 403 Forbidden
- `EntityNotFoundException` - HTTP 404 Not Found  
- `ValidationException` - HTTP 422 Unprocessable Entity

### File: `exception/handler/GlobalExceptionHandler.java`

**Handles**:
- `TenantNotFoundException` → 400 Bad Request
- `AccessDeniedException` → 403 Forbidden
- `EntityNotFoundException` → 404 Not Found
- `ValidationException` → 422 Unprocessable Entity
- `MethodArgumentNotValidException` → 422 (with field details)
- `IllegalArgumentException` → 400 Bad Request
- `Exception` → 500 Internal Server Error

**Features**:
- Consistent error response format
- Structured field validation errors
- Comprehensive logging (WARN for business errors, ERROR for system errors)
- Never exposes internal stack traces to clients

---

## 3. Standard API Response DTOs

### `dto/response/ApiResponse.java`

Standard envelope for all API responses:

```json
{
  "success": true,
  "data": { ... },
  "error": null,
  "meta": { ... },
  "timestamp": "2026-01-30T12:00:00Z"
}
```

**Factory Methods**:
- `ApiResponse.success(data)` - Success response
- `ApiResponse.success(data, meta)` - Success with metadata
- `ApiResponse.error(code, message)` - Error response
- `ApiResponse.error(code, message, details)` - Error with details

### `dto/response/PagedResponse.java`

Standard pagination wrapper:

```json
{
  "content": [...],
  "page": 0,
  "size": 20,
  "totalElements": 150,
  "totalPages": 8,
  "hasNext": true,
  "hasPrevious": false
}
```

**Factory Methods**:
- `PagedResponse.of(Page)` - From Spring Data Page
- `PagedResponse.of(Page, List)` - With mapped content

---

## 4. Organization API

### Controller: `controller/api/OrganizationController.java`

### Endpoints

#### `GET /api/v1/organizations`
List organizations (filtered by user's org path)

**Query Parameters**:
- `page` (default: 0)
- `size` (default: 20, max: 100)
- `sortBy` (default: createdAt)
- `direction` (ASC/DESC, default: DESC)

**Response**: `ApiResponse<PagedResponse<OrganizationDto>>`

#### `GET /api/v1/organizations/{id}`
Get organization details

**Authorization**: User must have access to target org path

**Response**: `ApiResponse<OrganizationDto>`

#### `GET /api/v1/organizations/{id}/descendants`
Get hierarchy tree of all descendants

**Authorization**: User must have access to parent org

**Response**: `ApiResponse<List<OrganizationDto>>`

#### `POST /api/v1/organizations`
Create child organization

**Request Body**: `OrganizationCreateRequest`

**Validation**:
- User must have access to parent org path
- Organization code must be unique
- Auto-generates ltree path based on type

**Response**: `ApiResponse<OrganizationDto>` (HTTP 201)

#### `PUT /api/v1/organizations/{id}`
Update organization

**Request Body**: `OrganizationUpdateRequest`

**Authorization**: User must have access to target org

**Response**: `ApiResponse<OrganizationDto>`

### Service: `service/organization/OrganizationService.java`

**Key Features**:
- Automatic ltree path generation with prefixes:
  - DISTRIBUTOR → `dist_`
  - AGENCY → `agcy_`
  - DEALER → `deal_`
  - SELLER → `sell_`
  - VENDOR → `vend_`
- Sequential numbering within siblings (e.g., `dist_001`, `dist_002`)
- Hierarchy validation on create
- Path-based filtering for list operations

---

## 5. Merchant API

### Controller: `controller/api/MerchantController.java`

### Endpoints

#### `GET /api/v1/merchants`
List merchants (filtered by org_path)

**Query Parameters**: Same as organizations

**Response**: `ApiResponse<PagedResponse<MerchantDto>>`

#### `GET /api/v1/merchants/{id}`
Get merchant details

**Authorization**: Merchant's org_path must be descendant of user's org_path

**Response**: `ApiResponse<MerchantDto>`

#### `GET /api/v1/merchants/{id}/statistics`
Get merchant transaction statistics

**Returns**:
- Total transactions/amount
- Approved transactions/amount
- Cancelled transactions/amount
- Pending transactions/amount

**Response**: `ApiResponse<MerchantStatisticsDto>`

#### `POST /api/v1/merchants`
Create merchant

**Request Body**: `MerchantCreateRequest`

**Validation**:
- User must have access to parent organization
- Merchant code must be unique
- Auto-copies org_path from organization

**Response**: `ApiResponse<MerchantDto>` (HTTP 201)

#### `PUT /api/v1/merchants/{id}`
Update merchant

**Request Body**: `MerchantUpdateRequest`

**Response**: `ApiResponse<MerchantDto>`

### Service: `service/merchant/MerchantService.java`

**Key Features**:
- Automatic org_path inheritance from organization
- Access control on all operations
- Real-time statistics calculation from transactions

---

## 6. Transaction API

### Controller: `controller/api/TransactionController.java`

### Endpoints

#### `GET /api/v1/transactions`
List transactions (filtered by merchant_path)

**Query Parameters**:
- `merchantId` (UUID, optional)
- `status` (TransactionStatus, optional)
- `startDate` (ISO 8601, optional)
- `endDate` (ISO 8601, optional)
- `page`, `size`, `sortBy`, `direction`

**Authorization**: Only transactions where `merchant_path <@ user.org_path`

**Response**: `ApiResponse<PagedResponse<TransactionDto>>`

#### `GET /api/v1/transactions/{id}`
Get transaction details

**Authorization**: User must have access to transaction's org_path

**Response**: `ApiResponse<TransactionDto>`

#### `GET /api/v1/transactions/{id}/events`
Get transaction event history

**Returns**: All transaction_events for the transaction (APPROVAL, CANCEL, PARTIAL_CANCEL)

**Response**: `ApiResponse<List<TransactionEventDto>>`

### Service: `service/transaction/TransactionQueryService.java`

**Key Features**:
- Read-only operations (separation from webhook processing)
- Complex filtering with multiple criteria
- ltree-based access control
- Event history retrieval ordered by sequence

---

## 7. Settlement API

### Controller: `controller/api/SettlementController.java`

### Endpoints

#### `GET /api/v1/settlements`
List settlements (filtered by entity_path)

**Query Parameters**:
- `entityType` (OrganizationType, optional)
- `status` (SettlementStatus, optional)
- `startDate` (ISO 8601, optional)
- `endDate` (ISO 8601, optional)
- `page`, `size`, `sortBy`, `direction`

**Authorization**: Only settlements where `entity_path <@ user.org_path`

**Response**: `ApiResponse<PagedResponse<SettlementDto>>`

#### `GET /api/v1/settlements/summary`
Get aggregate settlement summary

**Query Parameters**:
- `entityType` (optional)
- `startDate` (optional)
- `endDate` (optional)

**Returns**:
- Total amount, fee amount, net amount
- Credit/debit totals
- Transaction count

**Response**: `ApiResponse<SettlementSummaryDto>`

#### `GET /api/v1/settlements/batch/{date}`
Get daily batch settlement report

**Path Parameter**: `date` (ISO 8601 date, e.g., 2026-01-30)

**Returns**: All settlements for the specified date

**Response**: `ApiResponse<List<SettlementDto>>`

### Service: `service/settlement/SettlementQueryService.java`

**Key Features**:
- Read-only operations
- Aggregation calculations for summaries
- Daily batch reporting by date
- ltree-based access control on entity_path

---

## 8. DTOs (Request/Response)

### Organization DTOs
- `OrganizationDto` - Full organization details
- `OrganizationCreateRequest` - Create validation
- `OrganizationUpdateRequest` - Update validation

### Merchant DTOs
- `MerchantDto` - Full merchant details
- `MerchantCreateRequest` - Create validation
- `MerchantUpdateRequest` - Update validation
- `MerchantStatisticsDto` - Transaction statistics

### Transaction DTOs
- `TransactionDto` - Transaction details
- `TransactionEventDto` - Event history

### Settlement DTOs
- `SettlementDto` - Settlement ledger entry
- `SettlementSummaryDto` - Aggregate summary

### Validation
All request DTOs use Jakarta Bean Validation:
- `@NotBlank`, `@NotNull` for required fields
- `@Email` for email validation
- Custom validators can be added for ltree path format

---

## 9. User Context Management

### File: `service/auth/UserContextHolder.java`

**Purpose**: Temporary user context management (will be replaced by JWT authentication)

**Methods**:
- `setCurrentUserId(UUID)` - Set current user ID
- `getCurrentUser()` - Retrieve authenticated user
- `clear()` - Clear context

**Usage**: ThreadLocal-based storage for current authenticated user

**Note**: This is a simplified implementation. In production:
- Replace with Spring Security SecurityContext
- Extract user from JWT token
- Use @AuthenticationPrincipal in controllers

---

## 10. Authorization Flow

### Request Flow
1. **Request arrives** at controller endpoint
2. **UserContextHolder** retrieves current authenticated user
3. **Service layer** receives user parameter
4. **AccessControlService** validates permissions:
   - Checks if user has access to target path
   - Throws `AccessDeniedException` if denied
5. **Repository layer** executes query
6. **Service layer** filters results by accessible paths
7. **Controller** maps entities to DTOs
8. **Response** wrapped in `ApiResponse` envelope

### Access Control Rules (PRD-02)

```
User Path: 'dist_001.agcy_001'

✅ CAN ACCESS:
- 'dist_001.agcy_001' (self)
- 'dist_001.agcy_001.deal_001' (descendant)
- 'dist_001.agcy_001.deal_001.sell_001' (descendant)

❌ CANNOT ACCESS:
- 'dist_001' (parent)
- 'dist_002' (sibling)
- 'dist_001.agcy_002' (sibling)
```

---

## 11. API Standards

### URL Pattern
```
/api/v1/{resource}
/api/v1/{resource}/{id}
/api/v1/{resource}/{id}/{sub-resource}
```

### HTTP Methods
- `GET` - Retrieve resources
- `POST` - Create resources (returns 201)
- `PUT` - Update resources (full update)
- `DELETE` - Not implemented (use status field instead)

### Response Status Codes
- `200 OK` - Successful GET/PUT
- `201 Created` - Successful POST
- `400 Bad Request` - Invalid tenant, illegal arguments
- `403 Forbidden` - Access denied
- `404 Not Found` - Entity not found
- `422 Unprocessable Entity` - Validation errors
- `500 Internal Server Error` - System errors

### Pagination
- Default page size: 20
- Maximum page size: 100
- 0-based page numbering
- Configurable sort field and direction

### Date/Time Format
- ISO 8601: `2026-01-30T12:00:00+09:00`
- Spring `@DateTimeFormat` for parsing
- OffsetDateTime for timezone awareness

---

## 12. Security Considerations

### Implemented
✅ ltree path-based access control  
✅ Input validation (Jakarta Bean Validation)  
✅ Exception handling (no internal details exposed)  
✅ Access logging for denied requests  
✅ Master admin bypass for system operations  

### Not Yet Implemented (Future Work)
⏳ JWT authentication  
⏳ Rate limiting  
⏳ API key authentication  
⏳ CORS configuration  
⏳ HTTPS enforcement  
⏳ Request signing (HMAC)  
⏳ Audit logging  

---

## 13. Testing Recommendations

### Unit Tests
- AccessControlService permission logic
- DTO mapping (entity to DTO)
- Service layer business logic
- Path generation algorithm

### Integration Tests
- Controller endpoints with mock authentication
- Access control enforcement
- Pagination behavior
- Filter combinations

### API Tests (Postman/REST Assured)
- Happy path scenarios
- Authorization scenarios (403 cases)
- Validation errors (422 cases)
- Edge cases (empty results, max page size)

### Test Data Setup
```sql
-- Create test hierarchy
INSERT INTO organizations (path, org_type, ...) VALUES
  ('dist_001', 'DISTRIBUTOR', ...),
  ('dist_001.agcy_001', 'AGENCY', ...),
  ('dist_001.agcy_001.deal_001', 'DEALER', ...);

-- Create test users with different paths
INSERT INTO users (org_path, role, ...) VALUES
  ('dist_001', 'MASTER_ADMIN', ...),
  ('dist_001.agcy_001', 'ORG_ADMIN', ...);

-- Create test merchants
INSERT INTO merchants (org_path, ...) VALUES
  ('dist_001.agcy_001.deal_001', ...);
```

---

## 14. Next Steps

### Immediate
1. **Add JWT Authentication**
   - Replace UserContextHolder with SecurityContext
   - Implement JwtAuthenticationFilter
   - Configure Spring Security

2. **Add Integration Tests**
   - Test all controller endpoints
   - Verify access control enforcement
   - Test edge cases

3. **Add API Documentation**
   - Swagger/OpenAPI spec
   - Example requests/responses
   - Authentication instructions

### Short-term
4. **Add Rate Limiting**
   - Per-user rate limits
   - Per-endpoint rate limits
   - Bucket4j integration

5. **Add Audit Logging**
   - Log all API access
   - Include user, action, timestamp
   - Store in audit table

6. **Add CORS Configuration**
   - Configure allowed origins
   - Set appropriate headers
   - Handle preflight requests

### Long-term
7. **Add Caching**
   - Redis for frequently accessed data
   - Cache invalidation strategy
   - ETag support

8. **Add GraphQL API** (optional)
   - For complex nested queries
   - Reduce over-fetching
   - Client-driven queries

---

## 15. File Structure

```
src/main/java/com/korpay/billpay/
├── controller/
│   └── api/
│       ├── OrganizationController.java
│       ├── MerchantController.java
│       ├── TransactionController.java
│       └── SettlementController.java
├── dto/
│   ├── request/
│   │   ├── OrganizationCreateRequest.java
│   │   ├── OrganizationUpdateRequest.java
│   │   ├── MerchantCreateRequest.java
│   │   └── MerchantUpdateRequest.java
│   └── response/
│       ├── ApiResponse.java
│       ├── PagedResponse.java
│       ├── OrganizationDto.java
│       ├── MerchantDto.java
│       ├── MerchantStatisticsDto.java
│       ├── TransactionDto.java
│       ├── TransactionEventDto.java
│       ├── SettlementDto.java
│       └── SettlementSummaryDto.java
├── exception/
│   ├── AccessDeniedException.java
│   ├── EntityNotFoundException.java
│   ├── ValidationException.java
│   └── handler/
│       └── GlobalExceptionHandler.java
└── service/
    ├── auth/
    │   ├── AccessControlService.java
    │   └── UserContextHolder.java
    ├── organization/
    │   └── OrganizationService.java
    ├── merchant/
    │   └── MerchantService.java
    ├── transaction/
    │   └── TransactionQueryService.java
    └── settlement/
        └── SettlementQueryService.java
```

---

## 16. Example API Calls

### List Organizations (with pagination)
```bash
GET /api/v1/organizations?page=0&size=20&sortBy=createdAt&direction=DESC
```

### Get Organization Descendants
```bash
GET /api/v1/organizations/{id}/descendants
```

### Create Organization
```bash
POST /api/v1/organizations
Content-Type: application/json

{
  "orgCode": "DEALER_001",
  "name": "서울 대리점",
  "orgType": "DEALER",
  "parentId": "550e8400-e29b-41d4-a716-446655440000",
  "businessNumber": "123-45-67890",
  "email": "contact@dealer.com",
  "phone": "02-1234-5678"
}
```

### List Transactions (with filters)
```bash
GET /api/v1/transactions?merchantId={id}&status=APPROVED&startDate=2026-01-01T00:00:00Z&endDate=2026-01-31T23:59:59Z
```

### Get Settlement Summary
```bash
GET /api/v1/settlements/summary?entityType=DEALER&startDate=2026-01-01T00:00:00Z&endDate=2026-01-31T23:59:59Z
```

### Get Daily Batch Report
```bash
GET /api/v1/settlements/batch/2026-01-30
```

---

## 17. Configuration Notes

### Application Properties
```yaml
# No additional configuration required for this implementation
# Existing configurations (database, multi-tenancy) remain unchanged
```

### Required Dependencies
All dependencies already present in `build.gradle`:
- spring-boot-starter-web
- spring-boot-starter-data-jpa
- spring-boot-starter-validation
- lombok
- postgresql

---

## Summary

✅ **Completed**: All 10 required features from the task  
✅ **Access Control**: Full ltree-based hierarchy authorization  
✅ **Exception Handling**: Comprehensive error responses  
✅ **DTOs**: Complete request/response mapping  
✅ **Pagination**: Standard implementation with limits  
✅ **Validation**: Jakarta Bean Validation on all inputs  
✅ **Documentation**: This summary + inline JavaDoc  

🎯 **Ready for**:
- JWT authentication integration
- Integration testing
- Production deployment (after security additions)

📋 **Next Priority**:
1. Add JWT authentication
2. Write integration tests
3. Add OpenAPI documentation
