<script lang="ts">
  import { onMount } from 'svelte';
  import { settlementApi } from '../../lib/settlementApi';
  import type { DailySettlementSummary, DailySettlementDetail } from '../../types/api';
  import { format } from 'date-fns';
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

  let summaries = $state<DailySettlementSummary[]>([]);
  let selectedDetail = $state<DailySettlementDetail | null>(null);
  let selectedDate = $state<string | null>(null);
  let loading = $state(true);
  let detailLoading = $state(false);
  let error = $state<string | null>(null);
  let startDate = $state('');
  let endDate = $state('');

  const totalStats = $derived({
    netAmount: summaries.reduce((sum, s) => sum + s.netAmount, 0),
    feeAmount: summaries.reduce((sum, s) => sum + s.feeAmount, 0),
    transactionCount: summaries.reduce((sum, s) => sum + s.transactionCount, 0),
    merchantCount: new Set(summaries.flatMap(s => Array.from({ length: s.merchantCount }, (_, i) => `${s.settlementDate}-${i}`))).size > 0
      ? summaries.reduce((max, s) => Math.max(max, s.merchantCount), 0)
      : 0,
    totalMerchantCount: summaries.reduce((sum, s) => sum + s.merchantCount, 0),
  });

  function formatCurrency(amount: number): string {
    return new Intl.NumberFormat('ko-KR', {
      style: 'currency',
      currency: 'KRW'
    }).format(amount);
  }

  function formatNumber(n: number): string {
    return n.toLocaleString('ko-KR');
  }

  function setDateRange(days: number) {
    const end = new Date();
    end.setDate(end.getDate() + 5);
    const start = new Date();
    start.setDate(new Date().getDate() - days);
    startDate = format(start, 'yyyy/MM/dd');
    endDate = format(end, 'yyyy/MM/dd');
    loadSummary();
  }

  async function loadSummary() {
    loading = true;
    error = null;
    selectedDetail = null;
    selectedDate = null;

    try {
      const response = await settlementApi.getMerchantDailySummary({
        startDate: startDate || undefined,
        endDate: endDate || undefined,
      });

      if (response.success && response.data) {
        summaries = response.data;
      } else {
        error = response.error?.message || '데이터를 불러올 수 없습니다.';
      }
    } catch (err) {
      error = err instanceof Error ? err.message : '데이터를 불러올 수 없습니다.';
      console.error('Settlement load error:', err);
    } finally {
      loading = false;
    }
  }

  async function loadDetail(date: string) {
    if (selectedDate === date) {
      selectedDate = null;
      selectedDetail = null;
      return;
    }

    selectedDate = date;
    detailLoading = true;

    try {
      const response = await settlementApi.getMerchantDailyDetail(date);
      if (response.success && response.data) {
        selectedDetail = response.data;
      }
    } catch (err) {
      console.error('Detail load error:', err);
    } finally {
      detailLoading = false;
    }
  }

  function getStatusLabel(status: string): string {
    const labels: Record<string, string> = {
      'COMPLETED': '정산완료',
      'SCHEDULED': '정산예정',
      'PROCESSING': '처리중',
      'ON_HOLD': '보류',
      'PENDING': '대기',
      'FAILED': '실패',
    };
    return labels[status] || status;
  }

  function getStatusVariant(status: string): 'default' | 'secondary' | 'destructive' | 'outline' {
    const variants: Record<string, 'default' | 'secondary' | 'destructive' | 'outline'> = {
      'COMPLETED': 'default',
      'SCHEDULED': 'secondary',
      'PROCESSING': 'outline',
      'ON_HOLD': 'destructive',
      'PENDING': 'secondary',
      'FAILED': 'destructive',
    };
    return variants[status] || 'outline';
  }

  function getEntryTypeLabel(type: string): string {
    return type === 'CREDIT' ? '승인(입금)' : '취소(출금)';
  }

  function getSettlementCycleLabel(cycle?: string): string {
    if (!cycle) return '-';
    const labels: Record<string, string> = {
      'D_PLUS_1': 'D+1',
      'D_PLUS_3': 'D+3',
      'REALTIME': '실시간',
    };
    return labels[cycle] || cycle;
  }

  function getPaymentTypeLabel(type?: string): string {
    if (!type) return '-';
    const typeMap: Record<string, string> = {
      'CAT': '단말기',
      'POS': '단말기',
      'MOBILE': '수기',
      'KIOSK': '키오스크',
      'ONLINE': '인증결제',
    };
    return type.split(',').map(t => typeMap[t.trim()] || t.trim()).filter((v, i, a) => a.indexOf(v) === i).join('/');
  }

  function formatRate(rate: number): string {
    return rate ? `${(rate * 100).toFixed(2)}%` : '-';
  }

  function formatShortDate(dateStr: string): string {
    try {
      const d = new Date(dateStr);
      return format(d, 'MM-dd');
    } catch {
      return dateStr;
    }
  }

  function formatFullDate(dateStr: string): string {
    try {
      const d = new Date(dateStr);
      return format(d, 'yyyy-MM-dd');
    } catch {
      return dateStr;
    }
  }

  function formatDateTime(dateStr: string): string {
    try {
      const d = new Date(dateStr);
      return format(d, 'MM-dd HH:mm');
    } catch {
      return dateStr;
    }
  }

  onMount(() => {
    setDateRange(30);
  });
