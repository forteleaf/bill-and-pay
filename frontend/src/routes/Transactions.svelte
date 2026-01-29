<script lang="ts">
  import { apiClient } from '../lib/api';
  import { tenantStore } from '../lib/stores';
  import { format } from 'date-fns';
  import type { Transaction } from '../types/api';
  
  let transactions = $state<Transaction[]>([]);
  let loading = $state(true);
  let error = $state<string | null>(null);
  
  // Filters
  let statusFilter = $state<string>('ALL');
  let searchQuery = $state('');
  let currentPage = $state(0);
  let pageSize = $state(20);
  let totalCount = $state(0);
  
  // Sorting
  let sortField = $state<keyof Transaction>('transactedAt');
  let sortDirection = $state<'asc' | 'desc'>('desc');
  
  const filteredTransactions = $derived(() => {
    let result = transactions;
    
    if (statusFilter !== 'ALL') {
      result = result.filter(t => t.status === statusFilter);
    }
    
    if (searchQuery) {
      const query = searchQuery.toLowerCase();
      result = result.filter(t => 
        t.pgTid.toLowerCase().includes(query) ||
        t.merchantId.toLowerCase().includes(query)
      );
    }
    
    // Sorting
    result = [...result].sort((a, b) => {
      const aVal = a[sortField];
      const bVal = b[sortField];
      
      if (typeof aVal === 'string' && typeof bVal === 'string') {
        return sortDirection === 'asc' 
          ? aVal.localeCompare(bVal)
          : bVal.localeCompare(aVal);
      }
      
      if (typeof aVal === 'number' && typeof bVal === 'number') {
        return sortDirection === 'asc' ? aVal - bVal : bVal - aVal;
      }
      
      return 0;
    });
    
    return result;
  });
  
  const paginatedTransactions = $derived(() => {
    const start = currentPage * pageSize;
    return filteredTransactions().slice(start, start + pageSize);
  });
  
  const totalPages = $derived(Math.ceil(filteredTransactions().length / pageSize));
  
  function formatCurrency(amount: number): string {
    return new Intl.NumberFormat('ko-KR', {
      style: 'currency',
      currency: 'KRW'
    }).format(amount);
  }
  
  function getStatusBadge(status: string): string {
    const badges: Record<string, string> = {
      'APPROVED': 'success',
      'CANCELED': 'danger',
      'PARTIAL_CANCELED': 'warning',
      'PENDING': 'info'
    };
    return badges[status] || 'default';
  }
  
  function getStatusLabel(status: string): string {
    const labels: Record<string, string> = {
      'APPROVED': '승인',
      'CANCELED': '취소',
      'PARTIAL_CANCELED': '부분취소',
      'PENDING': '대기'
    };
    return labels[status] || status;
  }
  
  function sortBy(field: keyof Transaction) {
    if (sortField === field) {
      sortDirection = sortDirection === 'asc' ? 'desc' : 'asc';
    } else {
      sortField = field;
      sortDirection = 'asc';
    }
  }
  
  async function loadTransactions() {
    if (!tenantStore.current) {
      return;
    }
    
    loading = true;
    error = null;
    
    try {
      // Mock data for demonstration
      transactions = Array.from({ length: 50 }, (_, i) => ({
        id: `txn-${i + 1}`,
        pgTid: `TID${1000 + i}`,
        merchantId: `merchant-${(i % 5) + 1}`,
        originalAmount: Math.floor(Math.random() * 500000) + 10000,
        currentAmount: Math.floor(Math.random() * 500000) + 10000,
        status: ['APPROVED', 'CANCELED', 'PARTIAL_CANCELED'][Math.floor(Math.random() * 3)],
        transactedAt: new Date(Date.now() - Math.random() * 30 * 24 * 60 * 60 * 1000).toISOString()
      }));
      
      totalCount = transactions.length;
      loading = false;
    } catch (err) {
      error = '데이터를 불러오는데 실패했습니다.';
      loading = false;
    }
  }
  
  $effect(() => {
    loadTransactions();
  });
</script>

