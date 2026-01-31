<script lang="ts">
  import { apiClient } from '../lib/api';
  import { tenantStore } from '../lib/stores';
  import { format } from 'date-fns';
  import type { Transaction, PagedResponse } from '../types/api';
  import { Card, CardContent } from '$lib/components/ui/card';
  import { Button } from '$lib/components/ui/button';
  import { Badge } from '$lib/components/ui/badge';
  import { Input } from '$lib/components/ui/input';
  import { Label } from '$lib/components/ui/label';
  import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '$lib/components/ui/table';
  
  let transactions = $state<Transaction[]>([]);
  let loading = $state(true);
  let error = $state<string | null>(null);
  
  let statusFilter = $state<string>('ALL');
  let searchQuery = $state('');
  let currentPage = $state(0);
  let pageSize = $state(20);
  let totalCount = $state(0);
  let totalPages = $state(0);
  
  let sortField = $state<string>('createdAt');
  let sortDirection = $state<'asc' | 'desc'>('desc');
  
  const displayTransactions = $derived(transactions);
  
  function formatCurrency(amount: number): string {
    return new Intl.NumberFormat('ko-KR', {
      style: 'currency',
      currency: 'KRW'
    }).format(amount);
  }
  
  function getStatusVariant(status: string): 'default' | 'secondary' | 'destructive' | 'outline' {
    const variants: Record<string, 'default' | 'secondary' | 'destructive' | 'outline'> = {
      'APPROVED': 'default',
      'CANCELED': 'destructive',
      'PARTIAL_CANCELED': 'secondary',
      'PENDING': 'outline'
    };
    return variants[status] || 'outline';
  }
  
  function getStatusLabel(status: string): string {
    const labels: Record<string, string> = {
      'APPROVED': 'Approved',
      'CANCELED': 'Canceled',
      'PARTIAL_CANCELED': 'Partial Cancel',
      'PENDING': 'Pending'
    };
    return labels[status] || status;
  }
  
  function sortBy(field: string) {
    if (sortField === field) {
      sortDirection = sortDirection === 'asc' ? 'desc' : 'asc';
    } else {
      sortField = field;
      sortDirection = 'asc';
    }
    loadTransactions();
  }
  
  async function loadTransactions() {
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
      
      const response = await apiClient.get<PagedResponse<Transaction>>(`/transactions?${params}`);
      
      if (response.success && response.data) {
        transactions = response.data.content;
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
  
  $effect(() => {
    loadTransactions();
  });

  function getSortIcon(field: string): string {
    if (sortField !== field) return '';
    return sortDirection === 'asc' ? ' ^' : ' v';
  }
</script>

<div class="max-w-7xl mx-auto space-y-6">
  <div>
    <h1 class="text-3xl font-bold text-foreground">Transactions</h1>
    <p class="text-muted-foreground mt-1">Total {totalCount} transactions</p>
  </div>
  
  <Card>
    <CardContent class="pt-6">
      <div class="flex flex-wrap gap-4 items-end">
        <div class="space-y-2">
          <Label for="status">Status</Label>
          <select 
            id="status" 
            bind:value={statusFilter}
            class="flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
          >
            <option value="ALL">All</option>
            <option value="APPROVED">Approved</option>
            <option value="CANCELED">Canceled</option>
            <option value="PARTIAL_CANCELED">Partial Cancel</option>
          </select>
        </div>
        
        <div class="flex-1 min-w-[250px] space-y-2">
          <Label for="search">Search</Label>
          <Input 
            id="search"
            type="text" 
            placeholder="Search by transaction ID or merchant..." 
            value={searchQuery}
            oninput={(e) => searchQuery = e.currentTarget.value}
          />
        </div>
        
        <Button onclick={() => loadTransactions()}>
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
            <TableHead class="cursor-pointer hover:bg-muted/50" onclick={() => sortBy('tid')}>
              Transaction ID{getSortIcon('tid')}
            </TableHead>
            <TableHead class="cursor-pointer hover:bg-muted/50" onclick={() => sortBy('merchantId')}>
              Merchant{getSortIcon('merchantId')}
            </TableHead>
            <TableHead class="cursor-pointer hover:bg-muted/50 text-right" onclick={() => sortBy('amount')}>
              Amount{getSortIcon('amount')}
            </TableHead>
            <TableHead class="cursor-pointer hover:bg-muted/50" onclick={() => sortBy('status')}>
              Status{getSortIcon('status')}
            </TableHead>
            <TableHead class="cursor-pointer hover:bg-muted/50" onclick={() => sortBy('approvedAt')}>
              Approved At{getSortIcon('approvedAt')}
            </TableHead>
            <TableHead class="cursor-pointer hover:bg-muted/50" onclick={() => sortBy('createdAt')}>
              Created At{getSortIcon('createdAt')}
            </TableHead>
            <TableHead>Actions</TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          {#each displayTransactions as transaction}
            <TableRow>
              <TableCell class="font-mono text-sm text-primary">{transaction.tid}</TableCell>
              <TableCell>{transaction.merchantId}</TableCell>
              <TableCell class="text-right font-semibold">{formatCurrency(transaction.amount)}</TableCell>
              <TableCell>
                <Badge variant={getStatusVariant(transaction.status)}>
                  {getStatusLabel(transaction.status)}
                </Badge>
              </TableCell>
              <TableCell>{transaction.approvedAt ? format(new Date(transaction.approvedAt), 'yyyy-MM-dd HH:mm:ss') : '-'}</TableCell>
              <TableCell>{format(new Date(transaction.createdAt), 'yyyy-MM-dd HH:mm:ss')}</TableCell>
              <TableCell>
                <Button variant="outline" size="sm">Details</Button>
              </TableCell>
            </TableRow>
          {/each}
          
          {#if displayTransactions.length === 0}
            <TableRow>
              <TableCell colspan={7} class="text-center py-12 text-muted-foreground">
                No transactions found.
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
        onclick={() => { currentPage = 0; loadTransactions(); }}
      >
        First
      </Button>
      <Button 
        variant="outline"
        size="sm"
        disabled={currentPage === 0}
        onclick={() => { currentPage--; loadTransactions(); }}
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
        onclick={() => { currentPage++; loadTransactions(); }}
      >
        Next
      </Button>
      <Button 
        variant="outline"
        size="sm"
        disabled={currentPage >= totalPages - 1}
        onclick={() => { currentPage = totalPages - 1; loadTransactions(); }}
      >
        Last
      </Button>
    </div>
  {/if}
</div>
