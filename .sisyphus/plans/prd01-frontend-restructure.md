# PRD-01 Frontend Restructure - Bill&Pay

## TL;DR

> **Quick Summary**: Restructure Bill&Pay frontend with 2-depth collapsible sidebar, tab-based content management (max 10 tabs), and implement 영업점 관리 screens (등록 wizard, 목록, 상세 panel).
> 
> **Deliverables**:
> - New Layout system (Header, 2-depth Sidebar, Tab content area)
> - Tab management store with localStorage persistence
> - 영업점 등록 (3-step wizard)
> - 영업점 목록 (infinite scroll, filters, Excel export)
> - 영업점 상세 (Sheet side panel)
> - Migration of all existing routes to tab system
> 
> **Estimated Effort**: Large (15-20 tasks)
> **Parallel Execution**: YES - 3 waves
> **Critical Path**: Task 1 → Task 2 → Task 3 → Task 6 → Task 10

---

## Context

### Original Request
Restructure the frontend to match PRD-01:
1. 전체 화면 layout - Header, 2-depth sidebar, tab-based content area
2. 영업점관리 - 영업점 등록, 영업점 목록, 영업점 상세

### Interview Summary
**Key Discussions**:
- 영업점 = Organization (same entity with 5-level hierarchy)
- All existing pages migrate to tab system
- bits-ui components for Tabs, Collapsible, DropdownMenu, Sheet
- Backend API ready - use existing organizations endpoints
- CSS-in-component approach maintained (no Tailwind)

**Research Findings**:
- bits-ui v0.21.0 installed but unused - supports Svelte 5 Runes
- bits-ui Tabs needs custom logic for closeable tabs
- IntersectionObserver for infinite scroll
- Multi-step wizard with step-based validation

### Self-Analysis (Metis-style Gap Check)

**Identified Gaps Addressed**:
1. Tab state restoration on refresh → localStorage persistence included
2. Duplicate tab prevention → Tab manager checks existing tabs before opening
3. Maximum tab limit UX → Alert user when reaching 10 tabs
4. Sidebar collapsed state persistence → localStorage for sidebar state
5. Mobile responsiveness → Not in scope (desktop admin panel)
6. Keyboard navigation → bits-ui provides built-in a11y

**Guardrails Applied**:
- DO NOT modify backend APIs
- DO NOT implement actual authentication changes
- DO NOT add new npm dependencies beyond what's installed
- DO NOT use Tailwind (CSS-in-component only)
- Other menu items are placeholder stubs only

---

## Work Objectives

### Core Objective
Transform the single-route layout into a tab-based multi-document interface with 2-depth navigation sidebar and implement complete 영업점 관리 module.

### Concrete Deliverables
1. `frontend/src/lib/tabStore.ts` - Tab management store
2. `frontend/src/components/NewLayout.svelte` - New layout wrapper
3. `frontend/src/components/NewHeader.svelte` - Enhanced header
4. `frontend/src/components/NewSidebar.svelte` - 2-depth collapsible sidebar
5. `frontend/src/components/TabBar.svelte` - Tab bar with close buttons
6. `frontend/src/components/StatusBar.svelte` - Footer status bar
7. `frontend/src/routes/branch/BranchRegistration.svelte` - 3-step wizard
8. `frontend/src/routes/branch/BranchList.svelte` - List with infinite scroll
9. `frontend/src/routes/branch/BranchDetail.svelte` - Sheet panel
10. `frontend/src/types/branch.ts` - Branch-specific types (extends Organization)
11. `frontend/src/lib/branchApi.ts` - Branch API service (wraps existing org API)
12. Updated `frontend/src/App.svelte` - Tab-based routing

### Definition of Done
- [ ] All menu items in sidebar open corresponding tabs
- [ ] Tabs can be closed (except Dashboard which is always open)
- [ ] Maximum 10 tabs enforced with user notification
- [ ] Tab state persists across page refresh
- [ ] 영업점 등록 wizard completes all 3 steps
- [ ] 영업점 목록 loads more data on scroll
- [ ] 영업점 상세 opens as side panel from list
- [ ] Existing pages (Dashboard, Transactions, etc.) work in tabs
- [ ] No TypeScript errors (`bun run check` passes)

