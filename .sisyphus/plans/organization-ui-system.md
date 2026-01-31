# Organization Registration/Edit System UI

## TL;DR

> **Quick Summary**: Build a complete organization CRUD system in Svelte 5 with hierarchical tree view (ltree-based), master-detail layout, auto-generated orgCode, permission-aware parent selection, and full form validation. Integrates with 5 existing backend APIs.
> 
> **Deliverables**:
> - Organizations.svelte route (master-detail layout with tree + form)
> - OrgTree.svelte component (expandable/collapsible hierarchy)
> - OrgForm.svelte component (create/edit with validation)
> - Type definitions (Organization, CreateOrgRequest, UpdateOrgRequest)
> - Sidebar navigation integration
> - Automated verification procedures
> 
> **Estimated Effort**: Medium (8-12 components, 5 API integrations)
> **Parallel Execution**: YES - 3 waves
> **Critical Path**: Task 1 (types) → Task 6 (main route) → Task 10 (integration)

---

## Context

### Original Request
User: "영업조직 등록/변경 시스템 화면 만들어 줘"
(Create organization registration/edit system screen)

### Interview Summary
**Key Discussions**:
- **Layout**: Master-detail pattern (tree left 30%, form right 70%) with responsive mobile fallback
- **orgCode**: Auto-generated, read-only format: `{parent.path}.{type_prefix}_{sequence}`
- **Permissions**: User sees own org + descendants only; can create one level below own orgType
- **Validation**: Required fields + email format + Korean phone (010-XXXX-XXXX)
- **Status Changes**: Simple confirmation dialog (no cascade logic initially)
- **Config Field**: Skip for initial version (future enhancement)
- **Tree Features**: Expandable/collapsible, show org count per node
- **Parent Selector**: Dropdown with indented labels

**Research Findings**:
- Existing frontend uses Svelte 5 runes consistently ($state, $derived, $effect)
- API client provides generic typing, auto token injection, 401 auto-refresh
- Standard patterns: try-catch error handling, loading states, status mapping with Record<string, string>
- Formatting: Intl API for currency/numbers, date-fns for dates
- Librarian research confirmed best practices for hierarchical trees and form validation with runes

### Backend APIs (Confirmed Working)
- **POST /v1/organizations** - Create new organization
  - Required: orgCode, name, orgType, parentId
  - Optional: businessNumber, businessName, representativeName, email, phone, address, config
  
- **PUT /v1/organizations/{id}** - Update existing organization
  - All fields optional: name, status, businessNumber, etc.
  
- **GET /v1/organizations** - List organizations (paginated)
  - Query params: page, size, sortBy, direction
  - Returns: PagedResponse<Organization>
  
- **GET /v1/organizations/{id}** - Get single organization
  - Returns: ApiResponse<Organization>
  
- **GET /v1/organizations/{id}/descendants** - Get sub-organizations
  - Returns: ApiResponse<Organization[]>

### Organization Hierarchy (PRD-02)
- 5-level structure: DISTRIBUTOR → AGENCY → DEALER → SELLER → VENDOR
- ltree paths in PostgreSQL: `dist_001.agcy_001.deal_001`
- orgCode prefixes: dist_, agcy_, deal_, sell_, vend_
- Permission model: upward visibility blocked, can create one level below only

---

## Work Objectives

### Core Objective
Implement a complete organization management UI with hierarchical tree visualization, CRUD operations (create/edit/list), form validation, and permission-aware interactions following existing Svelte 5 patterns.

### Concrete Deliverables
- `frontend/src/routes/Organizations.svelte` - Main route with master-detail layout
- `frontend/src/components/OrgTree.svelte` - Recursive tree component
- `frontend/src/components/OrgTreeNode.svelte` - Individual tree node
- `frontend/src/components/OrgForm.svelte` - Create/edit form
- `frontend/src/components/ParentSelector.svelte` - Parent organization dropdown
- `frontend/src/components/ConfirmModal.svelte` - Reusable confirmation modal
- `frontend/src/types/api.ts` - Updated with org types
- `frontend/src/components/Sidebar.svelte` - Updated with navigation

### Definition of Done
- [ ] User can view hierarchical organization tree with expand/collapse
- [ ] User can create new organization (auto-generated orgCode, validated fields)
- [ ] User can edit existing organization (name, status, contact info)
- [ ] User can change organization status with confirmation
- [ ] Parent selector shows only eligible organizations based on permissions
- [ ] All forms validate required fields, email format, Korean phone format
- [ ] Error messages display for validation failures and API errors
- [ ] Loading states show during API calls
- [ ] Navigation to Organizations route works from Sidebar
- [ ] Responsive layout adapts to mobile (<768px switches to tabs)

### Must Have
- Master-detail layout (tree + form side-by-side)
- Auto-generated orgCode (read-only, format: `parent.path.type_prefix_sequence`)
- Permission-aware parent selection (only current org + descendants)
- Form validation (required fields, email, Korean phone)
- Hierarchical tree with expand/collapse
- Status change confirmation dialog
- Error handling and loading states
- Integration with existing patterns ($state, $derived, $effect)

