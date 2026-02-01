# Features 2, 3, 4: Settlement Accounts UI, User Management, Merchant List Improvement

## TL;DR

> **Quick Summary**: Implement settlement account management UI (frontend only), complete user management system (backend + frontend), and enhance merchant list with contact information.
> 
> **Deliverables**:
> - Settlement Accounts: API client, types, embeddable CRUD component
> - User Management: Backend (Service, Controller, DTOs) + Frontend (API, types, List/Detail/Registration)
> - Merchant List: Backend contact loading + frontend columns + multi-contact UI
> 
> **Estimated Effort**: Large (12-15 tasks across 3 features)
> **Parallel Execution**: YES - 4 waves
> **Critical Path**: User Backend DTOs → User Service → User Controller → User Frontend

---

## Context

### Original Request
Implement 3 features for Bill&Pay 정산 플랫폼:
1. Feature 2: Settlement Accounts Frontend (정산 계좌 관리 UI)
2. Feature 3: User Management UI (사용자 관리)
3. Feature 4: Merchant List Improvement (가맹점 목록 개선)

### Interview Summary
**Key Discussions**:
- Feature 2: Backend already complete with 8 API endpoints at `/v1/settlement-accounts`
- Feature 3: User entity exists but NO service/controller/DTOs - needs complete implementation
- Feature 4: List endpoint doesn't include contacts; detail endpoint does via `findByIdWithContacts()`

**Research Findings**:
- Backend patterns: `@Service` + `@RequiredArgsConstructor` + `@Transactional(readOnly=true)`
- Frontend patterns: API client classes with typed methods, Svelte 5 Runes, shadcn-svelte
- Existing APIs: Contact and SettlementAccount controllers follow same pattern with entity-based queries
- User entity fields: username, email, passwordHash, orgId, orgPath, fullName, phone, role, permissions, twoFactorEnabled, status

### Self-Analysis (Gap Review)
**Identified Gaps** (addressed):
- User role values: Will use String field, allow any role value (flexibility for admin configuration)
- Password encoding: Will use BCrypt via Spring Security's `PasswordEncoder`
- Bank code list: Will hardcode Korean bank codes as frontend constant
- Settlement account embedding: Will embed in both MerchantDetail and BranchDetail (supports MERCHANT and BUSINESS_ENTITY)

---

## Work Objectives

### Core Objective
Deliver three interconnected features that complete the platform's entity management capabilities: settlement account CRUD UI, user management system, and enhanced merchant list with contact visibility.

### Concrete Deliverables

**Feature 2 - Settlement Accounts Frontend:**
- `frontend/src/lib/settlementAccountApi.ts` - API client
- `frontend/src/types/settlementAccount.ts` - TypeScript types
- `frontend/src/components/SettlementAccountManager.svelte` - Embeddable CRUD component
- Integration in `MerchantDetail.svelte` and `BranchDetail.svelte`

**Feature 3 - User Management:**
- `src/main/java/.../dto/request/UserCreateRequest.java`
- `src/main/java/.../dto/request/UserUpdateRequest.java`
- `src/main/java/.../dto/response/UserResponse.java`
- `src/main/java/.../service/user/UserService.java`
- `src/main/java/.../controller/api/UserController.java`
- `frontend/src/lib/userApi.ts`
- `frontend/src/types/user.ts`
- `frontend/src/routes/user/UserList.svelte`
- `frontend/src/routes/user/UserDetail.svelte`
- `frontend/src/routes/user/UserRegistration.svelte`

**Feature 4 - Merchant List Improvement:**
- Modified `MerchantController.listMerchants()` to include primaryContact
- Modified `MerchantService.findAccessibleMerchants()` to fetch contacts
- Updated `MerchantList.svelte` with 담당자/연락처 columns
- Added `ContactManager.svelte` for multi-contact UI in MerchantDetail

### Definition of Done
- [ ] All settlement account CRUD operations work via UI
- [ ] Users can be created, listed, viewed, edited, deleted
- [ ] Merchant list shows contact name and phone columns
- [ ] Multi-contact management works in merchant detail
- [ ] All new endpoints return proper ApiResponse wrapper
- [ ] Frontend types match backend DTOs
- [ ] No console errors in browser

### Must Have
- Settlement account list/add/edit/delete/set-primary UI
- User CRUD with password handling and org assignment
- Merchant list with primary contact columns
- Multi-contact management in merchant detail

### Must NOT Have (Guardrails)
- NO new database migrations (tables already exist)
- NO changes to authentication/authorization logic
- NO 2FA implementation (flag exists but implementation deferred)
- NO user password reset via email (out of scope)
- NO settlement account verification automation (manual admin action)
- NO changes to existing Contact API endpoints
- NO pagination changes to existing endpoints
- NO additional validation beyond what's required
- NO over-engineered abstraction layers

---

## Verification Strategy (MANDATORY)

### Test Decision
- **Infrastructure exists**: NO (per CLAUDE.md: "통합 테스트 미구현")
- **User wants tests**: Manual-only (as specified in request context)
- **Framework**: None

### Automated Verification (Agent-Executable)

Each TODO includes verification procedures that agents can run directly:

**For Backend changes** (using Bash curl):
```bash
# Agent runs curl commands to verify API responses
curl -s -X GET http://localhost:8080/api/v1/users \
  -H "Authorization: Bearer $TOKEN" \
  -H "X-Tenant-ID: $TENANT" \
  | jq '.success'
# Assert: Returns true
```

