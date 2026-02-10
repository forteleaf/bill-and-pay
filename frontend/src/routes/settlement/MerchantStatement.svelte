<script lang="ts">
  import { settlementApi } from '@/api/settlement';
  import { merchantApi } from '@/api/merchant';
  import { tenantStore } from '@/stores/tenant';
  import { format } from 'date-fns';
  import type { MerchantStatement as MerchantStatementType, DailyStatementRow } from '@/types/api';
  import type { MerchantDto } from '@/types/merchant';
  import { Card, CardContent } from '$lib/components/ui/card';
  import { Button } from '$lib/components/ui/button';
  import { Badge } from '$lib/components/ui/badge';
  import { Label } from '$lib/components/ui/label';
  import { Input } from '$lib/components/ui/input';
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
  import { Separator } from '$lib/components/ui/separator';

  let searchQuery = $state('');
  let searchResults = $state<MerchantDto[]>([]);
  let searching = $state(false);
  let showSearchResults = $state(false);
  let selectedMerchant = $state<MerchantDto | null>(null);
  let statement = $state<MerchantStatementType | null>(null);
  let loading = $state(false);
  let error = $state<string | null>(null);
  let startDate = $state('');
  let endDate = $state('');

  function formatCurrency(amount: number): string {
    return new Intl.NumberFormat('ko-KR', {
      style: 'currency',
      currency: 'KRW'
    }).format(amount);
  }

  function formatPercent(rate: number): string {
    return `${(rate * 100).toFixed(2)}%`;
  }

  function formatBusinessNumber(num?: string): string {
    if (!num) return '-';
    const cleaned = num.replace(/\D/g, '');
    if (cleaned.length === 10) {
      return `${cleaned.slice(0, 3)}-${cleaned.slice(3, 5)}-${cleaned.slice(5)}`;
    }
    return num;
  }

  function formatAccountNumber(account?: string): string {
    if (!account) return '-';
    if (account.length > 4) {
      return '***' + account.slice(-4);
    }
    return account;
  }

  function getCycleLabel(cycle?: string): string {
    if (!cycle) return '-';
    const labels: Record<string, string> = {
      'D_PLUS_1': 'D+1 (익영업일)',
      'D_PLUS_2': 'D+2 (2영업일)',
      'D_PLUS_3': 'D+3 (3영업일)',
      'REALTIME': '실시간',
      'WEEKLY': '주간',
      'MONTHLY': '월간'
    };
    return labels[cycle] || cycle;
  }

  function formatDate(dateStr: string): string {
    try {
      return format(new Date(dateStr), 'MM-dd');
    } catch {
      return dateStr;
    }
  }

  function formatFullDate(dateStr: string): string {
    try {
      return format(new Date(dateStr), 'yyyy-MM-dd');
    } catch {
      return dateStr;
    }
  }

  function setDateRange(days: number) {
    const end = new Date();
    const start = new Date();
    start.setDate(end.getDate() - days);
    startDate = format(start, 'yyyy/MM/dd');
    endDate = format(end, 'yyyy/MM/dd');
  }

  async function searchMerchants() {
    if (!searchQuery.trim() || !tenantStore.current) return;

    searching = true;
    showSearchResults = true;
    try {
      const response = await merchantApi.getMerchants({ search: searchQuery, size: 10 });
      if (response.success && response.data) {
        searchResults = response.data.content || [];
      } else {
        searchResults = [];
      }
    } catch (err) {
      console.error('Search error:', err);
      searchResults = [];
    } finally {
      searching = false;
    }
  }

  function selectMerchant(merchant: MerchantDto) {
    selectedMerchant = merchant;
    searchResults = [];
    showSearchResults = false;
    searchQuery = '';
    if (startDate && endDate) loadStatement();
  }

  function clearMerchant() {
    selectedMerchant = null;
    statement = null;
    error = null;
  }

  async function loadStatement() {
    if (!selectedMerchant || !startDate || !endDate || !tenantStore.current) return;

    loading = true;
    error = null;
    try {
      const response = await settlementApi.getMerchantStatement({
        merchantId: selectedMerchant.id,
        startDate,
        endDate
      });
      if (response.success && response.data) {
        statement = response.data;
      } else {
        error = response.error?.message || '정산내역서를 불러올 수 없습니다.';
        statement = null;
      }
    } catch (err) {
      error = err instanceof Error ? err.message : '정산내역서를 불러올 수 없습니다.';
      statement = null;
    } finally {
      loading = false;
    }
  }

  function handleSearch() {
    if (selectedMerchant && startDate && endDate) {
      loadStatement();
    }
  }

  function handlePrint() {
    window.print();
  }

  function handleSearchKeydown(e: KeyboardEvent) {
    if (e.key === 'Enter') searchMerchants();
  }

  let dailyTotals = $derived(statement ? {
    transactionCount: statement.dailyDetails.reduce((sum: number, d: DailyStatementRow) => sum + d.transactionCount, 0),
    approvalAmount: statement.dailyDetails.reduce((sum: number, d: DailyStatementRow) => sum + d.approvalAmount, 0),
    cancelAmount: statement.dailyDetails.reduce((sum: number, d: DailyStatementRow) => sum + d.cancelAmount, 0),
    feeAmount: statement.dailyDetails.reduce((sum: number, d: DailyStatementRow) => sum + d.feeAmount, 0),
    netAmount: statement.dailyDetails.reduce((sum: number, d: DailyStatementRow) => sum + d.netAmount, 0)
  } : null);
