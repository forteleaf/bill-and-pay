<script lang="ts">
  import { settlementApi, type OrganizationSettlementParams } from '../../lib/settlementApi';
  import type { OrganizationSettlementSummary } from '../../types/api';
  import { format } from 'date-fns';
  import { Button } from '$lib/components/ui/button';
  import { Input } from '$lib/components/ui/input';
  import { Label } from '$lib/components/ui/label';
  import { Badge } from '$lib/components/ui/badge';
  import { Card, CardContent } from '$lib/components/ui/card';
  import { DateRangePicker } from '$lib/components/ui/date-range-picker';
  import {
    Table,
    TableBody,
    TableCell,
    TableHead,
    TableHeader,
    TableRow
  } from '$lib/components/ui/table';
  import OrganizationSettlementDetailModal from '../../components/OrganizationSettlementDetailModal.svelte';

  let organizations = $state<OrganizationSettlementSummary[]>([]);
  let loading = $state(false);
  let error = $state<string | null>(null);
  let selectedOrg = $state<OrganizationSettlementSummary | null>(null);
  let showDetailModal = $state(false);

  let orgTypeFilter = $state<string>('');
  let searchQuery = $state('');
  let startDate = $state('');
  let endDate = $state('');

  const ORG_TYPE_TABS = [
    { value: '', label: '전체' },
    { value: 'DISTRIBUTOR', label: '총판' },
    { value: 'AGENCY', label: '대리점' },
    { value: 'DEALER', label: '딜러' },
    { value: 'SELLER', label: '셀러' },
    { value: 'VENDOR', label: '벤더' }
  ];

  const STATUS_LABELS: Record<string, string> = {
    COMPLETED: '완료',
    PENDING: '대기',
    FAILED: '실패'
  };

  function getStatusVariant(status?: string): 'default' | 'secondary' | 'destructive' | 'outline' | 'success' | 'warning' {
    const variants: Record<string, 'default' | 'secondary' | 'destructive' | 'outline' | 'success' | 'warning'> = {
      COMPLETED: 'success',
      PENDING: 'warning',
      FAILED: 'destructive'
    };
    return status ? (variants[status] || 'outline') : 'outline';
  }

  function formatCurrency(amount: number): string {
    return new Intl.NumberFormat('ko-KR', {
      style: 'currency',
      currency: 'KRW'
    }).format(amount);
  }

  function formatPhone(phone?: string): string {
    if (!phone) return '-';
    const cleaned = phone.replace(/\D/g, '');
    if (cleaned.length === 11) {
      return `${cleaned.slice(0, 3)}-${cleaned.slice(3, 7)}-${cleaned.slice(7)}`;
    } else if (cleaned.length === 10) {
      return `${cleaned.slice(0, 3)}-${cleaned.slice(3, 6)}-${cleaned.slice(6)}`;
    } else if (cleaned.length === 9) {
      return `${cleaned.slice(0, 2)}-${cleaned.slice(2, 5)}-${cleaned.slice(5)}`;
    }
    return phone;
  }

  function setDateRange(days: number) {
    const end = new Date();
    const start = new Date();
    start.setDate(end.getDate() - days);
    startDate = format(start, 'yyyy/MM/dd');
    endDate = format(end, 'yyyy/MM/dd');
  }

  async function loadData() {
    if (loading) return;
    
    loading = true;
    error = null;

    try {
      const params: OrganizationSettlementParams = {
        orgType: orgTypeFilter || undefined,
        search: searchQuery || undefined,
        startDate: startDate || undefined,
        endDate: endDate || undefined
      };

      const response = await settlementApi.getSettlementsByOrganization(params);

      if (response.success && response.data) {
        organizations = response.data;
      } else {
        error = response.error?.message || '데이터를 불러올 수 없습니다.';
        organizations = [];
      }
    } catch (err) {
      error = err instanceof Error ? err.message : '데이터를 불러올 수 없습니다.';
      organizations = [];
    } finally {
      loading = false;
    }
  }

  function handleSearch() {
    loadData();
  }

  function handleReset() {
    orgTypeFilter = '';
    searchQuery = '';
    startDate = '';
    endDate = '';
    loadData();
  }

  function handleTabChange(value: string) {
    orgTypeFilter = value;
    loadData();
  }

  function handleRowClick(org: OrganizationSettlementSummary) {
    selectedOrg = org;
    showDetailModal = true;
  }

  function closeDetailModal() {
    showDetailModal = false;
    selectedOrg = null;
  }

  let summary = $derived({
    approvalAmount: organizations.reduce((sum, o) => sum + o.approvalAmount, 0),
    approvalCount: organizations.reduce((sum, o) => sum + o.approvalCount, 0),
    cancelAmount: organizations.reduce((sum, o) => sum + o.cancelAmount, 0),
    cancelCount: organizations.reduce((sum, o) => sum + o.cancelCount, 0),
    netPaymentAmount: organizations.reduce((sum, o) => sum + o.netPaymentAmount, 0),
    netPaymentCount: organizations.reduce((sum, o) => sum + o.approvalCount - o.cancelCount, 0),
    totalFee: organizations.reduce((sum, o) => sum + o.merchantFeeAmount + o.orgFeeAmount, 0),
    completedCount: organizations.reduce((sum, o) => sum + o.completedCount, 0),
    pendingCount: organizations.reduce((sum, o) => sum + o.pendingCount, 0),
    failedCount: organizations.reduce((sum, o) => sum + o.failedCount, 0)
  });

  let prevParams = $state<string>('');
  $effect(() => {
    const currentParams = JSON.stringify({ orgTypeFilter, searchQuery, startDate, endDate });
    if (prevParams === '') {
      prevParams = currentParams;
      loadData();
    }
  });
