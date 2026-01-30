<script lang="ts">
  import { apiClient } from '../lib/api';
  import { tenantStore } from '../lib/stores';
  import type { SettlementSummary } from '../types/api';
  
  let summary = $state<SettlementSummary | null>(null);
  let loading = $state(true);
  let error = $state<string | null>(null);
  
  let startDate = $state<string>('');
  let endDate = $state<string>('');
  let entityType = $state<string>('');
  
  const isZeroSumValid = $derived(
    summary ? Math.abs(summary.creditAmount + summary.debitAmount - summary.totalAmount) < 1 : false
  );
  
  function formatCurrency(amount: number): string {
    return new Intl.NumberFormat('ko-KR', {
      style: 'currency',
      currency: 'KRW'
    }).format(amount);
  }
  
  function formatNumber(num: number): string {
    return new Intl.NumberFormat('ko-KR').format(num);
  }
  
  async function loadSummary() {
    if (!tenantStore.current) {
      return;
    }
    
    apiClient.setTenantId(tenantStore.current);
    loading = true;
    error = null;
    
    try {
      const params = new URLSearchParams();
      
      if (entityType) {
        params.append('entityType', entityType);
      }
      if (startDate) {
        params.append('startDate', startDate);
      }
      if (endDate) {
        params.append('endDate', endDate);
      }
      
      const queryString = params.toString();
      const endpoint = queryString ? `/settlements/summary?${queryString}` : '/settlements/summary';
      
      const response = await apiClient.get<SettlementSummary>(endpoint);
      
      if (response.success && response.data) {
        summary = response.data;
      }
      
      loading = false;
    } catch (err) {
      error = '데이터를 불러오는데 실패했습니다.';
      loading = false;
      console.error(err);
    }
  }
  
  $effect(() => {
    loadSummary();
  });
</script>

