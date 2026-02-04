<script lang="ts">
  import { onMount } from 'svelte';
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
  let dateType = $state<string>('CREATED');
  let startDate = $state<string>('');
  let endDate = $state<string>('');
  let tidSearch = $state<string>('');
  let approvalNumberSearch = $state<string>('');
  let pgConnectionId = $state<string>('');
  
  let currentPage = $state(0);
  let pageSize = $state(20);
  let totalCount = $state(0);
  let totalPages = $state(0);
  
  let sortField = $state<string>('createdAt');
  let sortDirection = $state<'asc' | 'desc'>('desc');
  
  let showAdvancedFilters = $state(false);
  
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
      
      if (startDate && endDate) {
        const startDateTime = `${startDate}T00:00:00+09:00`;
        const endDateTime = `${endDate}T23:59:59+09:00`;
        
        if (dateType === 'CREATED') {
          params.append('startDate', startDateTime);
          params.append('endDate', endDateTime);
        } else if (dateType === 'APPROVED') {
          params.append('approvedAtStart', startDateTime);
          params.append('approvedAtEnd', endDateTime);
        } else if (dateType === 'CANCELLED') {
          params.append('cancelledAtStart', startDateTime);
          params.append('cancelledAtEnd', endDateTime);
        } else if (dateType === 'ALL') {
          params.append('startDate', startDateTime);
          params.append('endDate', endDateTime);
        }
      }
      
      if (tidSearch.trim()) {
        params.append('transactionId', tidSearch.trim());
      }
      
      if (approvalNumberSearch.trim()) {
        params.append('approvalNumber', approvalNumberSearch.trim());
      }
      
      if (pgConnectionId.trim()) {
        params.append('pgConnectionId', pgConnectionId.trim());
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
  
  function resetFilters() {
    statusFilter = 'ALL';
    dateType = 'CREATED';
    startDate = '';
    endDate = '';
    tidSearch = '';
    approvalNumberSearch = '';
    pgConnectionId = '';
    currentPage = 0;
    loadTransactions();
  }
  
  function handleSearch() {
    currentPage = 0;
    loadTransactions();
  }
  
  onMount(() => {
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
    <CardContent class="pt-6 space-y-4">
      <div class="flex flex-wrap gap-4 items-end">
        <div class="space-y-2">
          <Label for="status">상태</Label>
          <select 
            id="status" 
            bind:value={statusFilter}
            class="flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
          >
            <option value="ALL">전체</option>
            <option value="APPROVED">승인</option>
            <option value="CANCELLED">취소</option>
            <option value="PARTIAL_CANCELLED">부분취소</option>
          </select>
        </div>
        
        <div class="space-y-2">
          <Label for="dateType">날짜 유형</Label>
          <select 
            id="dateType" 
            bind:value={dateType}
            class="flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
          >
            <option value="CREATED">거래일자</option>
            <option value="APPROVED">승인일자</option>
            <option value="CANCELLED">취소일자</option>
          </select>
        </div>
        
        <div class="space-y-2">
          <Label for="startDate">시작일</Label>
          <Input 
            id="startDate"
            type="date" 
            bind:value={startDate}
          />
        </div>
        
        <div class="space-y-2">
          <Label for="endDate">종료일</Label>
          <Input 
            id="endDate"
            type="date" 
            bind:value={endDate}
          />
        </div>
        
        <Button onclick={handleSearch}>
          <svg class="w-4 h-4 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
          </svg>
          조회
        </Button>
        
        <Button variant="outline" onclick={resetFilters}>
          초기화
        </Button>
      </div>
      
      <div class="flex items-center gap-2">
        <Button 
          variant="ghost" 
          size="sm"
          onclick={() => showAdvancedFilters = !showAdvancedFilters}
        >
          {showAdvancedFilters ? '▲ 상세검색 접기' : '▼ 상세검색 펼치기'}
        </Button>
      </div>
      
      {#if showAdvancedFilters}
        <div class="flex flex-wrap gap-4 items-end border-t pt-4">
          <div class="space-y-2">
            <Label for="tidSearch">거래 TID</Label>
            <Input 
              id="tidSearch"
              type="text" 
              placeholder="TID 검색"
              bind:value={tidSearch}
            />
          </div>
          
          <div class="space-y-2">
            <Label for="approvalNumber">승인번호</Label>
            <Input 
              id="approvalNumber"
              type="text" 
              placeholder="승인번호 검색"
              bind:value={approvalNumberSearch}
            />
          </div>
          
          <div class="space-y-2">
            <Label for="pgConnectionId">PG ID</Label>
            <Input 
              id="pgConnectionId"
              type="text" 
              placeholder="PG 연결 ID"
              bind:value={pgConnectionId}
            />
          </div>
        </div>
      {/if}
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