</script>

<div class="flex flex-col gap-6 p-6 max-w-[1800px] mx-auto">
  <header class="flex justify-between items-center">
    <div class="flex items-baseline gap-4">
      <h1 class="text-2xl font-bold text-foreground tracking-tight">영업점 정산내역</h1>
      <span class="text-sm text-muted-foreground font-medium">총 {organizations.length.toLocaleString()}건</span>
    </div>
  </header>

  <div class="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-6 gap-3">
    <Card class="border-l-2 border-l-emerald-500">
      <CardContent class="p-3">
        <p class="text-[10px] font-medium text-muted-foreground uppercase tracking-wide">승인금액</p>
        <p class="text-base font-semibold text-emerald-600 mt-0.5">{formatCurrency(summary.approvalAmount)}</p>
        <p class="text-[10px] text-muted-foreground">{summary.approvalCount.toLocaleString()}건</p>
      </CardContent>
    </Card>

    <Card class="border-l-2 border-l-rose-500">
      <CardContent class="p-3">
        <p class="text-[10px] font-medium text-muted-foreground uppercase tracking-wide">취소금액</p>
        <p class="text-base font-semibold text-rose-600 mt-0.5">{formatCurrency(summary.cancelAmount)}</p>
        <p class="text-[10px] text-muted-foreground">{summary.cancelCount.toLocaleString()}건</p>
      </CardContent>
    </Card>

    <Card class="border-l-2 border-l-blue-500">
      <CardContent class="p-3">
        <p class="text-[10px] font-medium text-muted-foreground uppercase tracking-wide">결제금액</p>
        <p class="text-base font-semibold text-blue-600 mt-0.5">{formatCurrency(summary.netPaymentAmount)}</p>
        <p class="text-[10px] text-muted-foreground">{summary.netPaymentCount.toLocaleString()}건</p>
      </CardContent>
    </Card>

    <Card class="border-l-2 border-l-violet-500">
      <CardContent class="p-3">
        <p class="text-[10px] font-medium text-muted-foreground uppercase tracking-wide">총 수수료</p>
        <p class="text-base font-semibold text-violet-600 mt-0.5">{formatCurrency(summary.totalFee)}</p>
      </CardContent>
    </Card>

    <Card class="border-l-2 border-l-amber-500">
      <CardContent class="p-3">
        <p class="text-[10px] font-medium text-muted-foreground uppercase tracking-wide">정산현황</p>
        <div class="flex items-center gap-1.5 mt-1">
          <Badge variant="success" class="h-5 px-1.5 text-[10px]">{summary.completedCount}</Badge>
          <Badge variant="warning" class="h-5 px-1.5 text-[10px]">{summary.pendingCount}</Badge>
          <Badge variant="destructive" class="h-5 px-1.5 text-[10px]">{summary.failedCount}</Badge>
        </div>
        <p class="text-[10px] text-muted-foreground mt-0.5">완료/대기/실패</p>
      </CardContent>
    </Card>

    <Card class="border-l-2 border-l-slate-500">
      <CardContent class="p-3">
        <p class="text-[10px] font-medium text-muted-foreground uppercase tracking-wide">영업점수</p>
        <p class="text-base font-semibold text-foreground mt-0.5">{organizations.length.toLocaleString()}</p>
        <p class="text-[10px] text-muted-foreground">조회된 영업점</p>
      </CardContent>
    </Card>
  </div>

  <div class="bg-muted/50 border border-border rounded-xl p-5 space-y-4">
    <div class="flex gap-1 p-1 bg-background border border-border rounded-lg w-fit">
      {#each ORG_TYPE_TABS as tab}
        <button
          type="button"
          class="px-4 py-2 text-sm font-medium rounded-md transition-all duration-200 {orgTypeFilter === tab.value 
            ? 'bg-primary text-primary-foreground shadow-sm' 
            : 'text-muted-foreground hover:text-foreground hover:bg-muted'}"
          onclick={() => handleTabChange(tab.value)}
        >
          {tab.label}
        </button>
      {/each}
    </div>

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

      <div class="flex flex-col gap-1.5 flex-1 min-w-[200px] max-w-[300px]">
        <Label>영업점 검색</Label>
        <Input
          type="text"
          placeholder="영업점명 또는 코드 검색"
          bind:value={searchQuery}
          class="h-10"
        />
      </div>

      <div class="flex gap-2 ml-auto">
        <Button variant="outline" onclick={handleReset}>초기화</Button>
        <Button onclick={handleSearch}>조회</Button>
      </div>
    </div>
  </div>

  <div class="bg-background border border-border rounded-xl overflow-hidden shadow-sm">
    <Table>
      <TableHeader>
        <TableRow class="bg-gradient-to-b from-muted/50 to-muted">
          <TableHead class="w-[60px] text-center font-bold text-xs uppercase tracking-wide">NO</TableHead>
          <TableHead class="w-[100px] font-bold text-xs uppercase tracking-wide">영업점코드</TableHead>
          <TableHead class="min-w-[140px] font-bold text-xs uppercase tracking-wide">영업점명</TableHead>
          <TableHead class="w-[100px] font-bold text-xs uppercase tracking-wide">대표자</TableHead>
          <TableHead class="w-[130px] font-bold text-xs uppercase tracking-wide">연락처</TableHead>
          <TableHead class="w-[80px] text-right font-bold text-xs uppercase tracking-wide">가맹점수</TableHead>
          <TableHead class="w-[130px] text-right font-bold text-xs uppercase tracking-wide">결제금액</TableHead>
          <TableHead class="w-[120px] text-right font-bold text-xs uppercase tracking-wide">가맹점수수료</TableHead>
          <TableHead class="w-[120px] text-right font-bold text-xs uppercase tracking-wide">영업점수수료</TableHead>
          <TableHead class="w-[90px] text-center font-bold text-xs uppercase tracking-wide">상태</TableHead>
        </TableRow>
      </TableHeader>
      <TableBody>
        {#if loading}
          {#each Array(8) as _}
            <TableRow>
              <TableCell><div class="h-4 w-8 bg-muted animate-pulse rounded mx-auto"></div></TableCell>
              <TableCell><div class="h-4 w-20 bg-muted animate-pulse rounded"></div></TableCell>
              <TableCell><div class="h-4 w-28 bg-muted animate-pulse rounded"></div></TableCell>
              <TableCell><div class="h-4 w-16 bg-muted animate-pulse rounded"></div></TableCell>
              <TableCell><div class="h-4 w-24 bg-muted animate-pulse rounded"></div></TableCell>
              <TableCell class="text-right"><div class="h-4 w-10 bg-muted animate-pulse rounded ml-auto"></div></TableCell>
              <TableCell class="text-right"><div class="h-4 w-20 bg-muted animate-pulse rounded ml-auto"></div></TableCell>
              <TableCell class="text-right"><div class="h-4 w-18 bg-muted animate-pulse rounded ml-auto"></div></TableCell>
              <TableCell class="text-right"><div class="h-4 w-18 bg-muted animate-pulse rounded ml-auto"></div></TableCell>
              <TableCell class="text-center"><div class="h-4 w-12 bg-muted animate-pulse rounded mx-auto"></div></TableCell>
            </TableRow>
          {/each}
        {:else if error}
          <TableRow>
            <TableCell colspan={10} class="py-16 text-center">
              <div class="flex flex-col items-center gap-3">
                <svg class="w-16 h-16 text-destructive/50" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                  <circle cx="12" cy="12" r="10"/>
                  <path d="M12 8v4m0 4h.01"/>
                </svg>
                <p class="text-base font-semibold text-destructive">{error}</p>
                <Button variant="outline" onclick={loadData}>다시 시도</Button>
              </div>
            </TableCell>
          </TableRow>
        {:else if organizations.length === 0}
          <TableRow>
            <TableCell colspan={10} class="py-16 text-center">
              <div class="flex flex-col items-center gap-3">
                <svg class="w-16 h-16 text-muted-foreground/50" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                  <path d="M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16m14 0h2m-2 0h-5m-9 0H3m2 0h5M9 7h1m-1 4h1m4-4h1m-1 4h1m-5 10v-5a1 1 0 011-1h2a1 1 0 011 1v5m-4 0h4"/>
                </svg>
                <p class="text-base font-semibold text-muted-foreground">정산내역이 없습니다</p>
                <p class="text-sm text-muted-foreground/70">다른 검색 조건을 시도해 보세요</p>
              </div>
            </TableCell>
          </TableRow>
        {:else}
          {#each organizations as org, index (org.organizationId)}
            <TableRow 
              class="cursor-pointer hover:bg-primary/5 transition-colors even:bg-muted/30 {selectedOrg?.organizationId === org.organizationId ? 'bg-primary/10 ring-1 ring-primary/30' : ''}"
              onclick={() => handleRowClick(org)}
              onkeydown={(e) => e.key === 'Enter' && handleRowClick(org)}
              role="button"
              tabindex={0}
            >
              <TableCell class="text-center text-muted-foreground font-mono text-sm">
                {index + 1}
              </TableCell>
              <TableCell>
                <span class="font-mono text-sm font-medium">{org.orgCode}</span>
              </TableCell>
              <TableCell>
                <div class="flex flex-col">
                  <span class="font-medium">{org.orgName}</span>
                  <span class="text-xs text-muted-foreground">{org.orgType}</span>
                </div>
              </TableCell>
              <TableCell class="text-muted-foreground">
                {org.representativeName || '-'}
              </TableCell>
              <TableCell class="text-muted-foreground font-mono text-sm">
                {formatPhone(org.mainPhone)}
              </TableCell>
              <TableCell class="text-right font-semibold">
                {org.merchantCount.toLocaleString()}
              </TableCell>
              <TableCell class="text-right font-bold text-blue-600">
                {formatCurrency(org.netPaymentAmount)}
              </TableCell>
              <TableCell class="text-right text-muted-foreground">
                {formatCurrency(org.merchantFeeAmount)}
              </TableCell>
              <TableCell class="text-right text-violet-600 font-medium">
                {formatCurrency(org.orgFeeAmount)}
              </TableCell>
              <TableCell class="text-center">
                <Badge variant={getStatusVariant(org.primaryStatus)}>
                  {org.primaryStatus ? (STATUS_LABELS[org.primaryStatus] || org.primaryStatus) : '-'}
                </Badge>
              </TableCell>
            </TableRow>
          {/each}
        {/if}
      </TableBody>
    </Table>

    {#if !loading && organizations.length > 0}
      <div class="py-4 px-6 text-sm text-muted-foreground border-t border-border bg-muted/30 flex justify-between items-center">
        <span>총 {organizations.length.toLocaleString()}개 영업점</span>
        <span class="text-xs">
          결제금액 합계: <strong class="text-foreground">{formatCurrency(summary.netPaymentAmount)}</strong>
        </span>
      </div>
    {/if}
  </div>
</div>

<OrganizationSettlementDetailModal
  bind:show={showDetailModal}
  organization={selectedOrg}
  {startDate}
  {endDate}
  onClose={closeDetailModal}
/>