### Must NOT Have (Guardrails)
- **No organization deletion** (not in API spec, not requested)
- **No config field editor** (skip for initial version per agreement)
- **No cascade status changes** (future enhancement)
- **No organization transfer** (changing parent - not in scope)
- **No bulk operations** (single org at a time)
- **No export/import** (not requested)
- **No over-abstraction** (don't create generic tree library, implement specific use case)
- **No AI slop patterns**:
  - Don't add unused props "for future flexibility"
  - Don't create 5 abstraction layers for a simple tree
  - Don't add 20 validation checks for 3 inputs
  - Don't generate JSDoc everywhere (follow existing code style)

---

## Verification Strategy

> This project uses **Manual Verification Only** (no test infrastructure).
> All acceptance criteria use **agent-executable** procedures via Bash and playwright skill.

### Test Decision
- **Infrastructure exists**: NO (no test framework configured)
- **User wants tests**: NO (not requested, manual QA acceptable)
- **Framework**: N/A
- **QA approach**: Manual verification with detailed procedures

### Manual Verification Procedures

**All verification steps are agent-executable using:**
- **Frontend/UI**: Playwright browser automation (via playwright skill)
- **API calls**: curl via Bash
- **File checks**: cat, grep via Bash

**Evidence Requirements (Agent-Executable)**:
- Screenshots saved to `.sisyphus/evidence/`
- Terminal output captured and verified
- Browser DOM assertions via Playwright

**Example Frontend Verification** (Task 6):
```
# Agent executes via playwright skill:
1. Navigate to: http://localhost:5173
2. Login with test credentials
3. Click: Sidebar button with text "조직 관리"
4. Wait for: selector ".org-tree" to be visible
5. Assert: text "Organizations" appears in header
6. Screenshot: .sisyphus/evidence/task-6-org-route.png
```

---

## Execution Strategy

### Parallel Execution Waves

> Maximize throughput by grouping independent tasks into parallel waves.
> Each wave completes before the next begins.

```
Wave 1 (Start Immediately - Foundation):
├── Task 1: Add Organization Types to api.ts
├── Task 2: Create ConfirmModal Component
└── Task 3: Create OrgTreeNode Component

Wave 2 (After Wave 1 - Core Components):
├── Task 4: Create OrgTree Component (depends: 3)
├── Task 5: Create ParentSelector Component (depends: 1)
└── Task 7: Create OrgForm Component (depends: 1, 2)

Wave 3 (After Wave 2 - Integration):
├── Task 6: Create Organizations.svelte Route (depends: 4, 5, 7)
└── Task 8: Update Sidebar Navigation (independent)

Wave 4 (After Wave 3 - Final Polish):
├── Task 9: Add Responsive Styles (depends: 6)
└── Task 10: End-to-End Verification (depends: 6, 8, 9)

Critical Path: Task 1 → Task 5 → Task 6 → Task 9 → Task 10
Parallel Speedup: ~50% faster than sequential (10 tasks → 4 waves)
```

### Dependency Matrix

| Task | Depends On | Blocks | Can Parallelize With |
|------|------------|--------|---------------------|
| 1 | None | 5, 7 | 2, 3 |
| 2 | None | 7 | 1, 3 |
| 3 | None | 4 | 1, 2 |
| 4 | 3 | 6 | 5, 7 |
| 5 | 1 | 6 | 4, 7 |
| 7 | 1, 2 | 6 | 4, 5 |
| 6 | 4, 5, 7 | 9, 10 | 8 |
| 8 | None | 10 | 6 |
| 9 | 6 | 10 | None (needs 6) |
| 10 | 6, 8, 9 | None | None (final) |

### Agent Dispatch Summary

| Wave | Tasks | Recommended Agents |
|------|-------|-------------------|
| 1 | 1, 2, 3 | 3x visual-engineering (parallel) |
| 2 | 4, 5, 7 | 3x visual-engineering (parallel) |
| 3 | 6, 8 | 2x visual-engineering (parallel) |
| 4 | 9, 10 | visual-engineering (sequential: 9 → 10) |

**Dispatch Command Example**:
```typescript
// Wave 1
delegate_task(category="visual-engineering", load_skills=["frontend-ui-ux"], prompt="[Task 1 details]", run_in_background=true)
delegate_task(category="visual-engineering", load_skills=["frontend-ui-ux"], prompt="[Task 2 details]", run_in_background=true)
delegate_task(category="visual-engineering", load_skills=["frontend-ui-ux"], prompt="[Task 3 details]", run_in_background=true)
// Wait for Wave 1 completion, then dispatch Wave 2...
```

---

## TODOs

> Implementation + Verification = ONE Task. Never separate.
> EVERY task MUST have: Recommended Agent Profile + Parallelization info.

---

### WAVE 1: Foundation (Parallel Start)

---

- [ ] 1. Add Organization Types to api.ts

  **What to do**:
  - Add `OrgType` enum: DISTRIBUTOR, AGENCY, DEALER, SELLER, VENDOR
  - Add `OrgStatus` enum: ACTIVE, SUSPENDED, TERMINATED
  - Update existing `Organization` interface (already exists, verify fields)
  - Add `CreateOrgRequest` interface with required/optional fields
  - Add `UpdateOrgRequest` interface with all optional fields
  - Add `OrgTree` interface for hierarchical data structure

  **Must NOT do**:
  - Don't modify existing interfaces unless adding missing fields
  - Don't create overly generic types (keep it specific to organization domain)
  - Don't add unused fields "for future use"

  **Recommended Agent Profile**:
  - **Category**: `visual-engineering`
    - Reason: Frontend type definitions, requires understanding of Svelte/TypeScript patterns
  - **Skills**: None (straightforward type definition, no special skills needed)
  - **Skills Evaluated but Omitted**:
    - `frontend-ui-ux`: Not needed (no UI/UX design, just type definitions)
    - `api-design-agent`: Not needed (backend API already exists, just adding frontend types)

  **Parallelization**:
  - **Can Run In Parallel**: YES
  - **Parallel Group**: Wave 1 (with Tasks 2, 3)
  - **Blocks**: Tasks 5, 7 (need types for ParentSelector and OrgForm)
  - **Blocked By**: None (can start immediately)

  **References**:

  **Pattern References** (existing code to follow):
  - `frontend/src/types/api.ts:1-130` - Existing type definitions (ApiResponse, PagedResponse, Organization, Merchant, etc.) - follow this structure and naming conventions

  **API/Type References** (contracts to implement against):
  - Backend API spec from user request:
    - POST /v1/organizations: required (orgCode, name, orgType, parentId), optional (businessNumber, businessName, representativeName, email, phone, address, config)
    - PUT /v1/organizations/{id}: all fields optional (name, status, businessNumber, etc.)
    - Response DTO: id, orgCode, name, orgType, path, level/depth, status, timestamps, contact fields

  **External References**:
  - TypeScript handbook enum syntax: https://www.typescriptlang.org/docs/handbook/enums.html

  **WHY Each Reference Matters**:
  - `frontend/src/types/api.ts` - Shows existing naming conventions (camelCase for fields, PascalCase for types), response wrapper pattern (ApiResponse<T>), and how related types are grouped
  - Backend API spec - Provides exact field names and requirement levels, ensuring type definitions match what backend expects/returns
  - Organization hierarchy from PRD-02 - Defines the 5 orgType enum values and ltree path structure

  **Acceptance Criteria**:

  **Automated Verification** (File Checks via Bash):
  ```bash
  # Agent runs:
  cat frontend/src/types/api.ts | grep -A 5 "export enum OrgType"
  # Assert: Output contains "DISTRIBUTOR, AGENCY, DEALER, SELLER, VENDOR"
  
  cat frontend/src/types/api.ts | grep -A 5 "export enum OrgStatus"
  # Assert: Output contains "ACTIVE, SUSPENDED, TERMINATED"
  
  cat frontend/src/types/api.ts | grep -A 15 "export interface CreateOrgRequest"
  # Assert: Output contains required fields: orgCode, name, orgType, parentId
  
  cat frontend/src/types/api.ts | grep -A 15 "export interface UpdateOrgRequest"
  # Assert: All fields optional (use "?" syntax)
  
  cat frontend/src/types/api.ts | grep -A 10 "export interface OrgTree"
  # Assert: Contains id, name, path, children?: OrgTree[]
  ```

  **Evidence to Capture**:
  - [ ] Terminal output showing grep results for all new types
  - [ ] Confirm file compiles without TypeScript errors (run: `cd frontend && bun run build`)

  **Commit**: YES
  - Message: `feat(frontend): add organization type definitions`
  - Files: `frontend/src/types/api.ts`
  - Pre-commit: `cd frontend && bun run build` (verify TypeScript compiles)

---

- [ ] 2. Create ConfirmModal Component

  **What to do**:
  - Create `frontend/src/components/ConfirmModal.svelte`
  - Props: show (boolean), title (string), message (string), confirmText (string), cancelText (string), type ('warning'|'danger'|'info'), onConfirm (callback), onCancel (callback)
  - Use Svelte 5 $props() for prop declaration
  - Implement modal backdrop with fade transition
  - Implement modal content with slide transition
  - Style with existing color scheme (warning/danger/info variants)
  - Make responsive (mobile-friendly)

  **Must NOT do**:
  - Don't add unused features (e.g., multi-step modals, form inputs inside modal)
  - Don't create generic "ModalProvider" context (keep it simple, direct prop usage)
  - Don't add animations beyond fade/slide (keep it lightweight)

  **Recommended Agent Profile**:
  - **Category**: `visual-engineering`
    - Reason: UI component with styling, transitions, and responsive design
  - **Skills**: `frontend-ui-ux`
    - `frontend-ui-ux`: Needed for modal UX patterns, backdrop behavior, focus management, and responsive design
  - **Skills Evaluated but Omitted**:
    - None (frontend-ui-ux covers all needed aspects)

  **Parallelization**:
  - **Can Run In Parallel**: YES
  - **Parallel Group**: Wave 1 (with Tasks 1, 3)
  - **Blocks**: Task 7 (OrgForm needs ConfirmModal for status changes)
  - **Blocked By**: None (can start immediately)

  **References**:

  **Pattern References** (existing code to follow):
  - Librarian research findings - Toast notifications + confirmation modals pattern (from research output)
  - `frontend/src/routes/Dashboard.svelte:20-30` - Example of $state usage with loading/error states
  - `frontend/src/app.css` - Global color variables (--error-color, --warning-color, --info-color)

  **API/Type References** (contracts to implement against):
  - Props interface:
    ```typescript
    interface Props {
      show: boolean;
      title: string;
      message: string;
      confirmText?: string; // default: "Confirm"
      cancelText?: string;  // default: "Cancel"
      type?: 'warning' | 'danger' | 'info'; // default: 'warning'
      onConfirm: () => void;
      onCancel: () => void;
    }
    ```

  **External References**:
  - Svelte 5 transitions: https://svelte-5-preview.vercel.app/docs/transitions
  - Modal accessibility: https://www.w3.org/WAI/ARIA/apg/patterns/dialog-modal/

  **WHY Each Reference Matters**:
  - Librarian research - Provides production-ready modal pattern with backdrop, transitions, and responsive design
  - Dashboard.svelte - Shows correct Svelte 5 runes usage ($state) which must be used in this component
  - app.css - Ensures color consistency with existing UI (use same CSS variables for warning/danger/info)
  - Svelte 5 transitions - Official API for fade/slide effects
  - Modal accessibility - Ensures keyboard navigation (Escape key), focus trap, and ARIA attributes work correctly

  **Acceptance Criteria**:

  **Automated Verification** (Playwright Browser Automation):
  ```
  # Agent executes via playwright skill:
  1. Create test HTML file that imports ConfirmModal component
  2. Navigate to test page in browser
  3. Click button to show modal with type='warning'
  4. Assert: modal backdrop visible (selector: ".modal-backdrop")
  5. Assert: modal content visible with correct title and message
  6. Assert: "Confirm" and "Cancel" buttons present
  7. Click Cancel button
  8. Assert: modal backdrop no longer visible
  9. Screenshot: .sisyphus/evidence/task-2-confirm-modal.png
  ```

  **Evidence to Capture**:
  - [ ] Screenshot showing modal with warning type styling
  - [ ] Terminal output confirming Escape key closes modal
  - [ ] Screenshot showing responsive layout on mobile (narrow viewport)

  **Commit**: YES
  - Message: `feat(frontend): add reusable confirmation modal component`
  - Files: `frontend/src/components/ConfirmModal.svelte`
  - Pre-commit: `cd frontend && bun run build`

---

- [ ] 3. Create OrgTreeNode Component

  **What to do**:
  - Create `frontend/src/components/OrgTreeNode.svelte`
  - Props: node (OrgTree), level (number, default 0), selectedId (string|null), onSelect (callback)
  - Use Svelte 5 $props() and $state for expand/collapse state
  - Implement recursive rendering (svelte:self) for children
  - Show expand/collapse icon (▶/▼) only if hasChildren
  - Display org icon based on depth/type (🏢 DISTRIBUTOR, 🏪 AGENCY, etc.)
  - Display org name, orgCode, and status badge
  - Indent based on level (level * 20px padding-left)
  - Highlight selected node
  - Clickable node label to trigger onSelect(node)

  **Must NOT do**:
  - Don't add drag-and-drop (not requested, out of scope)
  - Don't add context menu (not needed for MVP)
  - Don't add inline editing (editing happens in form panel)
  - Don't make it a generic tree library (specific to Organization domain)

  **Recommended Agent Profile**:
  - **Category**: `visual-engineering`
    - Reason: Recursive UI component with state management and styling
  - **Skills**: `frontend-ui-ux`
    - `frontend-ui-ux`: Needed for tree node UX (hover states, selection highlighting, icon selection)
  - **Skills Evaluated but Omitted**:
    - None (frontend-ui-ux covers all needed aspects)

  **Parallelization**:
  - **Can Run In Parallel**: YES
  - **Parallel Group**: Wave 1 (with Tasks 1, 2)
  - **Blocks**: Task 4 (OrgTree uses OrgTreeNode)
  - **Blocked By**: None (can start immediately, types can be inferred)

  **References**:

  **Pattern References** (existing code to follow):
  - Librarian research findings - Custom tree component pattern with recursive svelte:self (from research output)
  - `frontend/src/routes/Transactions.svelte:` - Example of clickable rows with hover states

  **API/Type References** (contracts to implement against):
  - Props interface:
    ```typescript
    interface Props {
      node: OrgTree; // from Task 1
      level?: number; // default 0
      selectedId?: string | null;
      onSelect: (node: OrgTree) => void;
    }
    ```
  - OrgTree structure (from Task 1):
    ```typescript
    interface OrgTree {
      id: string;
      name: string;
      orgCode: string;
      orgType: OrgType;
      path: string;
      status: OrgStatus;
      children?: OrgTree[];
    }
    ```

  **External References**:
  - Svelte recursive components: https://svelte.dev/docs/special-elements#svelte-self

  **WHY Each Reference Matters**:
  - Librarian research - Provides production-ready recursive tree pattern with expand/collapse state management
  - Transactions.svelte - Shows how to implement hover effects and clickable rows in existing codebase style
  - Svelte recursive components - Official API for svelte:self pattern needed for tree recursion
  - OrgType enum - Determines which icon to display (need mapping: DISTRIBUTOR → 🏢, AGENCY → 🏪, etc.)

  **Acceptance Criteria**:

  **Automated Verification** (Playwright Browser Automation):
  ```
  # Agent executes via playwright skill:
  1. Create test page with OrgTreeNode and sample 3-level tree data
  2. Navigate to test page
  3. Assert: Root node visible with icon and name
  4. Assert: Expand icon (▶) visible for node with children
  5. Click expand icon
  6. Assert: Child nodes visible with increased indentation
  7. Assert: Expand icon changed to collapse icon (▼)
  8. Click node label
  9. Assert: Node highlighted (background color change)
  10. Screenshot: .sisyphus/evidence/task-3-tree-node.png
  ```

  **Evidence to Capture**:
  - [ ] Screenshot showing expanded tree with 3 levels
  - [ ] Screenshot showing selected node highlighting
  - [ ] Terminal output confirming onSelect callback triggered

  **Commit**: YES
  - Message: `feat(frontend): add recursive organization tree node component`
  - Files: `frontend/src/components/OrgTreeNode.svelte`
  - Pre-commit: `cd frontend && bun run build`

---

### WAVE 2: Core Components (After Wave 1)

---

- [ ] 4. Create OrgTree Component

  **What to do**:
  - Create `frontend/src/components/OrgTree.svelte`
  - Props: organizations (OrgTree[]), selectedId (string|null), onSelect (callback), loading (boolean), error (string|null)
  - Fetch descendants from API when tree node expands
  - Use $state for expanded node IDs (Set<string>)
  - Render each root org using OrgTreeNode (from Task 3)
  - Show loading spinner during API fetch
  - Show error message if API fails
  - Implement search/filter input (optional: filters by org name/code)

  **Must NOT do**:
  - Don't load entire tree at once (use lazy loading via descendants API)
  - Don't add sorting options (tree shows hierarchy, not flat list)
  - Don't add pagination (tree is hierarchical, not paged)

  **Recommended Agent Profile**:
  - **Category**: `visual-engineering`
    - Reason: Component with API integration, state management, and child component composition
  - **Skills**: `frontend-ui-ux`
    - `frontend-ui-ux`: Needed for loading states, error display, and search input UX
  - **Skills Evaluated but Omitted**:
    - None (frontend-ui-ux covers all aspects)

  **Parallelization**:
  - **Can Run In Parallel**: YES
  - **Parallel Group**: Wave 2 (with Tasks 5, 7)
  - **Blocks**: Task 6 (Organizations.svelte uses OrgTree)
  - **Blocked By**: Task 3 (needs OrgTreeNode component)

  **References**:

  **Pattern References** (existing code to follow):
  - `frontend/src/routes/Dashboard.svelte:83-end` - $effect usage for data loading on mount
  - `frontend/src/routes/Transactions.svelte:` - Error/loading pattern with try-catch
  - `frontend/src/lib/api.ts:100-102` - apiClient.get<T> usage pattern
  - `frontend/src/components/OrgTreeNode.svelte` - Child component to render (from Task 3)

  **API/Type References** (contracts to implement against):
  - Props interface:
    ```typescript
    interface Props {
      organizations: OrgTree[];
      selectedId?: string | null;
      onSelect: (org: OrgTree) => void;
      loading?: boolean;
      error?: string | null;
    }
    ```
  - API endpoint: GET /v1/organizations/{id}/descendants → ApiResponse<Organization[]>
  - Need to transform flat Organization[] to hierarchical OrgTree[] structure

  **External References**:
  - ltree path parsing for building hierarchy

  **WHY Each Reference Matters**:
  - Dashboard.svelte - Shows correct $effect pattern for API loading (useEffect equivalent in Svelte 5)
  - Transactions.svelte - Shows consistent error/loading state pattern used across codebase
  - api.ts - Shows how to use apiClient with generic types (apiClient.get<T>)
  - OrgTreeNode - Child component that this component wraps/renders
  - Descendants API - Provides flat list that needs transformation to tree structure (path-based grouping)

  **Acceptance Criteria**:

  **Automated Verification** (Playwright + API):
  ```bash
  # Agent runs (API check):
  curl -s http://localhost:8080/api/v1/organizations/[test-org-id]/descendants \
    -H "Authorization: Bearer [test-token]" \
    -H "X-Tenant-ID: [test-tenant]" | jq '.success'
  # Assert: Output is "true"
  ```
  
  ```
  # Agent executes via playwright skill:
  1. Navigate to test page with OrgTree component
  2. Mock API response with 2-level tree data
  3. Assert: Root nodes rendered (selector: ".tree-node")
  4. Assert: Loading spinner NOT visible
  5. Click expand icon on root node
  6. Wait for API call to complete
  7. Assert: Child nodes rendered
  8. Screenshot: .sisyphus/evidence/task-4-org-tree.png
  ```

  **Evidence to Capture**:
  - [ ] Screenshot showing expanded tree with loaded descendants
  - [ ] Terminal output showing successful API call
  - [ ] Screenshot showing error state if API fails

  **Commit**: YES
  - Message: `feat(frontend): add organization tree component with lazy loading`
  - Files: `frontend/src/components/OrgTree.svelte`
  - Pre-commit: `cd frontend && bun run build`

---

- [ ] 5. Create ParentSelector Component

  **What to do**:
  - Create `frontend/src/components/ParentSelector.svelte`
  - Props: selectedParentId (string|null), currentUserOrgId (string), currentUserOrgType (OrgType), onSelect (callback), disabled (boolean)
  - Fetch eligible parent organizations from API (current org + descendants)
  - Use $state for parent options (Organization[])
  - Use $derived to compute allowed child orgType based on selected parent
  - Render dropdown <select> with indented labels showing hierarchy depth
  - Format option labels: "└─ Org Name (orgCode) [TYPE]"
  - Disable if no eligible parents or disabled prop is true

  **Must NOT do**:
  - Don't show organizations above current user in hierarchy (permission rule)
  - Don't allow creating same-level orgs (only one level below)
  - Don't add tree picker (dropdown is simpler, per agreement)

  **Recommended Agent Profile**:
  - **Category**: `visual-engineering`
    - Reason: Form input component with API integration and permission logic
  - **Skills**: `frontend-ui-ux`
    - `frontend-ui-ux`: Needed for dropdown UX, option formatting, and disabled states
  - **Skills Evaluated but Omitted**:
    - None (frontend-ui-ux covers all aspects)

  **Parallelization**:
  - **Can Run In Parallel**: YES
  - **Parallel Group**: Wave 2 (with Tasks 4, 7)
  - **Blocks**: Task 6 (Organizations.svelte uses ParentSelector in create mode)
  - **Blocked By**: Task 1 (needs OrgType enum and types)

  **References**:

  **Pattern References** (existing code to follow):
  - Librarian research findings - Cascading selects with derived options (from research output)
  - `frontend/src/routes/MerchantManagement.svelte:` - Dropdown with API-loaded options
  - `frontend/src/types/api.ts` - OrgType enum (from Task 1)

  **API/Type References** (contracts to implement against):
  - Props interface:
    ```typescript
    interface Props {
      selectedParentId?: string | null;
      currentUserOrgId: string;
      currentUserOrgType: OrgType;
      onSelect: (parentId: string, allowedChildType: OrgType) => void;
      disabled?: boolean;
    }
    ```
  - API endpoint: GET /v1/organizations/{currentUserOrgId}/descendants → ApiResponse<Organization[]>
  - Permission rules (from PRD-02):
    - DISTRIBUTOR can create AGENCY only
    - AGENCY can create DEALER only
    - DEALER can create SELLER only
    - SELLER can create VENDOR only
    - VENDOR cannot create (leaf level)

  **External References**:
  - Organization hierarchy from PRD-02 (5-level structure)

  **WHY Each Reference Matters**:
  - Librarian research - Provides pattern for derived options based on parent selection
  - MerchantManagement.svelte - Shows how to load dropdown options from API in existing codebase
  - OrgType enum - Needed to compute next allowed level (DISTRIBUTOR → AGENCY, etc.)
  - Descendants API - Provides list of eligible parent orgs (current user's org + all below)
  - Permission rules - Critical business logic for which orgs user can create under which parents

  **Acceptance Criteria**:

  **Automated Verification** (Playwright + API):
  ```bash
  # Agent runs (API check):
  curl -s http://localhost:8080/api/v1/organizations/[test-distributor-id]/descendants \
    -H "Authorization: Bearer [test-token]" \
    -H "X-Tenant-ID: [test-tenant]" | jq '.data | length'
  # Assert: Returns number > 0
  ```
  
  ```
  # Agent executes via playwright skill:
  1. Navigate to test page with ParentSelector
  2. Set currentUserOrgType = DISTRIBUTOR
  3. Assert: Dropdown options loaded (selector: "select option")
  4. Assert: Dropdown NOT disabled
  5. Select first parent option
  6. Assert: onSelect callback called with parentId and allowedChildType=AGENCY
  7. Change currentUserOrgType to VENDOR
  8. Assert: Dropdown disabled (VENDOR cannot create children)
  9. Screenshot: .sisyphus/evidence/task-5-parent-selector.png
  ```

  **Evidence to Capture**:
  - [ ] Screenshot showing dropdown with indented options
  - [ ] Terminal output showing onSelect callback parameters
  - [ ] Screenshot showing disabled state for VENDOR user

  **Commit**: YES
  - Message: `feat(frontend): add permission-aware parent selector component`
  - Files: `frontend/src/components/ParentSelector.svelte`
  - Pre-commit: `cd frontend && bun run build`

---

- [ ] 7. Create OrgForm Component

  **What to do**:
  - Create `frontend/src/components/OrgForm.svelte`
  - Props: mode ('create'|'edit'), initialData (Organization|null), currentUserOrgId (string), currentUserOrgType (OrgType), onSubmit (callback), onCancel (callback)
  - Use $state for form fields (name, orgType, parentId, businessNumber, businessName, representativeName, email, phone, address, status)
  - Use $state for touched fields and validation errors
  - Use $derived for field-level validation rules
  - Use $derived for form validity (isValid)
  - Implement auto-generated orgCode (read-only display, computed from parent + type)
  - Show ParentSelector in create mode only (from Task 5)
  - Show status selector in edit mode only
  - Show ConfirmModal for status changes (from Task 2)
  - Validate required fields: name, orgType, parentId (in create mode)
  - Validate email format: RFC 5322 regex
  - Validate Korean phone: 010-XXXX-XXXX or 010XXXXXXXX
  - Show inline error messages for touched + invalid fields
  - Disable submit button if form invalid or submitting
  - Call onSubmit with CreateOrgRequest or UpdateOrgRequest data
  - Handle API errors and show error message

  **Must NOT do**:
  - Don't show config field editor (skipped per agreement)
  - Don't allow editing orgCode (auto-generated, read-only)
  - Don't allow editing parentId in edit mode (no org transfer feature)
  - Don't add business number validation (optional field, no format check per agreement)

  **Recommended Agent Profile**:
  - **Category**: `visual-engineering`
    - Reason: Complex form with validation, conditional fields, and API integration
  - **Skills**: `frontend-ui-ux`
    - `frontend-ui-ux`: Needed for form UX, validation patterns, error display, and conditional field visibility
  - **Skills Evaluated but Omitted**:
    - None (frontend-ui-ux covers all aspects)

  **Parallelization**:
  - **Can Run In Parallel**: YES
  - **Parallel Group**: Wave 2 (with Tasks 4, 5)
  - **Blocks**: Task 6 (Organizations.svelte uses OrgForm)
  - **Blocked By**: Task 1 (needs types), Task 2 (needs ConfirmModal)

  **References**:

  **Pattern References** (existing code to follow):
  - Librarian research findings - Form validation with $derived and touched fields (from research output)
  - `frontend/src/routes/Login.svelte:` - Form submission pattern with loading/error states
  - `frontend/src/routes/MerchantManagement.svelte:` - Example form with validation and error display
  - `frontend/src/components/ParentSelector.svelte` - Component to use (from Task 5)
  - `frontend/src/components/ConfirmModal.svelte` - Component to use for status changes (from Task 2)

  **API/Type References** (contracts to implement against):
  - Props interface:
    ```typescript
    interface Props {
      mode: 'create' | 'edit';
      initialData?: Organization | null;
      currentUserOrgId: string;
      currentUserOrgType: OrgType;
      onSubmit: (data: CreateOrgRequest | UpdateOrgRequest) => Promise<void>;
      onCancel: () => void;
    }
    ```
  - Validation rules:
    - name: required, min 3 chars
    - email: optional but if provided must match /^[^\s@]+@[^\s@]+\.[^\s@]+$/
    - phone: optional but if provided must match /^010-?\d{4}-?\d{4}$/
    - orgType: required in create mode
    - parentId: required in create mode
  - orgCode generation format: `{parent.path}.{type_prefix}_{sequence}`
  - Prefixes: dist, agcy, deal, sell, vend

  **External References**:
  - Korean phone format: https://en.wikipedia.org/wiki/Telephone_numbers_in_South_Korea
  - RFC 5322 email regex

  **WHY Each Reference Matters**:
  - Librarian research - Provides production-ready reactive validation pattern with $derived and touched state
  - Login.svelte - Shows form submission pattern with loading state and error handling used in codebase
  - MerchantManagement.svelte - Shows inline error message display pattern
  - ParentSelector - Child component used in create mode for parent selection
  - ConfirmModal - Child component used in edit mode for status change confirmation
  - orgCode format - Critical business rule for generating unique organization codes

  **Acceptance Criteria**:

  **Automated Verification** (Playwright Browser Automation):
  ```
  # Agent executes via playwright skill (CREATE MODE):
  1. Navigate to test page with OrgForm in create mode
  2. Assert: ParentSelector visible, orgCode field NOT visible (will show after parent selected)
  3. Fill: name = "" (empty)
  4. Blur: name field
  5. Assert: Error message "Name is required" visible
  6. Fill: name = "Test Org"
  7. Fill: email = "invalid-email"
  8. Blur: email field
  9. Assert: Error message "Invalid email format" visible
  10. Fill: email = "test@example.com"
  11. Fill: phone = "010-1234-5678"
  12. Select parent from ParentSelector
  13. Assert: orgCode preview visible with correct format
  14. Assert: Submit button enabled
  15. Click: Submit button
  16. Assert: onSubmit callback called with CreateOrgRequest data
  17. Screenshot: .sisyphus/evidence/task-7-org-form-create.png
  
  # EDIT MODE:
  1. Navigate to test page with OrgForm in edit mode (initialData provided)
  2. Assert: ParentSelector NOT visible
  3. Assert: orgCode field visible and readonly
  4. Assert: Status selector visible
  5. Change status from ACTIVE to SUSPENDED
  6. Assert: ConfirmModal appears
  7. Click: Confirm in modal
  8. Assert: Submit button enabled
  9. Click: Submit button
  10. Assert: onSubmit callback called with UpdateOrgRequest data
  11. Screenshot: .sisyphus/evidence/task-7-org-form-edit.png
  ```

  **Evidence to Capture**:
  - [ ] Screenshot showing create form with validation errors
  - [ ] Screenshot showing edit form with status change confirmation
  - [ ] Terminal output showing onSubmit data structure

  **Commit**: YES
  - Message: `feat(frontend): add organization create/edit form with validation`
  - Files: `frontend/src/components/OrgForm.svelte`
  - Pre-commit: `cd frontend && bun run build`

---

### WAVE 3: Integration (After Wave 2)

---

- [ ] 6. Create Organizations.svelte Route

  **What to do**:
  - Create `frontend/src/routes/Organizations.svelte`
  - Implement master-detail layout: OrgTree (left 30%) + OrgForm (right 70%)
  - Use $state for view mode: 'list' | 'create' | 'edit'
  - Use $state for selectedOrg (Organization|null)
  - Use $state for organizations (root-level orgs for tree)
  - Load root organizations on mount via GET /v1/organizations
  - Render OrgTree with loaded organizations (from Task 4)
  - Render OrgForm conditionally based on view mode (from Task 7)
  - Handle tree node selection → switch to 'edit' mode with selected org
  - Handle "New Organization" button → switch to 'create' mode
  - Handle form submit (create) → POST /v1/organizations → reload tree → switch to 'list'
  - Handle form submit (edit) → PUT /v1/organizations/{id} → reload tree → switch to 'list'
  - Handle form cancel → switch to 'list' mode
  - Show error toast for API failures
  - Show loading overlay during API calls

  **Must NOT do**:
  - Don't implement organization deletion (not in API spec)
  - Don't add bulk operations
  - Don't add export/import
  - Don't add org transfer (changing parent)

  **Recommended Agent Profile**:
  - **Category**: `visual-engineering`
    - Reason: Main route component orchestrating multiple child components and API calls
  - **Skills**: `frontend-ui-ux`
    - `frontend-ui-ux`: Needed for master-detail layout, view state management, and responsive design
  - **Skills Evaluated but Omitted**:
    - None (frontend-ui-ux covers all aspects)

  **Parallelization**:
  - **Can Run In Parallel**: YES (with Task 8 - independent)
  - **Parallel Group**: Wave 3 (with Task 8)
  - **Blocks**: Tasks 9, 10 (need main route for styling and E2E testing)
  - **Blocked By**: Tasks 4, 5, 7 (needs OrgTree, ParentSelector, OrgForm)

  **References**:

  **Pattern References** (existing code to follow):
  - Librarian research findings - CRUD pattern with modal/view state management (from research output)
  - `frontend/src/routes/Dashboard.svelte:` - Route component structure with $effect for data loading
  - `frontend/src/routes/Transactions.svelte:` - Pagination and API error handling
  - `frontend/src/routes/MerchantManagement.svelte:` - Example of managing view modes (list/form)
  - `frontend/src/lib/api.ts:104-109` - apiClient.post and apiClient.put usage
  - `frontend/src/components/OrgTree.svelte` - Child component (from Task 4)
  - `frontend/src/components/OrgForm.svelte` - Child component (from Task 7)

  **API/Type References** (contracts to implement against):
  - API endpoints:
    - GET /v1/organizations?page=0&size=100 → PagedResponse<Organization>
    - POST /v1/organizations → ApiResponse<Organization>
    - PUT /v1/organizations/{id} → ApiResponse<Organization>
  - Layout structure:
    ```html
    <div class="organizations-container">
      <aside class="tree-panel"><!-- OrgTree --></aside>
      <main class="form-panel"><!-- OrgForm or empty state --></main>
    </div>
    ```

  **External References**:
  - CSS Grid for master-detail layout

  **WHY Each Reference Matters**:
  - Librarian research - Provides state machine pattern for view modes (list/create/edit)
  - Dashboard.svelte - Shows route-level $effect pattern for loading data on mount
  - Transactions.svelte - Shows error handling for API failures (toast/alert pattern)
  - MerchantManagement.svelte - Shows how to manage form visibility with state (similar to view mode switching)
  - api.ts - Shows how to call POST/PUT endpoints with correct signatures
  - OrgTree/OrgForm - Child components that this route orchestrates
  - Master-detail layout - Standard pattern for tree + form side-by-side

  **Acceptance Criteria**:

  **Automated Verification** (Playwright Browser Automation):
  ```bash
  # Agent runs (API check):
  curl -s http://localhost:8080/api/v1/organizations?page=0&size=100 \
    -H "Authorization: Bearer [test-token]" \
    -H "X-Tenant-ID: [test-tenant]" | jq '.data.content | length'
  # Assert: Returns number >= 0
  ```
  
  ```
  # Agent executes via playwright skill:
  1. Navigate to: http://localhost:5173
  2. Login with test credentials
  3. Click: Organizations navigation button
  4. Wait for: selector ".organizations-container" visible
  5. Assert: Tree panel visible (selector: ".tree-panel")
  6. Assert: Form panel shows empty state or welcome message
  7. Click: "New Organization" button
  8. Assert: OrgForm visible in create mode
  9. Assert: ParentSelector visible in form
  10. Click: Cancel button in form
  11. Assert: Form hidden, tree still visible
  12. Click: Tree node
  13. Assert: OrgForm visible in edit mode
  14. Assert: Form populated with selected org data
  15. Screenshot: .sisyphus/evidence/task-6-organizations-route.png
  ```

  **Evidence to Capture**:
  - [ ] Screenshot showing master-detail layout
  - [ ] Screenshot showing create mode with form
  - [ ] Screenshot showing edit mode with populated form
  - [ ] Terminal output showing successful API calls

  **Commit**: YES
  - Message: `feat(frontend): add organizations management route with master-detail layout`
  - Files: `frontend/src/routes/Organizations.svelte`
  - Pre-commit: `cd frontend && bun run build`

---

- [ ] 8. Update Sidebar Navigation

  **What to do**:
  - Open `frontend/src/components/Sidebar.svelte`
  - Add navigation button for Organizations route
  - Button text: "🏢 조직 관리" (Korean for "Organization Management")
  - Button calls: `navigate('organizations')`
  - Position: After "🏪 가맹점 관리" button (line 20)

  **Must NOT do**:
  - Don't reorder existing buttons (keep current order, just insert new one)
  - Don't change existing button styles
  - Don't add permissions check here (handle in App.svelte route guard if needed)

  **Recommended Agent Profile**:
  - **Category**: `quick`
    - Reason: Simple single-line addition to existing file
  - **Skills**: None (trivial change, no special skills needed)
  - **Skills Evaluated but Omitted**:
    - `frontend-ui-ux`: Not needed (no design work, just adding a button following existing pattern)

  **Parallelization**:
  - **Can Run In Parallel**: YES (with Task 6 - independent)
  - **Parallel Group**: Wave 3 (with Task 6)
  - **Blocks**: Task 10 (E2E test needs navigation to work)
  - **Blocked By**: None (independent of other tasks)

  **References**:

  **Pattern References** (existing code to follow):
  - `frontend/src/components/Sidebar.svelte:15-21` - Existing navigation buttons (copy this pattern)

  **API/Type References** (contracts to implement against):
  - New button should match existing button structure:
    ```html
    <button onclick={() => navigate('organizations')}>🏢 조직 관리</button>
    ```

  **WHY Each Reference Matters**:
  - Sidebar.svelte lines 15-21 - Shows exact button syntax and onclick pattern to replicate

  **Acceptance Criteria**:

  **Automated Verification** (File Check + Playwright):
  ```bash
  # Agent runs:
  cat frontend/src/components/Sidebar.svelte | grep -n "조직 관리"
  # Assert: Output contains "🏢 조직 관리" with line number
  
  cat frontend/src/components/Sidebar.svelte | grep -n "navigate('organizations')"
  # Assert: Output contains navigate call
  ```
  
  ```
  # Agent executes via playwright skill:
  1. Navigate to: http://localhost:5173
  2. Login with test credentials
  3. Assert: Sidebar visible (selector: ".sidebar")
  4. Assert: Button with text "🏢 조직 관리" visible
  5. Click: "🏢 조직 관리" button
  6. Assert: URL changed or route changed to organizations
  7. Screenshot: .sisyphus/evidence/task-8-sidebar-nav.png
  ```

  **Evidence to Capture**:
  - [ ] Terminal output showing grep result for new button
  - [ ] Screenshot showing sidebar with new button
  - [ ] Screenshot showing organizations route after clicking button

  **Commit**: YES
  - Message: `feat(frontend): add organizations route to sidebar navigation`
  - Files: `frontend/src/components/Sidebar.svelte`
  - Pre-commit: `cd frontend && bun run build`

---

### WAVE 4: Final Polish (After Wave 3)

---

- [ ] 9. Add Responsive Styles

  **What to do**:
  - Open `frontend/src/routes/Organizations.svelte`
  - Add responsive CSS for mobile (<768px)
  - On mobile: Switch from side-by-side to stacked layout
  - Alternative: Use tabs (Tree tab / Form tab) on mobile
  - Ensure tree is scrollable with fixed height
  - Ensure form scrolls independently
  - Test on narrow viewports (375px, 768px)

  **Must NOT do**:
  - Don't break desktop layout (only add mobile styles, don't change existing)
  - Don't add complex animations (keep it simple)
  - Don't add new features (only styling for responsiveness)

  **Recommended Agent Profile**:
  - **Category**: `visual-engineering`
    - Reason: CSS styling and responsive design
  - **Skills**: `frontend-ui-ux`
    - `frontend-ui-ux`: Needed for responsive layout strategies and mobile UX
  - **Skills Evaluated but Omitted**:
    - None (frontend-ui-ux covers all aspects)

  **Parallelization**:
  - **Can Run In Parallel**: NO (depends on Task 6)
  - **Parallel Group**: Sequential in Wave 4
  - **Blocks**: Task 10 (E2E test should verify responsive behavior)
  - **Blocked By**: Task 6 (needs Organizations.svelte to exist)

  **References**:

  **Pattern References** (existing code to follow):
  - `frontend/src/routes/Dashboard.svelte:` - Example of responsive grid with media queries
  - `frontend/src/app.css:` - Global responsive breakpoints if any

  **API/Type References** (contracts to implement against):
  - Media query breakpoint: `@media (max-width: 768px)`
  - Mobile layout strategy:
    - Option A: Stacked (tree on top, form below)
    - Option B: Tabs (switch between tree view and form view)

  **External References**:
  - CSS Grid responsive patterns: https://css-tricks.com/snippets/css/complete-guide-grid/

  **WHY Each Reference Matters**:
  - Dashboard.svelte - Shows responsive grid pattern used in codebase (media queries with grid-template-columns)
  - app.css - Ensures consistency with global responsive styles
  - CSS Grid - Used for master-detail layout, needs responsive adjustment

  **Acceptance Criteria**:

  **Automated Verification** (Playwright Browser Automation):
  ```
  # Agent executes via playwright skill:
  1. Navigate to: http://localhost:5173
  2. Login and go to Organizations route
  3. Set viewport to desktop size (1280x720)
  4. Assert: Tree panel and form panel visible side-by-side
  5. Screenshot: .sisyphus/evidence/task-9-responsive-desktop.png
  6. Set viewport to mobile size (375x667)
  7. Assert: Tree and form stacked OR tabs visible
  8. Assert: Both panels still functional (can click tree nodes, see form)
  9. Screenshot: .sisyphus/evidence/task-9-responsive-mobile.png
  ```

  **Evidence to Capture**:
  - [ ] Screenshot at 1280px (desktop layout)
  - [ ] Screenshot at 768px (tablet layout)
  - [ ] Screenshot at 375px (mobile layout)
  - [ ] Terminal output confirming media query exists in CSS

  **Commit**: YES
  - Message: `style(frontend): add responsive layout for organizations route`
  - Files: `frontend/src/routes/Organizations.svelte`
  - Pre-commit: `cd frontend && bun run build`

---

- [ ] 10. End-to-End Verification

  **What to do**:
  - Start backend server (if not running): `cd backend && ./gradlew bootRun`
  - Start frontend dev server: `cd frontend && bun run dev`
  - Open browser to http://localhost:5173
  - Login with test credentials
  - Navigate to Organizations via sidebar
  - Verify tree loads and displays organizations
  - Verify clicking tree node loads edit form
  - Verify clicking "New Organization" shows create form
  - Verify form validation (empty fields, invalid email/phone)
  - Verify submitting create form creates org and refreshes tree
  - Verify submitting edit form updates org
  - Verify status change shows confirmation modal
  - Verify error messages display for API failures
  - Verify responsive layout on mobile viewport
  - Document any bugs or issues found

  **Must NOT do**:
  - Don't add new features (verification only)
  - Don't modify code unless fixing critical bugs

  **Recommended Agent Profile**:
  - **Category**: `quick`
    - Reason: Verification task, no implementation
  - **Skills**: `playwright`
    - `playwright`: Needed for browser-based E2E testing and verification
  - **Skills Evaluated but Omitted**:
    - None (playwright covers all needed verification)

  **Parallelization**:
  - **Can Run In Parallel**: NO (final verification after all work done)
  - **Parallel Group**: Sequential in Wave 4 (after Task 9)
  - **Blocks**: None (final task)
  - **Blocked By**: Tasks 6, 8, 9 (needs complete implementation)

  **References**:

  **Pattern References** (existing code to follow):
  - All components from Tasks 1-9

  **API/Type References** (contracts to implement against):
  - Full user workflow:
    1. Login → 2. Navigate → 3. View tree → 4. Create org → 5. Edit org → 6. Change status

  **External References**:
  - Playwright best practices for E2E testing

  **WHY Each Reference Matters**:
  - All previous tasks - This task verifies integration of all components
  - User workflow - Ensures complete feature works end-to-end, not just individual components

  **Acceptance Criteria**:

  **Automated Verification** (Playwright E2E):
  ```
  # Agent executes via playwright skill:
  1. Navigate to: http://localhost:5173
  2. Fill: username = "test-user"
  3. Fill: password = "test-pass"
  4. Click: Login button
  5. Assert: Logged in successfully
  6. Click: Sidebar "🏢 조직 관리" button
  7. Wait for: Organizations route loaded
  8. Assert: Tree visible with organizations
  9. Click: "New Organization" button
  10. Assert: Create form visible
  11. Fill: name = "Test Org"
  12. Fill: email = "test@example.com"
  13. Fill: phone = "010-1234-5678"
  14. Select: parent from dropdown
  15. Assert: orgCode preview visible
  16. Click: Submit button
  17. Wait for: Success message or tree refresh
  18. Assert: New org appears in tree
  19. Click: Newly created org in tree
  20. Assert: Edit form visible with populated data
  21. Change: status to SUSPENDED
  22. Assert: Confirmation modal appears
  23. Click: Confirm in modal
  24. Click: Submit button
  25. Wait for: Success message
  26. Screenshot: .sisyphus/evidence/task-10-e2e-success.png
  ```

  **Evidence to Capture**:
  - [ ] Screenshot showing complete workflow (login → navigate → create → edit)
  - [ ] Terminal output showing all API calls succeeded
  - [ ] Screenshot showing error handling (if API fails)
  - [ ] Screenshot showing mobile responsive layout
  - [ ] List of any bugs found with reproduction steps

  **Commit**: NO (verification only, no code changes unless bugs found)
  - If bugs found and fixed: Commit with message `fix(frontend): [bug description]`

---

## Commit Strategy

| After Task | Message | Files | Verification |
|------------|---------|-------|--------------|
| 1 | feat(frontend): add organization type definitions | frontend/src/types/api.ts | bun run build |
| 2 | feat(frontend): add reusable confirmation modal component | frontend/src/components/ConfirmModal.svelte | bun run build |
| 3 | feat(frontend): add recursive organization tree node component | frontend/src/components/OrgTreeNode.svelte | bun run build |
| 4 | feat(frontend): add organization tree component with lazy loading | frontend/src/components/OrgTree.svelte | bun run build |
| 5 | feat(frontend): add permission-aware parent selector component | frontend/src/components/ParentSelector.svelte | bun run build |
| 7 | feat(frontend): add organization create/edit form with validation | frontend/src/components/OrgForm.svelte | bun run build |
| 6 | feat(frontend): add organizations management route with master-detail layout | frontend/src/routes/Organizations.svelte | bun run build |
| 8 | feat(frontend): add organizations route to sidebar navigation | frontend/src/components/Sidebar.svelte | bun run build |
| 9 | style(frontend): add responsive layout for organizations route | frontend/src/routes/Organizations.svelte | bun run build |

---

## Success Criteria

### Verification Commands
```bash
# Frontend builds without errors
cd frontend && bun run build

# Backend running (for E2E test)
cd backend && ./gradlew bootRun

# Frontend dev server running (for E2E test)
cd frontend && bun run dev
```

### Final Checklist
- [ ] All "Must Have" present:
  - [ ] Master-detail layout (tree + form)
  - [ ] Auto-generated orgCode (read-only)
  - [ ] Permission-aware parent selection
  - [ ] Form validation (required, email, phone)
  - [ ] Hierarchical tree with expand/collapse
  - [ ] Status change confirmation
  - [ ] Error handling and loading states
  - [ ] Sidebar navigation integration
- [ ] All "Must NOT Have" absent:
  - [ ] No organization deletion
  - [ ] No config field editor
  - [ ] No cascade status changes
  - [ ] No organization transfer
  - [ ] No bulk operations
  - [ ] No export/import
- [ ] All verification procedures pass (Tasks 1-10)
- [ ] Frontend builds successfully
- [ ] E2E workflow completes without errors
- [ ] Responsive layout works on mobile