### Must Have
- Tab system with open/close/focus/max limit
- 2-depth collapsible sidebar with all PRD menu items
- Header with tenant selector dropdown
- 영업점 등록/목록/상세 full implementation
- localStorage persistence for tabs and sidebar state

### Must NOT Have (Guardrails)
- No Tailwind CSS classes
- No new npm dependencies
- No backend API changes
- No actual data persistence (Mock only for 영업점)
- No implementation of 우대사업자, 가맹점관리, 지급이체, 결제관리, 운영관리 screens (stubs only)
- No mobile responsive design
- No excessive abstraction (keep it simple)

---

## Verification Strategy

### Test Decision
- **Infrastructure exists**: NO (no test setup in frontend)
- **User wants tests**: Manual verification
- **Framework**: None

### Manual Verification Procedures

Each TODO includes specific verification steps the executor will perform manually or via dev server inspection.

**Evidence to Capture:**
- Screenshots of UI states
- Browser console for errors
- `bun run check` output for TypeScript validation

---

## Execution Strategy

### Parallel Execution Waves

```
Wave 1 (Start Immediately - Foundation):
├── Task 1: Create tabStore.ts (Tab management)
├── Task 2: Create branch types and mock data
└── Task 3: Create NewHeader.svelte

Wave 2 (After Wave 1 - Layout Components):
├── Task 4: Create NewSidebar.svelte (depends: 1)
├── Task 5: Create TabBar.svelte (depends: 1)
├── Task 6: Create StatusBar.svelte
└── Task 7: Create NewLayout.svelte (depends: 3,4,5,6)

Wave 3 (After Wave 2 - Feature Screens):
├── Task 8: Create BranchRegistration.svelte (depends: 2,7)
├── Task 9: Create BranchList.svelte (depends: 2,7)
├── Task 10: Create BranchDetail.svelte (depends: 2,7)
└── Task 11: Update App.svelte for tab routing (depends: 7)

Wave 4 (After Wave 3 - Migration & Polish):
├── Task 12: Migrate existing routes to tab system (depends: 11)
├── Task 13: Add placeholder stubs for other menus (depends: 7)
└── Task 14: Final integration testing (depends: all)

Critical Path: Task 1 → Task 4 → Task 7 → Task 11 → Task 12 → Task 14
Parallel Speedup: ~50% faster than sequential
```

### Dependency Matrix

| Task | Depends On | Blocks | Can Parallelize With |
|------|------------|--------|---------------------|
| 1 | None | 4, 5, 11 | 2, 3 |
| 2 | None | 8, 9, 10 | 1, 3 |
| 3 | None | 7 | 1, 2 |
| 4 | 1 | 7 | 5, 6 |
| 5 | 1 | 7 | 4, 6 |
| 6 | None | 7 | 4, 5 |
| 7 | 3, 4, 5, 6 | 8, 9, 10, 11 | None |
| 8 | 2, 7 | 14 | 9, 10 |
| 9 | 2, 7 | 10, 14 | 8 |
| 10 | 2, 7, 9 | 14 | None |
| 11 | 7 | 12 | 8, 9, 10 |
| 12 | 11 | 14 | 13 |
| 13 | 7 | 14 | 12 |
| 14 | All | None | None |

---

## TODOs

### Wave 1: Foundation

