# Z-Index Layer Hierarchy Fix

## TL;DR

> **Quick Summary**: Fix z-index inconsistencies across 8 Svelte frontend components to establish a proper visual layering hierarchy where header > sidebar, tooltips < modals, and StatusBar stays below overlays.
> 
> **Deliverables**:
> - Fixed header visibility (z-[5] → z-[15])
> - Reduced dropdown menu z-index (z-[1000] → z-[60])
> - StatusBar below modals (z-50 → z-[30])
> - Tooltips/popovers/selects separated from modals (z-50 → z-[40])
> - BranchRegistration wizard steps normalized (z-100 → z-10)
> 
> **Estimated Effort**: Quick
> **Parallel Execution**: YES - 3 waves
> **Critical Path**: All tasks are independent after Wave 1 verification

---

## Context

### Original Request
Fix all z-index inconsistencies in the layout components to establish a proper layering hierarchy.

### Problem Analysis

**Current Layer Hierarchy (BROKEN):**
```
z-0      → BranchRegistration.svelte (progress bar background)
z-[5]    → NewHeader.svelte (sticky header) ❌ TOO LOW
z-10     → sidebar.svelte (fixed sidebar), OrgFlowNode.svelte
z-20     → sidebar-rail.svelte, calendar.svelte
z-50     → 13+ COMPONENTS COLLISION:
          - MerchantManagement.svelte (modal)
          - BranchOrganization.svelte (3 modals + toast)
          - ConfirmModal.svelte
          - SettlementAccountManager.svelte (2 modals)
          - ContactManager.svelte (2 modals)
          - OrganizationPickerDialog.svelte
          - date-picker.svelte, date-range-picker.svelte
          - popover-content.svelte, select-content.svelte
          - tooltip-content.svelte
          - StatusBar.svelte (fixed footer) ❌ SHOULD BE LOWER
z-[99]   → sheet-overlay.svelte (fixed backdrop)
z-[100]  → sheet-content.svelte (sidebar mobile drawer)
          → BranchRegistration.svelte (wizard steps) ❌ UNNECESSARY
z-[1000] → NewHeader.svelte (dropdown menus) ❌ WAY TOO HIGH
```

**Target Layer Hierarchy (FIXED):**
```
z-0      → Background elements
z-10     → Sidebar (KEEP)
z-[15]   → Sticky header (INCREASE from z-[5])
z-20     → Sidebar rail, calendar focus (KEEP)
z-[30]   → StatusBar footer (REDUCE from z-50)
z-[40]   → Tooltips, Popovers, Select dropdowns (SEPARATE from modals)
z-50     → Modals/Dialogs only (CONSOLIDATE)
z-[60]   → Header dropdown menus (REDUCE from z-[1000])
z-[99]   → Sheet overlay (KEEP)
z-[100]  → Sheet content (KEEP)
```

### Verified File Locations
All z-index values confirmed via file read:
- `NewHeader.svelte:51` - header z-[5]
- `NewHeader.svelte:87,116` - DropdownMenu.Content z-[1000]
- `StatusBar.svelte:29` - z-50
- `BranchRegistration.svelte:440` - z-100
- `tooltip-content.svelte:30,40` - z-50
- `popover-content.svelte:26` - z-50
- `select-content.svelte:30` - z-50
- `date-picker.svelte:92` - z-50
- `date-range-picker.svelte:128` - z-50

---

## Work Objectives

### Core Objective
Establish a consistent z-index hierarchy that prevents layer conflicts and ensures proper visual stacking across all UI components.

### Concrete Deliverables
- 8 files modified with corrected z-index values
- Visual verification that header appears above sidebar
- Visual verification that tooltips appear correctly relative to modals

### Definition of Done
- [ ] All z-index values updated per target hierarchy
- [ ] Header visible above sidebar during scroll
- [ ] Dropdown menus appear above all content when opened
- [ ] StatusBar stays below modal overlays
- [ ] Tooltips/popovers appear above content but don't compete with modals
- [ ] No visual regressions in existing functionality

### Must Have
- Header z-index > Sidebar z-index (z-[15] > z-10)
- StatusBar z-index < Modal z-index (z-[30] < z-50)
- Tooltip/Popover/Select z-index < Modal z-index (z-[40] < z-50)
- Dropdown menus usable from header (z-[60] > z-50)

