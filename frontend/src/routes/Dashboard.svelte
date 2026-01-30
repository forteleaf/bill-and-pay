<script lang="ts">
  import { apiClient } from '../lib/api';
  import { tenantStore } from '../lib/stores';
  import { format, startOfMonth, endOfMonth, eachDayOfInterval, isSameDay } from 'date-fns';
  import { ko } from 'date-fns/locale';
  
  interface DashboardMetrics {
    todaySales: number;
    monthSales: number;
    pendingSettlements: number;
    transactionCount: number;
  }
  
  interface MerchantRanking {
    merchantName: string;
    amount: number;
    transactionCount: number;
  }
  
  interface MerchantRankingResponse {
    merchantId: string;
    merchantName: string;
    totalAmount: number;
    transactionCount: number;
  }
  
  let metrics = $state<DashboardMetrics>({
    todaySales: 0,
    monthSales: 0,
    pendingSettlements: 0,
    transactionCount: 0
  });
  
  let topMerchants = $state<MerchantRanking[]>([]);
  let loading = $state(true);
  let error = $state<string | null>(null);
  let currentMonth = $state(new Date());
  
  const monthDays = $derived(
    eachDayOfInterval({
      start: startOfMonth(currentMonth),
      end: endOfMonth(currentMonth)
    })
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
  
  async function loadDashboardData() {
    if (!tenantStore.current) {
      return;
    }
    
    apiClient.setTenantId(tenantStore.current);
    loading = true;
    error = null;
    
    try {
      const [metricsResponse, merchantsResponse] = await Promise.all([
        apiClient.get<DashboardMetrics>('/dashboard/metrics'),
        apiClient.get<MerchantRankingResponse[]>('/dashboard/top-merchants')
      ]);
      
      if (metricsResponse.success && metricsResponse.data) {
        metrics = metricsResponse.data;
      }
      
      if (merchantsResponse.success && merchantsResponse.data) {
        topMerchants = merchantsResponse.data.map(m => ({
          merchantName: m.merchantName,
          amount: m.totalAmount,
          transactionCount: m.transactionCount
        }));
      }
      
      loading = false;
    } catch (err) {
      error = '데이터를 불러오는데 실패했습니다.';
      loading = false;
      console.error(err);
    }
  }
  
  $effect(() => {
    loadDashboardData();
  });
</script>

<div class="dashboard">
  <div class="header">
    <h1>대시보드</h1>
    <p class="subtitle">{format(new Date(), 'yyyy년 M월 d일 (E)', { locale: ko })}</p>
  </div>
  
  {#if loading}
    <div class="loading">데이터를 불러오는 중...</div>
  {:else if error}
    <div class="error">{error}</div>
  {:else}
    <!-- Metrics Cards -->
    <div class="metrics-grid">
      <div class="metric-card primary">
        <div class="metric-icon">💰</div>
        <div class="metric-content">
          <h3>오늘 매출</h3>
          <p class="metric-value">{formatCurrency(metrics.todaySales)}</p>
          <span class="metric-change positive">+12.5%</span>
        </div>
      </div>
      
      <div class="metric-card">
        <div class="metric-icon">📊</div>
        <div class="metric-content">
          <h3>이번 달 매출</h3>
          <p class="metric-value">{formatCurrency(metrics.monthSales)}</p>
          <span class="metric-change positive">+8.3%</span>
        </div>
      </div>
      
      <div class="metric-card">
        <div class="metric-icon">⏳</div>
        <div class="metric-content">
          <h3>정산 대기</h3>
          <p class="metric-value">{formatNumber(metrics.pendingSettlements)} 건</p>
          <span class="metric-change">전일 대비</span>
        </div>
      </div>
      
      <div class="metric-card">
        <div class="metric-icon">🔢</div>
        <div class="metric-content">
          <h3>총 거래 건수</h3>
          <p class="metric-value">{formatNumber(metrics.transactionCount)} 건</p>
          <span class="metric-change positive">+5.2%</span>
        </div>
      </div>
    </div>
    
    <!-- Rankings and Calendar -->
    <div class="content-grid">
      <!-- Top Merchants -->
      <div class="section">
        <div class="section-header">
          <h2>🏆 상위 매출 가맹점</h2>
        </div>
        <div class="ranking-list">
          {#each topMerchants as merchant, index}
            <div class="ranking-item">
              <div class="rank-badge rank-{index + 1}">{index + 1}</div>
              <div class="ranking-info">
                <span class="merchant-name">{merchant.merchantName}</span>
                <span class="transaction-count">{formatNumber(merchant.transactionCount)}건</span>
              </div>
              <div class="ranking-amount">{formatCurrency(merchant.amount)}</div>
            </div>
          {/each}
        </div>
      </div>
      
      <!-- Calendar -->
      <div class="section">
        <div class="section-header">
          <h2>📅 월간 캘린더</h2>
          <span class="month-label">{format(currentMonth, 'yyyy년 M월', { locale: ko })}</span>
        </div>
        <div class="calendar">
          <div class="calendar-header">
            <div class="day-label">일</div>
            <div class="day-label">월</div>
            <div class="day-label">화</div>
            <div class="day-label">수</div>
            <div class="day-label">목</div>
            <div class="day-label">금</div>
            <div class="day-label">토</div>
          </div>
          <div class="calendar-body">
            {#each monthDays as day}
              <div 
                class="calendar-day"
                class:today={isSameDay(day, new Date())}
                class:has-transactions={Math.random() > 0.3}
              >
                <span class="day-number">{format(day, 'd')}</span>
                {#if Math.random() > 0.3}
                  <span class="day-sales">₩{(Math.random() * 5000000).toFixed(0)}K</span>
                {/if}
              </div>
            {/each}
          </div>
        </div>
      </div>
    </div>
  {/if}
</div>

<style>
  .dashboard {
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
  
  /* Metrics Grid */
  .metrics-grid {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
    gap: 1.5rem;
    margin-bottom: 2rem;
  }
  
  .metric-card {
    background: white;
    border-radius: 1rem;
    padding: 1.5rem;
    box-shadow: 0 2px 8px rgba(0,0,0,0.08);
    display: flex;
    gap: 1rem;
    transition: transform 0.2s, box-shadow 0.2s;
  }
  
  .metric-card:hover {
    transform: translateY(-2px);
    box-shadow: 0 4px 12px rgba(0,0,0,0.12);
  }
  
  .metric-card.primary {
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    color: white;
  }
  
  .metric-icon {
    font-size: 2.5rem;
  }
  
  .metric-content {
    flex: 1;
  }
  
  .metric-content h3 {
    font-size: 0.875rem;
    font-weight: 500;
    margin-bottom: 0.5rem;
    opacity: 0.9;
  }
  
  .metric-value {
    font-size: 1.75rem;
    font-weight: 700;
    margin-bottom: 0.25rem;
  }
  
  .metric-change {
    font-size: 0.8rem;
    opacity: 0.8;
  }
  
  .metric-change.positive {
    color: #10b981;
  }
  
  .metric-card.primary .metric-change.positive {
    color: #d1fae5;
  }
  
  /* Content Grid */
  .content-grid {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 1.5rem;
  }
  
  @media (max-width: 1024px) {
    .content-grid {
      grid-template-columns: 1fr;
    }
  }
  
  .section {
    background: white;
    border-radius: 1rem;
    padding: 1.5rem;
    box-shadow: 0 2px 8px rgba(0,0,0,0.08);
  }
  
  .section-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 1.5rem;
  }
  
  .section-header h2 {
    font-size: 1.25rem;
    font-weight: 600;
  }
  
  .month-label {
    font-size: 0.9rem;
    color: #666;
  }
  
  /* Rankings */
  .ranking-list {
    display: flex;
    flex-direction: column;
    gap: 1rem;
  }
  
  .ranking-item {
    display: flex;
    align-items: center;
    gap: 1rem;
    padding: 1rem;
    background: #f9fafb;
    border-radius: 0.5rem;
    transition: background 0.2s;
  }
  
  .ranking-item:hover {
    background: #f3f4f6;
  }
  
  .rank-badge {
    width: 2rem;
    height: 2rem;
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    font-weight: 700;
    font-size: 0.9rem;
    flex-shrink: 0;
  }
  
  .rank-badge.rank-1 {
    background: #ffd700;
    color: #9a3412;
  }
  
  .rank-badge.rank-2 {
    background: #c0c0c0;
    color: #374151;
  }
  
  .rank-badge.rank-3 {
    background: #cd7f32;
    color: white;
  }
  
  .rank-badge.rank-4,
  .rank-badge.rank-5 {
    background: #e5e7eb;
    color: #6b7280;
  }
  
  .ranking-info {
    flex: 1;
    display: flex;
    flex-direction: column;
    gap: 0.25rem;
  }
  
  .merchant-name {
    font-weight: 600;
    font-size: 0.95rem;
  }
  
  .transaction-count {
    font-size: 0.8rem;
    color: #6b7280;
  }
  
  .ranking-amount {
    font-weight: 700;
    font-size: 1rem;
    color: #667eea;
  }
  
  /* Calendar */
  .calendar {
    font-size: 0.875rem;
  }
  
  .calendar-header {
    display: grid;
    grid-template-columns: repeat(7, 1fr);
    gap: 0.25rem;
    margin-bottom: 0.5rem;
  }
  
  .day-label {
    text-align: center;
    font-weight: 600;
    color: #6b7280;
    padding: 0.5rem 0;
  }
  
  .calendar-body {
    display: grid;
    grid-template-columns: repeat(7, 1fr);
    gap: 0.25rem;
  }
  
  .calendar-day {
    aspect-ratio: 1;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    padding: 0.5rem;
    border-radius: 0.375rem;
    cursor: pointer;
    transition: all 0.2s;
    position: relative;
  }
  
  .calendar-day:hover {
    background: #f3f4f6;
  }
  
  .calendar-day.today {
    background: #667eea;
    color: white;
    font-weight: 700;
  }
  
  .calendar-day.has-transactions {
    background: #e0e7ff;
  }
  
  .calendar-day.today.has-transactions {
    background: #667eea;
  }
  
  .day-number {
    font-weight: 500;
  }
  
  .day-sales {
    font-size: 0.65rem;
    color: #667eea;
    margin-top: 0.25rem;
  }
  
  .calendar-day.today .day-sales {
    color: white;
  }
</style>