**For Frontend changes** (using playwright skill):
```
# Agent navigates, interacts, and verifies UI state
1. Navigate to http://localhost:5173
2. Click on element matching selector
3. Assert expected text/state appears
4. Screenshot evidence saved
```

---

## Execution Strategy

### Parallel Execution Waves

```
Wave 1 (Start Immediately - No Dependencies):
├── Task 1: Settlement Account API Client + Types (Frontend)
├── Task 5: User DTOs (Backend)
├── Task 10: Merchant List Backend - Load Contacts
└── Task 14: Contact Manager Component (Frontend)

Wave 2 (After Wave 1):
├── Task 2: Settlement Account Manager Component (depends: 1)
├── Task 6: User Service (depends: 5)
├── Task 11: Merchant List Frontend - Add Columns (depends: 10)
└── Task 15: Integrate Contact Manager in MerchantDetail (depends: 14)

Wave 3 (After Wave 2):
├── Task 3: Integrate Settlement Accounts in MerchantDetail (depends: 2)
├── Task 4: Integrate Settlement Accounts in BranchDetail (depends: 2)
├── Task 7: User Controller (depends: 6)
├── Task 8: User API Client + Types (Frontend) (depends: 7)

Wave 4 (After Wave 3):
├── Task 9: User List Component (depends: 8)
├── Task 12: User Detail Component (depends: 8)
└── Task 13: User Registration Component (depends: 8)

Critical Path: Task 5 → Task 6 → Task 7 → Task 8 → Task 9
Parallel Speedup: ~50% faster than sequential
```

### Dependency Matrix

| Task | Depends On | Blocks | Can Parallelize With |
|------|------------|--------|---------------------|
| 1 | None | 2 | 5, 10, 14 |
| 2 | 1 | 3, 4 | 6, 11, 15 |
| 3 | 2 | None | 4, 7 |
| 4 | 2 | None | 3, 7 |
| 5 | None | 6 | 1, 10, 14 |
| 6 | 5 | 7 | 2, 11, 15 |
| 7 | 6 | 8 | 3, 4 |
| 8 | 7 | 9, 12, 13 | None |
| 9 | 8 | None | 12, 13 |
| 10 | None | 11 | 1, 5, 14 |
| 11 | 10 | None | 2, 6, 15 |
| 12 | 8 | None | 9, 13 |
| 13 | 8 | None | 9, 12 |
| 14 | None | 15 | 1, 5, 10 |
| 15 | 14 | None | 2, 6, 11 |

### Agent Dispatch Summary

| Wave | Tasks | Estimated Time |
|------|-------|----------------|
| 1 | 1, 5, 10, 14 | 20-30 min each |
| 2 | 2, 6, 11, 15 | 30-45 min each |
| 3 | 3, 4, 7, 8 | 20-30 min each |
| 4 | 9, 12, 13 | 30-45 min each |

---

## TODOs

### Feature 2: Settlement Accounts Frontend

- [ ] 1. Create Settlement Account API Client and Types

  **What to do**:
  - Create `frontend/src/types/settlementAccount.ts` with TypeScript interfaces
  - Create `frontend/src/lib/settlementAccountApi.ts` API client class
  - Define interfaces: SettlementAccountDto, SettlementAccountCreateRequest, AccountStatus enum
  - Implement API methods: getByEntity, getById, create, update, delete, setPrimary, verify
  - Add Korean bank code constants (은행코드)

  **Must NOT do**:
  - Do not modify existing api.ts base client
  - Do not add authentication logic (uses existing apiClient)

  **Recommended Agent Profile**:
  - **Category**: `quick`
    - Reason: Straightforward file creation following established patterns
  - **Skills**: []
    - No special skills needed - standard TypeScript/API patterns

  **Parallelization**:
  - **Can Run In Parallel**: YES
  - **Parallel Group**: Wave 1 (with Tasks 5, 10, 14)
  - **Blocks**: Task 2
  - **Blocked By**: None

  **References**:
  - `frontend/src/lib/merchantApi.ts:1-51` - API client pattern (class wrapper, typed methods, URLSearchParams)
  - `frontend/src/types/merchant.ts:1-103` - Type definition pattern (enums, interfaces, label maps)
  - `src/main/java/com/korpay/billpay/dto/response/SettlementAccountResponse.java:1-74` - Backend DTO structure to match
  - `src/main/java/com/korpay/billpay/domain/enums/AccountStatus.java` - Enum values: ACTIVE, INACTIVE, PENDING_VERIFICATION
  - `src/main/java/com/korpay/billpay/controller/api/SettlementAccountController.java:1-95` - API endpoints to call

  **Acceptance Criteria**:
  - [ ] File exists: `frontend/src/types/settlementAccount.ts`
  - [ ] File exists: `frontend/src/lib/settlementAccountApi.ts`
  - [ ] TypeScript compiles without errors: `cd frontend && bun run check` → no type errors
  - [ ] API client exports: `settlementAccountApi` singleton
  - [ ] Korean bank codes constant exported: `KOREAN_BANK_CODES`

  **Commit**: YES
  - Message: `feat(frontend): add settlement account API client and types`
  - Files: `frontend/src/types/settlementAccount.ts`, `frontend/src/lib/settlementAccountApi.ts`