### Must NOT Have (Guardrails)
- DO NOT change modal z-index values (keep z-50)
- DO NOT change sheet overlay/content values (keep z-[99]/z-[100])
- DO NOT change sidebar values (keep z-10)
- DO NOT change sidebar-rail values (keep z-20)
- DO NOT modify component behavior, only z-index CSS classes
- DO NOT remove or modify animation classes when editing
- DO NOT introduce CSS custom properties (stay with Tailwind classes)

---

## Verification Strategy (MANDATORY)

### Test Decision
- **Infrastructure exists**: NO (no automated tests for visual z-index)
- **User wants tests**: Manual visual verification via Playwright
- **QA approach**: Browser automation with screenshots

### Automated Verification (via Playwright)

Each task includes browser-executable verification steps using the playwright skill. The agent will:
1. Navigate to relevant pages
2. Trigger UI interactions that test z-index stacking
3. Take screenshots as evidence
4. Assert DOM visibility states

---

## Execution Strategy

### Parallel Execution Waves

```
Wave 1 (Start Immediately):
├── Task 1: Fix NewHeader z-index (header + dropdowns)
├── Task 2: Fix StatusBar z-index
├── Task 3: Fix BranchRegistration wizard z-index
├── Task 4: Fix tooltip-content z-index
├── Task 5: Fix popover-content z-index
├── Task 6: Fix select-content z-index
├── Task 7: Fix date-picker z-index
└── Task 8: Fix date-range-picker z-index

Wave 2 (After Wave 1):
└── Task 9: Visual verification via Playwright (all changes)

No dependencies between individual file edits - all can run in parallel.
Verification depends on ALL edits completing first.
```

### Dependency Matrix

| Task | Depends On | Blocks | Can Parallelize With |
|------|------------|--------|---------------------|
| 1 | None | 9 | 2, 3, 4, 5, 6, 7, 8 |
| 2 | None | 9 | 1, 3, 4, 5, 6, 7, 8 |
| 3 | None | 9 | 1, 2, 4, 5, 6, 7, 8 |
| 4 | None | 9 | 1, 2, 3, 5, 6, 7, 8 |
| 5 | None | 9 | 1, 2, 3, 4, 6, 7, 8 |
| 6 | None | 9 | 1, 2, 3, 4, 5, 7, 8 |
| 7 | None | 9 | 1, 2, 3, 4, 5, 6, 8 |
| 8 | None | 9 | 1, 2, 3, 4, 5, 6, 7 |
| 9 | 1,2,3,4,5,6,7,8 | None | None (final) |

### Agent Dispatch Summary

| Wave | Tasks | Recommended Agents |
|------|-------|-------------------|
| 1 | 1-8 | All quick category, parallel dispatch |
| 2 | 9 | visual-engineering with playwright skill |

---

## TODOs

