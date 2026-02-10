<script lang="ts">
  import { onMount } from 'svelte';
  import { apiClient } from '@/api/client';
  import { format } from 'date-fns';
  import type { Transaction, PagedResponse } from '@/types/api';
  import { Badge } from '$lib/components/ui/badge';
  import { Button } from '$lib/components/ui/button';
  import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '$lib/components/ui/table';
  import { Skeleton } from '$lib/components/ui/skeleton';

  interface Props {
    merchantId: string;
  }

  let { merchantId }: Props = $props();

  let transactions = $state<Transaction[]>([]);
  let loading = $state(true);
  let error = $state<string | null>(null);

  let currentPage = $state(0);
  let pageSize = $state(10);
  let totalCount = $state(0);
  let totalPages = $state(0);

  const STATUS_LABELS: Record<string, string> = {
    'APPROVED': '승인',
    'CANCELED': '취소',
    'PARTIAL_CANCELED': '부분취소',
    'PENDING': '대기'
  };

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

  async function loadTransactions() {
    loading = true;
    error = null;

    try {
      const params = new URLSearchParams({
        merchantId: merchantId,
        page: currentPage.toString(),
        size: pageSize.toString(),
        sortBy: 'createdAt',
        direction: 'DESC'
      });

      const response = await apiClient.get<PagedResponse<Transaction>>(`/transactions?${params}`);

      if (response.success && response.data) {
        transactions = response.data.content;
        totalCount = response.data.totalElements;
        totalPages = response.data.totalPages;
        currentPage = response.data.page;
      } else {
        error = response.error?.message || '거래내역을 불러올 수 없습니다.';
      }
    } catch (err) {
      error = err instanceof Error ? err.message : '거래내역을 불러올 수 없습니다.';
      console.error('API Error:', err);
    } finally {
      loading = false;
    }
  }

  onMount(() => {
    if (merchantId) {
      loadTransactions();
    }
  });
</script>

<div class="flex flex-col gap-4">
  <div class="flex justify-between items-center">
    <span class="text-sm text-muted-foreground">총 {totalCount}건</span>
    <Button variant="outline" size="sm" onclick={loadTransactions}>
      <svg class="w-4 h-4 mr-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
      </svg>
      새로고침
    </Button>
  </div>

  {#if loading}
    <div class="border rounded-lg overflow-hidden">
      <Table>
        <TableHeader>
          <TableRow>
            <TableHead>거래번호</TableHead>
            <TableHead class="text-right">금액</TableHead>
            <TableHead>상태</TableHead>
            <TableHead>승인번호</TableHead>
            <TableHead>승인일시</TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          {#each Array(5) as _}
            <TableRow>
              <TableCell><Skeleton class="h-4 w-32" /></TableCell>
              <TableCell class="text-right"><Skeleton class="h-4 w-24 ml-auto" /></TableCell>
              <TableCell><Skeleton class="h-5 w-16" /></TableCell>
              <TableCell><Skeleton class="h-4 w-20" /></TableCell>
              <TableCell><Skeleton class="h-4 w-28" /></TableCell>
            </TableRow>
          {/each}
        </TableBody>
      </Table>
    </div>
  {:else if error}
    <div class="text-center py-12 text-destructive">{error}</div>
  {:else if transactions.length === 0}
    <div class="flex flex-col items-center justify-center py-12 text-center text-muted-foreground">
      <span class="text-4xl mb-4">💳</span>
      <p class="text-sm">거래내역이 없습니다.</p>
    </div>
  {:else}
    <div class="border rounded-lg overflow-hidden">
      <Table>
        <TableHeader>
          <TableRow>
            <TableHead>거래번호</TableHead>
            <TableHead class="text-right">금액</TableHead>
            <TableHead>상태</TableHead>
            <TableHead>승인번호</TableHead>
            <TableHead>승인일시</TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          {#each transactions as tx}
            <TableRow>
              <TableCell class="font-mono text-sm">{tx.tid}</TableCell>
              <TableCell class="text-right font-semibold">{formatCurrency(tx.amount)}</TableCell>
              <TableCell>
                <Badge variant={getStatusVariant(tx.status)}>
                  {STATUS_LABELS[tx.status] || tx.status}
                </Badge>
              </TableCell>
              <TableCell class="font-mono text-sm">{tx.approvalNumber || '-'}</TableCell>
              <TableCell class="text-sm text-muted-foreground">
                {tx.approvedAt ? format(new Date(tx.approvedAt), 'yyyy-MM-dd HH:mm') : '-'}
              </TableCell>
            </TableRow>
          {/each}
        </TableBody>
      </Table>
    </div>

    {#if totalPages > 1}
      <div class="flex justify-center items-center gap-2 mt-4">
        <Button
          variant="outline"
          size="sm"
          disabled={currentPage === 0}
          onclick={() => { currentPage--; loadTransactions(); }}
        >
          이전
        </Button>

        <span class="px-4 text-sm text-muted-foreground">
          {currentPage + 1} / {totalPages}
        </span>

        <Button
          variant="outline"
          size="sm"
          disabled={currentPage >= totalPages - 1}
          onclick={() => { currentPage++; loadTransactions(); }}
        >
          다음
        </Button>
      </div>
    {/if}
  {/if}
</div>