---

- [ ] 2. Create Settlement Account Manager Component

  **What to do**:
  - Create `frontend/src/components/SettlementAccountManager.svelte`
  - Props: `entityType: 'MERCHANT' | 'BUSINESS_ENTITY'`, `entityId: string`
  - Display list of settlement accounts with bank name, masked account number, status badge, primary indicator
  - Add account dialog: bank select dropdown, account number input, account holder input
  - Edit account: inline edit or modal
  - Delete account: confirmation dialog
  - Set primary: one-click action with optimistic UI update
  - Use shadcn-svelte components: Button, Card, Badge, Input, Label, Table
  - Use bits-ui Dialog for add/edit modals

  **Must NOT do**:
  - Do not implement account verification UI (admin-only feature)
  - Do not add bank account validation beyond basic format check
  - Do not create separate route - this is an embeddable component

  **Recommended Agent Profile**:
  - **Category**: `visual-engineering`
    - Reason: Complex UI component with modals, tables, and state management
  - **Skills**: [`frontend-ui-ux`]
    - `frontend-ui-ux`: Needed for shadcn-svelte patterns and dialog implementation

  **Parallelization**:
  - **Can Run In Parallel**: YES
  - **Parallel Group**: Wave 2 (with Tasks 6, 11, 15)
  - **Blocks**: Tasks 3, 4
  - **Blocked By**: Task 1

  **References**:
  - `frontend/src/routes/merchant/MerchantDetail.svelte:1-330` - Component structure pattern (edit mode, loading states, Card sections)
  - `frontend/src/components/OrganizationPickerDialog.svelte` - bits-ui Dialog pattern
  - `frontend/src/components/ConfirmModal.svelte` - Confirmation modal pattern
  - `frontend/src/routes/merchant/MerchantList.svelte:258-347` - Table rendering pattern with badges
  - `frontend/src/lib/settlementAccountApi.ts` - API client (from Task 1)

  **Acceptance Criteria**:
  - [ ] File exists: `frontend/src/components/SettlementAccountManager.svelte`
  - [ ] Component accepts props: entityType, entityId
  - [ ] Agent verification via playwright:
    - Navigate to test page with component mounted
    - Verify accounts table renders
    - Click "계좌 추가" button → dialog opens
    - Fill form and submit → new account appears in list
    - Click delete → confirmation → account removed
    - Screenshot: `.sisyphus/evidence/task-2-settlement-manager.png`

  **Commit**: YES
  - Message: `feat(frontend): add SettlementAccountManager component with CRUD operations`
  - Files: `frontend/src/components/SettlementAccountManager.svelte`

---

- [ ] 3. Integrate Settlement Accounts in MerchantDetail

  **What to do**:
  - Import SettlementAccountManager in MerchantDetail.svelte
  - Add new tab "정산계좌" after "정산내역" tab
  - Render SettlementAccountManager with entityType="MERCHANT" and entityId={merchantId}
  - Wrap in Card component for consistent styling

  **Must NOT do**:
  - Do not modify existing tabs or sections
  - Do not add settlement account data to merchant DTO

  **Recommended Agent Profile**:
  - **Category**: `quick`
    - Reason: Simple integration of existing component into existing page
  - **Skills**: []

  **Parallelization**:
  - **Can Run In Parallel**: YES
  - **Parallel Group**: Wave 3 (with Tasks 4, 7)
  - **Blocks**: None
  - **Blocked By**: Task 2

  **References**:
  - `frontend/src/routes/merchant/MerchantDetail.svelte:189-211` - Tab navigation pattern
  - `frontend/src/routes/merchant/MerchantDetail.svelte:307-325` - Tab content rendering pattern
  - `frontend/src/components/SettlementAccountManager.svelte` - Component to integrate (from Task 2)

  **Acceptance Criteria**:
  - [ ] MerchantDetail.svelte imports SettlementAccountManager
  - [ ] New tab "정산계좌" appears in tab navigation
  - [ ] Agent verification via playwright:
    - Navigate to merchant detail page
    - Click "정산계좌" tab
    - Verify SettlementAccountManager renders with merchant data
    - Screenshot: `.sisyphus/evidence/task-3-merchant-settlement.png`

  **Commit**: YES (groups with Task 4)
  - Message: `feat(frontend): integrate settlement accounts in merchant and branch detail`
  - Files: `frontend/src/routes/merchant/MerchantDetail.svelte`, `frontend/src/routes/branch/BranchDetail.svelte`

---