- [ ] 1. Fix NewHeader z-index values

  **What to do**:
  - Change header element z-[5] to z-[15] on line 51
  - Change first DropdownMenu.Content z-[1000] to z-[60] on line 87
  - Change second DropdownMenu.Content z-[1000] to z-[60] on line 116
  - Preserve all other classes and attributes exactly

  **Must NOT do**:
  - Change any other styling or behavior
  - Remove animation classes
  - Modify the header structure

  **Recommended Agent Profile**:
  - **Category**: `quick`
    - Reason: Simple find-and-replace of 3 z-index values in a single file
  - **Skills**: None needed
    - Simple text substitution, no specialized knowledge required
  - **Skills Evaluated but Omitted**:
    - `svelte-programmer`: Not needed - no component logic changes
    - `frontend-ui-ux`: Not needed - no design decisions

  **Parallelization**:
  - **Can Run In Parallel**: YES
  - **Parallel Group**: Wave 1 (with Tasks 2-8)
  - **Blocks**: Task 9 (verification)
  - **Blocked By**: None (can start immediately)

  **References**:
  
  **File Reference**:
  - `frontend/src/components/NewHeader.svelte:51` - Header element with z-[5]
  - `frontend/src/components/NewHeader.svelte:87` - First DropdownMenu.Content with z-[1000]
  - `frontend/src/components/NewHeader.svelte:116` - Second DropdownMenu.Content with z-[1000]
  
  **Pattern Reference**:
  - Current: `sticky top-0 z-[5]` → Target: `sticky top-0 z-[15]`
  - Current: `z-[1000]` → Target: `z-[60]`

  **Acceptance Criteria**:

  **Automated Verification**:
  ```bash
  # Agent runs:
  grep -n "z-\[15\]" frontend/src/components/NewHeader.svelte
  # Assert: Returns line 51
  
  grep -n "z-\[60\]" frontend/src/components/NewHeader.svelte
  # Assert: Returns lines 87 and 116
  
  grep -n "z-\[5\]\|z-\[1000\]" frontend/src/components/NewHeader.svelte
  # Assert: No matches (old values removed)
  ```

  **Evidence to Capture**:
  - [ ] grep output showing new z-index values on expected lines
  - [ ] grep output confirming old values removed

  **Commit**: YES (groups with Tasks 2-8)
  - Message: `fix(frontend): update NewHeader z-index for proper layer hierarchy`
  - Files: `frontend/src/components/NewHeader.svelte`
  - Pre-commit: grep verification

---

- [ ] 2. Fix StatusBar z-index value

  **What to do**:
  - Change z-50 to z-[30] on line 29
  - Preserve all other classes and attributes exactly

  **Must NOT do**:
  - Change position (fixed), dimensions, or other styling
  - Modify the component structure

  **Recommended Agent Profile**:
  - **Category**: `quick`
    - Reason: Single z-index value change in a small file
  - **Skills**: None needed
  - **Skills Evaluated but Omitted**:
    - `svelte-programmer`: Not needed - trivial text change

  **Parallelization**:
  - **Can Run In Parallel**: YES
  - **Parallel Group**: Wave 1 (with Tasks 1, 3-8)
  - **Blocks**: Task 9 (verification)
  - **Blocked By**: None (can start immediately)

  **References**:
  
  **File Reference**:
  - `frontend/src/components/StatusBar.svelte:29` - Footer element with z-50
  
  **Pattern Reference**:
  - Current: `fixed bottom-0 left-0 right-0 z-50` → Target: `fixed bottom-0 left-0 right-0 z-[30]`

  **Acceptance Criteria**:

  **Automated Verification**:
  ```bash
  # Agent runs:
  grep -n "z-\[30\]" frontend/src/components/StatusBar.svelte
  # Assert: Returns line 29
  
  grep -n "z-50" frontend/src/components/StatusBar.svelte
  # Assert: No matches (old value removed)
  ```

  **Evidence to Capture**:
  - [ ] grep output confirming z-[30] on line 29
  - [ ] grep output confirming z-50 removed

  **Commit**: YES (groups with Tasks 1, 3-8)
  - Message: `fix(frontend): reduce StatusBar z-index to stay below modals`
  - Files: `frontend/src/components/StatusBar.svelte`
  - Pre-commit: grep verification

---

- [ ] 3. Fix BranchRegistration wizard z-index value

  **What to do**:
  - Change z-100 to z-10 on line 440 (wizard steps container)
  - Preserve all other classes and attributes exactly

  **Must NOT do**:
  - Change the wizard logic or step navigation
  - Modify other z-index values in the file (z-0 on progress bar is correct)

  **Recommended Agent Profile**:
  - **Category**: `quick`
    - Reason: Single z-index value change
  - **Skills**: None needed
  - **Skills Evaluated but Omitted**:
    - `svelte-programmer`: Not needed - no logic changes

  **Parallelization**:
  - **Can Run In Parallel**: YES
  - **Parallel Group**: Wave 1 (with Tasks 1-2, 4-8)
  - **Blocks**: Task 9 (verification)
  - **Blocked By**: None (can start immediately)

  **References**:
  
  **File Reference**:
  - `frontend/src/routes/branch/BranchRegistration.svelte:440` - Line with z-100
  
  **Pattern Reference**:
  - Current: `relative z-100` (or similar) → Target: `relative z-10`

  **Acceptance Criteria**:

  **Automated Verification**:
  ```bash
  # Agent runs:
  grep -n "z-10" frontend/src/routes/branch/BranchRegistration.svelte | head -5
  # Assert: Contains line 440 area with z-10
  
  grep -n "z-100" frontend/src/routes/branch/BranchRegistration.svelte
  # Assert: No matches (old value removed)
  ```

  **Evidence to Capture**:
  - [ ] grep output confirming z-10 on expected line
  - [ ] grep output confirming z-100 removed

  **Commit**: YES (groups with Tasks 1-2, 4-8)
  - Message: `fix(frontend): normalize BranchRegistration wizard step z-index`
  - Files: `frontend/src/routes/branch/BranchRegistration.svelte`
  - Pre-commit: grep verification

