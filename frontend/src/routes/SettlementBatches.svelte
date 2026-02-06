<script lang="ts">
  import { onMount } from 'svelte';
  import { apiClient } from '../lib/api';
  import { tenantStore } from '../lib/stores';
  import { format } from 'date-fns';
  import type { SettlementBatch, PagedResponse } from '../types/api';
  import { Card, CardContent } from '$lib/components/ui/card';
  import { Button } from '$lib/components/ui/button';
  import { Badge } from '$lib/components/ui/badge';
  import { Label } from '$lib/components/ui/label';
  import { DateRangePicker } from '$lib/components/ui/date-range-picker';
  import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '$lib/components/ui/table';
  import { Skeleton } from '$lib/components/ui/skeleton';
  
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
  
  function getStatusVariant(status: string): 'default' | 'secondary' | 'destructive' | 'outline' {
    const variants: Record<string, 'default' | 'secondary' | 'destructive' | 'outline'> = {
      'PENDING': 'secondary',
      'PROCESSING': 'outline',
      'COMPLETED': 'default',
      'FAILED': 'destructive'
    };
    return variants[status] || 'outline';
  }
  
  function getStatusLabel(status: string): string {
    const labels: Record<string, string> = {
      'PENDING': 'Pending',
      'PROCESSING': 'Processing',
      'COMPLETED': 'Completed',
      'FAILED': 'Failed'
    };
    return labels[status] || status;
  }

  function setDateRange(days: number) {
    const end = new Date();
    const start = new Date();
    start.setDate(end.getDate() - days);
    startDate = format(start, 'yyyy/MM/dd');
    endDate = format(end, 'yyyy/MM/dd');
    currentPage = 0;
    loadBatches();
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
      error = err instanceof Error ? err.message : '데이터를 불러올 수 없습니다.';
      loading = false;
      console.error('API Error:', err);
    }
  }
  
  onMount(() => {
    loadBatches();
  });
</script>

<div class="max-w-7xl mx-auto space-y-6">
  <div>
    <h1 class="text-3xl font-bold text-foreground">Settlement Batches</h1>
    <p class="text-muted-foreground mt-1">Total {totalCount} batches</p>
  </div>
  
  <Card>
    <CardContent class="pt-6">
      <div class="flex flex-wrap gap-4 items-end">
        <div class="space-y-2">
          <Label for="status">Status</Label>
          <select 
            id="status" 
            bind:value={statusFilter}
            onchange={() => { currentPage = 0; loadBatches(); }}
            class="flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
          >
            <option value="ALL">All</option>
            <option value="PENDING">Pending</option>
            <option value="PROCESSING">Processing</option>
            <option value="COMPLETED">Completed</option>
            <option value="FAILED">Failed</option>
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

        <Button onclick={() => loadBatches()}>
          <svg class="w-4 h-4 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
          </svg>
          Refresh
        </Button>
      </div>
    </CardContent>
  </Card>

  {#if loading}
    <Card>
      <Table>
        <TableHeader>
          <TableRow>
            <TableHead>Batch Number</TableHead>
            <TableHead>Settlement Date</TableHead>
            <TableHead>Status</TableHead>
            <TableHead class="text-center">Transactions</TableHead>
            <TableHead class="text-right">Total Amount</TableHead>
            <TableHead class="text-right">Total Fee</TableHead>
            <TableHead>Processed At</TableHead>
            <TableHead>Approved At</TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          {#each Array(5) as _}
            <TableRow>
              <TableCell><Skeleton class="h-4 w-32" /></TableCell>
              <TableCell><Skeleton class="h-4 w-24" /></TableCell>
              <TableCell><Skeleton class="h-5 w-16" /></TableCell>
              <TableCell class="text-center"><Skeleton class="h-4 w-12 mx-auto" /></TableCell>
              <TableCell class="text-right"><Skeleton class="h-4 w-24 ml-auto" /></TableCell>
              <TableCell class="text-right"><Skeleton class="h-4 w-20 ml-auto" /></TableCell>
              <TableCell><Skeleton class="h-4 w-32" /></TableCell>
              <TableCell><Skeleton class="h-4 w-32" /></TableCell>
            </TableRow>
          {/each}
        </TableBody>
      </Table>
    </Card>
  {:else if error}
    <div class="text-center py-12 text-lg text-destructive">{error}</div>
  {:else}
    <Card>
      <Table>
        <TableHeader>
          <TableRow>
            <TableHead>Batch Number</TableHead>
            <TableHead>Settlement Date</TableHead>
            <TableHead>Status</TableHead>
            <TableHead class="text-center">Transactions</TableHead>
            <TableHead class="text-right">Total Amount</TableHead>
            <TableHead class="text-right">Total Fee</TableHead>
            <TableHead>Processed At</TableHead>
            <TableHead>Approved At</TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          {#each batches as batch}
            <TableRow>
              <TableCell class="font-mono font-semibold text-primary">{batch.batchNumber}</TableCell>
              <TableCell>{format(new Date(batch.settlementDate), 'yyyy-MM-dd')}</TableCell>
              <TableCell>
                <Badge variant={getStatusVariant(batch.status)}>
                  {getStatusLabel(batch.status)}
                </Badge>
              </TableCell>
              <TableCell class="text-center">{batch.totalTransactions.toLocaleString()}</TableCell>
              <TableCell class="text-right font-semibold">{formatCurrency(batch.totalAmount)}</TableCell>
              <TableCell class="text-right">{formatCurrency(batch.totalFeeAmount)}</TableCell>
              <TableCell>{format(new Date(batch.processedAt), 'yyyy-MM-dd HH:mm:ss')}</TableCell>
              <TableCell>
                {#if batch.approvedAt}
                  {format(new Date(batch.approvedAt), 'yyyy-MM-dd HH:mm:ss')}
                {:else}
                  <span class="text-muted-foreground">-</span>
                {/if}
              </TableCell>
            </TableRow>
          {/each}
          
          {#if batches.length === 0}
            <TableRow>
              <TableCell colspan={8} class="text-center py-12 text-muted-foreground">
                No batches found.
              </TableCell>
            </TableRow>
          {/if}
        </TableBody>
      </Table>
    </Card>
    
    <div class="flex justify-center items-center gap-2">
      <Button 
        variant="outline"
        size="sm"
        disabled={currentPage === 0}
        onclick={() => { currentPage = 0; loadBatches(); }}
      >
        First
      </Button>
      <Button 
        variant="outline"
        size="sm"
        disabled={currentPage === 0}
        onclick={() => { currentPage--; loadBatches(); }}
      >
        Previous
      </Button>
      
      <span class="px-4 font-medium text-sm">
        {currentPage + 1} / {totalPages || 1} pages
      </span>
      
      <Button 
        variant="outline"
        size="sm"
        disabled={currentPage >= totalPages - 1 || totalPages === 0}
        onclick={() => { currentPage++; loadBatches(); }}
      >
        Next
      </Button>
      <Button 
        variant="outline"
        size="sm"
        disabled={currentPage >= totalPages - 1 || totalPages === 0}
        onclick={() => { currentPage = totalPages - 1; loadBatches(); }}
      >
        Last
      </Button>
    </div>
  {/if}
</div>
