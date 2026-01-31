<script lang="ts">
  import { branchApi } from '../../lib/branchApi';
  import { tabStore } from '../../lib/tabStore';
  import {
    type Branch,
    type BranchListParams,
    BRANCH_TYPE_LABELS,
    OrgType,
    OrgStatus
  } from '../../types/branch';
  import { format } from 'date-fns';

  let branches = $state<Branch[]>([]);
  let loading = $state(false);
  let initialLoading = $state(true);
  let hasMore = $state(true);
  let page = $state(0);
  const pageSize = 20;
  let totalCount = $state(0);

  let searchQuery = $state('');
  let typeFilter = $state<OrgType | ''>('');
  let statusFilter = $state<OrgStatus | ''>('');
  let startDate = $state('');
  let endDate = $state('');

  let sentinelEl: HTMLDivElement;

  const STATUS_LABELS: Record<OrgStatus, string> = {
    [OrgStatus.ACTIVE]: '정상',
    [OrgStatus.SUSPENDED]: '정지',
    [OrgStatus.TERMINATED]: '해지'
  };

  function getStatusClass(status: string): string {
    switch (status) {
      case OrgStatus.ACTIVE:
        return 'status-active';
      case OrgStatus.SUSPENDED:
        return 'status-suspended';
      case OrgStatus.TERMINATED:
        return 'status-terminated';
      default:
        return 'status-default';
    }
  }

  function formatDate(dateStr?: string): string {
    if (!dateStr) return '-';
    try {
      return format(new Date(dateStr), 'yyyy-MM-dd');
    } catch {
      return '-';
    }
  }

  async function loadBranches(reset: boolean = false) {
    if (loading) return;
    if (!reset && !hasMore) return;

    loading = true;

    if (reset) {
      page = 0;
      branches = [];
      hasMore = true;
    }

    try {
      const params: BranchListParams = {
        page,
        size: pageSize,
        type: typeFilter || undefined,
        status: statusFilter || undefined,
        search: searchQuery.trim() || undefined,
        startDate: startDate || undefined,
        endDate: endDate || undefined
      };

      const response = await branchApi.getBranches(params);

      if (response.success && response.data) {
        const newData = response.data.content;
        if (reset) {
          branches = newData;
        } else {
          branches = [...branches, ...newData];
        }
        totalCount = response.data.totalElements;
        hasMore = response.data.hasNext;
        page++;
      }
    } catch (err) {
      console.error('Failed to load branches:', err);
    } finally {
      loading = false;
      initialLoading = false;
    }
  }

  function handleSearch() {
    loadBranches(true);
  }

  function handleReset() {
    searchQuery = '';
    typeFilter = '';
    statusFilter = '';
    startDate = '';
    endDate = '';
    loadBranches(true);
  }

  function setDateRange(days: number) {
    const end = new Date();
    const start = new Date();
    start.setDate(end.getDate() - days);
    startDate = format(start, 'yyyy-MM-dd');
    endDate = format(end, 'yyyy-MM-dd');
  }

  function handleRowClick(branch: Branch) {
    tabStore.openTab({
      id: `branch-${branch.id}`,
      title: branch.name,
      icon: '🏢',
      component: 'BranchDetail',
      closeable: true,
      props: { branchId: branch.id }
    });
  }

  $effect(() => {
    loadBranches(true);
  });

  $effect(() => {
    if (!sentinelEl) return;

    const observer = new IntersectionObserver(
      (entries) => {
        const entry = entries[0];
        if (entry.isIntersecting && !loading && hasMore && !initialLoading) {
          loadBranches(false);
        }
      },
      { rootMargin: '100px', threshold: 0 }
    );

    observer.observe(sentinelEl);

    return () => observer.disconnect();
  });
</script>