---

- [ ] 4. Fix tooltip-content z-index values

  **What to do**:
  - Change z-50 to z-[40] on line 30 (content class)
  - Change z-50 to z-[40] on line 40 (arrow class)
  - Preserve all other classes including animations

  **Must NOT do**:
  - Change TooltipPrimitive component imports or structure
  - Modify animation or positioning classes

  **Recommended Agent Profile**:
  - **Category**: `quick`
    - Reason: Two simple z-index value changes
  - **Skills**: None needed
  - **Skills Evaluated but Omitted**:
    - `svelte-programmer`: Not needed - just CSS class changes

  **Parallelization**:
  - **Can Run In Parallel**: YES
  - **Parallel Group**: Wave 1 (with Tasks 1-3, 5-8)
  - **Blocks**: Task 9 (verification)
  - **Blocked By**: None (can start immediately)

  **References**:
  
  **File Reference**:
  - `frontend/src/lib/components/ui/tooltip/tooltip-content.svelte:30` - TooltipPrimitive.Content class with z-50
  - `frontend/src/lib/components/ui/tooltip/tooltip-content.svelte:40` - Arrow div class with z-50
  
  **Pattern Reference**:
  - Current: `z-50` → Target: `z-[40]` (both occurrences)

  **Acceptance Criteria**:

  **Automated Verification**:
  ```bash
  # Agent runs:
  grep -c "z-\[40\]" frontend/src/lib/components/ui/tooltip/tooltip-content.svelte
  # Assert: Returns 2 (two occurrences)
  
  grep -n "z-50" frontend/src/lib/components/ui/tooltip/tooltip-content.svelte
  # Assert: No matches (old values removed)
  ```

  **Evidence to Capture**:
  - [ ] grep count showing 2 occurrences of z-[40]
  - [ ] grep output confirming z-50 removed

  **Commit**: YES (groups with Tasks 1-3, 5-8)
  - Message: `fix(frontend): separate tooltip z-index from modal layer`
  - Files: `frontend/src/lib/components/ui/tooltip/tooltip-content.svelte`
  - Pre-commit: grep verification

---

- [ ] 5. Fix popover-content z-index value

  **What to do**:
  - Change z-50 to z-[40] on line 26
  - Preserve all other classes including animations

  **Must NOT do**:
  - Change PopoverPrimitive component structure
  - Modify animation or positioning classes

  **Recommended Agent Profile**:
  - **Category**: `quick`
    - Reason: Single z-index value change
  - **Skills**: None needed
  - **Skills Evaluated but Omitted**:
    - `svelte-programmer`: Not needed

  **Parallelization**:
  - **Can Run In Parallel**: YES
  - **Parallel Group**: Wave 1 (with Tasks 1-4, 6-8)
  - **Blocks**: Task 9 (verification)
  - **Blocked By**: None (can start immediately)

  **References**:
  
  **File Reference**:
  - `frontend/src/lib/components/ui/popover/popover-content.svelte:26` - z-50 in class string
  
  **Pattern Reference**:
  - Current: `"z-50 w-72 rounded-md...` → Target: `"z-[40] w-72 rounded-md...`

  **Acceptance Criteria**:

  **Automated Verification**:
  ```bash
  # Agent runs:
  grep -n "z-\[40\]" frontend/src/lib/components/ui/popover/popover-content.svelte
  # Assert: Returns line 26
  
  grep -n "z-50" frontend/src/lib/components/ui/popover/popover-content.svelte
  # Assert: No matches
  ```

  **Evidence to Capture**:
  - [ ] grep output confirming z-[40] on line 26
  - [ ] grep output confirming z-50 removed

  **Commit**: YES (groups with Tasks 1-4, 6-8)
  - Message: `fix(frontend): separate popover z-index from modal layer`
  - Files: `frontend/src/lib/components/ui/popover/popover-content.svelte`
  - Pre-commit: grep verification