- [ ] 4. Integrate Settlement Accounts in BranchDetail

  **What to do**:
  - Import SettlementAccountManager in BranchDetail.svelte
  - Add new tab "정산계좌" in appropriate position
  - Render SettlementAccountManager with entityType="BUSINESS_ENTITY" and entityId={branchId}
  - Wrap in Card component for consistent styling

  **Must NOT do**:
  - Do not modify existing tabs or sections
  - Do not change entityType - BranchDetail uses BUSINESS_ENTITY

  **Recommended Agent Profile**:
  - **Category**: `quick`
    - Reason: Simple integration following same pattern as Task 3
  - **Skills**: []

  **Parallelization**:
  - **Can Run In Parallel**: YES
  - **Parallel Group**: Wave 3 (with Tasks 3, 7)
  - **Blocks**: None
  - **Blocked By**: Task 2

  **References**:
  - `frontend/src/routes/branch/BranchDetail.svelte` - Target file for integration
  - `frontend/src/routes/merchant/MerchantDetail.svelte:189-211` - Tab pattern reference
  - `frontend/src/components/SettlementAccountManager.svelte` - Component to integrate (from Task 2)

  **Acceptance Criteria**:
  - [ ] BranchDetail.svelte imports SettlementAccountManager
  - [ ] New tab "정산계좌" appears in tab navigation
  - [ ] Agent verification via playwright:
    - Navigate to branch detail page
    - Click "정산계좌" tab
    - Verify SettlementAccountManager renders with BUSINESS_ENTITY type
    - Screenshot: `.sisyphus/evidence/task-4-branch-settlement.png`

  **Commit**: YES (groups with Task 3)
  - Message: `feat(frontend): integrate settlement accounts in merchant and branch detail`
  - Files: `frontend/src/routes/merchant/MerchantDetail.svelte`, `frontend/src/routes/branch/BranchDetail.svelte`

---

### Feature 3: User Management

- [ ] 5. Create User DTOs (Backend)

  **What to do**:
  - Create `UserCreateRequest.java` with validation annotations
    - Fields: username, email, password, orgId, fullName, phone, role, permissions
    - Validations: @NotBlank username, @Email email, @NotBlank password (min 8 chars), @NotNull orgId
  - Create `UserUpdateRequest.java` for partial updates
    - Fields: fullName, phone, role, permissions, status (all optional)
  - Create `UserResponse.java` with factory method
    - Fields: id, username, email, orgId, orgPath, organizationName, fullName, phone, role, permissions, twoFactorEnabled, status, lastLoginAt, createdAt, updatedAt
    - Exclude: passwordHash, twoFactorSecret, deletedAt
    - Static method: `from(User user)`

  **Must NOT do**:
  - Do not include passwordHash in response
  - Do not add password to UserUpdateRequest (separate endpoint)
  - Do not add 2FA management fields

  **Recommended Agent Profile**:
  - **Category**: `quick`
    - Reason: DTO creation following established patterns
  - **Skills**: []

  **Parallelization**:
  - **Can Run In Parallel**: YES
  - **Parallel Group**: Wave 1 (with Tasks 1, 10, 14)
  - **Blocks**: Task 6
  - **Blocked By**: None

  **References**:
  - `src/main/java/com/korpay/billpay/dto/request/MerchantCreateRequest.java` - Request DTO pattern with validation
  - `src/main/java/com/korpay/billpay/dto/response/MerchantDto.java:1-66` - Response DTO with factory method
  - `src/main/java/com/korpay/billpay/domain/entity/User.java:1-88` - User entity fields to map
  - `src/main/java/com/korpay/billpay/domain/enums/UserStatus.java` - UserStatus enum values

  **Acceptance Criteria**:
  - [ ] File exists: `src/main/java/com/korpay/billpay/dto/request/UserCreateRequest.java`
  - [ ] File exists: `src/main/java/com/korpay/billpay/dto/request/UserUpdateRequest.java`
  - [ ] File exists: `src/main/java/com/korpay/billpay/dto/response/UserResponse.java`
  - [ ] Validation annotations present on required fields
  - [ ] UserResponse.from(User) method exists
  - [ ] Build succeeds: `./gradlew compileJava` → BUILD SUCCESSFUL

  **Commit**: YES
  - Message: `feat(backend): add User DTOs for CRUD operations`
  - Files: `src/main/java/com/korpay/billpay/dto/request/UserCreateRequest.java`, `src/main/java/com/korpay/billpay/dto/request/UserUpdateRequest.java`, `src/main/java/com/korpay/billpay/dto/response/UserResponse.java`

---

- [ ] 6. Create User Service (Backend)

  **What to do**:
  - Create `src/main/java/com/korpay/billpay/service/user/UserService.java`
  - Inject: UserRepository, OrganizationRepository, PasswordEncoder, AccessControlService
  - Methods:
    - `Page<User> findAccessibleUsers(User currentUser, Pageable pageable)` - filtered by org path access
    - `User findById(UUID id, User currentUser)` - with access control
    - `User create(UserCreateRequest request, User currentUser)` - encode password, validate org access
    - `User update(UUID id, UserUpdateRequest request, User currentUser)` - partial update
    - `void changePassword(UUID id, String newPassword, User currentUser)` - encode and update
    - `void delete(UUID id, User currentUser)` - soft delete via deletedAt
  - Access control: validate currentUser can access target user's organization
  - Password encoding: use BCrypt via PasswordEncoder bean

  **Must NOT do**:
  - Do not implement email verification
  - Do not implement password reset flow
  - Do not implement 2FA logic
  - Do not duplicate users with same username or email

  **Recommended Agent Profile**:
  - **Category**: `unspecified-high`
    - Reason: Core business logic with security implications
  - **Skills**: []

  **Parallelization**:
  - **Can Run In Parallel**: YES
  - **Parallel Group**: Wave 2 (with Tasks 2, 11, 15)
  - **Blocks**: Task 7
  - **Blocked By**: Task 5

  **References**:
  - `src/main/java/com/korpay/billpay/service/merchant/MerchantService.java:1-304` - Service pattern with access control
  - `src/main/java/com/korpay/billpay/service/auth/AccessControlService.java` - Access control service to use
  - `src/main/java/com/korpay/billpay/repository/UserRepository.java:1-25` - User repository methods
  - `src/main/java/com/korpay/billpay/domain/entity/User.java:1-88` - User entity

  **Acceptance Criteria**:
  - [ ] File exists: `src/main/java/com/korpay/billpay/service/user/UserService.java`
  - [ ] Class has @Service and @RequiredArgsConstructor annotations
  - [ ] All 6 methods implemented: findAccessibleUsers, findById, create, update, changePassword, delete
  - [ ] Password encoding uses PasswordEncoder.encode()
  - [ ] Access control validation present in all methods
  - [ ] Build succeeds: `./gradlew compileJava` → BUILD SUCCESSFUL

  **Commit**: YES
  - Message: `feat(backend): add UserService with CRUD and access control`
  - Files: `src/main/java/com/korpay/billpay/service/user/UserService.java`