</script>

<div class="flex flex-col gap-6 p-6 max-w-[1800px] mx-auto">
  <!-- Header -->
  <header class="flex justify-between items-center">
    <div class="flex items-baseline gap-4">
      <h1 class="text-2xl font-bold text-foreground tracking-tight">가맹점 일자별 정산</h1>
      <span class="text-sm text-muted-foreground font-medium">총 {summaries.length}건</span>
    </div>
  </header>

  <!-- Summary Cards -->
  <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-3">
    <Card class="border-l-2 border-l-emerald-500">
      <CardContent class="p-3">
        <p class="text-[10px] font-medium text-muted-foreground uppercase tracking-wide">총 정산금액</p>
        <p class="text-base font-semibold text-emerald-600 mt-0.5">{formatCurrency(totalStats.netAmount)}</p>
      </CardContent>
    </Card>

    <Card class="border-l-2 border-l-rose-500">
      <CardContent class="p-3">
        <p class="text-[10px] font-medium text-muted-foreground uppercase tracking-wide">총 수수료</p>
        <p class="text-base font-semibold text-rose-600 mt-0.5">{formatCurrency(totalStats.feeAmount)}</p>
      </CardContent>
    </Card>

    <Card class="border-l-2 border-l-blue-500">
      <CardContent class="p-3">
        <p class="text-[10px] font-medium text-muted-foreground uppercase tracking-wide">총 거래건수</p>
        <p class="text-base font-semibold text-blue-600 mt-0.5">{formatNumber(totalStats.transactionCount)}건</p>
      </CardContent>
    </Card>

    <Card class="border-l-2 border-l-violet-500">
      <CardContent class="p-3">
        <p class="text-[10px] font-medium text-muted-foreground uppercase tracking-wide">가맹점 수</p>
        <p class="text-base font-semibold text-violet-600 mt-0.5">{formatNumber(totalStats.totalMerchantCount)}개</p>
      </CardContent>
    </Card>
  </div>

  <!-- Filters -->
  <div class="bg-muted/50 border border-border rounded-xl p-5">
    <div class="flex flex-wrap items-end gap-4">
      <div class="flex flex-col gap-1.5">
        <Label>기간</Label>
        <DateRangePicker
          {startDate}
          {endDate}
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

      <Button onclick={() => loadSummary()} class="ml-auto">
        <svg class="w-4 h-4 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
        </svg>
        새로고침
      </Button>
    </div>
  </div>

  <!-- Main Table -->
  {#if loading}
    <div class="bg-background border border-border rounded-xl overflow-hidden shadow-sm">
      <Table>
        <TableHeader>
          <TableRow class="bg-gradient-to-b from-muted/50 to-muted">
            <TableHead class="font-bold text-xs uppercase tracking-wide">정산일</TableHead>
            <TableHead class="font-bold text-xs uppercase tracking-wide">거래기간</TableHead>
            <TableHead class="text-right font-bold text-xs uppercase tracking-wide">거래건수</TableHead>
            <TableHead class="text-right font-bold text-xs uppercase tracking-wide">총 거래금액</TableHead>
            <TableHead class="text-right font-bold text-xs uppercase tracking-wide">수수료</TableHead>
            <TableHead class="text-right font-bold text-xs uppercase tracking-wide">정산금액</TableHead>
            <TableHead class="text-center font-bold text-xs uppercase tracking-wide">상태</TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          {#each Array(5) as _}
            <TableRow>
              <TableCell><Skeleton class="h-4 w-20" /></TableCell>
              <TableCell><Skeleton class="h-4 w-32" /></TableCell>
              <TableCell class="text-right"><Skeleton class="h-4 w-16 ml-auto" /></TableCell>
              <TableCell class="text-right"><Skeleton class="h-4 w-24 ml-auto" /></TableCell>
              <TableCell class="text-right"><Skeleton class="h-4 w-20 ml-auto" /></TableCell>
              <TableCell class="text-right"><Skeleton class="h-4 w-24 ml-auto" /></TableCell>
              <TableCell class="text-center"><Skeleton class="h-5 w-16 mx-auto" /></TableCell>
            </TableRow>
          {/each}
        </TableBody>
      </Table>
    </div>
  {:else if error}
    <div class="bg-background border border-border rounded-xl overflow-hidden shadow-sm">
      <Table>
        <TableBody>
          <TableRow>
            <TableCell colspan={7} class="py-16 text-center">
              <div class="flex flex-col items-center gap-3">
                <svg class="w-16 h-16 text-destructive/50" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                  <circle cx="12" cy="12" r="10"/>
                  <path d="M12 8v4m0 4h.01"/>
                </svg>
                <p class="text-base font-semibold text-destructive">{error}</p>
                <Button variant="outline" onclick={loadSummary}>다시 시도</Button>
              </div>
            </TableCell>
          </TableRow>
        </TableBody>
      </Table>
    </div>
  {:else}
    <div class="bg-background border border-border rounded-xl overflow-hidden shadow-sm">
      <Table>
        <TableHeader>
          <TableRow class="bg-gradient-to-b from-muted/50 to-muted">
            <TableHead class="font-bold text-xs uppercase tracking-wide">정산일</TableHead>
            <TableHead class="font-bold text-xs uppercase tracking-wide">거래기간</TableHead>
            <TableHead class="text-right font-bold text-xs uppercase tracking-wide">거래건수</TableHead>
            <TableHead class="text-right font-bold text-xs uppercase tracking-wide">총 거래금액</TableHead>
            <TableHead class="text-right font-bold text-xs uppercase tracking-wide">수수료</TableHead>
            <TableHead class="text-right font-bold text-xs uppercase tracking-wide">정산금액</TableHead>
            <TableHead class="text-center font-bold text-xs uppercase tracking-wide">상태</TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          {#each summaries as row (row.settlementDate)}
            <TableRow
              class="cursor-pointer transition-colors hover:bg-primary/5 {selectedDate === row.settlementDate ? 'bg-primary/10 ring-1 ring-primary/30' : 'even:bg-muted/30'}"
              onclick={() => loadDetail(row.settlementDate)}
              onkeydown={(e) => e.key === 'Enter' && loadDetail(row.settlementDate)}
              role="button"
              tabindex={0}
            >
              <TableCell class="font-semibold">{formatFullDate(row.settlementDate)}</TableCell>
              <TableCell class="text-muted-foreground">
                {formatShortDate(row.periodStart)}~{formatShortDate(row.periodEnd)}
              </TableCell>
              <TableCell class="text-right font-medium">{formatNumber(row.transactionCount)}</TableCell>
              <TableCell class="text-right font-semibold text-blue-600">
                {formatCurrency(row.approvalAmount + row.cancelAmount)}
              </TableCell>
              <TableCell class="text-right text-rose-600">
                {formatCurrency(row.feeAmount)}
              </TableCell>
              <TableCell class="text-right font-bold text-foreground">
                {formatCurrency(row.netAmount)}
              </TableCell>
              <TableCell class="text-center">
                <Badge variant={getStatusVariant(row.status)}>
                  {getStatusLabel(row.status)}
                </Badge>
              </TableCell>
            </TableRow>
          {/each}

          {#if summaries.length === 0}
            <TableRow>
              <TableCell colspan={7} class="py-16 text-center">
                <div class="flex flex-col items-center gap-3">
                  <svg class="w-16 h-16 text-muted-foreground/50" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                    <path d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2"/>
                  </svg>
                  <p class="text-base font-semibold text-muted-foreground">정산 데이터가 없습니다.</p>
                  <p class="text-sm text-muted-foreground/70">다른 기간을 선택해 보세요</p>
                </div>
              </TableCell>
            </TableRow>
          {/if}
        </TableBody>
      </Table>

      {#if summaries.length > 0}
        <div class="py-4 px-6 text-sm text-muted-foreground border-t border-border bg-muted/30 flex justify-between items-center">
          <span>총 {summaries.length}일</span>
          <span class="text-xs">
            정산금액 합계: <strong class="text-foreground">{formatCurrency(totalStats.netAmount)}</strong>
          </span>
        </div>
      {/if}
    </div>

    <!-- Detail Section -->
    {#if selectedDate}
      <div class="bg-background border border-border rounded-xl overflow-hidden shadow-sm">
        <div class="px-6 py-4 border-b border-border bg-muted/30">
          <div class="flex items-center gap-2">
            <svg class="w-5 h-5 text-primary" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2m-3 7h3m-3 4h3m-6-4h.01M9 16h.01"/>
            </svg>
            <h2 class="text-lg font-bold text-foreground">{formatFullDate(selectedDate)} 정산 상세</h2>
          </div>
        </div>

        {#if detailLoading}
          <div class="p-6 space-y-6">
            <div>
              <h3 class="text-sm font-bold text-muted-foreground uppercase tracking-wide mb-3">가맹점별 집계</h3>
              <div class="overflow-x-auto">
                <Table>
                  <TableHeader>
                    <TableRow>
                      <TableHead>가맹점코드</TableHead>
                      <TableHead>가맹점</TableHead>
                      <TableHead class="text-right">승인건수</TableHead>
                      <TableHead class="text-right">취소건수</TableHead>
                      <TableHead class="text-right">결제건수</TableHead>
                      <TableHead class="text-right">승인금액</TableHead>
                      <TableHead class="text-right">취소금액</TableHead>
                      <TableHead class="text-right">결제금액</TableHead>
                      <TableHead class="text-right">수수료율</TableHead>
                      <TableHead class="text-right">정산수수료</TableHead>
                      <TableHead class="text-right">정산금액</TableHead>
                      <TableHead>영업점코드</TableHead>
                      <TableHead>정산주기</TableHead>
                      <TableHead>결제유형</TableHead>
                      <TableHead>입금은행</TableHead>
                      <TableHead>계좌번호</TableHead>
                      <TableHead>예금주</TableHead>
                      <TableHead class="text-center">상태</TableHead>
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {#each Array(3) as _}
                      <TableRow>
                        {#each Array(18) as __}
                          <TableCell><Skeleton class="h-4 w-16" /></TableCell>
                        {/each}
                      </TableRow>
                    {/each}
                  </TableBody>
                </Table>
              </div>
            </div>
          </div>
        {:else if selectedDetail}
          <div class="p-6 space-y-6">
            <!-- Merchant Breakdown -->
            <div>
              <h3 class="text-sm font-bold text-muted-foreground uppercase tracking-wide mb-3">가맹점별 집계</h3>
              <div class="border border-border rounded-lg overflow-x-auto">
                <Table class="min-w-[1600px]">
                  <TableHeader>
                    <TableRow class="bg-muted/50">
                      <TableHead class="font-bold text-xs whitespace-nowrap sticky left-0 bg-muted/50 z-10">가맹점코드</TableHead>
                      <TableHead class="font-bold text-xs whitespace-nowrap">가맹점</TableHead>
                      <TableHead class="text-right font-bold text-xs whitespace-nowrap">승인건수</TableHead>
                      <TableHead class="text-right font-bold text-xs whitespace-nowrap">취소건수</TableHead>
                      <TableHead class="text-right font-bold text-xs whitespace-nowrap">결제건수</TableHead>
                      <TableHead class="text-right font-bold text-xs whitespace-nowrap">승인금액</TableHead>
                      <TableHead class="text-right font-bold text-xs whitespace-nowrap">취소금액</TableHead>
                      <TableHead class="text-right font-bold text-xs whitespace-nowrap">결제금액</TableHead>
                      <TableHead class="text-right font-bold text-xs whitespace-nowrap">수수료율</TableHead>
                      <TableHead class="text-right font-bold text-xs whitespace-nowrap">정산수수료</TableHead>
                      <TableHead class="text-right font-bold text-xs whitespace-nowrap">정산금액</TableHead>
                      <TableHead class="font-bold text-xs whitespace-nowrap">영업점코드</TableHead>
                      <TableHead class="font-bold text-xs whitespace-nowrap">정산주기</TableHead>
                      <TableHead class="font-bold text-xs whitespace-nowrap">결제유형</TableHead>
                      <TableHead class="font-bold text-xs whitespace-nowrap">입금은행</TableHead>
                      <TableHead class="font-bold text-xs whitespace-nowrap">계좌번호</TableHead>
                      <TableHead class="font-bold text-xs whitespace-nowrap">예금주</TableHead>
                      <TableHead class="text-center font-bold text-xs whitespace-nowrap">상태</TableHead>
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {#each selectedDetail.merchantBreakdown as merchant (merchant.merchantId)}
                      <TableRow class="even:bg-muted/30">
                        <TableCell class="font-mono text-sm whitespace-nowrap sticky left-0 bg-background z-10">{merchant.merchantCode || '-'}</TableCell>
                        <TableCell class="font-medium whitespace-nowrap">{merchant.merchantName}</TableCell>
                        <TableCell class="text-right">{formatNumber(merchant.approvalCount)}</TableCell>
                        <TableCell class="text-right text-rose-600">{formatNumber(merchant.cancelCount)}</TableCell>
                        <TableCell class="text-right font-medium">{formatNumber(merchant.approvalCount - merchant.cancelCount)}</TableCell>
                        <TableCell class="text-right text-emerald-600 font-medium whitespace-nowrap">
                          {formatCurrency(merchant.approvalAmount)}
                        </TableCell>
                        <TableCell class="text-right text-rose-600 whitespace-nowrap">
                          {merchant.cancelAmount !== 0 ? formatCurrency(merchant.cancelAmount) : '-'}
                        </TableCell>
                        <TableCell class="text-right font-medium whitespace-nowrap">
                          {formatCurrency(merchant.approvalAmount - merchant.cancelAmount)}
                        </TableCell>
                        <TableCell class="text-right whitespace-nowrap">{formatRate(merchant.feeRate)}</TableCell>
                        <TableCell class="text-right text-rose-600 whitespace-nowrap">
                          {formatCurrency(merchant.feeAmount)}
                        </TableCell>
                        <TableCell class="text-right font-bold whitespace-nowrap">
                          {formatCurrency(merchant.netAmount)}
                        </TableCell>
                        <TableCell class="font-mono text-sm whitespace-nowrap">{merchant.orgCode || '-'}</TableCell>
                        <TableCell class="whitespace-nowrap">{getSettlementCycleLabel(merchant.settlementCycle)}</TableCell>
                        <TableCell class="whitespace-nowrap">{getPaymentTypeLabel(merchant.paymentType)}</TableCell>
                        <TableCell class="whitespace-nowrap">{merchant.bankName || '-'}</TableCell>
                        <TableCell class="font-mono text-sm whitespace-nowrap">{merchant.accountNumber || '-'}</TableCell>
                        <TableCell class="whitespace-nowrap">{merchant.accountHolder || '-'}</TableCell>
                        <TableCell class="text-center">
                          <Badge variant={getStatusVariant(merchant.status || 'PENDING')}>
                            {getStatusLabel(merchant.status || 'PENDING')}
                          </Badge>
                        </TableCell>
                      </TableRow>
                    {/each}

                    {#if selectedDetail.merchantBreakdown.length === 0}
                      <TableRow>
                        <TableCell colspan={18} class="text-center py-8 text-muted-foreground">
                          가맹점 데이터가 없습니다.
                        </TableCell>
                      </TableRow>
                    {/if}

                    {#if selectedDetail.merchantBreakdown.length > 0}
                      {@const summaryApprovalCount = selectedDetail.merchantBreakdown.reduce((sum, m) => sum + m.approvalCount, 0)}
                      {@const summaryCancelCount = selectedDetail.merchantBreakdown.reduce((sum, m) => sum + m.cancelCount, 0)}
                      <TableRow class="bg-muted/60 font-bold border-t-2 border-border">
                        <TableCell class="sticky left-0 bg-muted/60 z-10">합계</TableCell>
                        <TableCell></TableCell>
                        <TableCell class="text-right">{formatNumber(summaryApprovalCount)}</TableCell>
                        <TableCell class="text-right text-rose-600">{formatNumber(summaryCancelCount)}</TableCell>
                        <TableCell class="text-right">{formatNumber(summaryApprovalCount - summaryCancelCount)}</TableCell>
                        <TableCell class="text-right text-emerald-600 whitespace-nowrap">
                          {formatCurrency(selectedDetail.summary.approvalAmount)}
                        </TableCell>
                        <TableCell class="text-right text-rose-600 whitespace-nowrap">
                          {selectedDetail.summary.cancelAmount !== 0 ? formatCurrency(selectedDetail.summary.cancelAmount) : '-'}
                        </TableCell>
                        <TableCell class="text-right whitespace-nowrap">
                          {formatCurrency(selectedDetail.summary.approvalAmount - selectedDetail.summary.cancelAmount)}
                        </TableCell>
                        <TableCell class="text-right">-</TableCell>
                        <TableCell class="text-right text-rose-600 whitespace-nowrap">
                          {formatCurrency(selectedDetail.summary.feeAmount)}
                        </TableCell>
                        <TableCell class="text-right whitespace-nowrap">
                          {formatCurrency(selectedDetail.summary.netAmount)}
                        </TableCell>
                        <TableCell colspan={7}></TableCell>
                      </TableRow>
                    {/if}
                  </TableBody>
                </Table>
              </div>
            </div>

            <!-- Individual Settlements -->
            <div>
              <h3 class="text-sm font-bold text-muted-foreground uppercase tracking-wide mb-3">개별 정산내역</h3>
              <div class="border border-border rounded-lg overflow-hidden">
                <Table>
                  <TableHeader>
                    <TableRow class="bg-muted/50">
                      <TableHead class="font-bold text-xs">거래일시</TableHead>
                      <TableHead class="font-bold text-xs">가맹점</TableHead>
                      <TableHead class="font-bold text-xs">구분</TableHead>
                      <TableHead class="text-right font-bold text-xs">금액</TableHead>
                      <TableHead class="text-right font-bold text-xs">수수료</TableHead>
                      <TableHead class="text-right font-bold text-xs">정산금액</TableHead>
                      <TableHead class="text-center font-bold text-xs">상태</TableHead>
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {#each selectedDetail.settlements as settlement (settlement.id)}
                      <TableRow class="even:bg-muted/30">
                        <TableCell class="text-muted-foreground font-mono text-sm">
                          {formatDateTime(settlement.createdAt)}
                        </TableCell>
                        <TableCell class="font-medium">
                          {settlement.merchantName || settlement.merchantId}
                        </TableCell>
                        <TableCell>
                          <Badge variant={settlement.entryType === 'CREDIT' ? 'default' : 'destructive'}>
                            {getEntryTypeLabel(settlement.entryType)}
                          </Badge>
                        </TableCell>
                        <TableCell class="text-right font-medium">
                          {formatCurrency(settlement.amount)}
                        </TableCell>
                        <TableCell class="text-right text-rose-600">
                          {formatCurrency(settlement.feeAmount)}
                        </TableCell>
                        <TableCell class="text-right font-bold">
                          {formatCurrency(settlement.netAmount)}
                        </TableCell>
                        <TableCell class="text-center">
                          <Badge variant={getStatusVariant(settlement.status)}>
                            {getStatusLabel(settlement.status)}
                          </Badge>
                        </TableCell>
                      </TableRow>
                    {/each}

                    {#if selectedDetail.settlements.length === 0}
                      <TableRow>
                        <TableCell colspan={7} class="text-center py-8 text-muted-foreground">
                          정산 내역이 없습니다.
                        </TableCell>
                      </TableRow>
                    {/if}
                  </TableBody>
                </Table>
              </div>

              {#if selectedDetail.settlements.length > 0}
                <div class="py-3 px-4 text-sm text-muted-foreground bg-muted/30 border-t border-border rounded-b-lg flex justify-between items-center">
                  <span>총 {selectedDetail.settlements.length}건</span>
                  <span class="text-xs">
                    정산금액 합계: <strong class="text-foreground">{formatCurrency(selectedDetail.summary.netAmount)}</strong>
                  </span>
                </div>
              {/if}
            </div>
          </div>
        {/if}
      </div>
    {/if}
  {/if}
</div>
