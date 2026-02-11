<script lang="ts">
  import { onMount } from 'svelte';
  import { apiClient } from '@/api/client';
  import { format } from 'date-fns';
  import type { Settlement, PagedResponse } from '@/types/api';
  import { Badge } from '$lib/components/ui/badge';
  import { Button } from '$lib/components/ui/button';
  import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '$lib/components/ui/table';
  import { Skeleton } from '$lib/components/ui/skeleton';

  interface Props {
    merchantId: string;
  }

  let { merchantId }: Props = $props();

  let allSettlements = $state<Settlement[]>([]);
  let loading = $state(true);
  let error = $state<string | null>(null);

  let currentPage = $state(0);
  let pageSize = 10;

  let settlements = $derived(
    allSettlements.slice(currentPage * pageSize, (currentPage + 1) * pageSize)
  );
  let totalCount = $derived(allSettlements.length);
  let totalPages = $derived(Math.ceil(allSettlements.length / pageSize));

  const STATUS_LABELS: Record<string, string> = {
    'PENDING': '대기',
    'CONFIRMED': '확정',
    'SETTLED': '정산완료',
    'CANCELLED': '취소'
  };

  const ENTRY_TYPE_LABELS: Record<string, string> = {
    'CREDIT': '입금',
    'DEBIT': '출금'
  };

  function formatCurrency(amount: number): string {
    return new Intl.NumberFormat('ko-KR', {
      style: 'currency',
      currency: 'KRW'
    }).format(amount);
  }

  function getStatusVariant(status: string): 'default' | 'secondary' | 'destructive' | 'outline' {
    const variants: Record<string, 'default' | 'secondary' | 'destructive' | 'outline'> = {
      'SETTLED': 'default',
      'CONFIRMED': 'secondary',
      'PENDING': 'outline',
      'CANCELLED': 'destructive'
    };
    return variants[status] || 'outline';
  }

  function getEntryTypeVariant(entryType: string): 'default' | 'destructive' {
    return entryType === 'CREDIT' ? 'default' : 'destructive';
  }

  async function loadSettlements() {
    loading = true;
    error = null;

    try {
      const params = new URLSearchParams({
        page: '0',
        size: '100',
        sortBy: 'created_at',
        direction: 'DESC',
        merchantOnly: 'true'
      });

      const response = await apiClient.get<PagedResponse<Settlement>>(`/settlements?${params}`);

      if (response.success && response.data) {
        allSettlements = response.data.content.filter(s => s.merchantId === merchantId);
        currentPage = 0;
      } else {
        error = response.error?.message || '정산내역을 불러올 수 없습니다.';
      }
    } catch (err) {
      error = err instanceof Error ? err.message : '정산내역을 불러올 수 없습니다.';
      console.error('API Error:', err);
    } finally {
      loading = false;
    }
  }

  onMount(() => {
    if (merchantId) {
      loadSettlements();
    }
  });
</script>

<div class="flex flex-col gap-4">
  <div class="flex justify-between items-center">
    <span class="text-sm text-muted-foreground">총 {totalCount}건</span>
    <Button variant="outline" size="sm" onclick={loadSettlements}>
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
            <TableHead>유형</TableHead>
            <TableHead class="text-right">거래금액</TableHead>
            <TableHead class="text-right">수수료</TableHead>
            <TableHead class="text-right">정산금액</TableHead>
            <TableHead>상태</TableHead>
            <TableHead>정산일시</TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          {#each Array(5) as _}
            <TableRow>
              <TableCell><Skeleton class="h-5 w-16" /></TableCell>
              <TableCell class="text-right"><Skeleton class="h-4 w-24 ml-auto" /></TableCell>
              <TableCell class="text-right"><Skeleton class="h-4 w-20 ml-auto" /></TableCell>
              <TableCell class="text-right"><Skeleton class="h-4 w-28 ml-auto" /></TableCell>
              <TableCell><Skeleton class="h-5 w-16" /></TableCell>
              <TableCell><Skeleton class="h-4 w-28" /></TableCell>
            </TableRow>
          {/each}
        </TableBody>
      </Table>
    </div>
  {:else if error}
    <div class="text-center py-12 text-destructive">{error}</div>
  {:else if settlements.length === 0}
    <div class="flex flex-col items-center justify-center py-12 text-center text-muted-foreground">
      <span class="text-4xl mb-4">📊</span>
      <p class="text-sm">정산내역이 없습니다.</p>
    </div>
  {:else}
    <div class="border rounded-lg overflow-hidden">
      <Table>
        <TableHeader>
          <TableRow>
            <TableHead>유형</TableHead>
            <TableHead class="text-right">거래금액</TableHead>
            <TableHead class="text-right">수수료</TableHead>
            <TableHead class="text-right">정산금액</TableHead>
            <TableHead>상태</TableHead>
            <TableHead>정산일시</TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          {#each settlements as settlement}
            <TableRow>
              <TableCell>
                <Badge variant={getEntryTypeVariant(settlement.entryType)}>
                  {ENTRY_TYPE_LABELS[settlement.entryType] || settlement.entryType}
                </Badge>
              </TableCell>
              <TableCell class="text-right font-medium">{formatCurrency(settlement.amount)}</TableCell>
              <TableCell class="text-right text-muted-foreground">{formatCurrency(settlement.feeAmount)}</TableCell>
              <TableCell class="text-right font-semibold">{formatCurrency(settlement.netAmount)}</TableCell>
              <TableCell>
                <Badge variant={getStatusVariant(settlement.status)}>
                  {STATUS_LABELS[settlement.status] || settlement.status}
                </Badge>
              </TableCell>
              <TableCell class="text-sm text-muted-foreground">
                {settlement.settledAt ? format(new Date(settlement.settledAt), 'yyyy-MM-dd HH:mm') : '-'}
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
          onclick={() => { currentPage--; }}
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
          onclick={() => { currentPage++; }}
        >
          다음
        </Button>
      </div>
    {/if}
  {/if}
</div>