---

- [ ] 7. Create User Controller (Backend)

  **What to do**:
  - Create `src/main/java/com/korpay/billpay/controller/api/UserController.java`
  - Inject: UserService, UserContextHolder
  - Endpoints:
    - `GET /v1/users` - list users (paginated)
    - `GET /v1/users/{id}` - get user by ID
    - `POST /v1/users` - create user
    - `PUT /v1/users/{id}` - update user
    - `PUT /v1/users/{id}/password` - change password
    - `DELETE /v1/users/{id}` - delete user
  - All endpoints return `ResponseEntity<ApiResponse<T>>`
  - Use @Valid for request validation
  - Get current user via UserContextHolder.getCurrentUser()

  **Must NOT do**:
  - Do not add endpoints for 2FA management
  - Do not expose password hash in responses
  - Do not add bulk operations

  **Recommended Agent Profile**:
  - **Category**: `quick`
    - Reason: Controller following established pattern
  - **Skills**: []

  **Parallelization**:
  - **Can Run In Parallel**: YES
  - **Parallel Group**: Wave 3 (with Tasks 3, 4)
  - **Blocks**: Task 8
  - **Blocked By**: Task 6

  **References**:
  - `src/main/java/com/korpay/billpay/controller/api/MerchantController.java:1-167` - Controller pattern
  - `src/main/java/com/korpay/billpay/dto/response/ApiResponse.java` - Response wrapper
  - `src/main/java/com/korpay/billpay/dto/response/PagedResponse.java` - Pagination wrapper
  - `src/main/java/com/korpay/billpay/service/user/UserService.java` - Service to call (from Task 6)

  **Acceptance Criteria**:
  - [ ] File exists: `src/main/java/com/korpay/billpay/controller/api/UserController.java`
  - [ ] 6 endpoints implemented matching specification
  - [ ] Build succeeds: `./gradlew compileJava` → BUILD SUCCESSFUL
  - [ ] Agent verification via curl:
    ```bash
    curl -s http://localhost:8080/api/v1/users \
      -H "Authorization: Bearer $TOKEN" \
      -H "X-Tenant-ID: tenant_001" \
      | jq '.success'
    # Assert: true
    ```

  **Commit**: YES
  - Message: `feat(backend): add UserController with REST endpoints`
  - Files: `src/main/java/com/korpay/billpay/controller/api/UserController.java`

---

- [ ] 8. Create User API Client and Types (Frontend)

  **What to do**:
  - Create `frontend/src/types/user.ts` with TypeScript interfaces
    - UserDto, UserCreateRequest, UserUpdateRequest
    - UserStatus enum, UserRole type (string for flexibility)
    - UserListParams interface
    - STATUS_LABELS constant
  - Create `frontend/src/lib/userApi.ts` API client class
    - Methods: getUsers, getUserById, create, update, changePassword, delete
    - Export singleton: userApi

  **Must NOT do**:
  - Do not add password fields to UserDto response type
  - Do not implement client-side password validation

  **Recommended Agent Profile**:
  - **Category**: `quick`
    - Reason: Straightforward file creation following patterns
  - **Skills**: []

  **Parallelization**:
  - **Can Run In Parallel**: NO
  - **Parallel Group**: Wave 3 (after backend ready)
  - **Blocks**: Tasks 9, 12, 13
  - **Blocked By**: Task 7

  **References**:
  - `frontend/src/lib/merchantApi.ts:1-51` - API client pattern
  - `frontend/src/types/merchant.ts:1-103` - Type definition pattern
  - `src/main/java/com/korpay/billpay/dto/response/UserResponse.java` - Backend DTO to match (from Task 5)
  - `src/main/java/com/korpay/billpay/domain/enums/UserStatus.java` - Status enum values

  **Acceptance Criteria**:
  - [ ] File exists: `frontend/src/types/user.ts`
  - [ ] File exists: `frontend/src/lib/userApi.ts`
  - [ ] TypeScript compiles: `cd frontend && bun run check` → no type errors
  - [ ] userApi singleton exported

  **Commit**: YES
  - Message: `feat(frontend): add user API client and types`
  - Files: `frontend/src/types/user.ts`, `frontend/src/lib/userApi.ts`

---

