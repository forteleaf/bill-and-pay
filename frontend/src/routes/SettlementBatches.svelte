<script lang="ts">
  import { apiClient } from '../lib/api';
  import { tenantStore } from '../lib/stores';
  import { format } from 'date-fns';
  import type { SettlementBatch, PagedResponse } from '../types/api';
  
  let batches = $state<SettlementBatch[]>([]);
  let loading = $state(true);
  let error = $state<string | null>(null);
  
  let statusFilter = $state<string>('ALL');
  let startDate = $state<string>('');
  let endDate = $state<string>('');
  let currentPage = $state(0);
  let pageSize = $state(20);
  let totalCount = $state(0);
  let totalPages = $state(0);
  
  function formatCurrency(amount: number): string {
    return new Intl.NumberFormat('ko-KR', {
      style: 'currency',
      currency: 'KRW'
    }).format(amount);
  }
  
  function getStatusBadge(status: string): string {
    const badges: Record<string, string> = {
      'PENDING': 'warning',
      'PROCESSING': 'info',
      'COMPLETED': 'success',
      'FAILED': 'danger'
    };
    return badges[status] || 'default';
  }
  
  function getStatusLabel(status: string): string {
    const labels: Record<string, string> = {
      'PENDING': '대기',
      'PROCESSING': '처리중',
      'COMPLETED': '완료',
      'FAILED': '실패'
    };
    return labels[status] || status;
  }
  
  async function loadBatches() {
    if (!tenantStore.current) {
      return;
    }
    
    apiClient.setTenantId(tenantStore.current);
    loading = true;
    error = null;
    
    try {
      const params = new URLSearchParams({
        page: currentPage.toString(),
        size: pageSize.toString()
      });
      
      if (statusFilter !== 'ALL') {
        params.append('status', statusFilter);
      }
      if (startDate) {
        params.append('startDate', startDate);
      }
      if (endDate) {
        params.append('endDate', endDate);
      }
      
      const response = await apiClient.get<PagedResponse<SettlementBatch>>(`/settlements/batches?${params}`);
      
      if (response.success && response.data) {
        batches = response.data.content;
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
  
  $effect(() => {
    loadBatches();
  });
</script>

<div class="settlement-batches">
  <div class="header">
    <h1>정산 배치 관리</h1>
    <p class="subtitle">전체 {totalCount}건의 배치</p>
  </div>
  
  <div class="filters">
    <div class="filter-group">
      <label for="status">상태:</label>
      <select id="status" bind:value={statusFilter} onchange={() => { currentPage = 0; loadBatches(); }}>
        <option value="ALL">전체</option>
        <option value="PENDING">대기</option>
        <option value="PROCESSING">처리중</option>
        <option value="COMPLETED">완료</option>
        <option value="FAILED">실패</option>
      </select>
    </div>
    
    <div class="filter-group">
      <label for="startDate">시작일:</label>
      <input 
        type="date" 
        id="startDate" 
        bind:value={startDate}
        onchange={() => { currentPage = 0; loadBatches(); }}
      />
    </div>
    
    <div class="filter-group">
      <label for="endDate">종료일:</label>
      <input 
        type="date" 
        id="endDate" 
        bind:value={endDate}
        onchange={() => { currentPage = 0; loadBatches(); }}
      />
    </div>
    
    <button class="btn-primary" onclick={() => loadBatches()}>
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
            <th>배치번호</th>
            <th>정산일</th>
            <th>상태</th>
            <th>거래건수</th>
            <th>총 거래금액</th>
            <th>총 수수료</th>
            <th>처리 일시</th>
            <th>승인 일시</th>
          </tr>
        </thead>
        <tbody>
          {#each batches as batch}
            <tr>
              <td class="batch-number">{batch.batchNumber}</td>
              <td>{format(new Date(batch.settlementDate), 'yyyy-MM-dd')}</td>
              <td>
                <span class="badge badge-{getStatusBadge(batch.status)}">
                  {getStatusLabel(batch.status)}
                </span>
              </td>
              <td class="text-center">{batch.totalTransactions.toLocaleString()}건</td>
              <td class="amount">{formatCurrency(batch.totalAmount)}</td>
              <td class="amount">{formatCurrency(batch.totalFeeAmount)}</td>
              <td>{format(new Date(batch.processedAt), 'yyyy-MM-dd HH:mm:ss')}</td>
              <td>
                {#if batch.approvedAt}
                  {format(new Date(batch.approvedAt), 'yyyy-MM-dd HH:mm:ss')}
                {:else}
                  <span class="text-muted">-</span>
                {/if}
              </td>
            </tr>
          {/each}
          
          {#if batches.length === 0}
            <tr>
              <td colspan="8" class="empty">조회된 배치가 없습니다.</td>
            </tr>
          {/if}
        </tbody>
      </table>
    </div>
    
    <div class="pagination">
      <button 
        class="btn-page" 
        disabled={currentPage === 0}
        onclick={() => { currentPage = 0; loadBatches(); }}
      >
        처음
      </button>
      <button 
        class="btn-page" 
        disabled={currentPage === 0}
        onclick={() => { currentPage--; loadBatches(); }}
      >
        이전
      </button>
      
      <span class="page-info">
        {currentPage + 1} / {totalPages || 1} 페이지
      </span>
      
      <button 
        class="btn-page" 
        disabled={currentPage >= totalPages - 1 || totalPages === 0}
        onclick={() => { currentPage++; loadBatches(); }}
      >
        다음
      </button>
      <button 
        class="btn-page" 
        disabled={currentPage >= totalPages - 1 || totalPages === 0}
        onclick={() => { currentPage = totalPages - 1; loadBatches(); }}
      >
        마지막
      </button>
    </div>
  {/if}
</div>

<style>
  .settlement-batches {
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
  
  .filters {
    display: flex;
    gap: 1rem;
    margin-bottom: 1.5rem;
    flex-wrap: wrap;
    align-items: flex-end;
  }
  
  .filter-group {
    display: flex;
    flex-direction: column;
    gap: 0.5rem;
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
    white-space: nowrap;
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
  
  .data-table td.batch-number {
    font-family: monospace;
    font-weight: 600;
    color: #667eea;
  }
  
  .data-table td.text-center {
    text-align: center;
  }
  
  .data-table td.empty {
    text-align: center;
    padding: 3rem;
    color: #9ca3af;
  }
  
  .text-muted {
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