</script>

<div class="flex flex-col gap-6 p-6 max-w-[1400px] mx-auto">
  <header class="print:hidden">
    <h1 class="text-2xl font-bold text-foreground tracking-tight">가맹점 정산내역서</h1>
    <p class="text-sm text-muted-foreground mt-1">가맹점을 선택하고 기간별 정산내역서를 조회합니다</p>
  </header>

  <div class="bg-muted/50 border border-border rounded-xl p-5 space-y-4 print:hidden">
    <div class="flex flex-col gap-3">
      <div class="flex flex-wrap items-end gap-3">
        <div class="flex flex-col gap-1.5 flex-1 min-w-[200px] max-w-[400px]">
          <Label>가맹점 검색</Label>
          <div class="flex gap-2">
            <Input
              type="text"
              placeholder="가맹점명 또는 코드 입력"
              bind:value={searchQuery}
              onkeydown={handleSearchKeydown}
              class="h-10"
            />
            <Button variant="outline" onclick={searchMerchants} disabled={searching}>
              {#if searching}
                <svg class="w-4 h-4 animate-spin" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M12 2v4m0 12v4m-6.93-2.93l2.83-2.83m8.2-8.2l2.83-2.83M2 12h4m12 0h4m-2.93 6.93l-2.83-2.83m-8.2-8.2L4.93 4.93" />
                </svg>
              {:else}
                검색
              {/if}
            </Button>
          </div>
        </div>

        {#if selectedMerchant}
          <div class="flex items-center gap-2">
            <Badge variant="secondary" class="h-8 px-3 text-sm gap-1.5">
              <span class="font-semibold">{selectedMerchant.name}</span>
              <span class="text-muted-foreground">({selectedMerchant.merchantCode})</span>
              <button
                type="button"
                class="ml-1 hover:text-destructive transition-colors"
                onclick={clearMerchant}
                aria-label="선택 해제"
              >
                <svg class="w-3.5 h-3.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
                  <path d="M18 6L6 18M6 6l12 12" />
                </svg>
              </button>
            </Badge>
          </div>
        {/if}
      </div>

      {#if showSearchResults && (searchResults.length > 0 || searching)}
        <div class="bg-background border border-border rounded-lg shadow-lg overflow-hidden max-w-[600px]">
          {#if searching}
            <div class="p-4 text-center text-sm text-muted-foreground">검색 중...</div>
          {:else}
            <Table>
              <TableHeader>
                <TableRow class="bg-muted/50">
                  <TableHead class="text-xs font-semibold">가맹점명</TableHead>
                  <TableHead class="text-xs font-semibold">가맹점코드</TableHead>
                  <TableHead class="text-xs font-semibold">사업자번호</TableHead>
                  <TableHead class="text-xs font-semibold">상태</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {#each searchResults as merchant (merchant.id)}
                  <TableRow
                    class="cursor-pointer hover:bg-primary/5 transition-colors"
                    onclick={() => selectMerchant(merchant)}
                    onkeydown={(e) => e.key === 'Enter' && selectMerchant(merchant)}
                    role="button"
                    tabindex={0}
                  >
                    <TableCell class="font-medium">{merchant.name}</TableCell>
                    <TableCell class="font-mono text-sm">{merchant.merchantCode}</TableCell>
                    <TableCell class="text-muted-foreground">{formatBusinessNumber(merchant.businessNumber)}</TableCell>
                    <TableCell>
                      <Badge variant={merchant.status === 'ACTIVE' ? 'default' : 'secondary'} class="text-xs">
                        {merchant.status === 'ACTIVE' ? '정상' : merchant.status}
                      </Badge>
                    </TableCell>
                  </TableRow>
                {/each}
              </TableBody>
            </Table>
            {#if searchResults.length === 0}
              <div class="p-4 text-center text-sm text-muted-foreground">검색 결과가 없습니다</div>
            {/if}
          {/if}
        </div>
      {/if}
    </div>

    <Separator />

    <div class="flex flex-wrap items-end gap-3">
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

      <div class="ml-auto">
        <Button
          onclick={handleSearch}
          disabled={!selectedMerchant || !startDate || !endDate || loading}
        >
          {#if loading}
            <svg class="w-4 h-4 mr-2 animate-spin" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M12 2v4m0 12v4m-6.93-2.93l2.83-2.83m8.2-8.2l2.83-2.83M2 12h4m12 0h4m-2.93 6.93l-2.83-2.83m-8.2-8.2L4.93 4.93" />
            </svg>
          {/if}
          조회
        </Button>
      </div>
    </div>
  </div>

  {#if loading}
    <Card>
      <CardContent class="p-8 space-y-6">
        <div class="flex justify-center">
          <Skeleton class="h-8 w-48" />
        </div>
        <div class="grid grid-cols-2 gap-4">
          <Skeleton class="h-5 w-full" />
          <Skeleton class="h-5 w-full" />
          <Skeleton class="h-5 w-full" />
          <Skeleton class="h-5 w-full" />
        </div>
        <Skeleton class="h-32 w-full" />
        <Skeleton class="h-48 w-full" />
      </CardContent>
    </Card>
  {/if}

  {#if error && !loading}
    <Card class="border-destructive/50">
      <CardContent class="p-8 text-center">
        <div class="flex flex-col items-center gap-3">
          <svg class="w-12 h-12 text-destructive/50" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
            <circle cx="12" cy="12" r="10" />
            <path d="M12 8v4m0 4h.01" />
          </svg>
          <p class="text-destructive font-medium">{error}</p>
          <Button variant="outline" size="sm" onclick={loadStatement}>다시 시도</Button>
        </div>
      </CardContent>
    </Card>
  {/if}

  {#if !loading && !error && !statement && selectedMerchant}
    <Card>
      <CardContent class="p-12 text-center">
        <div class="flex flex-col items-center gap-3">
          <svg class="w-16 h-16 text-muted-foreground/40" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
            <path d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
          </svg>
          <p class="text-muted-foreground font-medium">기간을 선택하고 조회 버튼을 클릭하세요</p>
        </div>
      </CardContent>
    </Card>
  {/if}

  {#if !loading && !error && !statement && !selectedMerchant}
    <Card>
      <CardContent class="p-12 text-center">
        <div class="flex flex-col items-center gap-3">
          <svg class="w-16 h-16 text-muted-foreground/40" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
            <path d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
          </svg>
          <p class="text-muted-foreground font-medium">가맹점을 먼저 검색하여 선택하세요</p>
        </div>
      </CardContent>
    </Card>
  {/if}

  {#if statement && !loading}
    <div class="bg-background border border-border rounded-xl shadow-sm print:shadow-none print:border-0 print:rounded-none">
      <div class="p-8 print:p-4">
        <div class="flex justify-between items-start mb-6">
          <div></div>
          <h2 class="text-2xl font-bold text-foreground tracking-[0.3em] text-center">정 산 내 역 서</h2>
          <Button variant="outline" size="sm" onclick={handlePrint} class="print:hidden">
            <svg class="w-4 h-4 mr-1.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M6 9V2h12v7M6 18H4a2 2 0 01-2-2v-5a2 2 0 012-2h16a2 2 0 012 2v5a2 2 0 01-2 2h-2" />
              <rect x="6" y="14" width="12" height="8" />
            </svg>
            인쇄
          </Button>
        </div>

        <div class="text-sm text-muted-foreground text-right mb-6">
          조회기간: {formatFullDate(statement.periodStart)} ~ {formatFullDate(statement.periodEnd)}
        </div>

        <Separator class="mb-6" />

        <div class="mb-8">
          <h3 class="text-sm font-bold text-foreground uppercase tracking-wide mb-4 flex items-center gap-2">
            <svg class="w-4 h-4 text-primary" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16m14 0h2m-2 0h-5m-9 0H3m2 0h5M9 7h1m-1 4h1m4-4h1m-1 4h1m-5 10v-5a1 1 0 011-1h2a1 1 0 011 1v5m-4 0h4" />
            </svg>
            가맹점 정보
          </h3>
          <div class="grid grid-cols-1 md:grid-cols-2 gap-x-12 gap-y-2.5 bg-muted/30 rounded-lg p-5 border border-border/50">
            <div class="flex items-center gap-3">
              <span class="text-sm text-muted-foreground min-w-[80px]">상호명</span>
              <span class="text-sm font-semibold text-foreground">{statement.merchantName}</span>
            </div>
            <div class="flex items-center gap-3">
              <span class="text-sm text-muted-foreground min-w-[80px]">사업자번호</span>
              <span class="text-sm font-medium text-foreground font-mono">{formatBusinessNumber(statement.businessNumber)}</span>
            </div>
            <div class="flex items-center gap-3">
              <span class="text-sm text-muted-foreground min-w-[80px]">가맹점코드</span>
              <span class="text-sm font-medium text-foreground font-mono">{statement.merchantCode}</span>
            </div>
            <div class="flex items-center gap-3">
              <span class="text-sm text-muted-foreground min-w-[80px]">대표자</span>
              <span class="text-sm font-medium text-foreground">{statement.representativeName || '-'}</span>
            </div>
            <div class="flex items-center gap-3">
              <span class="text-sm text-muted-foreground min-w-[80px]">정산주기</span>
              <Badge variant="outline" class="text-xs">{getCycleLabel(statement.settlementCycle)}</Badge>
            </div>
            <div class="flex items-center gap-3">
              <span class="text-sm text-muted-foreground min-w-[80px]">정산계좌</span>
              <span class="text-sm font-medium text-foreground">
                {#if statement.bankName}
                  {statement.bankName} {formatAccountNumber(statement.accountNumber)}
                  {#if statement.accountHolder}
                    ({statement.accountHolder})
                  {/if}
                {:else}
                  -
                {/if}
              </span>
            </div>
          </div>
        </div>

        <div class="mb-8">
          <h3 class="text-sm font-bold text-foreground uppercase tracking-wide mb-4 flex items-center gap-2">
            <svg class="w-4 h-4 text-primary" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M9 7h6m-6 4h6m-3 4v4m-6-8a9 9 0 1118 0 9 9 0 01-18 0z" />
            </svg>
            정산 요약
          </h3>
          <div class="bg-muted/30 rounded-lg border border-border/50 overflow-hidden">
            <div class="p-5 space-y-3">
              <div class="flex justify-between items-center">
                <span class="text-sm text-muted-foreground">총 승인금액 ({statement.summary.totalApprovalCount.toLocaleString()}건)</span>
                <span class="text-sm font-semibold text-foreground">{formatCurrency(statement.summary.totalApprovalAmount)}</span>
              </div>
              <div class="flex justify-between items-center">
                <span class="text-sm text-muted-foreground">총 취소금액 ({statement.summary.totalCancelCount.toLocaleString()}건)</span>
                <span class="text-sm font-semibold text-destructive">{formatCurrency(-Math.abs(statement.summary.totalCancelAmount))}</span>
              </div>
              <Separator />
              <div class="flex justify-between items-center">
                <span class="text-sm text-muted-foreground">순 거래금액 ({statement.summary.transactionCount.toLocaleString()}건)</span>
                <span class="text-sm font-semibold text-foreground">{formatCurrency(statement.summary.grossAmount)}</span>
              </div>
              <div class="flex justify-between items-center">
                <span class="text-sm text-muted-foreground">수수료 ({formatPercent(statement.summary.feeRate)})</span>
                <span class="text-sm font-semibold text-destructive">{formatCurrency(-Math.abs(statement.summary.feeAmount))}</span>
              </div>
            </div>
            <div class="bg-primary/5 border-t-2 border-primary/20 p-5">
              <div class="flex justify-between items-center">
                <span class="text-base font-bold text-foreground">정산금액</span>
                <span class="text-2xl font-bold text-primary">{formatCurrency(statement.summary.netAmount)}</span>
              </div>
            </div>
          </div>
        </div>

        <div class="mb-8">
          <h3 class="text-sm font-bold text-foreground uppercase tracking-wide mb-4 flex items-center gap-2">
            <svg class="w-4 h-4 text-primary" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z" />
            </svg>
            일자별 상세
          </h3>
          <div class="bg-background border border-border rounded-lg overflow-hidden">
            <Table>
              <TableHeader>
                <TableRow class="bg-muted/50">
                  <TableHead class="text-xs font-bold">정산일</TableHead>
                  <TableHead class="text-xs font-bold">거래일</TableHead>
                  <TableHead class="text-xs font-bold text-right">건수</TableHead>
                  <TableHead class="text-xs font-bold text-right">승인금액</TableHead>
                  <TableHead class="text-xs font-bold text-right">취소금액</TableHead>
                  <TableHead class="text-xs font-bold text-right">수수료</TableHead>
                  <TableHead class="text-xs font-bold text-right">정산금액</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {#if statement.dailyDetails.length === 0}
                  <TableRow>
                    <TableCell colspan={7} class="py-8 text-center text-muted-foreground">
                      해당 기간의 정산 내역이 없습니다
                    </TableCell>
                  </TableRow>
                {:else}
                  {#each statement.dailyDetails as row (row.settlementDate + row.transactionDate)}
                    <TableRow class="even:bg-muted/20 hover:bg-muted/40 transition-colors">
                      <TableCell class="font-mono text-sm">{formatDate(row.settlementDate)}</TableCell>
                      <TableCell class="font-mono text-sm text-muted-foreground">{formatDate(row.transactionDate)}</TableCell>
                      <TableCell class="text-right font-medium">{row.transactionCount.toLocaleString()}</TableCell>
                      <TableCell class="text-right font-medium">{formatCurrency(row.approvalAmount)}</TableCell>
                      <TableCell class="text-right {row.cancelAmount !== 0 ? 'text-destructive' : 'text-muted-foreground'}">
                        {row.cancelAmount !== 0 ? formatCurrency(-Math.abs(row.cancelAmount)) : formatCurrency(0)}
                      </TableCell>
                      <TableCell class="text-right text-destructive">
                        {formatCurrency(-Math.abs(row.feeAmount))}
                      </TableCell>
                      <TableCell class="text-right font-bold text-primary">{formatCurrency(row.netAmount)}</TableCell>
                    </TableRow>
                  {/each}

                  {#if dailyTotals}
                    <TableRow class="font-bold bg-muted/50 border-t-2 border-border">
                      <TableCell class="text-sm">합계</TableCell>
                      <TableCell></TableCell>
                      <TableCell class="text-right">{dailyTotals.transactionCount.toLocaleString()}</TableCell>
                      <TableCell class="text-right">{formatCurrency(dailyTotals.approvalAmount)}</TableCell>
                      <TableCell class="text-right text-destructive">
                        {dailyTotals.cancelAmount !== 0 ? formatCurrency(-Math.abs(dailyTotals.cancelAmount)) : formatCurrency(0)}
                      </TableCell>
                      <TableCell class="text-right text-destructive">{formatCurrency(-Math.abs(dailyTotals.feeAmount))}</TableCell>
                      <TableCell class="text-right text-primary">{formatCurrency(dailyTotals.netAmount)}</TableCell>
                    </TableRow>
                  {/if}
                {/if}
              </TableBody>
            </Table>
          </div>
        </div>

        <div class="mb-8">
          <h3 class="text-sm font-bold text-foreground uppercase tracking-wide mb-4 flex items-center gap-2">
            <svg class="w-4 h-4 text-primary" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
            수수료 정보
          </h3>
          <div class="bg-muted/30 rounded-lg p-5 border border-border/50 space-y-2">
            <div class="flex items-center gap-2 text-sm">
              <span class="text-muted-foreground">•</span>
              <span class="text-muted-foreground">적용 수수료율:</span>
              <span class="font-semibold text-foreground">{formatPercent(statement.summary.feeRate)}</span>
            </div>
            <div class="flex items-center gap-2 text-sm">
              <span class="text-muted-foreground">•</span>
              <span class="text-muted-foreground">수수료 계산:</span>
              <span class="font-medium text-foreground">순거래금액 × 수수료율</span>
            </div>
          </div>
        </div>

        <Separator class="mb-6" />

        <div class="flex justify-end gap-3 print:hidden">
          <Button variant="outline" onclick={handlePrint}>
            <svg class="w-4 h-4 mr-1.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M6 9V2h12v7M6 18H4a2 2 0 01-2-2v-5a2 2 0 012-2h16a2 2 0 012 2v5a2 2 0 01-2 2h-2" />
              <rect x="6" y="14" width="12" height="8" />
            </svg>
            인쇄
          </Button>
        </div>
      </div>
    </div>
  {/if}
</div>