<div class="transactions">
  <div class="header">
    <h1>거래 내역</h1>
    <p class="subtitle">전체 {totalCount}건의 거래</p>
  </div>
  
  <!-- Filters -->
  <div class="filters">
    <div class="filter-group">
      <label for="status">상태:</label>
      <select id="status" bind:value={statusFilter}>
        <option value="ALL">전체</option>
        <option value="APPROVED">승인</option>
        <option value="CANCELED">취소</option>
        <option value="PARTIAL_CANCELED">부분취소</option>
      </select>
    </div>
    
    <div class="filter-group search">
      <input 
        type="text" 
        placeholder="거래 ID 또는 가맹점 검색..." 
        bind:value={searchQuery}
      />
    </div>
    
    <button class="btn-primary" onclick={() => loadTransactions()}>
      🔄 새로고침
    </button>
  </div>
  
  {#if loading}
    <div class="loading">데이터를 불러오는 중...</div>
  {:else if error}
    <div class="error">{error}</div>
  {:else}
    <!-- Table -->
    <div class="table-wrapper">
      <table class="data-table">
        <thead>
          <tr>
            <th onclick={() => sortBy('pgTid')} class="sortable">
              거래 ID
              {#if sortField === 'pgTid'}
                <span class="sort-icon">{sortDirection === 'asc' ? '↑' : '↓'}</span>
              {/if}
            </th>
            <th onclick={() => sortBy('merchantId')} class="sortable">
              가맹점
              {#if sortField === 'merchantId'}
                <span class="sort-icon">{sortDirection === 'asc' ? '↑' : '↓'}</span>
              {/if}
            </th>
            <th onclick={() => sortBy('originalAmount')} class="sortable">
              원 금액
              {#if sortField === 'originalAmount'}
                <span class="sort-icon">{sortDirection === 'asc' ? '↑' : '↓'}</span>
              {/if}
            </th>
            <th onclick={() => sortBy('currentAmount')} class="sortable">
              현재 금액
              {#if sortField === 'currentAmount'}
                <span class="sort-icon">{sortDirection === 'asc' ? '↑' : '↓'}</span>
              {/if}
            </th>
            <th onclick={() => sortBy('status')} class="sortable">
              상태
              {#if sortField === 'status'}
                <span class="sort-icon">{sortDirection === 'asc' ? '↑' : '↓'}</span>
              {/if}
            </th>
            <th onclick={() => sortBy('transactedAt')} class="sortable">
              거래 일시
              {#if sortField === 'transactedAt'}
                <span class="sort-icon">{sortDirection === 'asc' ? '↑' : '↓'}</span>
              {/if}
            </th>
            <th>작업</th>
          </tr>
        </thead>
        <tbody>
          {#each paginatedTransactions() as transaction}
            <tr>
              <td class="mono">{transaction.pgTid}</td>
              <td>{transaction.merchantId}</td>
              <td class="amount">{formatCurrency(transaction.originalAmount)}</td>
              <td class="amount">{formatCurrency(transaction.currentAmount)}</td>
              <td>
                <span class="badge badge-{getStatusBadge(transaction.status)}">
                  {getStatusLabel(transaction.status)}
                </span>
              </td>
              <td>{format(new Date(transaction.transactedAt), 'yyyy-MM-dd HH:mm:ss')}</td>
              <td>
                <button class="btn-small">상세</button>
              </td>
            </tr>
          {/each}
          
          {#if paginatedTransactions().length === 0}
            <tr>
              <td colspan="7" class="empty">조회된 거래가 없습니다.</td>
            </tr>
          {/if}
        </tbody>
      </table>
    </div>
    
    <!-- Pagination -->
    <div class="pagination">
      <button 
        class="btn-page" 
        disabled={currentPage === 0}
        onclick={() => currentPage = 0}
      >
        처음
      </button>
      <button 
        class="btn-page" 
        disabled={currentPage === 0}
        onclick={() => currentPage--}
      >
        이전
      </button>
      
      <span class="page-info">
        {currentPage + 1} / {totalPages} 페이지
      </span>
      
      <button 
        class="btn-page" 
        disabled={currentPage >= totalPages - 1}
        onclick={() => currentPage++}
      >
        다음
      </button>
      <button 
        class="btn-page" 
        disabled={currentPage >= totalPages - 1}
        onclick={() => currentPage = totalPages - 1}
      >
        마지막
      </button>
    </div>
  {/if}
</div>

<style>
  .transactions {
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
  
  /* Filters */
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
  
  .filter-group.search {
    flex: 1;
    min-width: 250px;
  }
  
  .filter-group label {
    font-weight: 500;
    font-size: 0.9rem;
  }
  
  .filter-group select,
  .filter-group input {
    padding: 0.5rem 1rem;
    border: 1px solid #d1d5db;
    border-radius: 0.5rem;
    font-size: 0.9rem;
  }
  
  .filter-group input {
    flex: 1;
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
  
  /* Table */
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
  
  .data-table td.mono {
    font-family: 'Monaco', 'Courier New', monospace;
    font-size: 0.85rem;
    color: #667eea;
  }
  
  .data-table td.amount {
    font-weight: 600;
    text-align: right;
  }
  
  .data-table td.empty {
    text-align: center;
    padding: 3rem;
    color: #9ca3af;
  }
  
  /* Status Badge */
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
  
  /* Buttons */
  .btn-small {
    padding: 0.375rem 0.75rem;
    background: white;
    border: 1px solid #d1d5db;
    border-radius: 0.375rem;
    font-size: 0.8rem;
    cursor: pointer;
    transition: all 0.2s;
  }
  
  .btn-small:hover {
    background: #f9fafb;
    border-color: #667eea;
    color: #667eea;
  }
  
  /* Pagination */
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
