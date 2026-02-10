<script lang="ts">
  import { onMount } from 'svelte';
  import { apiClient } from '@/api/client';
  import { tenantStore } from '@/stores/tenant';
  import { format, startOfMonth, endOfMonth, eachDayOfInterval, isSameDay } from 'date-fns';
  import { ko } from 'date-fns/locale';
  import { Card, CardContent, CardHeader, CardTitle } from '$lib/components/ui/card';
  import { Badge } from '$lib/components/ui/badge';
  import { Skeleton } from '$lib/components/ui/skeleton';
  import { Alert, AlertTitle, AlertDescription } from '$lib/components/ui/alert';
  import { Button } from '$lib/components/ui/button';
  
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
      error = err instanceof Error ? err.message : '데이터를 불러올 수 없습니다.';
      loading = false;
      console.error('API Error:', err);
    }
  }
  
  onMount(() => {
    loadDashboardData();
  });

  function getRankBadgeClass(index: number): string {
    if (index === 0) return 'bg-yellow-400 text-yellow-900';
    if (index === 1) return 'bg-gray-300 text-gray-700';
    if (index === 2) return 'bg-amber-600 text-white';
    return 'bg-muted text-muted-foreground';
  }
</script>

<div class="max-w-7xl mx-auto space-y-6">
  <div class="mb-8">
    <h1 class="text-3xl font-bold text-foreground">Dashboard</h1>
    <p class="text-muted-foreground mt-1">{format(new Date(), 'yyyy-MM-dd (E)', { locale: ko })}</p>
  </div>

  {#if loading}
    <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
      {#each Array(4) as _}
        <Card>
          <CardHeader class="pb-2">
            <Skeleton class="h-4 w-24" />
          </CardHeader>
          <CardContent>
            <Skeleton class="h-8 w-32 mb-2" />
            <Skeleton class="h-5 w-16" />
          </CardContent>
        </Card>
      {/each}
    </div>

    <div class="grid grid-cols-1 lg:grid-cols-2 gap-6">
      {#each Array(2) as _}
        <Card>
          <CardHeader>
            <Skeleton class="h-6 w-40" />
          </CardHeader>
          <CardContent class="space-y-3">
            {#each Array(5) as _}
              <Skeleton class="h-16 w-full" />
            {/each}
          </CardContent>
        </Card>
      {/each}
    </div>
  {:else if error}
    <div class="flex justify-center py-12">
      <Alert variant="destructive" class="max-w-lg">
        <AlertTitle>오류 발생</AlertTitle>
        <AlertDescription class="flex flex-col gap-3">
          <span>{error}</span>
          <Button variant="outline" size="sm" onclick={loadDashboardData} class="self-start">
            다시 시도
          </Button>
        </AlertDescription>
      </Alert>
    </div>
  {:else}
    <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
      <Card class="bg-gradient-to-br from-primary to-purple-600 text-primary-foreground">
        <CardHeader class="pb-2">
          <CardTitle class="text-sm font-medium opacity-90">Today's Sales</CardTitle>
        </CardHeader>
        <CardContent>
          <div class="text-2xl font-bold">{formatCurrency(metrics.todaySales)}</div>
          <Badge variant="secondary" class="mt-2 bg-white/20 text-white hover:bg-white/30">+12.5%</Badge>
        </CardContent>
      </Card>
      
      <Card>
        <CardHeader class="pb-2">
          <CardTitle class="text-sm font-medium text-muted-foreground">Monthly Sales</CardTitle>
        </CardHeader>
        <CardContent>
          <div class="text-2xl font-bold text-foreground">{formatCurrency(metrics.monthSales)}</div>
          <Badge variant="secondary" class="mt-2">+8.3%</Badge>
        </CardContent>
      </Card>
      
      <Card>
        <CardHeader class="pb-2">
          <CardTitle class="text-sm font-medium text-muted-foreground">Pending Settlements</CardTitle>
        </CardHeader>
        <CardContent>
          <div class="text-2xl font-bold text-foreground">{formatNumber(metrics.pendingSettlements)}</div>
          <span class="text-sm text-muted-foreground">vs yesterday</span>
        </CardContent>
      </Card>
      
      <Card>
        <CardHeader class="pb-2">
          <CardTitle class="text-sm font-medium text-muted-foreground">Total Transactions</CardTitle>
        </CardHeader>
        <CardContent>
          <div class="text-2xl font-bold text-foreground">{formatNumber(metrics.transactionCount)}</div>
          <Badge variant="secondary" class="mt-2">+5.2%</Badge>
        </CardContent>
      </Card>
    </div>
    
    <div class="grid grid-cols-1 lg:grid-cols-2 gap-6">
      <Card>
        <CardHeader>
          <CardTitle class="flex items-center gap-2">
            <svg class="h-5 w-5 text-yellow-500" fill="currentColor" viewBox="0 0 20 20">
              <path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm1-11a1 1 0 10-2 0v2H7a1 1 0 100 2h2v2a1 1 0 102 0v-2h2a1 1 0 100-2h-2V7z" clip-rule="evenodd" />
            </svg>
            Top Merchants
          </CardTitle>
        </CardHeader>
        <CardContent class="space-y-3">
          {#each topMerchants as merchant, index}
            <div class="flex items-center gap-4 p-3 rounded-lg bg-muted/50 hover:bg-muted transition-colors">
              <div class="w-8 h-8 rounded-full flex items-center justify-center text-sm font-bold {getRankBadgeClass(index)}">
                {index + 1}
              </div>
              <div class="flex-1 min-w-0">
                <div class="font-semibold text-foreground truncate">{merchant.merchantName}</div>
                <div class="text-sm text-muted-foreground">{formatNumber(merchant.transactionCount)} transactions</div>
              </div>
              <div class="font-bold text-primary">{formatCurrency(merchant.amount)}</div>
            </div>
          {/each}
          {#if topMerchants.length === 0}
            <div class="text-center py-8 text-muted-foreground">No merchant data available</div>
          {/if}
        </CardContent>
      </Card>
      
      <Card>
        <CardHeader>
          <div class="flex items-center justify-between">
            <CardTitle class="flex items-center gap-2">
              <svg class="h-5 w-5 text-primary" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z" />
              </svg>
              Monthly Calendar
            </CardTitle>
            <span class="text-sm text-muted-foreground">{format(currentMonth, 'yyyy-MM', { locale: ko })}</span>
          </div>
        </CardHeader>
        <CardContent>
          <div class="grid grid-cols-7 gap-1 text-center text-sm">
            {#each ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'] as day}
              <div class="py-2 font-semibold text-muted-foreground">{day}</div>
            {/each}
            {#each monthDays as day}
              {@const isToday = isSameDay(day, new Date())}
              {@const hasTransactions = false}
              <div
                class="aspect-square flex flex-col items-center justify-center rounded-md cursor-pointer transition-colors
                  {isToday ? 'bg-primary text-primary-foreground font-bold' : hasTransactions ? 'bg-primary/10' : 'hover:bg-muted'}"
              >
                <span class="text-sm">{format(day, 'd')}</span>
              </div>
            {/each}
          </div>
        </CardContent>
      </Card>
    </div>
  {/if}
</div>
