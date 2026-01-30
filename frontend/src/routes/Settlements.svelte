<script lang="ts">
  import { apiClient } from '../lib/api';
  import { tenantStore } from '../lib/stores';
  import { format } from 'date-fns';
  import type { Settlement, PagedResponse } from '../types/api';
  
  let settlements = $state<Settlement[]>([]);
  let loading = $state(true);
  let error = $state<string | null>(null);
  
  let pendingCount = $state(0);
  let approvedCount = $state(0);
  let paidCount = $state(0);
  
  let statusFilter = $state<string>('ALL');
  let currentPage = $state(0);
  let pageSize = $state(20);
  let totalCount = $state(0);
  let totalPages = $state(0);
  
  let sortField = $state<string>('created_at');
  let sortDirection = $state<'asc' | 'desc'>('desc');
  
  const displaySettlements = $derived(settlements);
  
  function formatCurrency(amount: number): string {
    return new Intl.NumberFormat('ko-KR', {
      style: 'currency',
      currency: 'KRW'
    }).format(amount);
  }
  
  function getStatusBadge(status: string): string {
    const badges: Record<string, string> = {
      'PENDING': 'warning',
      'APPROVED': 'info',
      'PAID': 'success',
      'FAILED': 'danger'
    };
    return badges[status] || 'default';
  }
  
  function getStatusLabel(status: string): string {
    const labels: Record<string, string> = {
      'PENDING': '대기',
      'APPROVED': '확정',
      'PAID': '완료',
      'FAILED': '실패'
    };
    return labels[status] || status;
  }
  
  function getEntryTypeLabel(entryType: string): string {
    return entryType === 'CREDIT' ? '수취' : '지급';
  }
  
  function sortBy(field: string) {
    if (sortField === field) {
      sortDirection = sortDirection === 'asc' ? 'desc' : 'asc';
    } else {
      sortField = field;
      sortDirection = 'asc';
    }
    loadSettlements();
  }
  
  async function loadSettlements() {
    if (!tenantStore.current) {
      return;
    }
    
    apiClient.setTenantId(tenantStore.current);
    loading = true;
    error = null;
    
    try {
      const params = new URLSearchParams({
        page: currentPage.toString(),
        size: pageSize.toString(),
        sortBy: sortField,
        direction: sortDirection.toUpperCase()
      });
      
      if (statusFilter !== 'ALL') {
        params.append('status', statusFilter);
      }
      
      const response = await apiClient.get<PagedResponse<Settlement>>(`/settlements?${params}`);
      
      if (response.success && response.data) {
        settlements = response.data.content;
        totalCount = response.data.totalElements;
        totalPages = response.data.totalPages;
        currentPage = response.data.page;
      }
      
      loading = false;
    } catch (err) {
      error = '데이터를 불러오는데 실패했습니다.';
      loading = false;
      console.error(err);
    }
  }
  
  async function loadSummary() {
    if (!tenantStore.current) {
      return;
    }
    
    apiClient.setTenantId(tenantStore.current);
    
    try {
      const response = await apiClient.get<any>('/settlements/summary');
      
      if (response.success && response.data) {
        pendingCount = response.data.pendingCount || 0;
        approvedCount = response.data.approvedCount || 0;
        paidCount = response.data.paidCount || 0;
      }
    } catch (err) {
      console.error('Failed to load summary:', err);
    }
  }
  
  $effect(() => {
    loadSettlements();
    loadSummary();
  });
</script>

