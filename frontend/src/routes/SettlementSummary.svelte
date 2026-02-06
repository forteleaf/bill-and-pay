<script lang="ts">
  import { onMount } from 'svelte';
  import { apiClient } from '../lib/api';
  import { tenantStore } from '../lib/stores';
  import type { SettlementSummary } from '../types/api';
  import { Card, CardContent, CardHeader, CardTitle } from '$lib/components/ui/card';
  import { Button } from '$lib/components/ui/button';
  import { Badge } from '$lib/components/ui/badge';
  import { Label } from '$lib/components/ui/label';
  import { DateRangePicker } from '$lib/components/ui/date-range-picker';
  import { Skeleton } from '$lib/components/ui/skeleton';
  import { format } from 'date-fns';
  
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

  function setDateRange(days: number) {
    const end = new Date();
    const start = new Date();
    start.setDate(end.getDate() - days);
    startDate = format(start, 'yyyy/MM/dd');
    endDate = format(end, 'yyyy/MM/dd');
    loadSummary();
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
      error = err instanceof Error ? err.message : '데이터를 불러올 수 없습니다.';
      loading = false;
      console.error('API Error:', err);
    }
  }
  
  onMount(() => {
    loadSummary();
  });
</script>

<div class="max-w-7xl mx-auto space-y-6">
  <div>
    <h1 class="text-3xl font-bold text-foreground">Settlement Summary</h1>
    <p class="text-muted-foreground mt-1">Overall settlement status dashboard</p>
  </div>
  
  <Card>
    <CardContent class="pt-6">
      <div class="flex flex-wrap gap-4 items-end">
        <div class="space-y-2">
          <Label for="entityType">Entity Type</Label>
          <select 
            id="entityType" 
            bind:value={entityType}
            onchange={() => loadSummary()}
            class="flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
          >
            <option value="">All</option>
            <option value="DISTRIBUTOR">Distributor</option>
            <option value="AGENCY">Agency</option>
            <option value="DEALER">Dealer</option>
            <option value="SELLER">Seller</option>
            <option value="VENDOR">Vendor</option>
          </select>
        </div>

        <div class="flex flex-row items-end gap-3">
          <div class="flex flex-col gap-1.5">
            <Label>기간</Label>
            <DateRangePicker
              startDate={startDate}
              endDate={endDate}
              onchange={(start, end) => { startDate = start; endDate = end; }}
              placeholder="기간 선택"
              class="w-[280px]"
            />
          </div>
          <div class="flex gap-1">
            <Button variant="outline" size="sm" onclick={() => setDateRange(7)}>7일</Button>
            <Button variant="outline" size="sm" onclick={() => setDateRange(30)}>30일</Button>
            <Button variant="outline" size="sm" onclick={() => setDateRange(90)}>90일</Button>
          </div>
        </div>

        <Button onclick={() => loadSummary()}>
          <svg class="w-4 h-4 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
          </svg>
          Refresh
        </Button>
      </div>
    </CardContent>
  </Card>


  {#if loading}
    <div class="grid grid-cols-1 md:grid-cols-3 gap-4">
      {#each Array(3) as _}
        <Card>
          <CardHeader class="pb-2">
            <Skeleton class="h-4 w-24" />
          </CardHeader>
          <CardContent>
            <Skeleton class="h-6 w-32" />
          </CardContent>
        </Card>
      {/each}
    </div>

    <Card>
      <CardHeader>
        <Skeleton class="h-6 w-48" />
      </CardHeader>
      <CardContent class="space-y-4">
        <Skeleton class="h-12 w-full" />
        <div class="grid grid-cols-2 gap-4">
          <Skeleton class="h-10 w-full" />
          <Skeleton class="h-10 w-full" />
        </div>
      </CardContent>
    </Card>

    <div class="grid grid-cols-1 md:grid-cols-3 gap-4">
      {#each Array(3) as _}
        <Card>
          <CardHeader>
            <Skeleton class="h-5 w-32" />
          </CardHeader>
          <CardContent>
            <Skeleton class="h-8 w-40 mb-2" />
            <Skeleton class="h-5 w-16" />
          </CardContent>
        </Card>
      {/each}
    </div>
  {:else if error}
    <div class="text-center py-12 text-lg text-destructive">{error}</div>
  {:else if summary}
    <Card class={isZeroSumValid ? 'border-green-500' : 'border-destructive'}>
      <CardContent class="pt-6">
        <div class="flex items-center gap-4 p-4 rounded-lg {isZeroSumValid ? 'bg-green-50 text-green-800' : 'bg-destructive/10 text-destructive'}">
          <span class="text-2xl">{isZeroSumValid ? 'V' : '!'}</span>
          <span class="font-semibold">Zero-Sum Validation:</span>
          <span>{isZeroSumValid ? 'Valid' : 'Invalid'}</span>
        </div>
        {#if !isZeroSumValid}
          <p class="mt-4 p-4 bg-yellow-50 text-yellow-800 rounded-lg text-sm">
            Warning: Credit + Debit does not equal Total Amount. Data inconsistency detected.
          </p>
        {/if}
      </CardContent>
    </Card>
    
    <div class="grid grid-cols-1 md:grid-cols-3 gap-4">
      <Card>
        <CardHeader class="pb-2">
          <CardTitle class="text-sm font-medium text-muted-foreground">Entity Type</CardTitle>
        </CardHeader>
        <CardContent>
          <div class="text-xl font-bold">{summary.entityType}</div>
        </CardContent>
      </Card>
      
      <Card>
        <CardHeader class="pb-2">
          <CardTitle class="text-sm font-medium text-muted-foreground">Entity Path</CardTitle>
        </CardHeader>
        <CardContent>
          <div class="font-mono text-sm text-primary">{summary.entityPath}</div>
        </CardContent>
      </Card>
      
      <Card>
        <CardHeader class="pb-2">
          <CardTitle class="text-sm font-medium text-muted-foreground">Transaction Count</CardTitle>
        </CardHeader>
        <CardContent>
          <div class="text-xl font-bold">{formatNumber(summary.transactionCount)}</div>
        </CardContent>
      </Card>
    </div>
    
    <Card class="bg-gradient-to-br from-primary to-purple-600 text-primary-foreground">
      <CardHeader>
        <CardTitle class="flex items-center gap-2 text-primary-foreground/90">
          <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8c-1.657 0-3 .895-3 2s1.343 2 3 2 3 .895 3 2-1.343 2-3 2m0-8c1.11 0 2.08.402 2.599 1M12 8V7m0 1v8m0 0v1m0-1c-1.11 0-2.08-.402-2.599-1M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
          </svg>
          Total Settlement Amount
        </CardTitle>
      </CardHeader>
      <CardContent class="space-y-4">
        <div class="text-4xl font-bold">{formatCurrency(summary.totalAmount)}</div>
        <div class="grid grid-cols-2 gap-4 text-sm">
          <div>
            <div class="opacity-80">Fee Amount</div>
            <div class="font-semibold">{formatCurrency(summary.totalFeeAmount)}</div>
          </div>
          <div>
            <div class="opacity-80">Net Amount</div>
            <div class="font-semibold">{formatCurrency(summary.totalNetAmount)}</div>
          </div>
        </div>
      </CardContent>
    </Card>
    
    <div class="grid grid-cols-1 md:grid-cols-3 gap-4">
      <Card class="border-l-4 border-l-green-500">
        <CardHeader>
          <CardTitle class="flex items-center gap-2 text-base">
            <span class="text-green-500">+</span>
            Credit (Incoming)
          </CardTitle>
        </CardHeader>
        <CardContent>
          <div class="text-2xl font-bold">{formatCurrency(summary.creditAmount)}</div>
          <Badge class="mt-2 bg-green-100 text-green-800 hover:bg-green-100">Receive</Badge>
        </CardContent>
      </Card>
      
      <Card class="border-l-4 border-l-red-500">
        <CardHeader>
          <CardTitle class="flex items-center gap-2 text-base">
            <span class="text-red-500">-</span>
            Debit (Outgoing)
          </CardTitle>
        </CardHeader>
        <CardContent>
          <div class="text-2xl font-bold">{formatCurrency(summary.debitAmount)}</div>
          <Badge variant="destructive" class="mt-2">Pay</Badge>
        </CardContent>
      </Card>
      
      <Card class="border-l-4 border-l-blue-500">
        <CardHeader>
          <CardTitle class="flex items-center gap-2 text-base">
            <svg class="w-5 h-5 text-blue-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
            </svg>
            Net Settlement
          </CardTitle>
        </CardHeader>
        <CardContent>
          <div class="text-2xl font-bold">{formatCurrency(summary.totalNetAmount)}</div>
          <span class="text-sm text-muted-foreground">After fee deduction</span>
        </CardContent>
      </Card>
    </div>
    
    <Card>
      <CardHeader>
        <CardTitle>Balance Visualization</CardTitle>
      </CardHeader>
      <CardContent class="space-y-6">
        {#if summary}
          {@const creditRatio = Math.abs(summary.creditAmount) / (Math.abs(summary.creditAmount) + Math.abs(summary.debitAmount)) * 100}
          {@const debitRatio = 100 - creditRatio}
          <div class="flex h-16 rounded-lg overflow-hidden shadow-inner">
            <div 
              class="bg-gradient-to-r from-green-500 to-green-600 flex items-center justify-center text-white font-semibold text-sm"
              style="width: {creditRatio}%"
            >
              Credit: {formatCurrency(summary.creditAmount)}
            </div>
            <div 
              class="bg-gradient-to-r from-red-500 to-red-600 flex items-center justify-center text-white font-semibold text-sm"
              style="width: {debitRatio}%"
            >
              Debit: {formatCurrency(summary.debitAmount)}
            </div>
          </div>
        {/if}
        
        <div class="flex items-center justify-center gap-4 p-4 bg-muted/50 rounded-lg text-lg font-semibold">
          <span class="px-4 py-2 bg-background rounded shadow-sm text-green-600">{formatCurrency(summary.creditAmount)}</span>
          <span class="text-muted-foreground">+</span>
          <span class="px-4 py-2 bg-background rounded shadow-sm text-red-600">({formatCurrency(summary.debitAmount)})</span>
          <span class="text-muted-foreground">=</span>
          <span class="px-4 py-2 bg-background rounded shadow-sm text-primary font-bold">{formatCurrency(summary.totalAmount)}</span>
          <span class="text-2xl">{isZeroSumValid ? 'V' : 'X'}</span>
        </div>
      </CardContent>
    </Card>
  {/if}
</div>