- [ ] 9. Create User List Component (Frontend)

  **What to do**:
  - Create `frontend/src/routes/user/UserList.svelte`
  - Infinite scroll with IntersectionObserver (follow MerchantList pattern)
  - Columns: 사용자ID, 이름, 이메일, 소속영업점, 역할, 상태, 등록일
  - Filters: search query, status, organization
  - Row click opens UserDetail via tabStore
  - "사용자 등록" button opens UserRegistration

  **Must NOT do**:
  - Do not show password or sensitive data
  - Do not add bulk actions

  **Recommended Agent Profile**:
  - **Category**: `visual-engineering`
    - Reason: Complex list component with infinite scroll and filters
  - **Skills**: [`frontend-ui-ux`]
    - `frontend-ui-ux`: For table layout, filter bar, infinite scroll UX

  **Parallelization**:
  - **Can Run In Parallel**: YES
  - **Parallel Group**: Wave 4 (with Tasks 12, 13)
  - **Blocks**: None
  - **Blocked By**: Task 8

  **References**:
  - `frontend/src/routes/merchant/MerchantList.svelte:1-368` - List component pattern (infinite scroll, filters, table)
  - `frontend/src/lib/tabStore.ts` - Tab navigation
  - `frontend/src/lib/userApi.ts` - API client (from Task 8)
  - `frontend/src/types/user.ts` - Types (from Task 8)

  **Acceptance Criteria**:
  - [ ] File exists: `frontend/src/routes/user/UserList.svelte`
  - [ ] Agent verification via playwright:
    - Navigate to user list page
    - Verify table renders with columns
    - Scroll to trigger infinite scroll load
    - Use filter and verify results change
    - Screenshot: `.sisyphus/evidence/task-9-user-list.png`

  **Commit**: YES
  - Message: `feat(frontend): add UserList component with infinite scroll`
  - Files: `frontend/src/routes/user/UserList.svelte`

---

- [ ] 10. Merchant List Backend - Load Primary Contacts

  **What to do**:
  - Modify `MerchantController.listMerchants()` to include primaryContact
  - Option A: Fetch contacts in batch after loading merchants (N+1 avoidance)
  - Option B: Use repository JOIN query to fetch merchants with contacts
  - Update the DTO mapping to include primaryContact in list response

  **Must NOT do**:
  - Do not fetch ALL contacts for each merchant (only primary)
  - Do not break existing pagination logic
  - Do not change endpoint signature

  **Recommended Agent Profile**:
  - **Category**: `unspecified-high`
    - Reason: Performance-sensitive data fetching change
  - **Skills**: []

  **Parallelization**:
  - **Can Run In Parallel**: YES
  - **Parallel Group**: Wave 1 (with Tasks 1, 5, 14)
  - **Blocks**: Task 11
  - **Blocked By**: None

  **References**:
  - `src/main/java/com/korpay/billpay/controller/api/MerchantController.java:43-66` - Current listMerchants implementation
  - `src/main/java/com/korpay/billpay/service/merchant/MerchantService.java:53-66` - findAccessibleMerchants method
  - `src/main/java/com/korpay/billpay/service/merchant/MerchantService.java:77-84` - findByIdWithContacts pattern to follow
  - `src/main/java/com/korpay/billpay/repository/ContactRepository.java` - Contact repository methods

  **Acceptance Criteria**:
  - [ ] Modified files compile: `./gradlew compileJava` → BUILD SUCCESSFUL
  - [ ] Agent verification via curl:
    ```bash
    curl -s http://localhost:8080/api/v1/merchants \
      -H "Authorization: Bearer $TOKEN" \
      -H "X-Tenant-ID: tenant_001" \
      | jq '.data.content[0].primaryContact'
    # Assert: Returns contact object (not null) if merchant has contact
    ```
  - [ ] Pagination still works correctly

  **Commit**: YES
  - Message: `feat(backend): include primaryContact in merchant list response`
  - Files: `src/main/java/com/korpay/billpay/controller/api/MerchantController.java`, `src/main/java/com/korpay/billpay/service/merchant/MerchantService.java`

---

- [ ] 11. Merchant List Frontend - Add Contact Columns

  **What to do**:
  - Modify `MerchantList.svelte` to add two new columns after "대표자":
    - "담당자" - shows `merchant.primaryContact?.name` or '-'
    - "연락처" - shows `merchant.primaryContact?.phone` or '-'
  - Update table header row
  - Update data row rendering
  - Update loading skeleton to include new columns
  - Update colspan in empty state

  **Must NOT do**:
  - Do not change column order of existing columns
  - Do not add contact email column (not requested)
  - Do not make contact columns clickable

  **Recommended Agent Profile**:
  - **Category**: `quick`
    - Reason: Simple column addition to existing component
  - **Skills**: []

  **Parallelization**:
  - **Can Run In Parallel**: YES
  - **Parallel Group**: Wave 2 (with Tasks 2, 6, 15)
  - **Blocks**: None
  - **Blocked By**: Task 10

  **References**:
  - `frontend/src/routes/merchant/MerchantList.svelte:259-269` - Table header pattern
  - `frontend/src/routes/merchant/MerchantList.svelte:303-343` - Table row rendering pattern
  - `frontend/src/types/merchant.ts:66-81` - MerchantDto with primaryContact field

  **Acceptance Criteria**:
  - [ ] MerchantList.svelte has 10 columns (was 8)
  - [ ] New columns: "담당자" and "연락처"
  - [ ] Agent verification via playwright:
    - Navigate to merchant list
    - Verify "담당자" column header exists
    - Verify "연락처" column header exists
    - Verify data displays in new columns
    - Screenshot: `.sisyphus/evidence/task-11-merchant-columns.png`

  **Commit**: YES
  - Message: `feat(frontend): add contact columns to merchant list`
  - Files: `frontend/src/routes/merchant/MerchantList.svelte`

