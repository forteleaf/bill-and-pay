<script lang="ts">
  import { onMount } from 'svelte';
  import { format } from 'date-fns';
  import { settlementApi } from '../../lib/settlementApi';
  import type {
    OrgDailySettlementSummary,
    OrgDailySettlementDetail
  } from '../../types/api';
  import { Card, CardContent } from '$lib/components/ui/card';
  import { Button } from '$lib/components/ui/button';
  import { Badge } from '$lib/components/ui/badge';
  import { Label } from '$lib/components/ui/label';
  import { DateRangePicker } from '$lib/components/ui/date-range-picker';
  import {
    Table,
    TableBody,
    TableCell,
    TableHead,
    TableHeader,
    TableRow
  } from '$lib/components/ui/table';
  import { Skeleton } from '$lib/components/ui/skeleton';

  let startDate = $state('');
  let endDate = $state('');

  let summaryList = $state<OrgDailySettlementSummary[]>([]);
  let summaryLoading = $state(false);
  let summaryError = $state<string | null>(null);

  let selectedDate = $state<string | null>(null);
  let detail = $state<OrgDailySettlementDetail | null>(null);
  let detailLoading = $state(false);
  let detailError = $state<string | null>(null);

  let totalFeeIncome = $derived(
    summaryList.reduce((sum, s) => sum + s.feeAmount, 0)
  );
  let totalTransactionCount = $derived(
    summaryList.reduce((sum, s) => sum + s.transactionCount, 0)
  );
  let uniqueOrgCount = $derived(
    summaryList.reduce((max, s) => Math.max(max, s.orgCount), 0)
  );
  let settlementDays = $derived(summaryList.length);

  const ORG_TYPE_LABELS: Record<string, string> = {
    DISTRIBUTOR: '총판',
    AGENCY: '대리점',
    DEALER: '딜러',
    SELLER: '셀러'
  };

  function orgTypeLabel(type: string): string {
    return ORG_TYPE_LABELS[type] ?? type;
  }

  function orgTypeBadgeVariant(type: string): 'default' | 'secondary' | 'outline' | 'destructive' {
    switch (type) {
      case 'DISTRIBUTOR':
        return 'default';
      case 'AGENCY':
        return 'secondary';
      case 'DEALER':
        return 'outline';
      case 'SELLER':
        return 'outline';
      default:
        return 'outline';
    }
  }

  function statusBadgeVariant(status: string): 'default' | 'secondary' | 'outline' | 'destructive' {
    switch (status) {
      case 'COMPLETED':
      case 'SETTLED':
        return 'default';
      case 'PENDING':
        return 'secondary';
      case 'FAILED':
      case 'CANCELLED':
        return 'destructive';
      default:
        return 'outline';
    }
  }

  function statusLabel(status: string): string {
    switch (status) {
      case 'COMPLETED':
        return '완료';
      case 'SETTLED':
        return '정산완료';
      case 'PENDING':
        return '대기';
      case 'PROCESSING':
        return '처리중';
      case 'FAILED':
        return '실패';
      case 'CANCELLED':
        return '취소';
      default:
        return status;
    }
  }

  function entryTypeLabel(type: string): string {
    switch (type) {
      case 'CREDIT':
        return '입금';
      case 'DEBIT':
        return '출금';
      default:
        return type;
    }
  }

  function entityTypeLabel(type: string): string {
    switch (type) {
      case 'MERCHANT':
        return '가맹점';
      case 'DISTRIBUTOR':
        return '총판';
      case 'AGENCY':
        return '대리점';
      case 'DEALER':
        return '딜러';
      case 'SELLER':
        return '셀러';
      case 'VENDOR':
        return '밴사';
      case 'PLATFORM':
        return '플랫폼';
      default:
        return type;
    }
  }

  function formatAmount(value: number): string {
    return new Intl.NumberFormat('ko-KR').format(value);
  }

  function formatDate(dateStr: string): string {
    if (!dateStr) return '-';
    try {
      return format(new Date(dateStr), 'yyyy-MM-dd');
    } catch {
      return dateStr;
    }
  }

  function formatDateTime(dateStr: string): string {
    if (!dateStr) return '-';
    try {
      return format(new Date(dateStr), 'yyyy-MM-dd HH:mm');
    } catch {
      return dateStr;
    }
  }

  function setDateRange(days: number) {
    const end = new Date();
    end.setDate(end.getDate() + 5);
    const start = new Date();
    start.setDate(start.getDate() - days);
    startDate = format(start, 'yyyy/MM/dd');
    endDate = format(end, 'yyyy/MM/dd');
    loadSummary();
  }

  async function loadSummary() {
    if (!startDate || !endDate) return;
    summaryLoading = true;
    summaryError = null;
    selectedDate = null;
    detail = null;
    try {
      const params = {
        startDate: startDate.replace(/\//g, '-'),
        endDate: endDate.replace(/\//g, '-')
      };
      const response = await settlementApi.getOrgDailySummary(params);
      if (response.success) {
        summaryList = response.data ?? [];
      } else {
        summaryError = response.error?.message || '정산 요약을 불러올 수 없습니다.';
      }
    } catch (err) {
      summaryError = err instanceof Error ? err.message : '정산 요약을 불러올 수 없습니다.';
      console.error('OrgDailySummary error:', err);
    } finally {
      summaryLoading = false;
    }
  }

  async function loadDetail(settlementDate: string) {
    if (selectedDate === settlementDate) {
      selectedDate = null;
      detail = null;
      return;
    }
    selectedDate = settlementDate;
    detailLoading = true;
    detailError = null;
    detail = null;
    try {
      const response = await settlementApi.getOrgDailyDetail(settlementDate);
      if (response.success) {
        detail = response.data ?? null;
      } else {
        detailError = response.error?.message || '정산 상세를 불러올 수 없습니다.';
      }
    } catch (err) {
      detailError = err instanceof Error ? err.message : '정산 상세를 불러올 수 없습니다.';
      console.error('OrgDailyDetail error:', err);
    } finally {
      detailLoading = false;
    }
  }

  function handleDateChange(start: string, end: string) {
    startDate = start;
    endDate = end;
  }

  function handleSearch() {
    loadSummary();
  }

  onMount(() => {
    setDateRange(30);
  });
</script>

<div class="flex flex-col gap-6 p-6">
  <div class="flex items-center justify-between">
    <div>
      <h1 class="text-2xl font-bold text-foreground">영업점 일자별 정산</h1>
      <p class="mt-1 text-sm text-muted-foreground">
        영업점별 일자 단위 정산 현황을 조회합니다.
      </p>
    </div>
  </div>

  <Card>
    <CardContent class="pt-6">
      <div class="flex flex-wrap items-end gap-3">
        <div class="flex flex-col gap-1.5">
          <Label>기간</Label>
          <DateRangePicker
            {startDate}
            {endDate}
            onchange={handleDateChange}
            placeholder="기간 선택"
            class="w-[280px]"
          />
        </div>
        <div class="flex gap-1">
          <Button variant="outline" size="sm" onclick={() => setDateRange(7)}>7일</Button>
          <Button variant="outline" size="sm" onclick={() => setDateRange(30)}>30일</Button>
          <Button variant="outline" size="sm" onclick={() => setDateRange(90)}>90일</Button>
        </div>
        <Button onclick={handleSearch}>조회</Button>
      </div>
    </CardContent>
  </Card>

  {#if summaryLoading}
    <div class="grid grid-cols-1 gap-4 md:grid-cols-2 lg:grid-cols-4">
      {#each Array(4) as _}
        <Card>
          <CardContent class="pt-6">
            <Skeleton class="mb-2 h-4 w-24" />
            <Skeleton class="h-8 w-32" />
          </CardContent>
        </Card>
      {/each}
    </div>
  {:else if !summaryError}
    <div class="grid grid-cols-1 gap-4 md:grid-cols-2 lg:grid-cols-4">
      <Card class="border-l-4 border-l-emerald-500">
        <CardContent class="pt-6">
          <p class="text-sm font-medium text-muted-foreground">총 수수료수익</p>
          <p class="mt-1 text-2xl font-bold text-emerald-600">
            {formatAmount(totalFeeIncome)}
            <span class="text-sm font-normal text-muted-foreground">원</span>
          </p>
        </CardContent>
      </Card>

      <Card class="border-l-4 border-l-blue-500">
        <CardContent class="pt-6">
          <p class="text-sm font-medium text-muted-foreground">총 거래건수</p>
          <p class="mt-1 text-2xl font-bold text-blue-600">
            {formatAmount(totalTransactionCount)}
            <span class="text-sm font-normal text-muted-foreground">건</span>
          </p>
        </CardContent>
      </Card>

      <Card class="border-l-4 border-l-violet-500">
        <CardContent class="pt-6">
          <p class="text-sm font-medium text-muted-foreground">영업점 수</p>
          <p class="mt-1 text-2xl font-bold text-violet-600">
            {formatAmount(uniqueOrgCount)}
            <span class="text-sm font-normal text-muted-foreground">개</span>
          </p>
        </CardContent>
      </Card>

      <Card class="border-l-4 border-l-amber-500">
        <CardContent class="pt-6">
          <p class="text-sm font-medium text-muted-foreground">정산일수</p>
          <p class="mt-1 text-2xl font-bold text-amber-600">
            {settlementDays}
            <span class="text-sm font-normal text-muted-foreground">일</span>
          </p>
        </CardContent>
      </Card>
    </div>
  {/if}

  {#if summaryError}
    <Card>
      <CardContent class="flex flex-col items-center justify-center py-12">
        <svg
          xmlns="http://www.w3.org/2000/svg"
          class="mb-3 h-10 w-10 text-destructive"
          fill="none"
          viewBox="0 0 24 24"
          stroke="currentColor"
          stroke-width="2"
        >
          <path
            stroke-linecap="round"
            stroke-linejoin="round"
            d="M12 9v2m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"
          />
        </svg>
        <p class="text-sm font-medium text-destructive">{summaryError}</p>
        <Button variant="outline" size="sm" class="mt-4" onclick={handleSearch}>
          다시 시도
        </Button>
      </CardContent>
    </Card>
  {/if}

  <Card>
    <CardContent class="pt-6">
      <div class="mb-4 flex items-center justify-between">
        <h2 class="text-lg font-semibold text-foreground">일자별 정산 현황</h2>
        {#if !summaryLoading && summaryList.length > 0}
          <span class="text-sm text-muted-foreground">
            총 {summaryList.length}건
          </span>
        {/if}
      </div>

      {#if summaryLoading}
        <div class="flex flex-col gap-2">
          {#each Array(5) as _}
            <Skeleton class="h-12 w-full" />
          {/each}
        </div>
      {:else if summaryList.length === 0 && !summaryError}
        <div class="flex flex-col items-center justify-center py-16">
          <svg
            xmlns="http://www.w3.org/2000/svg"
            class="mb-3 h-10 w-10 text-muted-foreground/50"
            fill="none"
            viewBox="0 0 24 24"
            stroke="currentColor"
            stroke-width="1.5"
          >
            <path
              stroke-linecap="round"
              stroke-linejoin="round"
              d="M20 13V6a2 2 0 00-2-2H6a2 2 0 00-2 2v7m16 0v5a2 2 0 01-2 2H6a2 2 0 01-2-2v-5m16 0h-2.586a1 1 0 00-.707.293l-2.414 2.414a1 1 0 01-.707.293h-3.172a1 1 0 01-.707-.293l-2.414-2.414A1 1 0 006.586 13H4"
            />
          </svg>
          <p class="text-sm text-muted-foreground">조회된 정산 내역이 없습니다.</p>
          <p class="mt-1 text-xs text-muted-foreground/70">
            조회 기간을 변경하여 다시 검색해 주세요.
          </p>
        </div>
      {:else}
        <div class="overflow-x-auto rounded-md border">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead class="w-[120px]">정산일</TableHead>
                <TableHead class="w-[200px]">거래기간</TableHead>
                <TableHead class="w-[100px] text-right">거래건수</TableHead>
                <TableHead class="w-[140px] text-right">총 거래금액</TableHead>
                <TableHead class="w-[140px] text-right">수수료수익</TableHead>
                <TableHead class="w-[100px] text-center">상태</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {#each summaryList as row}
                <TableRow
                  class="cursor-pointer transition-colors hover:bg-muted/50 {selectedDate === row.settlementDate ? 'bg-muted' : ''}"
                  onclick={() => loadDetail(row.settlementDate)}
                >
                  <TableCell class="font-medium">
                    {formatDate(row.settlementDate)}
                  </TableCell>
                  <TableCell class="text-muted-foreground">
                    {formatDate(row.periodStart)} ~ {formatDate(row.periodEnd)}
                  </TableCell>
                  <TableCell class="text-right">
                    {formatAmount(row.transactionCount)}건
                  </TableCell>
                  <TableCell class="text-right font-medium">
                    {formatAmount(row.approvalAmount - row.cancelAmount)}원
                  </TableCell>
                  <TableCell class="text-right font-semibold text-emerald-600">
                    {formatAmount(row.feeAmount)}원
                  </TableCell>
                  <TableCell class="text-center">
                    <Badge variant={statusBadgeVariant(row.status)}>
                      {statusLabel(row.status)}
                    </Badge>
                  </TableCell>
                </TableRow>

                {#if selectedDate === row.settlementDate}
                  <TableRow>
                    <TableCell colspan={6} class="bg-muted/30 p-0">
                      <div class="p-6">
                        {#if detailLoading}
                          <div class="flex flex-col gap-3">
                            <Skeleton class="h-6 w-48" />
                            <Skeleton class="h-32 w-full" />
                            <Skeleton class="h-6 w-48" />
                            <Skeleton class="h-32 w-full" />
                          </div>
                        {:else if detailError}
                          <div class="flex flex-col items-center py-8">
                            <p class="text-sm text-destructive">{detailError}</p>
                            <Button
                              variant="outline"
                              size="sm"
                              class="mt-3"
                              onclick={() => loadDetail(row.settlementDate)}
                            >
                              다시 시도
                            </Button>
                          </div>
                        {:else if detail}
                          <div class="mb-6 grid grid-cols-2 gap-4 md:grid-cols-5">
                            <div class="rounded-lg border bg-background p-3">
                              <p class="text-xs text-muted-foreground">거래건수</p>
                              <p class="mt-1 text-lg font-bold">
                                {formatAmount(detail.summary.transactionCount)}
                                <span class="text-xs font-normal text-muted-foreground">건</span>
                              </p>
                            </div>
                            <div class="rounded-lg border bg-background p-3">
                              <p class="text-xs text-muted-foreground">승인금액</p>
                              <p class="mt-1 text-lg font-bold text-blue-600">
                                {formatAmount(detail.summary.approvalAmount)}
                                <span class="text-xs font-normal text-muted-foreground">원</span>
                              </p>
                            </div>
                            <div class="rounded-lg border bg-background p-3">
                              <p class="text-xs text-muted-foreground">취소금액</p>
                              <p class="mt-1 text-lg font-bold text-red-500">
                                {formatAmount(detail.summary.cancelAmount)}
                                <span class="text-xs font-normal text-muted-foreground">원</span>
                              </p>
                            </div>
                            <div class="rounded-lg border bg-background p-3">
                              <p class="text-xs text-muted-foreground">수수료수익</p>
                              <p class="mt-1 text-lg font-bold text-emerald-600">
                                {formatAmount(detail.summary.feeAmount)}
                                <span class="text-xs font-normal text-muted-foreground">원</span>
                              </p>
                            </div>
                            <div class="rounded-lg border bg-background p-3">
                              <p class="text-xs text-muted-foreground">순정산금액</p>
                              <p class="mt-1 text-lg font-bold">
                                {formatAmount(detail.summary.netAmount)}
                                <span class="text-xs font-normal text-muted-foreground">원</span>
                              </p>
                            </div>
                          </div>

                          {#if detail.orgBreakdown && detail.orgBreakdown.length > 0}
                            <div class="mb-6">
                              <h3 class="mb-3 text-sm font-semibold text-foreground">
                                영업점별 집계
                                <span class="ml-1 text-xs font-normal text-muted-foreground">
                                  ({detail.orgBreakdown.length}개)
                                </span>
                              </h3>
                              <div class="overflow-x-auto rounded-md border">
                                <Table>
                                  <TableHeader>
                                    <TableRow>
                                      <TableHead>영업점명</TableHead>
                                      <TableHead class="w-[90px]">유형</TableHead>
                                      <TableHead class="w-[100px]">코드</TableHead>
                                      <TableHead class="w-[80px] text-right">건수</TableHead>
                                      <TableHead class="w-[120px] text-right">승인금액</TableHead>
                                      <TableHead class="w-[120px] text-right">취소금액</TableHead>
                                      <TableHead class="w-[90px] text-right">수수료율</TableHead>
                                      <TableHead class="w-[120px] text-right">수수료금액</TableHead>
                                    </TableRow>
                                  </TableHeader>
                                  <TableBody>
                                    {#each detail.orgBreakdown as org}
                                      <TableRow>
                                        <TableCell class="font-medium">
                                          {org.orgName}
                                        </TableCell>
                                        <TableCell>
                                          <Badge variant={orgTypeBadgeVariant(org.orgType)} class="text-xs">
                                            {orgTypeLabel(org.orgType)}
                                          </Badge>
                                        </TableCell>
                                        <TableCell class="font-mono text-xs text-muted-foreground">
                                          {org.orgCode}
                                        </TableCell>
                                        <TableCell class="text-right">
                                          {formatAmount(org.transactionCount)}
                                        </TableCell>
                                        <TableCell class="text-right text-blue-600">
                                          {formatAmount(org.approvalAmount)}원
                                        </TableCell>
                                        <TableCell class="text-right text-red-500">
                                          {formatAmount(org.cancelAmount)}원
                                        </TableCell>
                                        <TableCell class="text-right">
                                          {org.feeRate.toFixed(2)}%
                                        </TableCell>
                                        <TableCell class="text-right font-semibold text-emerald-600">
                                          {formatAmount(org.feeAmount)}원
                                        </TableCell>
                                      </TableRow>
                                    {/each}
                                  </TableBody>
                                </Table>
                              </div>
                            </div>
                          {/if}

                          {#if detail.settlements && detail.settlements.length > 0}
                            <div>
                              <h3 class="mb-3 text-sm font-semibold text-foreground">
                                개별 정산내역
                                <span class="ml-1 text-xs font-normal text-muted-foreground">
                                  ({detail.settlements.length}건)
                                </span>
                              </h3>
                              <div class="overflow-x-auto rounded-md border">
                                <Table>
                                  <TableHeader>
                                    <TableRow>
                                      <TableHead class="w-[80px]">정산ID</TableHead>
                                      <TableHead class="w-[80px]">유형</TableHead>
                                      <TableHead class="w-[80px]">구분</TableHead>
                                      <TableHead>가맹점</TableHead>
                                      <TableHead class="w-[120px] text-right">거래금액</TableHead>
                                      <TableHead class="w-[100px] text-right">수수료</TableHead>
                                      <TableHead class="w-[120px] text-right">정산금액</TableHead>
                                      <TableHead class="w-[80px] text-center">상태</TableHead>
                                      <TableHead class="w-[140px]">정산일시</TableHead>
                                    </TableRow>
                                  </TableHeader>
                                  <TableBody>
                                    {#each detail.settlements as stl}
                                      <TableRow>
                                        <TableCell class="font-mono text-xs text-muted-foreground">
                                          {stl.id?.toString().slice(-8) ?? '-'}
                                        </TableCell>
                                        <TableCell>
                                          <Badge variant="outline" class="text-xs">
                                            {entityTypeLabel(stl.entityType)}
                                          </Badge>
                                        </TableCell>
                                        <TableCell>
                                          <Badge
                                            variant={stl.entryType === 'CREDIT' ? 'default' : 'destructive'}
                                            class="text-xs"
                                          >
                                            {entryTypeLabel(stl.entryType)}
                                          </Badge>
                                        </TableCell>
                                        <TableCell class="font-medium">
                                          {stl.merchantName ?? '-'}
                                        </TableCell>
                                        <TableCell class="text-right">
                                          {formatAmount(stl.amount)}원
                                        </TableCell>
                                        <TableCell class="text-right text-muted-foreground">
                                          {formatAmount(stl.feeAmount)}원
                                        </TableCell>
                                        <TableCell class="text-right font-semibold">
                                          {formatAmount(stl.netAmount)}원
                                        </TableCell>
                                        <TableCell class="text-center">
                                          <Badge variant={statusBadgeVariant(stl.status)}>
                                            {statusLabel(stl.status)}
                                          </Badge>
                                        </TableCell>
                                        <TableCell class="text-xs text-muted-foreground">
                                          {formatDateTime(stl.createdAt)}
                                        </TableCell>
                                      </TableRow>
                                    {/each}
                                  </TableBody>
                                </Table>
                              </div>
                            </div>
                          {:else}
                            <div class="flex flex-col items-center py-8">
                              <p class="text-sm text-muted-foreground">
                                개별 정산내역이 없습니다.
                              </p>
                            </div>
                          {/if}
                        {/if}
                      </div>
                    </TableCell>
                  </TableRow>
                {/if}
              {/each}
            </TableBody>
          </Table>
        </div>
      {/if}
    </CardContent>
  </Card>
</div>
