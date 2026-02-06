<script lang="ts">
  import { onMount } from 'svelte';
  import { apiClient } from '../lib/api';
  import { tenantStore } from '../lib/stores';
  import { format } from 'date-fns';
  import type { Settlement, PagedResponse } from '../types/api';
  import { Card, CardContent, CardHeader, CardTitle } from '$lib/components/ui/card';
  import { Button } from '$lib/components/ui/button';
  import { Badge } from '$lib/components/ui/badge';
  import { Label } from '$lib/components/ui/label';
  import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '$lib/components/ui/table';
  import { Skeleton } from '$lib/components/ui/skeleton';
  
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
  
  function getStatusVariant(status: string): 'default' | 'secondary' | 'destructive' | 'outline' {
    const variants: Record<string, 'default' | 'secondary' | 'destructive' | 'outline'> = {
      'PENDING': 'secondary',
      'APPROVED': 'outline',
      'PAID': 'default',
      'FAILED': 'destructive'
    };
    return variants[status] || 'outline';
  }
  
  function getStatusLabel(status: string): string {
    const labels: Record<string, string> = {
      'PENDING': 'Pending',
      'APPROVED': 'Approved',
      'PAID': 'Paid',
      'FAILED': 'Failed'
    };
    return labels[status] || status;
  }
  
  function getEntryTypeLabel(entryType: string): string {
    return entryType === 'CREDIT' ? 'Credit' : 'Debit';
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


      if (!response.success) {
        error = response.error?.message || '데이터를 불러올 수 없습니다.';
      }

      loading = false;
    } catch (err) {
      error = err instanceof Error ? err.message : '데이터를 불러올 수 없습니다.';
      loading = false;
      console.error('API Error:', err);
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
  
  onMount(() => {
    loadSettlements();
    loadSummary();
  });

  function getSortIcon(field: string): string {
    if (sortField !== field) return '';
    return sortDirection === 'asc' ? ' ^' : ' v';
  }
</script>

<div class="max-w-7xl mx-auto space-y-6">
  <div>
    <h1 class="text-3xl font-bold text-foreground">Settlements</h1>
    <p class="text-muted-foreground mt-1">Total {totalCount} settlements</p>
  </div>
  
  <div class="grid grid-cols-1 md:grid-cols-3 gap-4">
    <Card class="border-l-4 border-l-yellow-500">
      <CardHeader class="pb-2">
        <CardTitle class="text-sm font-medium text-muted-foreground">Pending</CardTitle>
      </CardHeader>
      <CardContent>
        <div class="text-2xl font-bold">{pendingCount}</div>
      </CardContent>
    </Card>
    
    <Card class="border-l-4 border-l-blue-500">
      <CardHeader class="pb-2">
        <CardTitle class="text-sm font-medium text-muted-foreground">Approved</CardTitle>
      </CardHeader>
      <CardContent>
        <div class="text-2xl font-bold">{approvedCount}</div>
      </CardContent>
    </Card>
    
    <Card class="border-l-4 border-l-green-500">
      <CardHeader class="pb-2">
        <CardTitle class="text-sm font-medium text-muted-foreground">Paid</CardTitle>
      </CardHeader>
      <CardContent>
        <div class="text-2xl font-bold">{paidCount}</div>
      </CardContent>
    </Card>
  </div>
  
  <Card>
    <CardContent class="pt-6">
      <div class="flex flex-wrap gap-4 items-end">
        <div class="space-y-2">
          <Label for="status">Status</Label>
          <select 
            id="status" 
            bind:value={statusFilter}
            onchange={() => loadSettlements()}
            class="flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
          >
            <option value="ALL">All</option>
            <option value="PENDING">Pending</option>
            <option value="APPROVED">Approved</option>
            <option value="PAID">Paid</option>
          </select>
        </div>
        
        <Button onclick={() => loadSettlements()}>
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
            <Skeleton class="h-8 w-16" />
          </CardContent>
        </Card>
      {/each}
    </div>

    <Card>
      <Table>
        <TableHeader>
          <TableRow>
            <TableHead>Entity Type</TableHead>
            <TableHead>Entry Type</TableHead>
            <TableHead class="text-right">Amount</TableHead>
            <TableHead class="text-right">Fee</TableHead>
            <TableHead class="text-right">Net Amount</TableHead>
            <TableHead>Status</TableHead>
            <TableHead>Created At</TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          {#each Array(5) as _}
            <TableRow>
              <TableCell><Skeleton class="h-4 w-24" /></TableCell>
              <TableCell><Skeleton class="h-5 w-16" /></TableCell>
              <TableCell class="text-right"><Skeleton class="h-4 w-24 ml-auto" /></TableCell>
              <TableCell class="text-right"><Skeleton class="h-4 w-20 ml-auto" /></TableCell>
              <TableCell class="text-right"><Skeleton class="h-4 w-28 ml-auto" /></TableCell>
              <TableCell><Skeleton class="h-5 w-16" /></TableCell>
              <TableCell><Skeleton class="h-4 w-36" /></TableCell>
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
            <TableHead class="cursor-pointer hover:bg-muted/50" onclick={() => sortBy('entity_type')}>
              Entity Type{getSortIcon('entity_type')}
            </TableHead>
            <TableHead class="cursor-pointer hover:bg-muted/50" onclick={() => sortBy('entry_type')}>
              Entry Type{getSortIcon('entry_type')}
            </TableHead>
            <TableHead class="cursor-pointer hover:bg-muted/50 text-right" onclick={() => sortBy('amount')}>
              Amount{getSortIcon('amount')}
            </TableHead>
            <TableHead class="cursor-pointer hover:bg-muted/50 text-right" onclick={() => sortBy('fee_amount')}>
              Fee{getSortIcon('fee_amount')}
            </TableHead>
            <TableHead class="cursor-pointer hover:bg-muted/50 text-right" onclick={() => sortBy('net_amount')}>
              Net Amount{getSortIcon('net_amount')}
            </TableHead>
            <TableHead class="cursor-pointer hover:bg-muted/50" onclick={() => sortBy('status')}>
              Status{getSortIcon('status')}
            </TableHead>
            <TableHead class="cursor-pointer hover:bg-muted/50" onclick={() => sortBy('created_at')}>
              Created At{getSortIcon('created_at')}
            </TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          {#each displaySettlements as settlement}
            <TableRow>
              <TableCell>{settlement.entityType}</TableCell>
              <TableCell>
                <Badge variant={settlement.entryType === 'CREDIT' ? 'default' : 'destructive'}>
                  {getEntryTypeLabel(settlement.entryType)}
                </Badge>
              </TableCell>
              <TableCell class="text-right font-semibold">{formatCurrency(settlement.amount)}</TableCell>
              <TableCell class="text-right">{formatCurrency(settlement.feeAmount)}</TableCell>
              <TableCell class="text-right font-bold text-primary">{formatCurrency(settlement.netAmount)}</TableCell>
              <TableCell>
                <Badge variant={getStatusVariant(settlement.status)}>
                  {getStatusLabel(settlement.status)}
                </Badge>
              </TableCell>
              <TableCell>{format(new Date(settlement.createdAt), 'yyyy-MM-dd HH:mm:ss')}</TableCell>
            </TableRow>
          {/each}
          
          {#if displaySettlements.length === 0}
            <TableRow>
              <TableCell colspan={7} class="text-center py-12 text-muted-foreground">
                No settlements found.
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
        onclick={() => { currentPage = 0; loadSettlements(); }}
      >
        First
      </Button>
      <Button 
        variant="outline"
        size="sm"
        disabled={currentPage === 0}
        onclick={() => { currentPage--; loadSettlements(); }}
      >
        Previous
      </Button>
      
      <span class="px-4 font-medium text-sm">
        {currentPage + 1} / {totalPages} pages
      </span>
      
      <Button 
        variant="outline"
        size="sm"
        disabled={currentPage >= totalPages - 1}
        onclick={() => { currentPage++; loadSettlements(); }}
      >
        Next
      </Button>
      <Button 
        variant="outline"
        size="sm"
        disabled={currentPage >= totalPages - 1}
        onclick={() => { currentPage = totalPages - 1; loadSettlements(); }}
      >
        Last
      </Button>
    </div>
  {/if}
</div>