---

- [ ] 12. Create User Detail Component (Frontend)

  **What to do**:
  - Create `frontend/src/routes/user/UserDetail.svelte`
  - Props: userId
  - Display user info: username, email, fullName, phone, role, status, orgName
  - Edit mode toggle for editable fields (fullName, phone, role, status)
  - Separate "비밀번호 변경" button with modal
  - Delete user with confirmation modal
  - Follow MerchantDetail pattern (edit mode, Card sections)

  **Must NOT do**:
  - Do not show passwordHash
  - Do not implement 2FA toggle
  - Do not allow username/email change (immutable)

  **Recommended Agent Profile**:
  - **Category**: `visual-engineering`
    - Reason: Complex detail component with edit mode and modals
  - **Skills**: [`frontend-ui-ux`]

  **Parallelization**:
  - **Can Run In Parallel**: YES
  - **Parallel Group**: Wave 4 (with Tasks 9, 13)
  - **Blocks**: None
  - **Blocked By**: Task 8

  **References**:
  - `frontend/src/routes/merchant/MerchantDetail.svelte:1-330` - Detail component pattern
  - `frontend/src/components/ConfirmModal.svelte` - Delete confirmation
  - `frontend/src/lib/userApi.ts` - API client (from Task 8)
  - `frontend/src/types/user.ts` - Types (from Task 8)

  **Acceptance Criteria**:
  - [ ] File exists: `frontend/src/routes/user/UserDetail.svelte`
  - [ ] Agent verification via playwright:
    - Navigate to user detail page
    - Verify user info displays correctly
    - Click edit button → fields become editable
    - Make change and save → API called
    - Click password change → modal opens
    - Screenshot: `.sisyphus/evidence/task-12-user-detail.png`

  **Commit**: YES (groups with Task 13)
  - Message: `feat(frontend): add UserDetail and UserRegistration components`
  - Files: `frontend/src/routes/user/UserDetail.svelte`, `frontend/src/routes/user/UserRegistration.svelte`

---

- [ ] 13. Create User Registration Component (Frontend)

  **What to do**:
  - Create `frontend/src/routes/user/UserRegistration.svelte`
  - Form fields: username, email, password, confirmPassword, fullName, phone, role, orgId
  - Organization selector using OrganizationPickerDialog
  - Password requirements: min 8 chars, show/hide toggle
  - Client-side validation: required fields, email format, password match
  - Success state: show confirmation, navigate to user list
  - Follow BranchRegistration pattern (wizard or single-page form)

  **Must NOT do**:
  - Do not implement email verification
  - Do not add 2FA setup
  - Do not add permissions configuration (admin feature)

  **Recommended Agent Profile**:
  - **Category**: `visual-engineering`
    - Reason: Form component with validation and organization picker
  - **Skills**: [`frontend-ui-ux`]

  **Parallelization**:
  - **Can Run In Parallel**: YES
  - **Parallel Group**: Wave 4 (with Tasks 9, 12)
  - **Blocks**: None
  - **Blocked By**: Task 8

  **References**:
  - `frontend/src/routes/branch/BranchRegistration.svelte` - Registration form pattern
  - `frontend/src/components/OrganizationPickerDialog.svelte` - Org selector
  - `frontend/src/lib/userApi.ts` - API client (from Task 8)
  - `frontend/src/types/user.ts` - Types (from Task 8)

  **Acceptance Criteria**:
  - [ ] File exists: `frontend/src/routes/user/UserRegistration.svelte`
  - [ ] Agent verification via playwright:
    - Navigate to user registration
    - Fill all required fields
    - Select organization via picker
    - Submit form → user created
    - Verify success state displays
    - Screenshot: `.sisyphus/evidence/task-13-user-registration.png`

  **Commit**: YES (groups with Task 12)
  - Message: `feat(frontend): add UserDetail and UserRegistration components`
  - Files: `frontend/src/routes/user/UserDetail.svelte`, `frontend/src/routes/user/UserRegistration.svelte`

---

### Feature 4: Merchant List Improvement (Additional Tasks)