<div class="branch-list">
  <!-- Header -->
  <header class="list-header">
    <div class="header-title">
      <h1>영업점 목록</h1>
      <span class="total-count">총 {totalCount.toLocaleString()}개</span>
    </div>
  </header>

  <!-- Filter Bar -->
  <div class="filter-bar">
    <div class="filter-row">
      <!-- Search -->
      <div class="filter-item search-box">
        <label for="search">검색어</label>
        <div class="input-wrapper">
          <svg class="search-icon" viewBox="0 0 20 20" fill="currentColor">
            <path fill-rule="evenodd" d="M8 4a4 4 0 100 8 4 4 0 000-8zM2 8a6 6 0 1110.89 3.476l4.817 4.817a1 1 0 01-1.414 1.414l-4.816-4.816A6 6 0 012 8z" clip-rule="evenodd"/>
          </svg>
          <input
            id="search"
            type="text"
            placeholder="영업점코드, 영업점명, 대표자..."
            bind:value={searchQuery}
            onkeydown={(e) => e.key === 'Enter' && handleSearch()}
          />
        </div>
      </div>

      <!-- Type Filter -->
      <div class="filter-item">
        <label for="type">영업점유형</label>
        <select id="type" bind:value={typeFilter}>
          <option value="">전체</option>
          {#each Object.values(OrgType) as type}
            <option value={type}>{BRANCH_TYPE_LABELS[type]}</option>
          {/each}
        </select>
      </div>

      <!-- Status Filter -->
      <div class="filter-item">
        <label for="status">상태</label>
        <select id="status" bind:value={statusFilter}>
          <option value="">전체</option>
          {#each Object.values(OrgStatus) as status}
            <option value={status}>{STATUS_LABELS[status]}</option>
          {/each}
        </select>
      </div>

      <!-- Date Range -->
      <div class="filter-item date-range">
        <label for="startDate">기간</label>
        <div class="date-inputs">
          <input id="startDate" type="date" bind:value={startDate} />
          <span class="date-separator">~</span>
          <input id="endDate" type="date" bind:value={endDate} aria-label="종료일" />
        </div>
        <div class="quick-dates">
          <button type="button" onclick={() => setDateRange(7)}>7일</button>
          <button type="button" onclick={() => setDateRange(30)}>30일</button>
          <button type="button" onclick={() => setDateRange(90)}>90일</button>
        </div>
      </div>
    </div>

    <div class="filter-actions">
      <button type="button" class="btn btn-secondary" onclick={handleReset}>
        초기화
      </button>
      <button type="button" class="btn btn-primary" onclick={handleSearch}>
        조회
      </button>
    </div>
  </div>

  <!-- Table Container -->
  <div class="table-container">
    <table class="data-table">
      <thead>
        <tr>
          <th class="col-code">영업점코드</th>
          <th class="col-name">영업점명</th>
          <th class="col-type">유형</th>
          <th class="col-rep">대표자</th>
          <th class="col-status">상태</th>
          <th class="col-date">등록일</th>
        </tr>
      </thead>
      <tbody>
        {#if initialLoading}
          <!-- Loading Skeleton -->
          {#each Array(8) as _}
            <tr class="skeleton-row">
              <td><div class="skeleton skeleton-code"></div></td>
              <td><div class="skeleton skeleton-name"></div></td>
              <td><div class="skeleton skeleton-type"></div></td>
              <td><div class="skeleton skeleton-rep"></div></td>
              <td><div class="skeleton skeleton-status"></div></td>
              <td><div class="skeleton skeleton-date"></div></td>
            </tr>
          {/each}
        {:else if branches.length === 0}
          <!-- Empty State -->
          <tr>
            <td colspan="6" class="empty-state">
              <div class="empty-content">
                <svg class="empty-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                  <path d="M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16m14 0h2m-2 0h-5m-9 0H3m2 0h5M9 7h1m-1 4h1m4-4h1m-1 4h1m-5 10v-5a1 1 0 011-1h2a1 1 0 011 1v5m-4 0h4"/>
                </svg>
                <p class="empty-title">검색 결과가 없습니다</p>
                <p class="empty-desc">다른 검색 조건을 시도해 보세요</p>
              </div>
            </td>
          </tr>
        {:else}
          <!-- Data Rows -->
          {#each branches as branch (branch.id)}
            <tr 
              class="data-row" 
              onclick={() => handleRowClick(branch)}
              onkeydown={(e) => e.key === 'Enter' && handleRowClick(branch)}
              role="button"
              tabindex="0"
            >
              <td class="col-code">
                <span class="code-text">{branch.orgCode}</span>
              </td>
              <td class="col-name">
                <span class="name-text">{branch.name}</span>
              </td>
              <td class="col-type">
                <span class="type-badge">{BRANCH_TYPE_LABELS[branch.orgType as OrgType] || branch.orgType}</span>
              </td>
              <td class="col-rep">
                {branch.businessInfo?.representative || '-'}
              </td>
              <td class="col-status">
                <span class="status-badge {getStatusClass(branch.status)}">
                  {STATUS_LABELS[branch.status as OrgStatus] || branch.status}
                </span>
              </td>
              <td class="col-date">
                {formatDate(branch.createdAt)}
              </td>
            </tr>
          {/each}
        {/if}
      </tbody>
    </table>

    <!-- Loading More Indicator -->
    {#if loading && !initialLoading}
      <div class="loading-more">
        <div class="spinner"></div>
        <span>더 불러오는 중...</span>
      </div>
    {/if}

    <!-- Infinite Scroll Sentinel -->
    <div bind:this={sentinelEl} class="scroll-sentinel"></div>

    <!-- End of List -->
    {#if !hasMore && branches.length > 0 && !loading}
      <div class="end-of-list">
        모든 데이터를 불러왔습니다
      </div>
    {/if}
  </div>
</div>

<style>
  .branch-list {
    display: flex;
    flex-direction: column;
    gap: 1.5rem;
    padding: 1.5rem;
    max-width: 1400px;
    margin: 0 auto;
  }

  /* Header */
  .list-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }

  .header-title {
    display: flex;
    align-items: baseline;
    gap: 1rem;
  }

  .header-title h1 {
    margin: 0;
    font-size: 1.75rem;
    font-weight: 700;
    color: #1e293b;
    letter-spacing: -0.02em;
  }

  .total-count {
    font-size: 0.9375rem;
    color: #64748b;
    font-weight: 500;
  }

  /* Filter Bar */
  .filter-bar {
    background: #f8fafc;
    border: 1px solid #e2e8f0;
    border-radius: 0.75rem;
    padding: 1.25rem;
  }

  .filter-row {
    display: flex;
    flex-wrap: wrap;
    gap: 1rem;
    margin-bottom: 1rem;
  }

  .filter-item {
    display: flex;
    flex-direction: column;
    gap: 0.375rem;
  }

  .filter-item label {
    font-size: 0.8125rem;
    font-weight: 600;
    color: #475569;
  }

  .filter-item.search-box {
    flex: 1;
    min-width: 240px;
  }

  .input-wrapper {
    position: relative;
    display: flex;
    align-items: center;
  }

  .search-icon {
    position: absolute;
    left: 0.75rem;
    width: 1rem;
    height: 1rem;
    color: #94a3b8;
    pointer-events: none;
  }

  .input-wrapper input {
    width: 100%;
    padding: 0.625rem 0.75rem 0.625rem 2.25rem;
    border: 1px solid #cbd5e1;
    border-radius: 0.5rem;
    font-size: 0.875rem;
    color: #334155;
    background: #fff;
    transition: border-color 0.15s, box-shadow 0.15s;
  }

  .input-wrapper input:focus {
    outline: none;
    border-color: #3b82f6;
    box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
  }

  .input-wrapper input::placeholder {
    color: #94a3b8;
  }

  .filter-item select {
    padding: 0.625rem 2rem 0.625rem 0.75rem;
    border: 1px solid #cbd5e1;
    border-radius: 0.5rem;
    font-size: 0.875rem;
    color: #334155;
    background: #fff url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='12' height='12' viewBox='0 0 12 12'%3E%3Cpath fill='%23475569' d='M6 8L1 3h10z'/%3E%3C/svg%3E") no-repeat right 0.75rem center;
    appearance: none;
    cursor: pointer;
    min-width: 140px;
    transition: border-color 0.15s, box-shadow 0.15s;
  }

  .filter-item select:focus {
    outline: none;
    border-color: #3b82f6;
    box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
  }

  .date-range {
    flex-direction: row;
    align-items: flex-end;
    gap: 0.75rem;
    flex-wrap: wrap;
  }

  .date-range > label {
    align-self: center;
    margin-bottom: 0.5rem;
  }

  .date-inputs {
    display: flex;
    align-items: center;
    gap: 0.5rem;
  }

  .date-inputs input[type="date"] {
    padding: 0.5rem 0.75rem;
    border: 1px solid #cbd5e1;
    border-radius: 0.5rem;
    font-size: 0.875rem;
    color: #334155;
    background: #fff;
    transition: border-color 0.15s, box-shadow 0.15s;
  }

  .date-inputs input[type="date"]:focus {
    outline: none;
    border-color: #3b82f6;
    box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
  }

  .date-separator {
    color: #64748b;
    font-weight: 500;
  }

  .quick-dates {
    display: flex;
    gap: 0.25rem;
  }

  .quick-dates button {
    padding: 0.375rem 0.625rem;
    border: 1px solid #cbd5e1;
    border-radius: 0.375rem;
    background: #fff;
    font-size: 0.75rem;
    font-weight: 500;
    color: #64748b;
    cursor: pointer;
    transition: all 0.15s;
  }

  .quick-dates button:hover {
    border-color: #3b82f6;
    color: #3b82f6;
    background: #eff6ff;
  }

  .filter-actions {
    display: flex;
    justify-content: flex-end;
    gap: 0.5rem;
    padding-top: 0.75rem;
    border-top: 1px solid #e2e8f0;
  }

  .btn {
    padding: 0.625rem 1.25rem;
    border-radius: 0.5rem;
    font-size: 0.875rem;
    font-weight: 600;
    cursor: pointer;
    transition: all 0.15s;
  }

  .btn-primary {
    background: #3b82f6;
    color: #fff;
    border: none;
  }

  .btn-primary:hover {
    background: #2563eb;
  }

  .btn-secondary {
    background: #fff;
    color: #475569;
    border: 1px solid #cbd5e1;
  }

  .btn-secondary:hover {
    background: #f1f5f9;
    border-color: #94a3b8;
  }

  /* Table */
  .table-container {
    background: #fff;
    border: 1px solid #e2e8f0;
    border-radius: 0.75rem;
    overflow: hidden;
    box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
  }

  .data-table {
    width: 100%;
    border-collapse: collapse;
  }

  .data-table thead {
    background: linear-gradient(to bottom, #f8fafc, #f1f5f9);
    position: sticky;
    top: 0;
    z-index: 10;
  }

  .data-table th {
    padding: 0.875rem 1rem;
    text-align: left;
    font-size: 0.8125rem;
    font-weight: 700;
    color: #475569;
    text-transform: uppercase;
    letter-spacing: 0.03em;
    border-bottom: 2px solid #e2e8f0;
    white-space: nowrap;
  }

  .data-table td {
    padding: 0.875rem 1rem;
    font-size: 0.875rem;
    color: #334155;
    border-bottom: 1px solid #f1f5f9;
    vertical-align: middle;
  }

  /* Column widths */
  .col-code { width: 140px; }
  .col-name { width: auto; min-width: 180px; }
  .col-type { width: 100px; text-align: center; }
  .col-rep { width: 100px; }
  .col-status { width: 90px; text-align: center; }
  .col-date { width: 110px; }

  .data-table th.col-type,
  .data-table th.col-status {
    text-align: center;
  }

  /* Data rows */
  .data-row {
    cursor: pointer;
    transition: background-color 0.15s;
  }

  .data-row:nth-child(even) {
    background: #fafbfc;
  }

  .data-row:hover {
    background: #eff6ff;
  }

  .code-text {
    font-family: 'SF Mono', 'Monaco', 'Inconsolata', monospace;
    font-size: 0.8125rem;
    color: #6366f1;
    font-weight: 500;
  }

  .name-text {
    font-weight: 500;
    color: #1e293b;
  }

  .type-badge {
    display: inline-block;
    padding: 0.25rem 0.625rem;
    background: #f1f5f9;
    border-radius: 0.375rem;
    font-size: 0.75rem;
    font-weight: 600;
    color: #475569;
  }

  /* Status badges */
  .status-badge {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    padding: 0.25rem 0.625rem;
    border-radius: 9999px;
    font-size: 0.75rem;
    font-weight: 600;
  }

  .status-active {
    background: #dcfce7;
    color: #15803d;
  }

  .status-suspended {
    background: #fef3c7;
    color: #b45309;
  }

  .status-terminated {
    background: #fee2e2;
    color: #dc2626;
  }

  .status-default {
    background: #f1f5f9;
    color: #64748b;
  }

  /* Skeleton Loading */
  .skeleton-row td {
    padding: 1rem;
  }

  .skeleton {
    background: linear-gradient(90deg, #e2e8f0 25%, #f1f5f9 50%, #e2e8f0 75%);
    background-size: 200% 100%;
    animation: shimmer 1.5s infinite;
    border-radius: 0.375rem;
    height: 1rem;
  }

  .skeleton-code { width: 100px; }
  .skeleton-name { width: 160px; }
  .skeleton-type { width: 70px; margin: 0 auto; }
  .skeleton-rep { width: 60px; }
  .skeleton-status { width: 50px; margin: 0 auto; }
  .skeleton-date { width: 80px; }

  @keyframes shimmer {
    0% { background-position: 200% 0; }
    100% { background-position: -200% 0; }
  }

  /* Empty State */
  .empty-state {
    padding: 4rem 2rem !important;
    text-align: center;
  }

  .empty-content {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 0.75rem;
  }

  .empty-icon {
    width: 4rem;
    height: 4rem;
    color: #cbd5e1;
    stroke-width: 1;
  }

  .empty-title {
    margin: 0;
    font-size: 1rem;
    font-weight: 600;
    color: #64748b;
  }

  .empty-desc {
    margin: 0;
    font-size: 0.875rem;
    color: #94a3b8;
  }

  /* Loading More */
  .loading-more {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 0.75rem;
    padding: 1.5rem;
    color: #64748b;
    font-size: 0.875rem;
  }

  .spinner {
    width: 1.25rem;
    height: 1.25rem;
    border: 2px solid #e2e8f0;
    border-top-color: #3b82f6;
    border-radius: 50%;
    animation: spin 0.8s linear infinite;
  }

  @keyframes spin {
    to { transform: rotate(360deg); }
  }

  /* Scroll Sentinel */
  .scroll-sentinel {
    height: 1px;
    visibility: hidden;
  }

  /* End of List */
  .end-of-list {
    padding: 1.25rem;
    text-align: center;
    font-size: 0.8125rem;
    color: #94a3b8;
    border-top: 1px solid #f1f5f9;
  }

  /* Responsive */
  @media (max-width: 1024px) {
    .filter-row {
      flex-direction: column;
    }

    .filter-item.search-box {
      min-width: 100%;
    }

    .date-range {
      flex-direction: column;
      align-items: flex-start;
    }

    .date-range > label {
      margin-bottom: 0;
    }
  }

  @media (max-width: 768px) {
    .branch-list {
      padding: 1rem;
    }

    .table-container {
      overflow-x: auto;
    }

    .data-table {
      min-width: 700px;
    }

    .filter-actions {
      flex-direction: column;
    }

    .filter-actions .btn {
      width: 100%;
    }
  }
</style>
