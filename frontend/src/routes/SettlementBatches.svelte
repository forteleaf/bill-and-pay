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
  import { DatePicker } from '$lib/components/ui/date-picker';
  import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '$lib/components/ui/table';
  
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
      error = 'Failed to load data.';
      loading = false;
      console.error(err);
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
        
        <div class="space-y-2">
          <Label for="startDate">Start Date</Label>
          <DatePicker 
            value={startDate}
            onchange={(d) => { startDate = d; currentPage = 0; loadBatches(); }}
            placeholder="시작일"
          />
        </div>
        
        <div class="space-y-2">
          <Label for="endDate">End Date</Label>
          <DatePicker 
            value={endDate}
            onchange={(d) => { endDate = d; currentPage = 0; loadBatches(); }}
            placeholder="종료일"
          />
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
    <div class="text-center py-12 text-lg text-muted-foreground">Loading data...</div>
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