- [ ] 1. Create Tab Management Store

  **What to do**:
  - Create `frontend/src/lib/tabStore.ts`
  - Implement Tab interface: `{ id: string, title: string, icon: string, component: string, closeable: boolean, props?: any }`
  - Implement TabStore class with:
    - `tabs: Tab[]` - list of open tabs
    - `activeTabId: string` - currently active tab
    - `openTab(tab: Tab)` - opens new tab or focuses existing
    - `closeTab(id: string)` - closes tab (skip if not closeable)
    - `focusTab(id: string)` - sets active tab
    - `MAX_TABS = 10` constant
  - Persist to localStorage on every change
  - Load from localStorage on init
  - Export singleton instance

  **Must NOT do**:
  - Do not use Svelte stores ($store syntax) - use plain class with $state
  - Do not add complex undo/redo logic

  **References**:
  - `frontend/src/lib/authStore.ts:9-109` - Pattern for class-based store with localStorage
  - `frontend/src/lib/stores.ts:1-13` - Simple class store pattern

  **Acceptance Criteria**:
  - [ ] File created at `frontend/src/lib/tabStore.ts`
  - [ ] `bun run check` passes with no errors in tabStore.ts
  - [ ] Can import and use: `import { tabStore } from './lib/tabStore'`
  - [ ] `tabStore.openTab()` adds tab and persists to localStorage
  - [ ] `tabStore.closeTab()` removes tab and updates localStorage
  - [ ] Opening same tab ID focuses existing instead of duplicating
  - [ ] Opening 11th tab shows alert and doesn't add

  **Commit**: YES
  - Message: `feat(frontend): add tab management store with localStorage persistence`
  - Files: `frontend/src/lib/tabStore.ts`

---

- [ ] 2. Create Branch Types and API Service

  **What to do**:
  - Create `frontend/src/types/branch.ts` with interfaces:
    - `BranchType`: enum (DISTRIBUTOR, AGENCY, DEALER, SELLER, VENDOR) - reuse OrgType
    - `BusinessType`: enum (CORPORATION, INDIVIDUAL, NON_BUSINESS)
    - `BranchBasicInfo`: extends Organization with additional fields
    - `BusinessInfo`: { businessType, businessNumber, representative, address, phone, email }
    - `BankAccountInfo`: { bankCode, bankName, accountNumber, accountHolder }
    - `FeeConfig`: { terminalFee, oldAuthFee, nonAuthFee, authPayFee, recurringFee }
    - `LimitConfig`: { perTransaction, perDay }
    - `Branch`: combines all above (extends Organization)
    - `BranchCreateRequest`: for wizard form data (extends CreateOrgRequest)
  - Create `frontend/src/lib/branchApi.ts`:
    - Wrapper around existing apiClient for organization endpoints
    - `getBranches(params)` - calls `/organizations` with filters
    - `getBranchById(id)` - calls `/organizations/{id}`
    - `createBranch(data)` - calls POST `/organizations`
    - `updateBranch(id, data)` - calls PUT `/organizations/{id}`
    - `getBranchTree()` - calls `/organizations/tree` for hierarchy

  **Must NOT do**:
  - Do not create mock data - use real API
  - Do not duplicate existing Organization types unnecessarily

  **References**:
  - `frontend/src/types/api.ts:21-29` - Organization interface pattern
  - `frontend/src/types/api.ts:131-167` - Existing types (OrgType, OrgStatus, CreateOrgRequest)
  - `frontend/src/lib/api.ts:1-124` - ApiClient pattern

  **Acceptance Criteria**:
  - [ ] `frontend/src/types/branch.ts` created extending existing types
  - [ ] `frontend/src/lib/branchApi.ts` created wrapping apiClient
  - [ ] `bun run check` passes
  - [ ] `getBranches({ page: 0, size: 20 })` calls real API
  - [ ] `getBranchById(id)` returns organization from API
  - [ ] API calls include proper tenant header

  **Commit**: YES
  - Message: `feat(frontend): add branch types and API service`
  - Files: `frontend/src/types/branch.ts`, `frontend/src/lib/branchApi.ts`

---