---

- [ ] 6. Fix select-content z-index value

  **What to do**:
  - Change z-50 to z-[40] on line 30
  - Preserve all other classes including animations

  **Must NOT do**:
  - Change SelectPrimitive component structure
  - Modify viewport or scroll button components

  **Recommended Agent Profile**:
  - **Category**: `quick`
    - Reason: Single z-index value change
  - **Skills**: None needed
  - **Skills Evaluated but Omitted**:
    - `svelte-programmer`: Not needed

  **Parallelization**:
  - **Can Run In Parallel**: YES
  - **Parallel Group**: Wave 1 (with Tasks 1-5, 7-8)
  - **Blocks**: Task 9 (verification)
  - **Blocked By**: None (can start immediately)

  **References**:
  
  **File Reference**:
  - `frontend/src/lib/components/ui/select/select-content.svelte:30` - z-50 in class string
  
  **Pattern Reference**:
  - Current: `relative z-50 max-h-...` → Target: `relative z-[40] max-h-...`

  **Acceptance Criteria**:

  **Automated Verification**:
  ```bash
  # Agent runs:
  grep -n "z-\[40\]" frontend/src/lib/components/ui/select/select-content.svelte
  # Assert: Returns line 30
  
  grep -n "z-50" frontend/src/lib/components/ui/select/select-content.svelte
  # Assert: No matches
  ```

  **Evidence to Capture**:
  - [ ] grep output confirming z-[40] on line 30
  - [ ] grep output confirming z-50 removed

  **Commit**: YES (groups with Tasks 1-5, 7-8)
  - Message: `fix(frontend): separate select dropdown z-index from modal layer`
  - Files: `frontend/src/lib/components/ui/select/select-content.svelte`
  - Pre-commit: grep verification

---

- [ ] 7. Fix date-picker z-index value

  **What to do**:
  - Change z-50 to z-[40] on line 92 (PopoverPrimitive.Content class)
  - Preserve all other classes including animations

  **Must NOT do**:
  - Change calendar logic or date handling
  - Modify popover trigger or calendar component

  **Recommended Agent Profile**:
  - **Category**: `quick`
    - Reason: Single z-index value change
  - **Skills**: None needed
  - **Skills Evaluated but Omitted**:
    - `svelte-programmer`: Not needed

  **Parallelization**:
  - **Can Run In Parallel**: YES
  - **Parallel Group**: Wave 1 (with Tasks 1-6, 8)
  - **Blocks**: Task 9 (verification)
  - **Blocked By**: None (can start immediately)

  **References**:
  
  **File Reference**:
  - `frontend/src/lib/components/ui/date-picker/date-picker.svelte:92` - z-50 in PopoverPrimitive.Content class
  
  **Pattern Reference**:
  - Current: `class="z-50 w-auto rounded-md...` → Target: `class="z-[40] w-auto rounded-md...`

  **Acceptance Criteria**:

  **Automated Verification**:
  ```bash
  # Agent runs:
  grep -n "z-\[40\]" frontend/src/lib/components/ui/date-picker/date-picker.svelte
  # Assert: Returns line 92
  
  grep -n "z-50" frontend/src/lib/components/ui/date-picker/date-picker.svelte
  # Assert: No matches
  ```

  **Evidence to Capture**:
  - [ ] grep output confirming z-[40] on line 92
  - [ ] grep output confirming z-50 removed

  **Commit**: YES (groups with Tasks 1-6, 8)
  - Message: `fix(frontend): separate date-picker z-index from modal layer`
  - Files: `frontend/src/lib/components/ui/date-picker/date-picker.svelte`
  - Pre-commit: grep verification

---