<div class="settlement-summary">
  <div class="header">
    <h1>정산 요약</h1>
    <p class="subtitle">전체 정산 현황 대시보드</p>
  </div>
  
  <div class="filters">
    <div class="filter-group">
      <label for="entityType">엔티티 타입:</label>
      <select id="entityType" bind:value={entityType} onchange={() => loadSummary()}>
        <option value="">전체</option>
        <option value="DISTRIBUTOR">총판</option>
        <option value="AGENCY">대리점</option>
        <option value="DEALER">딜러</option>
        <option value="SELLER">판매점</option>
        <option value="VENDOR">벤더</option>
      </select>
    </div>
    
    <div class="filter-group">
      <label for="startDate">시작일:</label>
      <input 
        type="date" 
        id="startDate" 
        bind:value={startDate}
        onchange={() => loadSummary()}
      />
    </div>
    
    <div class="filter-group">
      <label for="endDate">종료일:</label>
      <input 
        type="date" 
        id="endDate" 
        bind:value={endDate}
        onchange={() => loadSummary()}
      />
    </div>
    
    <button class="btn-primary" onclick={() => loadSummary()}>
      🔄 새로고침
    </button>
  </div>
  
  {#if loading}
    <div class="loading">데이터를 불러오는 중...</div>
  {:else if error}
    <div class="error">{error}</div>
  {:else if summary}
    <div class="zero-sum-status">
      <div class="status-card {isZeroSumValid ? 'valid' : 'invalid'}">
        <span class="icon">{isZeroSumValid ? '✅' : '⚠️'}</span>
        <span class="label">Zero-Sum 검증:</span>
        <span class="value">{isZeroSumValid ? '정상' : '불일치'}</span>
      </div>
      {#if !isZeroSumValid}
        <p class="warning-text">
          ⚠️ Credit + Debit ≠ Total Amount. 데이터 불일치가 감지되었습니다.
        </p>
      {/if}
    </div>
    
    <div class="entity-info">
      <div class="info-item">
        <span class="label">엔티티 타입</span>
        <span class="value">{summary.entityType}</span>
      </div>
      <div class="info-item">
        <span class="label">엔티티 경로</span>
        <span class="value code">{summary.entityPath}</span>
      </div>
      <div class="info-item">
        <span class="label">거래 건수</span>
        <span class="value">{formatNumber(summary.transactionCount)}건</span>
      </div>
    </div>
    
    <div class="summary-grid">
      <div class="summary-card total">
        <div class="card-header">
          <span class="icon">💰</span>
          <span class="title">총 정산금액</span>
        </div>
        <div class="amount-large">{formatCurrency(summary.totalAmount)}</div>
        <div class="detail">
          수수료: {formatCurrency(summary.totalFeeAmount)}
        </div>
        <div class="detail">
          순액: {formatCurrency(summary.totalNetAmount)}
        </div>
      </div>
      
      <div class="summary-card credit">
        <div class="card-header">
          <span class="icon">➕</span>
          <span class="title">입금 (CREDIT)</span>
        </div>
        <div class="amount">{formatCurrency(summary.creditAmount)}</div>
        <div class="badge positive">수취</div>
      </div>
      
      <div class="summary-card debit">
        <div class="card-header">
          <span class="icon">➖</span>
          <span class="title">출금 (DEBIT)</span>
        </div>
        <div class="amount">{formatCurrency(summary.debitAmount)}</div>
        <div class="badge negative">지급</div>
      </div>
      
      <div class="summary-card net">
        <div class="card-header">
          <span class="icon">🧾</span>
          <span class="title">순 정산금액</span>
        </div>
        <div class="amount">{formatCurrency(summary.totalNetAmount)}</div>
        <div class="detail">
          수수료 차감 후
        </div>
      </div>
    </div>
    
    <div class="balance-visualization">
      <h3>정산 Balance 시각화</h3>
      <div class="balance-bar">
        <div class="bar-section credit" style="width: {Math.abs(summary.creditAmount) / (Math.abs(summary.creditAmount) + Math.abs(summary.debitAmount)) * 100}%">
          <span class="bar-label">Credit: {formatCurrency(summary.creditAmount)}</span>
        </div>
        <div class="bar-section debit" style="width: {Math.abs(summary.debitAmount) / (Math.abs(summary.creditAmount) + Math.abs(summary.debitAmount)) * 100}%">
          <span class="bar-label">Debit: {formatCurrency(summary.debitAmount)}</span>
        </div>
      </div>
      <div class="balance-formula">
        <span class="formula-item credit">{formatCurrency(summary.creditAmount)}</span>
        <span class="operator">+</span>
        <span class="formula-item debit">({formatCurrency(summary.debitAmount)})</span>
        <span class="operator">=</span>
        <span class="formula-item total">{formatCurrency(summary.totalAmount)}</span>
        {#if isZeroSumValid}
          <span class="check">✅</span>
        {:else}
          <span class="cross">❌</span>
        {/if}
      </div>
    </div>
  {/if}
</div>

<style>
  .settlement-summary {
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
  
  .zero-sum-status {
    background: white;
    border-radius: 1rem;
    padding: 1.5rem;
    margin-bottom: 2rem;
    box-shadow: 0 2px 8px rgba(0,0,0,0.08);
  }
  
  .status-card {
    display: flex;
    align-items: center;
    gap: 1rem;
    font-size: 1.1rem;
    font-weight: 600;
    padding: 1rem;
    border-radius: 0.5rem;
  }
  
  .status-card.valid {
    background: #d1fae5;
    color: #065f46;
  }
  
  .status-card.invalid {
    background: #fee2e2;
    color: #991b1b;
  }
  
  .status-card .icon {
    font-size: 1.5rem;
  }
  
  .warning-text {
    margin-top: 1rem;
    padding: 1rem;
    background: #fef3c7;
    color: #92400e;
    border-radius: 0.5rem;
    font-size: 0.95rem;
  }
  
  .entity-info {
    display: grid;
    grid-template-columns: repeat(3, 1fr);
    gap: 1rem;
    margin-bottom: 2rem;
  }
  
  .info-item {
    background: white;
    padding: 1.5rem;
    border-radius: 0.75rem;
    box-shadow: 0 2px 8px rgba(0,0,0,0.08);
    display: flex;
    flex-direction: column;
    gap: 0.5rem;
  }
  
  .info-item .label {
    font-size: 0.875rem;
    color: #6b7280;
    font-weight: 500;
  }
  
  .info-item .value {
    font-size: 1.25rem;
    font-weight: 700;
    color: #1f2937;
  }
  
  .info-item .value.code {
    font-family: monospace;
    font-size: 1rem;
    color: #667eea;
  }
  
  .summary-grid {
    display: grid;
    grid-template-columns: repeat(4, 1fr);
    gap: 1.5rem;
    margin-bottom: 2rem;
  }
  
  .summary-card {
    background: white;
    padding: 1.5rem;
    border-radius: 1rem;
    box-shadow: 0 2px 8px rgba(0,0,0,0.08);
    display: flex;
    flex-direction: column;
    gap: 1rem;
  }
  
  .summary-card.total {
    grid-column: span 4;
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    color: white;
  }
  
  .summary-card.credit {
    border-left: 4px solid #10b981;
  }
  
  .summary-card.debit {
    border-left: 4px solid #dc2626;
  }
  
  .summary-card.net {
    border-left: 4px solid #3b82f6;
  }
  
  .card-header {
    display: flex;
    align-items: center;
    gap: 0.5rem;
    font-size: 0.95rem;
    font-weight: 600;
    opacity: 0.9;
  }
  
  .card-header .icon {
    font-size: 1.5rem;
  }
  
  .amount-large {
    font-size: 2.5rem;
    font-weight: 800;
    line-height: 1;
  }
  
  .amount {
    font-size: 1.75rem;
    font-weight: 700;
    color: #1f2937;
  }
  
  .summary-card.total .amount-large,
  .summary-card.total .detail {
    color: white;
  }
  
  .detail {
    font-size: 0.875rem;
    color: #6b7280;
  }
  
  .badge {
    display: inline-block;
    padding: 0.25rem 0.75rem;
    border-radius: 9999px;
    font-size: 0.75rem;
    font-weight: 600;
    width: fit-content;
  }
  
  .badge.positive {
    background: #d1fae5;
    color: #065f46;
  }
  
  .badge.negative {
    background: #fee2e2;
    color: #991b1b;
  }
  
  .balance-visualization {
    background: white;
    border-radius: 1rem;
    padding: 2rem;
    box-shadow: 0 2px 8px rgba(0,0,0,0.08);
  }
  
  .balance-visualization h3 {
    font-size: 1.25rem;
    font-weight: 700;
    margin-bottom: 1.5rem;
    color: #1f2937;
  }
  
  .balance-bar {
    display: flex;
    height: 60px;
    border-radius: 0.5rem;
    overflow: hidden;
    margin-bottom: 1.5rem;
    box-shadow: 0 2px 4px rgba(0,0,0,0.1);
  }
  
  .bar-section {
    display: flex;
    align-items: center;
    justify-content: center;
    padding: 0 1rem;
    transition: all 0.3s;
  }
  
  .bar-section.credit {
    background: linear-gradient(135deg, #10b981 0%, #059669 100%);
  }
  
  .bar-section.debit {
    background: linear-gradient(135deg, #dc2626 0%, #b91c1c 100%);
  }
  
  .bar-label {
    color: white;
    font-weight: 600;
    font-size: 0.9rem;
    text-shadow: 0 1px 2px rgba(0,0,0,0.2);
  }
  
  .balance-formula {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 1rem;
    padding: 1.5rem;
    background: #f9fafb;
    border-radius: 0.5rem;
    font-size: 1.1rem;
    font-weight: 600;
  }
  
  .formula-item {
    padding: 0.5rem 1rem;
    border-radius: 0.5rem;
    background: white;
    box-shadow: 0 1px 3px rgba(0,0,0,0.1);
  }
  
  .formula-item.credit {
    color: #059669;
  }
  
  .formula-item.debit {
    color: #b91c1c;
  }
  
  .formula-item.total {
    color: #667eea;
    font-weight: 700;
  }
  
  .operator {
    color: #6b7280;
    font-weight: 700;
  }
  
  .check {
    font-size: 1.5rem;
  }
  
  .cross {
    font-size: 1.5rem;
  }
  
  @media (max-width: 1024px) {
    .summary-grid {
      grid-template-columns: repeat(2, 1fr);
    }
    
    .summary-card.total {
      grid-column: span 2;
    }
    
    .entity-info {
      grid-template-columns: repeat(2, 1fr);
    }
  }
  
  @media (max-width: 768px) {
    .summary-grid {
      grid-template-columns: 1fr;
    }
    
    .summary-card.total {
      grid-column: span 1;
    }
    
    .entity-info {
      grid-template-columns: 1fr;
    }
  }
</style>