<div class="settlements">
  <div class="header">
    <h1>정산 관리</h1>
    <p class="subtitle">전체 {totalCount}건의 정산</p>
  </div>
  
  <div class="summary">
    <div class="summary-item pending">
      <span class="label">정산 대기</span>
      <span class="value">{pendingCount} 건</span>
    </div>
    <div class="summary-item approved">
      <span class="label">정산 확정</span>
      <span class="value">{approvedCount} 건</span>
    </div>
    <div class="summary-item paid">
      <span class="label">지급 완료</span>
      <span class="value">{paidCount} 건</span>
    </div>
  </div>
  
  <div class="filters">
    <div class="filter-group">
      <label for="status">상태:</label>
      <select id="status" bind:value={statusFilter} onchange={() => loadSettlements()}>
        <option value="ALL">전체</option>
        <option value="PENDING">대기</option>
        <option value="APPROVED">확정</option>
        <option value="PAID">완료</option>
      </select>
    </div>
    
    <button class="btn-primary" onclick={() => loadSettlements()}>
      🔄 새로고침
    </button>
  </div>
  
  {#if loading}
    <div class="loading">데이터를 불러오는 중...</div>
  {:else if error}
    <div class="error">{error}</div>
  {:else}
    <div class="table-wrapper">
      <table class="data-table">
        <thead>
          <tr>
        <th onclick={() => sortBy('entity_type')} class="sortable">
          엔티티 타입
          {#if sortField === 'entity_type'}
            <span class="sort-icon">{sortDirection === 'asc' ? '↑' : '↓'}</span>
          {/if}
        </th>
        <th onclick={() => sortBy('entry_type')} class="sortable">
          입출금
          {#if sortField === 'entry_type'}
            <span class="sort-icon">{sortDirection === 'asc' ? '↑' : '↓'}</span>
          {/if}
        </th>
        <th onclick={() => sortBy('amount')} class="sortable">
          금액
          {#if sortField === 'amount'}
            <span class="sort-icon">{sortDirection === 'asc' ? '↑' : '↓'}</span>
          {/if}
        </th>
        <th onclick={() => sortBy('fee_amount')} class="sortable">
          수수료
          {#if sortField === 'fee_amount'}
            <span class="sort-icon">{sortDirection === 'asc' ? '↑' : '↓'}</span>
          {/if}
        </th>
        <th onclick={() => sortBy('net_amount')} class="sortable">
          정산 금액
          {#if sortField === 'net_amount'}
            <span class="sort-icon">{sortDirection === 'asc' ? '↑' : '↓'}</span>
          {/if}
        </th>
        <th onclick={() => sortBy('status')} class="sortable">
          상태
          {#if sortField === 'status'}
            <span class="sort-icon">{sortDirection === 'asc' ? '↑' : '↓'}</span>
          {/if}
        </th>
        <th onclick={() => sortBy('created_at')} class="sortable">
          생성 일시
          {#if sortField === 'created_at'}
                <span class="sort-icon">{sortDirection === 'asc' ? '↑' : '↓'}</span>
              {/if}
            </th>
          </tr>
        </thead>
        <tbody>
          {#each displaySettlements as settlement}
            <tr>
              <td>{settlement.entityType}</td>
              <td>
                <span class="entry-type entry-{settlement.entryType.toLowerCase()}">
                  {getEntryTypeLabel(settlement.entryType)}
                </span>
              </td>
              <td class="amount">{formatCurrency(settlement.amount)}</td>
              <td class="amount">{formatCurrency(settlement.feeAmount)}</td>
              <td class="amount net">{formatCurrency(settlement.netAmount)}</td>
              <td>
                <span class="badge badge-{getStatusBadge(settlement.status)}">
                  {getStatusLabel(settlement.status)}
                </span>
              </td>
              <td>{format(new Date(settlement.createdAt), 'yyyy-MM-dd HH:mm:ss')}</td>
            </tr>
          {/each}
          
          {#if displaySettlements.length === 0}
            <tr>
              <td colspan="7" class="empty">조회된 정산이 없습니다.</td>
            </tr>
          {/if}
        </tbody>
      </table>
    </div>
    
    <div class="pagination">
      <button 
        class="btn-page" 
        disabled={currentPage === 0}
        onclick={() => { currentPage = 0; loadSettlements(); }}
      >
        처음
      </button>
      <button 
        class="btn-page" 
        disabled={currentPage === 0}
        onclick={() => { currentPage--; loadSettlements(); }}
      >
        이전
      </button>
      
      <span class="page-info">
        {currentPage + 1} / {totalPages} 페이지
      </span>
      
      <button 
        class="btn-page" 
        disabled={currentPage >= totalPages - 1}
        onclick={() => { currentPage++; loadSettlements(); }}
      >
        다음
      </button>
      <button 
        class="btn-page" 
        disabled={currentPage >= totalPages - 1}
        onclick={() => { currentPage = totalPages - 1; loadSettlements(); }}
      >
        마지막
      </button>
    </div>
  {/if}
</div>

<style>
  .settlements {
    max-width: 1400px;
    margin: 0 auto;
  }
  
  .header {
    margin-bottom: 2rem;
  }
  
  h1 {
    font-size: 2rem;
    font-weight: 700;
    margin-bottom: 0.5rem;
  }
  
  .subtitle {
    color: #666;
    font-size: 0.95rem;
  }
  
  .loading, .error {
    text-align: center;
    padding: 3rem;
    font-size: 1.1rem;
  }
  
  .error {
    color: #dc2626;
  }
  
  .summary {
    display: grid;
    grid-template-columns: repeat(3, 1fr);
    gap: 1.5rem;
    margin-bottom: 2rem;
  }
  
  .summary-item {
    background: white;
    padding: 1.5rem;
    border-radius: 0.75rem;
    display: flex;
    flex-direction: column;
    gap: 0.75rem;
    box-shadow: 0 2px 8px rgba(0,0,0,0.08);
  }
  
  .summary-item.pending {
    border-left: 4px solid #f59e0b;
  }
  
  .summary-item.approved {
    border-left: 4px solid #3b82f6;
  }
  
  .summary-item.paid {
    border-left: 4px solid #10b981;
  }
  
  .label {
    font-size: 0.875rem;
    color: #6b7280;
    font-weight: 500;
  }
  
  .value {
    font-size: 1.75rem;
    font-weight: 700;
    color: #1f2937;
  }
  
  .filters {
    display: flex;
    gap: 1rem;
    margin-bottom: 1.5rem;
    flex-wrap: wrap;
  }
  
  .filter-group {
    display: flex;
    align-items: center;
    gap: 0.5rem;
  }
  
  .filter-group label {
    font-weight: 500;
    font-size: 0.9rem;
  }
  
  .filter-group select {
    padding: 0.5rem 1rem;
    border: 1px solid #d1d5db;
    border-radius: 0.5rem;
    font-size: 0.9rem;
  }
  
  .btn-primary {
    padding: 0.5rem 1.5rem;
    background: #667eea;
    color: white;
    border: none;
    border-radius: 0.5rem;
    font-weight: 500;
    cursor: pointer;
    transition: background 0.2s;
  }
  
  .btn-primary:hover {
    background: #5568d3;
  }
  
  .table-wrapper {
    background: white;
    border-radius: 1rem;
    overflow: hidden;
    box-shadow: 0 2px 8px rgba(0,0,0,0.08);
  }
  
  .data-table {
    width: 100%;
    border-collapse: collapse;
  }
  
  .data-table thead {
    background: #f9fafb;
  }
  
  .data-table th {
    padding: 1rem;
    text-align: left;
    font-weight: 600;
    font-size: 0.875rem;
    color: #374151;
    border-bottom: 2px solid #e5e7eb;
  }
  
  .data-table th.sortable {
    cursor: pointer;
    user-select: none;
    transition: background 0.2s;
  }
  
  .data-table th.sortable:hover {
    background: #f3f4f6;
  }
  
  .sort-icon {
    margin-left: 0.25rem;
    color: #667eea;
  }
  
  .data-table td {
    padding: 1rem;
    border-bottom: 1px solid #e5e7eb;
    font-size: 0.9rem;
  }
  
  .data-table tbody tr:hover {
    background: #f9fafb;
  }
  
  .data-table td.amount {
    font-weight: 600;
    text-align: right;
  }
  
  .data-table td.amount.net {
    color: #667eea;
    font-size: 1rem;
  }
  
  .data-table td.empty {
    text-align: center;
    padding: 3rem;
    color: #9ca3af;
  }
  
  .badge {
    display: inline-block;
    padding: 0.25rem 0.75rem;
    border-radius: 9999px;
    font-size: 0.75rem;
    font-weight: 600;
  }
  
  .badge-success {
    background: #d1fae5;
    color: #065f46;
  }
  
  .badge-danger {
    background: #fee2e2;
    color: #991b1b;
  }
  
  .badge-warning {
    background: #fef3c7;
    color: #92400e;
  }
  
  .badge-info {
    background: #dbeafe;
    color: #1e40af;
  }
  
  .entry-type {
    display: inline-block;
    padding: 0.125rem 0.5rem;
    border-radius: 0.25rem;
    font-size: 0.8rem;
    font-weight: 600;
  }
  
  .entry-credit {
    background: #d1fae5;
    color: #065f46;
  }
  
  .entry-debit {
    background: #fee2e2;
    color: #991b1b;
  }
  
  .pagination {
    display: flex;
    justify-content: center;
    align-items: center;
    gap: 0.5rem;
    margin-top: 2rem;
    padding: 1rem;
  }
  
  .btn-page {
    padding: 0.5rem 1rem;
    background: white;
    border: 1px solid #d1d5db;
    border-radius: 0.5rem;
    font-size: 0.9rem;
    cursor: pointer;
    transition: all 0.2s;
  }
  
  .btn-page:hover:not(:disabled) {
    background: #667eea;
    color: white;
    border-color: #667eea;
  }
  
  .btn-page:disabled {
    opacity: 0.5;
    cursor: not-allowed;
  }
  
  .page-info {
    padding: 0 1rem;
    font-weight: 500;
  }
</style>