- [ ] 8. Fix date-range-picker z-index value

  **What to do**:
  - Change z-50 to z-[40] on line 128 (PopoverPrimitive.Content class)
  - Preserve all other classes including animations

  **Must NOT do**:
  - Change range calendar logic or date handling
  - Modify popover trigger or calendar component

  **Recommended Agent Profile**:
  - **Category**: `quick`
    - Reason: Single z-index value change
  - **Skills**: None needed
  - **Skills Evaluated but Omitted**:
    - `svelte-programmer`: Not needed

  **Parallelization**:
  - **Can Run In Parallel**: YES
  - **Parallel Group**: Wave 1 (with Tasks 1-7)
  - **Blocks**: Task 9 (verification)
  - **Blocked By**: None (can start immediately)

  **References**:
  
  **File Reference**:
  - `frontend/src/lib/components/ui/date-range-picker/date-range-picker.svelte:128` - z-50 in PopoverPrimitive.Content class
  
  **Pattern Reference**:
  - Current: `class="z-50 w-auto rounded-md...` → Target: `class="z-[40] w-auto rounded-md...`

  **Acceptance Criteria**:

  **Automated Verification**:
  ```bash
  # Agent runs:
  grep -n "z-\[40\]" frontend/src/lib/components/ui/date-range-picker/date-range-picker.svelte
  # Assert: Returns line 128
  
  grep -n "z-50" frontend/src/lib/components/ui/date-range-picker/date-range-picker.svelte
  # Assert: No matches
  ```

  **Evidence to Capture**:
  - [ ] grep output confirming z-[40] on line 128
  - [ ] grep output confirming z-50 removed

  **Commit**: YES (groups with Tasks 1-7)
  - Message: `fix(frontend): separate date-range-picker z-index from modal layer`
  - Files: `frontend/src/lib/components/ui/date-range-picker/date-range-picker.svelte`
  - Pre-commit: grep verification

---

- [ ] 9. Visual Verification via Playwright

  **What to do**:
  - Start dev server if not running
  - Test header visibility above sidebar during scroll
  - Test dropdown menu appearance above modals
  - Test StatusBar stays below modal overlays
  - Test tooltip/popover appearance
  - Capture screenshots as evidence

  **Must NOT do**:
  - Make any code changes
  - Skip any verification step

  **Recommended Agent Profile**:
  - **Category**: `visual-engineering`
    - Reason: Browser-based visual verification requires UI expertise
  - **Skills**: [`playwright`]
    - `playwright`: Required for browser automation and visual verification
  - **Skills Evaluated but Omitted**:
    - `frontend-ui-ux`: Not needed - no design work, just verification

  **Parallelization**:
  - **Can Run In Parallel**: NO
  - **Parallel Group**: Wave 2 (sequential after Wave 1)
  - **Blocks**: None (final task)
  - **Blocked By**: Tasks 1-8 (all edits must complete first)

  **References**:
  
  **Application Reference**:
  - Dev server: `http://localhost:5173`
  - Login page for authentication
  - Dashboard for header/sidebar verification
  - Branch registration page for wizard verification
  - Any page with tooltips for tooltip verification
  
  **Test Scenarios**:
  1. Header above sidebar: Scroll down, verify header stays visible
  2. Dropdown above content: Click header dropdown, verify it's not cut off
  3. StatusBar below modal: Open any modal, verify StatusBar doesn't overlap
  4. Tooltip positioning: Hover on tooltip trigger, verify appears correctly

  **Acceptance Criteria**:

  **Automated Verification (via Playwright)**:
  ```
  # Agent executes via playwright browser automation:
  
  Test 1 - Header Visibility:
  1. Navigate to: http://localhost:5173
  2. Login if required
  3. Scroll down 500px
  4. Assert: Header element is still visible (getBoundingClientRect().top >= 0)
  5. Screenshot: .sisyphus/evidence/task-9-header-visibility.png
  
  Test 2 - Dropdown Menu:
  1. Navigate to: http://localhost:5173
  2. Click on user dropdown in header
  3. Assert: Dropdown menu is visible and not clipped
  4. Screenshot: .sisyphus/evidence/task-9-dropdown-menu.png
  
  Test 3 - StatusBar vs Modal:
  1. Navigate to a page with a modal (e.g., branch management)
  2. Open a modal/dialog
  3. Assert: StatusBar z-index (30) < Modal z-index (50)
  4. Screenshot: .sisyphus/evidence/task-9-statusbar-modal.png
  
  Test 4 - Tooltip:
  1. Navigate to a page with tooltip triggers
  2. Hover over a tooltip trigger
  3. Assert: Tooltip is visible
  4. Screenshot: .sisyphus/evidence/task-9-tooltip.png
  ```

  **Evidence to Capture**:
  - [ ] Screenshot: header-visibility.png
  - [ ] Screenshot: dropdown-menu.png
  - [ ] Screenshot: statusbar-modal.png
  - [ ] Screenshot: tooltip.png

  **Commit**: NO (verification only)