- [ ] 14. Create Contact Manager Component

  **What to do**:
  - Create `frontend/src/components/ContactManager.svelte`
  - Props: `entityType: 'MERCHANT' | 'BUSINESS_ENTITY'`, `entityId: string`
  - Display list of contacts with name, phone, email, role, primary badge
  - Add contact dialog: name, phone, email, role dropdown
  - Edit contact: inline edit or modal
  - Delete contact: confirmation dialog
  - Set primary: one-click action
  - Contact roles: PRIMARY, SECONDARY, SETTLEMENT, TECHNICAL

  **Must NOT do**:
  - Do not duplicate Contact API calls (use existing endpoints)
  - Do not add contact search

  **Recommended Agent Profile**:
  - **Category**: `visual-engineering`
    - Reason: CRUD component with modals
  - **Skills**: [`frontend-ui-ux`]

  **Parallelization**:
  - **Can Run In Parallel**: YES
  - **Parallel Group**: Wave 1 (with Tasks 1, 5, 10)
  - **Blocks**: Task 15
  - **Blocked By**: None

  **References**:
  - `src/main/java/com/korpay/billpay/controller/api/ContactController.java:1-88` - Contact API endpoints
  - `frontend/src/types/merchant.ts:46-64` - ContactDto and ContactRole already defined
  - `frontend/src/components/SettlementAccountManager.svelte` - Similar component pattern (from Task 2)

  **Acceptance Criteria**:
  - [ ] File exists: `frontend/src/components/ContactManager.svelte`
  - [ ] File exists: `frontend/src/lib/contactApi.ts` (API client for contacts)
  - [ ] Agent verification via playwright:
    - Mount component with test entity
    - Verify contact list renders
    - Add new contact → appears in list
    - Set contact as primary → badge updates
    - Screenshot: `.sisyphus/evidence/task-14-contact-manager.png`

  **Commit**: YES
  - Message: `feat(frontend): add ContactManager component and contact API client`
  - Files: `frontend/src/components/ContactManager.svelte`, `frontend/src/lib/contactApi.ts`

---

- [ ] 15. Integrate Contact Manager in MerchantDetail

  **What to do**:
  - Replace single contact display in MerchantDetail with ContactManager
  - Keep existing "담당자정보" Card but embed ContactManager inside
  - Pass entityType="MERCHANT" and entityId={merchant.id}
  - Remove edit fields for single contact (now managed by ContactManager)

  **Must NOT do**:
  - Do not break existing edit mode for other fields
  - Do not remove the Card wrapper

  **Recommended Agent Profile**:
  - **Category**: `quick`
    - Reason: Simple component integration
  - **Skills**: []

  **Parallelization**:
  - **Can Run In Parallel**: YES
  - **Parallel Group**: Wave 2 (with Tasks 2, 6, 11)
  - **Blocks**: None
  - **Blocked By**: Task 14

  **References**:
  - `frontend/src/routes/merchant/MerchantDetail.svelte:255-286` - Current "담당자정보" Card section
  - `frontend/src/components/ContactManager.svelte` - Component to integrate (from Task 14)

  **Acceptance Criteria**:
  - [ ] MerchantDetail.svelte imports ContactManager
  - [ ] "담당자정보" section uses ContactManager component
  - [ ] Agent verification via playwright:
    - Navigate to merchant detail
    - Verify ContactManager renders in basic info tab
    - Add a new contact via ContactManager
    - Verify contact appears in list
    - Screenshot: `.sisyphus/evidence/task-15-merchant-contacts.png`

  **Commit**: YES
  - Message: `feat(frontend): integrate ContactManager in MerchantDetail`
  - Files: `frontend/src/routes/merchant/MerchantDetail.svelte`

---

## Commit Strategy

| After Task | Message | Files | Verification |
|------------|---------|-------|--------------|
| 1 | `feat(frontend): add settlement account API client and types` | 2 files | bun run check |
| 2 | `feat(frontend): add SettlementAccountManager component` | 1 file | playwright |
| 3+4 | `feat(frontend): integrate settlement accounts in merchant and branch detail` | 2 files | playwright |
| 5 | `feat(backend): add User DTOs for CRUD operations` | 3 files | gradlew compileJava |
| 6 | `feat(backend): add UserService with CRUD and access control` | 1 file | gradlew compileJava |
| 7 | `feat(backend): add UserController with REST endpoints` | 1 file | curl test |
| 8 | `feat(frontend): add user API client and types` | 2 files | bun run check |
| 9 | `feat(frontend): add UserList component with infinite scroll` | 1 file | playwright |
| 10 | `feat(backend): include primaryContact in merchant list response` | 2 files | curl test |
| 11 | `feat(frontend): add contact columns to merchant list` | 1 file | playwright |
| 12+13 | `feat(frontend): add UserDetail and UserRegistration components` | 2 files | playwright |
| 14 | `feat(frontend): add ContactManager component and contact API client` | 2 files | playwright |
| 15 | `feat(frontend): integrate ContactManager in MerchantDetail` | 1 file | playwright |

---

## Success Criteria

### Verification Commands

```bash
# Backend compilation
./gradlew compileJava
# Expected: BUILD SUCCESSFUL

# Frontend type check
cd frontend && bun run check
# Expected: No errors

# Backend API test (Users)
curl -s http://localhost:8080/api/v1/users \
  -H "Authorization: Bearer $TOKEN" \
  -H "X-Tenant-ID: tenant_001"
# Expected: {"success":true,"data":{"content":[...]}}

# Backend API test (Merchants with contacts)
curl -s http://localhost:8080/api/v1/merchants \
  -H "Authorization: Bearer $TOKEN" \
  -H "X-Tenant-ID: tenant_001" \
  | jq '.data.content[0].primaryContact'
# Expected: Contact object or null
```

### Final Checklist
- [ ] All 15 tasks completed
- [ ] All commits pushed
- [ ] Settlement account CRUD works in MerchantDetail and BranchDetail
- [ ] User CRUD works via UserList, UserDetail, UserRegistration
- [ ] Merchant list shows 담당자 and 연락처 columns
- [ ] ContactManager works in MerchantDetail
- [ ] No TypeScript errors
- [ ] No Java compilation errors
- [ ] No console errors in browser
