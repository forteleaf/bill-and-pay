# Draft: PRD-01 Frontend Restructure

## Requirements (confirmed from user)

### Layout Restructure
- **Header (Top Bar)**: Logo, tenant selector, search, notifications, settings, user profile, last login
- **Sidebar**: 2-depth expandable menu with icons
- **Content Area**: Tab-based system (max 10 tabs, closeable, independent state)

### Menu Structure (confirmed)
```
📊 대시보드
📋 우대사업자 > 사업자 조회
🏢 영업점 관리 > 영업점 등록, 영업점 목록
🏪 가맹점 관리 > 가맹점 등록, 가맹점 목록, 단말기 관리
💰 정산 관리 > 영업점 정산내역, 가맹점 정산내역
💸 지급 이체 > 지급이체 등록, 지급이체 조회
💳 결제 관리 > 결제내역, 실패내역
⚙️ 운영 관리 > 공지사항, 계정관리, 환경설정
```

### Priority Screens (confirmed)
1. 영업점 등록 (3-step wizard)
2. 영업점 목록 (infinite scroll, filters, Excel export)
3. 영업점 상세 (modal or side panel)

## Research Findings

### Current Codebase Analysis
- **Routing**: Manual SPA routing using $state and conditional rendering in App.svelte
- **Styling**: Scoped CSS, no Tailwind, hand-written styles
- **bits-ui**: v0.21.0 installed but NOT USED anywhere
- **State**: Component-level $state, no global stores beyond auth/tenant
- **API**: ApiClient with JWT refresh, multi-tenant headers
- **Patterns**: 
  - Tables with sortable headers
  - Pagination with page buttons
  - Modal dialogs
  - Forms with labels and validation

### Key Files
- App.svelte: Route switching
- Layout.svelte: Sidebar + Header + Content
- Sidebar.svelte: 1-level nav buttons (needs 2-depth expansion)
- Header.svelte: Basic user info + logout (needs expansion)

### Dependencies Available
- bits-ui ^0.21.0 (Collapsible, Tabs, DropdownMenu, Dialog, Sheet, Select)
- @tanstack/svelte-table ^8.20.0 (not used, available for lists)
- date-fns ^3.6.0 (already used)

## Technical Decisions (Applied Defaults)

### 1. 영업점 = Organization
- **Decision**: 영업점 = 기존 Organization (같은 엔티티)
- **Rationale**: PRD-02에서 5단계 계층(DISTRIBUTOR/AGENCY/DEALER/SELLER/VENDOR)이 정의되어 있고, 이것이 영업점 구조와 일치함. 새 API 없이 기존 `/organizations` API 활용.

### 2. Migration Scope
- **Decision**: 전체 마이그레이션 (모든 페이지를 탭 시스템으로)
- **Rationale**: PRD-01에서 탭 기반 시스템을 전체 레이아웃으로 명시. 일관된 UX 제공.

### 3. Tab System Architecture
- **Decision**: bits-ui Tabs + 커스텀 탭 매니저
- **Rationale**: bits-ui가 이미 설치되어 있고 접근성 지원 내장. 탭 열기/닫기/최대 개수는 커스텀 로직으로.

### 4. 2-Depth Sidebar
- **Decision**: bits-ui Collapsible
- **Rationale**: 일관된 UI 라이브러리 사용. 애니메이션과 접근성 자동 지원.

### 5. 영업점 상세 View
- **Decision**: 사이드 패널 (Sheet)
- **Rationale**: PRD-01 참조 이미지에서 상세 정보가 패널 형태로 표시됨. 목록을 유지하면서 상세 확인 가능.

### 6. Backend API
- **Decision**: API 준비됨 - 실제 API 연동
- **Rationale**: 사용자 확인 - 영업점 CRUD API가 이미 존재. 기존 organizations API 활용하여 실제 데이터 연동.

## Applied Defaults (User can override)

| Item | Decision | Rationale |
|------|----------|-----------|
| 탭 상태 localStorage | YES | 새로고침 후 탭 복원 |
| 영업점 목록 스크롤 | Infinite scroll | PRD 명시 |
| Excel Export | 클라이언트 (SheetJS) | 서버 부하 없음 |
| Wizard Back 버튼 | 허용 | UX 개선 |
| 계층 캐스케이드 | YES | 상위 선택→하위 자동 로드 |

## Scope Boundaries (Finalized)

### INCLUDE
- New Layout system (Header, 2-depth Sidebar, Tab content area)
- Tab management system (open, close, max 10, focus existing, localStorage persistence)
- Header: Logo, tenant selector (DropdownMenu), search, notifications, settings, user profile, last login
- 2-depth Sidebar with Collapsible menus (PRD menu structure)
- 영업점 등록 wizard (3 steps: 구분→사업자정보→정산정보)
- 영업점 목록 (filters, infinite scroll, Excel export, date picker)
- 영업점 상세 Sheet panel (기본정보, 사업자정보, 계좌정보, 수수료, 한도)
- Migration of all existing routes to tab system
- Mock data layer for 영업점 (until backend ready)

### EXCLUDE
- New backend APIs (기존 organizations API 활용)
- Dashboard redesign (separate task per user)
- 다른 메뉴의 실제 구현 (우대사업자, 가맹점관리, 지급이체, 결제관리, 운영관리) - placeholder stubs만
- 2FA, user management
- 실제 Excel 파일 서버 다운로드 (클라이언트 생성만)
