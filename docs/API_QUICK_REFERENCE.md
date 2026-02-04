# Bill&Pay API Quick Reference Guide

## 🚀 Quick Start

### Base URL
```
http://localhost:8080/api/v1
```

### Required Headers
```
X-Tenant-ID: {tenant-uuid}
Content-Type: application/json
```

---

## 📚 API Endpoints

### Organizations

```http
# List organizations (paginated)
GET /api/v1/organizations?page=0&size=20&sortBy=createdAt&direction=DESC

# Get organization details
GET /api/v1/organizations/{id}

# Get organization hierarchy
GET /api/v1/organizations/{id}/descendants

# Create organization
POST /api/v1/organizations
{
  "orgCode": "DEALER_001",
  "name": "Seoul Dealer",
  "orgType": "DEALER",
  "parentId": "550e8400-e29b-41d4-a716-446655440000",
  "businessNumber": "123-45-67890",
  "email": "contact@dealer.com",
  "phone": "02-1234-5678"
}

# Update organization
PUT /api/v1/organizations/{id}
{
  "name": "Updated Name",
  "status": "ACTIVE"
}
```

### Merchants

```http
# List merchants (paginated)
GET /api/v1/merchants?page=0&size=20

# Get merchant details
GET /api/v1/merchants/{id}

# Get merchant statistics
GET /api/v1/merchants/{id}/statistics

# Create merchant
POST /api/v1/merchants
{
  "merchantCode": "M001",
  "name": "Test Store",
  "organizationId": "550e8400-e29b-41d4-a716-446655440000",
  "businessNumber": "123-45-67890",
  "contactEmail": "store@test.com",
  "contactPhone": "02-1234-5678"
}

# Update merchant
PUT /api/v1/merchants/{id}
{
  "name": "Updated Store Name",
  "status": "ACTIVE"
}
```

### Transactions

```http
# List transactions (with filters)
GET /api/v1/transactions?merchantId={id}&status=APPROVED&startDate=2026-01-01T00:00:00Z&endDate=2026-01-31T23:59:59Z

# Get transaction details
GET /api/v1/transactions/{id}

# Get transaction events (history)
GET /api/v1/transactions/{id}/events
```

### Settlements

```http
# List settlements (with filters)
GET /api/v1/settlements?entityType=DEALER&status=COMPLETED&startDate=2026-01-01T00:00:00Z

# Get settlement summary
GET /api/v1/settlements/summary?entityType=DEALER&startDate=2026-01-01T00:00:00Z&endDate=2026-01-31T23:59:59Z

# Get daily batch report
GET /api/v1/settlements/batch/2026-01-30
```

---

## 🔐 Authorization Rules

### Hierarchy Access

```
User Path: 'dist_001.agcy_001'

✅ CAN ACCESS:
- 'dist_001.agcy_001'              (self)
- 'dist_001.agcy_001.deal_001'     (child)
- 'dist_001.agcy_001.deal_001.*'   (descendants)

❌ CANNOT ACCESS:
- 'dist_001'                        (parent)
- 'dist_002'                        (sibling distributor)
- 'dist_001.agcy_002'              (sibling agency)
```

### Master Admin
- Role: `MASTER_ADMIN`
- Access: Full access to all data
- Bypasses all path restrictions

---

## 📄 Response Format

### Success Response
```json
{
  "success": true,
  "data": { ... },
  "error": null,
  "meta": null,
  "timestamp": "2026-01-30T12:00:00+09:00"
}
```

### Paginated Response
```json
{
  "success": true,
  "data": {
    "content": [...],
    "page": 0,
    "size": 20,
    "totalElements": 150,
    "totalPages": 8,
    "hasNext": true,
    "hasPrevious": false
  },
  "error": null,
  "timestamp": "2026-01-30T12:00:00+09:00"
}
```

### Error Response
```json
{
  "success": false,
  "data": null,
  "error": {
    "code": "ACCESS_DENIED",
    "message": "Access denied to organization: dist_001",
    "details": null
  },
  "timestamp": "2026-01-30T12:00:00+09:00"
}
```

### Validation Error Response
```json
{
  "success": false,
  "data": null,
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "Invalid request parameters",
    "details": {
      "email": "Invalid email format",
      "orgCode": "Organization code is required"
    }
  },
  "timestamp": "2026-01-30T12:00:00+09:00"
}
```

---

## 🔢 Status Codes

| Code | Meaning | When |
|------|---------|------|
| 200  | OK | Successful GET, PUT |
| 201  | Created | Successful POST |
| 400  | Bad Request | Invalid tenant, illegal argument |
| 403  | Forbidden | Access denied by authorization |
| 404  | Not Found | Entity not found |
| 422  | Unprocessable Entity | Validation errors |
| 500  | Internal Server Error | System error |

---

## 🔍 Query Parameters

### Pagination (all list endpoints)
| Parameter | Type | Default | Max | Description |
|-----------|------|---------|-----|-------------|
| page | int | 0 | - | Page number (0-based) |
| size | int | 20 | 100 | Items per page |
| sortBy | string | createdAt | - | Sort field |
| direction | ASC/DESC | DESC | - | Sort direction |

### Transaction Filters
| Parameter | Type | Description |
|-----------|------|-------------|
| merchantId | UUID | Filter by merchant |
| status | enum | PENDING, APPROVED, CANCELLED, PARTIAL_CANCELLED |
| startDate | ISO 8601 | Start of date range |
| endDate | ISO 8601 | End of date range |

### Settlement Filters
| Parameter | Type | Description |
|-----------|------|-------------|
| entityType | enum | DISTRIBUTOR, AGENCY, DEALER, SELLER, VENDOR |
| status | enum | PENDING, COMPLETED, FAILED |
| startDate | ISO 8601 | Start of date range |
| endDate | ISO 8601 | End of date range |