- [ ] 3. Create New Header Component

  **What to do**:
  - Create `frontend/src/components/NewHeader.svelte`
  - Structure:
    ```
    [Logo: Bill&Pay] [Search Bar] [Notifications] [Settings] [Tenant Selector] [User Profile]
    ```
  - Logo: clickable, opens Dashboard tab
  - Search: input field with search icon (placeholder, no functionality)
  - Notifications: bell icon with badge count (static "3")
  - Settings: gear icon (placeholder, opens alert)
  - Tenant Selector: bits-ui DropdownMenu with RadioGroup
    - Get tenants from mock list: ["tenant_001", "tenant_002", "tenant_003"]
    - Selected tenant updates tenantStore
  - User Profile: bits-ui DropdownMenu
    - Show username from authStore
    - Items: "내 정보", "로그아웃"
    - Logout calls authStore.logout()
  - Style: white background, border-bottom, height ~60px

  **Must NOT do**:
  - Do not implement actual search functionality
  - Do not fetch real tenant list from API
  - Do not use Tailwind classes

  **References**:
  - `frontend/src/components/Header.svelte:1-97` - Current header pattern
  - `frontend/src/lib/authStore.ts:90-108` - Auth methods
  - bits-ui DropdownMenu documentation (from research)

  **Acceptance Criteria**:
  - [ ] File created at `frontend/src/components/NewHeader.svelte`
  - [ ] Logo visible and clickable
  - [ ] Tenant selector dropdown opens and shows 3 tenants
  - [ ] Selecting tenant updates display
  - [ ] User dropdown shows username and logout option
  - [ ] Logout redirects to login page
  - [ ] `bun run check` passes

  **Commit**: YES
  - Message: `feat(frontend): add new header with tenant selector and user dropdown`
  - Files: `frontend/src/components/NewHeader.svelte`

---

### Wave 2: Layout Components