---

## Commit Strategy

| After Tasks | Message | Files | Verification |
|-------------|---------|-------|--------------|
| 1-8 (single commit) | `fix(frontend): establish proper z-index layer hierarchy` | All 8 modified files | grep for old z-index values returns 0 |

**Recommended: Single atomic commit for all z-index changes**

This ensures:
- All changes are applied together
- Easy to revert if issues found
- Clear audit trail

**Alternative: Group by component type**
- Layout components: NewHeader, StatusBar, BranchRegistration
- UI primitives: tooltip, popover, select, date-picker, date-range-picker

---

## Success Criteria

### Verification Commands
```bash
# Verify all old z-index values are removed
grep -r "z-\[5\]" frontend/src/components/NewHeader.svelte  # Should return nothing
grep -r "z-\[1000\]" frontend/src/components/NewHeader.svelte  # Should return nothing
grep -r "z-50" frontend/src/components/StatusBar.svelte  # Should return nothing
grep -r "z-100" frontend/src/routes/branch/BranchRegistration.svelte  # Should return nothing
grep -r "z-50" frontend/src/lib/components/ui/tooltip/tooltip-content.svelte  # Should return nothing
grep -r "z-50" frontend/src/lib/components/ui/popover/popover-content.svelte  # Should return nothing
grep -r "z-50" frontend/src/lib/components/ui/select/select-content.svelte  # Should return nothing
grep -r "z-50" frontend/src/lib/components/ui/date-picker/date-picker.svelte  # Should return nothing
grep -r "z-50" frontend/src/lib/components/ui/date-range-picker/date-range-picker.svelte  # Should return nothing

# Verify new z-index values are present
grep -r "z-\[15\]" frontend/src/components/NewHeader.svelte  # Should find header
grep -r "z-\[60\]" frontend/src/components/NewHeader.svelte  # Should find 2 dropdowns
grep -r "z-\[30\]" frontend/src/components/StatusBar.svelte  # Should find footer
grep -r "z-\[40\]" frontend/src/lib/components/ui/tooltip/tooltip-content.svelte  # Should find 2
grep -r "z-\[40\]" frontend/src/lib/components/ui/popover/popover-content.svelte  # Should find 1
grep -r "z-\[40\]" frontend/src/lib/components/ui/select/select-content.svelte  # Should find 1
grep -r "z-\[40\]" frontend/src/lib/components/ui/date-picker/date-picker.svelte  # Should find 1
grep -r "z-\[40\]" frontend/src/lib/components/ui/date-range-picker/date-range-picker.svelte  # Should find 1
```

### Final Checklist
- [ ] All "Must Have" z-index hierarchies established
- [ ] All "Must NOT Have" values unchanged (z-50 for modals, z-99/100 for sheets)
- [ ] Visual verification passed via Playwright
- [ ] Screenshots captured as evidence
- [ ] Single atomic commit created

---

## Edge Cases Addressed

1. **Tooltip inside Modal**: bits-ui uses portals, so tooltip (z-[40]) renders at body level, not inside modal DOM. Since modal is z-50 and tooltip is z-[40], tooltip from within modal might appear behind modal overlay. This is expected behavior - tooltips should not appear above modal overlays.

2. **Nested dropdowns**: Not affected - each dropdown uses its own z-index context via portal.

3. **Mobile sidebar sheet**: Sheet content at z-[100] is above header dropdown (z-[60]), which is correct - mobile menu should overlay everything except sheet backdrop.