---

## 📝 Enums Reference

### OrganizationType
```
DISTRIBUTOR, AGENCY, DEALER, SELLER, VENDOR
```

### OrganizationStatus
```
ACTIVE, SUSPENDED, TERMINATED
```

### MerchantStatus
```
ACTIVE, SUSPENDED, TERMINATED
```

### TransactionStatus
```
PENDING, APPROVED, CANCELLED, PARTIAL_CANCELLED
```

### SettlementStatus
```
PENDING, COMPLETED, FAILED
```

### EntryType
```
CREDIT, DEBIT
```

---

## 🛠️ Development Tips

### Setting Current User (temporary)
```java
// In your test/development code
@Autowired
private UserContextHolder userContextHolder;

@BeforeEach
void setup() {
    userContextHolder.setCurrentUserId(testUserId);
}

@AfterEach
void cleanup() {
    userContextHolder.clear();
}
```

### Testing Access Control
```java
// Test should succeed
User user = createUser("dist_001.agcy_001");
Organization org = createOrg("dist_001.agcy_001.deal_001");
assertTrue(accessControlService.hasAccessToOrganization(user, org.getPath()));

// Test should fail
Organization parentOrg = createOrg("dist_001");
assertFalse(accessControlService.hasAccessToOrganization(user, parentOrg.getPath()));
```

### Creating Test Hierarchy
```sql
-- Create distributor
INSERT INTO organizations (id, path, org_type, org_code, name, level) 
VALUES 
  ('d1-uuid', 'dist_001', 'DISTRIBUTOR', 'DIST001', 'Distributor 1', 1);

-- Create agency
INSERT INTO organizations (id, path, org_type, org_code, name, parent_id, level) 
VALUES 
  ('a1-uuid', 'dist_001.agcy_001', 'AGENCY', 'AGCY001', 'Agency 1', 'd1-uuid', 2);

-- Create dealer
INSERT INTO organizations (id, path, org_type, org_code, name, parent_id, level) 
VALUES 
  ('d1-uuid', 'dist_001.agcy_001.deal_001', 'DEALER', 'DEAL001', 'Dealer 1', 'a1-uuid', 3);
```

---

## 🐛 Common Errors

### Error: "Tenant not found"
**Cause**: Missing or invalid X-Tenant-ID header  
**Solution**: Include valid tenant UUID in header

### Error: "Access denied to organization"
**Cause**: User trying to access parent or sibling org  
**Solution**: User can only access own org and descendants

### Error: "Organization code already exists"
**Cause**: Duplicate org_code in create request  
**Solution**: Use unique organization code

### Error: "Parent organization not found"
**Cause**: Invalid parentId in create request  
**Solution**: Ensure parent organization exists

### Error: "Invalid email format"
**Cause**: Email validation failed  
**Solution**: Provide valid email format

---

## 📊 Example Workflows

### Create Complete Hierarchy
```bash
# 1. Create Distributor (by Master Admin)
POST /api/v1/organizations
{
  "orgCode": "DIST001",
  "name": "Seoul Distributor",
  "orgType": "DISTRIBUTOR",
  "parentId": "{master-org-id}"
}
# Response: { "id": "dist-uuid", "path": "dist_001" }

# 2. Create Agency
POST /api/v1/organizations
{
  "orgCode": "AGCY001",
  "name": "Gangnam Agency",
  "orgType": "AGENCY",
  "parentId": "{dist-uuid}"
}
# Response: { "id": "agcy-uuid", "path": "dist_001.agcy_001" }

# 3. Create Dealer
POST /api/v1/organizations
{
  "orgCode": "DEAL001",
  "name": "Gangnam Dealer",
  "orgType": "DEALER",
  "parentId": "{agcy-uuid}"
}
# Response: { "id": "deal-uuid", "path": "dist_001.agcy_001.deal_001" }

# 4. Create Merchant under Dealer
POST /api/v1/merchants
{
  "merchantCode": "M001",
  "name": "Coffee Shop",
  "organizationId": "{deal-uuid}"
}
# Response: { "id": "merchant-uuid", "orgPath": "dist_001.agcy_001.deal_001" }
```

### Query Transactions with Filters
```bash
# Get all approved transactions for a merchant in January
GET /api/v1/transactions?merchantId={merchant-uuid}&status=APPROVED&startDate=2026-01-01T00:00:00Z&endDate=2026-01-31T23:59:59Z

# Get transaction details
GET /api/v1/transactions/{transaction-id}

# Get transaction history (all events)
GET /api/v1/transactions/{transaction-id}/events
```

### Settlement Reporting
```bash
# Get monthly settlement summary for dealer
GET /api/v1/settlements/summary?entityType=DEALER&startDate=2026-01-01T00:00:00Z&endDate=2026-01-31T23:59:59Z

# Get daily batch report
GET /api/v1/settlements/batch/2026-01-30

# Get detailed settlement list
GET /api/v1/settlements?entityType=DEALER&status=COMPLETED&page=0&size=50
```

---

## 🔗 Related Documentation

- **Full Implementation**: See `API_IMPLEMENTATION_SUMMARY.md`
- **Checklist**: See `API_IMPLEMENTATION_CHECKLIST.md`
- **PRD-02**: Organization hierarchy rules
- **PRD-03**: Settlement and ledger rules

---

## 📞 Need Help?

1. **Authorization issues**: Check PRD-02 hierarchy rules
2. **Validation errors**: Review DTO field requirements
3. **Access denied**: Verify user's org_path vs target path
4. **Internal errors**: Check application logs

**Happy coding!** 🎉