- [ ] 4. Create 2-Depth Collapsible Sidebar

  **What to do**:
  - Create `frontend/src/components/NewSidebar.svelte`
  - Use bits-ui Collapsible for expandable menu groups
  - Menu structure (from PRD):
    ```
    📊 대시보드 (no children - direct link)
    📋 우대사업자
      └─ 사업자 조회
    🏢 영업점 관리
      ├─ 영업점 등록
      └─ 영업점 목록
    🏪 가맹점 관리
      ├─ 가맹점 등록
      ├─ 가맹점 목록
      └─ 단말기 관리
    💰 정산 관리
      ├─ 영업점 정산내역
      └─ 가맹점 정산내역
    💸 지급 이체
      ├─ 지급이체 등록
      └─ 지급이체 조회
    💳 결제 관리
      ├─ 결제내역
      └─ 실패내역
    ⚙️ 운영 관리
      ├─ 공지사항
      ├─ 계정관리
      └─ 환경설정
    ```
  - Each menu item click calls `tabStore.openTab({ id, title, icon, component, closeable: true })`
  - Dashboard is special: closeable: false
  - Persist expanded/collapsed state to localStorage
  - Sidebar width: 250px, dark theme (#1a1a1a background)
  - Add collapse/expand toggle button at bottom

  **Must NOT do**:
  - Do not implement actual screens for placeholder menus
  - Do not use Tailwind

  **References**:
  - `frontend/src/components/Sidebar.svelte:1-65` - Current sidebar
  - `frontend/src/lib/tabStore.ts` - Tab store (Task 1)
  - bits-ui Collapsible documentation

  **Acceptance Criteria**:
  - [ ] File created at `frontend/src/components/NewSidebar.svelte`
  - [ ] All 8 top-level menu groups visible
  - [ ] Clicking expandable menu shows/hides children with animation
  - [ ] Clicking menu item opens tab via tabStore
  - [ ] Expanded state persists on page refresh
  - [ ] Sidebar has collapse toggle
  - [ ] `bun run check` passes

  **Commit**: YES
  - Message: `feat(frontend): add 2-depth collapsible sidebar with bits-ui`
  - Files: `frontend/src/components/NewSidebar.svelte`

---

- [ ] 5. Create Tab Bar Component

  **What to do**:
  - Create `frontend/src/components/TabBar.svelte`
  - Props: none (reads from tabStore directly)
  - Use bits-ui Tabs.Root, Tabs.List, Tabs.Trigger
  - Features:
    - Horizontal tab bar at top of content area
    - Each tab shows: icon + title + close button (if closeable)
    - Active tab highlighted
    - Click tab → tabStore.focusTab(id)
    - Click close → tabStore.closeTab(id)
    - Overflow: horizontal scroll with [<] [>] arrow buttons
  - Style: gray background (#f5f5f5), tabs with rounded top corners

  **Must NOT do**:
  - Do not implement drag-and-drop reordering
  - Do not use Tailwind

  **References**:
  - `frontend/src/lib/tabStore.ts` - Tab store (Task 1)
  - bits-ui Tabs documentation

  **Acceptance Criteria**:
  - [ ] File created at `frontend/src/components/TabBar.svelte`
  - [ ] Displays all tabs from tabStore
  - [ ] Clicking tab focuses it
  - [ ] Close button removes closeable tabs
  - [ ] Non-closeable tabs (Dashboard) have no close button
  - [ ] Active tab visually distinct
  - [ ] `bun run check` passes

  **Commit**: YES
  - Message: `feat(frontend): add tab bar component with bits-ui tabs`
  - Files: `frontend/src/components/TabBar.svelte`

---

- [ ] 6. Create Status Bar Component

  **What to do**:
  - Create `frontend/src/components/StatusBar.svelte`
  - Fixed at bottom of screen, full width
  - Content:
    - Left: "Bill&Pay v1.0.0"
    - Center: Current date/time (updates every minute)
    - Right: "마지막 로그인: [date from authStore or mock]"
  - Height: 30px, subtle gray background

  **Must NOT do**:
  - Do not add complex system status indicators
  - Do not use Tailwind

  **References**:
  - `frontend/src/lib/authStore.ts` - For last login info

  **Acceptance Criteria**:
  - [ ] File created at `frontend/src/components/StatusBar.svelte`
  - [ ] Shows version, current time, last login
  - [ ] Time updates every minute
  - [ ] Fixed at viewport bottom
  - [ ] `bun run check` passes

  **Commit**: YES
  - Message: `feat(frontend): add status bar with version and login info`
  - Files: `frontend/src/components/StatusBar.svelte`

---

- [ ] 7. Create New Layout Component

  **What to do**:
  - Create `frontend/src/components/NewLayout.svelte`
  - Compose: NewHeader + NewSidebar + TabBar + Content + StatusBar
  - Layout structure:
    ```
    ┌─────────────────────────────────────────────┐
    │                  Header                      │
    ├──────────┬──────────────────────────────────┤
    │          │          TabBar                   │
    │ Sidebar  ├──────────────────────────────────┤
    │          │                                   │
    │          │         Content Area              │
    │          │   (renders active tab component)  │
    │          │                                   │
    ├──────────┴──────────────────────────────────┤
    │                 StatusBar                    │
    └─────────────────────────────────────────────┘
    ```
  - Content area renders component based on `tabStore.activeTabId`
  - Use dynamic component: `<svelte:component this={activeComponent} {...activeProps} />`
  - Map tab.component string to actual component imports

  **Must NOT do**:
  - Do not keep old Layout.svelte logic
  - Do not use Tailwind

  **References**:
  - `frontend/src/components/Layout.svelte:1-41` - Current layout
  - `frontend/src/components/NewHeader.svelte` (Task 3)
  - `frontend/src/components/NewSidebar.svelte` (Task 4)
  - `frontend/src/components/TabBar.svelte` (Task 5)
  - `frontend/src/components/StatusBar.svelte` (Task 6)

  **Acceptance Criteria**:
  - [ ] File created at `frontend/src/components/NewLayout.svelte`
  - [ ] All 4 components (Header, Sidebar, TabBar, StatusBar) visible
  - [ ] Content area renders based on active tab
  - [ ] Sidebar toggle works (collapses/expands)
  - [ ] Layout fills viewport height
  - [ ] `bun run check` passes

  **Commit**: YES
  - Message: `feat(frontend): add new layout with header, sidebar, tabs, statusbar`
  - Files: `frontend/src/components/NewLayout.svelte`

---

### Wave 3: Feature Screens

- [ ] 8. Create Branch Registration Wizard

  **What to do**:
  - Create `frontend/src/routes/branch/BranchRegistration.svelte`
  - 3-step wizard:
    - **Step 1: 구분 (계층 선택)**
      - Branch type dropdown: 대리점/에이전시/딜러/셀러/벤더
      - Parent selector (cascading dropdowns based on type)
      - If DISTRIBUTOR: no parent needed
      - If AGENCY: select DISTRIBUTOR parent
      - etc.
    - **Step 2: 사업자 정보**
      - Business type radio: 법인/개인/비사업자
      - Business number input (사업자등록번호)
      - Representative name
      - Address
      - Phone, Email
    - **Step 3: 정산정보**
      - Bank selector dropdown
      - Account number input
      - Account holder name
      - Fee settings (5 types with % input)
      - Limits: per transaction, per day (KRW input)
  - Stepper UI at top showing current step
  - Prev/Next/Submit buttons
  - Validation before each step transition
  - On submit: call branchApi.createBranch() and show success alert

  **Must NOT do**:
  - Do not implement complex validation beyond required fields
  - Do not use Tailwind

  **References**:
  - `frontend/src/routes/Organizations.svelte:1-300` - Form patterns
  - `frontend/src/components/OrgForm.svelte` - Form component pattern
  - `frontend/src/types/branch.ts` (Task 2)
  - `frontend/src/lib/branchApi.ts` (Task 2)
  - Svelte 5 wizard pattern from research

  **Acceptance Criteria**:
  - [ ] File created at `frontend/src/routes/branch/BranchRegistration.svelte`
  - [ ] Step indicator shows 3 steps with current highlighted
  - [ ] Step 1: Can select branch type and parent
  - [ ] Step 2: Can fill business info form
  - [ ] Step 3: Can fill bank and fee info
  - [ ] Prev button goes back, Next validates and proceeds
  - [ ] Submit shows success message
  - [ ] `bun run check` passes

  **Commit**: YES
  - Message: `feat(frontend): add 3-step branch registration wizard`
  - Files: `frontend/src/routes/branch/BranchRegistration.svelte`

---

- [ ] 9. Create Branch List with Infinite Scroll

  **What to do**:
  - Create `frontend/src/routes/branch/BranchList.svelte`
  - Features:
    - Tab filters at top: 전체/대리점/에이전시/딜러/셀러/벤더
    - Date range picker (bits-ui or native inputs)
    - Quick date buttons: 7일/15일/30일
    - Search input for name/code
    - Data table columns: NO, 대리점코드, 대리점명, 대표자, 연락처, E-Mail, 상태, 등록일
    - Infinite scroll: load more on scroll bottom (IntersectionObserver)
    - Excel download button (generate client-side with basic CSV for now)
    - Row click: opens BranchDetail sheet panel
  - Use branchApi.getBranches() for data (real API)
  - State: loading, items, hasMore, page, filters

  **Must NOT do**:
  - Do not use TanStack Table (keep native table like Transactions.svelte)
  - Do not implement server-side Excel generation
  - Do not use Tailwind

  **References**:
  - `frontend/src/routes/Transactions.svelte:1-484` - Table patterns, filtering, loading states
  - `frontend/src/lib/branchApi.ts` (Task 2)
  - IntersectionObserver infinite scroll pattern from research

  **Acceptance Criteria**:
  - [ ] File created at `frontend/src/routes/branch/BranchList.svelte`
  - [ ] Tab filters work (전체/대리점/etc)
  - [ ] Date range and quick buttons filter data
  - [ ] Search filters by name/code
  - [ ] Table displays all columns
  - [ ] Scrolling to bottom loads more items
  - [ ] Excel button downloads CSV file
  - [ ] Clicking row opens detail panel (Task 10)
  - [ ] `bun run check` passes

  **Commit**: YES
  - Message: `feat(frontend): add branch list with infinite scroll and filters`
  - Files: `frontend/src/routes/branch/BranchList.svelte`

---

- [ ] 10. Create Branch Detail Sheet Panel

  **What to do**:
  - Create `frontend/src/routes/branch/BranchDetail.svelte`
  - Use bits-ui Sheet (side panel from right)
  - Props: `branchId: string`, `open: boolean`, `onClose: () => void`
  - Sections:
    - 기본정보: code, name, type, status, created date
    - 사업자정보: business type, number, representative, address, contacts
    - 계좌정보: bank, account number, holder
    - 수수료 설정: tabs for (단말기/구인증/비인증/인증결제/정기과금)
    - 한도 설정: 1회 한도, 1일 한도
  - Edit button → toggles edit mode
  - Save button → calls branchApi.updateBranch() (real API)
  - Close button → calls onClose

  **Must NOT do**:
  - Do not use modal dialog (use Sheet side panel)
  - Do not use Tailwind

  **References**:
  - `frontend/src/routes/MerchantManagement.svelte:90-180` - Modal pattern (adapt for Sheet)
  - `frontend/src/lib/branchApi.ts` (Task 2)
  - bits-ui Sheet documentation

  **Acceptance Criteria**:
  - [ ] File created at `frontend/src/routes/branch/BranchDetail.svelte`
  - [ ] Sheet slides in from right when open=true
  - [ ] All 5 sections displayed with data
  - [ ] Fee tabs switch between fee types
  - [ ] Edit mode enables form inputs
  - [ ] Save calls API and closes
  - [ ] Close button closes panel
  - [ ] `bun run check` passes

  **Commit**: YES
  - Message: `feat(frontend): add branch detail sheet panel`
  - Files: `frontend/src/routes/branch/BranchDetail.svelte`

---

- [ ] 11. Update App.svelte for Tab-Based Routing

  **What to do**:
  - Modify `frontend/src/App.svelte`
  - Replace current route-based rendering with tab-based system
  - Changes:
    - Remove `currentRoute` state
    - Import NewLayout instead of Layout
    - Initialize tabStore with Dashboard tab on mount
    - Let NewLayout handle tab content rendering
  - Keep authentication logic (redirect to Login if not authenticated)

  **Must NOT do**:
  - Do not break login flow
  - Do not remove authentication checks
  - Do not use Tailwind

  **References**:
  - `frontend/src/App.svelte:1-58` - Current implementation
  - `frontend/src/lib/tabStore.ts` (Task 1)
  - `frontend/src/components/NewLayout.svelte` (Task 7)

  **Acceptance Criteria**:
  - [ ] App.svelte updated to use NewLayout
  - [ ] tabStore initialized with Dashboard on mount
  - [ ] Login flow still works
  - [ ] After login, Dashboard tab is active
  - [ ] `bun run check` passes

  **Commit**: YES
  - Message: `feat(frontend): update App.svelte for tab-based routing`
  - Files: `frontend/src/App.svelte`

---

### Wave 4: Migration & Polish

- [ ] 12. Migrate Existing Routes to Tab System

  **What to do**:
  - Ensure existing route components work within tab system:
    - Dashboard.svelte
    - Transactions.svelte
    - Settlements.svelte
    - SettlementBatches.svelte
    - SettlementSummary.svelte
    - MerchantManagement.svelte
    - Organizations.svelte (now also accessible as 영업점 목록 if same entity)
  - Update NewLayout component map to include all components
  - Test each existing page opens correctly in tab
  - No functional changes to these components (just integration)

  **Must NOT do**:
  - Do not refactor existing components
  - Do not change their functionality
  - Do not use Tailwind

  **References**:
  - All files in `frontend/src/routes/`
  - `frontend/src/components/NewLayout.svelte` (Task 7)

  **Acceptance Criteria**:
  - [ ] Dashboard opens in tab
  - [ ] Transactions opens in tab
  - [ ] Settlements opens in tab
  - [ ] Settlement Batches opens in tab
  - [ ] Settlement Summary opens in tab
  - [ ] Merchant Management opens in tab
  - [ ] Organizations opens in tab
  - [ ] All pages render correctly without errors
  - [ ] `bun run check` passes

  **Commit**: YES
  - Message: `feat(frontend): migrate existing routes to tab system`
  - Files: `frontend/src/components/NewLayout.svelte`

---

- [ ] 13. Add Placeholder Stubs for Other Menus

  **What to do**:
  - Create placeholder components for menu items not in scope:
    - `frontend/src/routes/placeholders/ComingSoon.svelte`
  - Generic component that shows:
    - Large icon
    - "준비 중입니다" message
    - Menu item name prop
  - Map these placeholder menus in sidebar:
    - 사업자 조회
    - 가맹점 등록, 가맹점 목록, 단말기 관리
    - 영업점 정산내역, 가맹점 정산내역
    - 지급이체 등록, 지급이체 조회
    - 결제내역, 실패내역
    - 공지사항, 계정관리, 환경설정

  **Must NOT do**:
  - Do not implement actual functionality
  - Do not use Tailwind

  **References**:
  - `frontend/src/components/NewSidebar.svelte` (Task 4)

  **Acceptance Criteria**:
  - [ ] ComingSoon.svelte created
  - [ ] All placeholder menu items open ComingSoon in tab
  - [ ] ComingSoon shows appropriate message
  - [ ] `bun run check` passes

  **Commit**: YES
  - Message: `feat(frontend): add placeholder stubs for unimplemented menus`
  - Files: `frontend/src/routes/placeholders/ComingSoon.svelte`, `frontend/src/components/NewSidebar.svelte`

---

- [ ] 14. Final Integration Testing

  **What to do**:
  - Manual verification checklist:
    1. Start dev server: `cd frontend && bun run dev`
    2. Login with test credentials
    3. Verify Dashboard tab opens automatically
    4. Test all sidebar menu items open tabs
    5. Test tab close functionality
    6. Test 10 tab limit
    7. Refresh page - verify tabs persist
    8. Test 영업점 등록 wizard (all 3 steps)
    9. Test 영업점 목록 (filters, scroll, search)
    10. Test 영업점 상세 panel (view, edit, save)
    11. Test tenant selector changes tenant
    12. Test logout
    13. Run `bun run check` for TypeScript errors
  - Fix any issues found
  - Clean up console.log statements
  - Ensure no TypeScript errors

  **Must NOT do**:
  - Do not add new features
  - Do not refactor working code
  - Do not use Tailwind

  **Acceptance Criteria**:
  - [ ] All 13 checklist items pass
  - [ ] `bun run check` returns 0 errors
  - [ ] No console errors in browser
  - [ ] All features work as specified
  - [ ] Code is clean (no debug logs)

  **Commit**: YES
  - Message: `chore(frontend): final cleanup and integration verification`
  - Files: Any files with fixes

---

## Commit Strategy

| After Task | Message | Files | Verification |
|------------|---------|-------|--------------|
| 1 | `feat(frontend): add tab management store with localStorage persistence` | tabStore.ts | bun run check |
| 2 | `feat(frontend): add branch types and mock data service` | types/branch.ts, mockBranchData.ts | bun run check |
| 3 | `feat(frontend): add new header with tenant selector and user dropdown` | NewHeader.svelte | bun run check |
| 4 | `feat(frontend): add 2-depth collapsible sidebar with bits-ui` | NewSidebar.svelte | bun run check |
| 5 | `feat(frontend): add tab bar component with bits-ui tabs` | TabBar.svelte | bun run check |
| 6 | `feat(frontend): add status bar with version and login info` | StatusBar.svelte | bun run check |
| 7 | `feat(frontend): add new layout with header, sidebar, tabs, statusbar` | NewLayout.svelte | bun run check |
| 8 | `feat(frontend): add 3-step branch registration wizard` | BranchRegistration.svelte | bun run check |
| 9 | `feat(frontend): add branch list with infinite scroll and filters` | BranchList.svelte | bun run check |
| 10 | `feat(frontend): add branch detail sheet panel` | BranchDetail.svelte | bun run check |
| 11 | `feat(frontend): update App.svelte for tab-based routing` | App.svelte | bun run check |
| 12 | `feat(frontend): migrate existing routes to tab system` | NewLayout.svelte | bun run check |
| 13 | `feat(frontend): add placeholder stubs for unimplemented menus` | ComingSoon.svelte, NewSidebar.svelte | bun run check |
| 14 | `chore(frontend): final cleanup and integration verification` | Various | bun run check + manual test |

---

## Success Criteria

### Verification Commands
```bash
cd frontend && bun run check  # Expected: 0 errors
cd frontend && bun run dev    # Expected: Server starts on localhost:5173
```

### Final Checklist
- [ ] All 14 tasks completed
- [ ] `bun run check` passes with 0 errors
- [ ] Tab system works (open, close, persist, max 10)
- [ ] 2-depth sidebar with all menus
- [ ] Header with tenant selector and user dropdown
- [ ] 영업점 등록 wizard completes successfully
- [ ] 영업점 목록 with infinite scroll works
- [ ] 영업점 상세 sheet panel opens and edits
- [ ] All existing pages work in tab system
- [ ] No console errors in browser
- [ ] All commits follow conventional commit format
