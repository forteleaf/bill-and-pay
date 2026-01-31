<script lang="ts">
  import { onMount } from 'svelte';
  import { branchApi } from '../../lib/branchApi';
  import { tabStore } from '../../lib/tabStore';
  import {
    type Branch,
    type BranchListParams,
    BRANCH_TYPE_LABELS,
    OrgType,
    OrgStatus
  } from '../../types/branch';
  import { format } from 'date-fns';
  import { Button } from '$lib/components/ui/button';
  import { Input } from '$lib/components/ui/input';
  import { Label } from '$lib/components/ui/label';
  import { Badge } from '$lib/components/ui/badge';
  import {
    Table,
    TableBody,
    TableCell,
    TableHead,
    TableHeader,
    TableRow
  } from '$lib/components/ui/table';

  let branches = $state<Branch[]>([]);
  let loading = $state(false);
  let initialLoading = $state(true);
  let hasMore = $state(true);
  let page = $state(0);
  const pageSize = 20;
  let totalCount = $state(0);

  let searchQuery = $state('');
  let typeFilter = $state<OrgType | ''>('');
  let statusFilter = $state<OrgStatus | ''>('');
  let startDate = $state('');
  let endDate = $state('');

  let sentinelEl: HTMLDivElement;

  const STATUS_LABELS: Record<OrgStatus, string> = {
    [OrgStatus.ACTIVE]: '정상',
    [OrgStatus.SUSPENDED]: '정지',
    [OrgStatus.TERMINATED]: '해지'
  };

  function getStatusVariant(status: string): 'default' | 'secondary' | 'destructive' | 'outline' {
    switch (status) {
      case OrgStatus.ACTIVE:
        return 'default';
      case OrgStatus.SUSPENDED:
        return 'secondary';
      case OrgStatus.TERMINATED:
        return 'destructive';
      default:
        return 'outline';
    }
  }

  function formatDate(dateStr?: string): string {
    if (!dateStr) return '-';
    try {
      return format(new Date(dateStr), 'yyyy-MM-dd');
    } catch {
      return '-';
    }
  }

  async function loadBranches(reset: boolean = false) {
    if (loading) return;
    if (!reset && !hasMore) return;

    loading = true;

    if (reset) {
      page = 0;
      branches = [];
      hasMore = true;
    }

    try {
      const params: BranchListParams = {
        page,
        size: pageSize,
        type: typeFilter || undefined,
        status: statusFilter || undefined,
        search: searchQuery.trim() || undefined,
        startDate: startDate || undefined,
        endDate: endDate || undefined
      };

      const response = await branchApi.getBranches(params);

      if (response.success && response.data) {
        const newData = response.data.content || [];
        if (reset) {
          branches = newData;
        } else {
          branches = [...branches, ...newData];
        }
        totalCount = response.data.totalElements || 0;
        
        const isLastPage = newData.length < pageSize;
        const apiHasNext = response.data.hasNext;
        hasMore = apiHasNext === true && !isLastPage;
        
        if (newData.length > 0) {
          page++;
        }
      } else {
        hasMore = false;
      }
    } catch (err) {
      console.error('Failed to load branches:', err);
      hasMore = false;
    } finally {
      loading = false;
      initialLoading = false;
    }
  }

  function handleSearch() {
    loadBranches(true);
  }

  function handleReset() {
    searchQuery = '';
    typeFilter = '';
    statusFilter = '';
    startDate = '';
    endDate = '';
    loadBranches(true);
  }

  function setDateRange(days: number) {
    const end = new Date();
    const start = new Date();
    start.setDate(end.getDate() - days);
    startDate = format(start, 'yyyy-MM-dd');
    endDate = format(end, 'yyyy-MM-dd');
  }

  function handleRowClick(branch: Branch) {
    tabStore.openTab({
      id: `branch-${branch.id}`,
      title: branch.name,
      icon: '🏢',
      component: 'BranchDetail',
      closeable: true,
      props: { branchId: branch.id }
    });
  }

  onMount(() => {
    loadBranches(true);
  });

  $effect(() => {
    if (!sentinelEl) return;
    if (initialLoading) return;

    const observer = new IntersectionObserver(
      (entries) => {
        const entry = entries[0];
        if (entry.isIntersecting && !loading && hasMore && branches.length > 0) {
          loadBranches(false);
        }
      },
      { rootMargin: '100px', threshold: 0 }
    );

    observer.observe(sentinelEl);

    return () => observer.disconnect();
  });
</script>

<div class="flex flex-col gap-6 p-6 max-w-[1400px] mx-auto">
  <!-- Header -->
  <header class="flex justify-between items-center">
    <div class="flex items-baseline gap-4">
      <h1 class="text-2xl font-bold text-foreground tracking-tight">영업점 목록</h1>
      <span class="text-sm text-muted-foreground font-medium">총 {totalCount.toLocaleString()}개</span>
    </div>
  </header>

  <!-- Filter Bar -->
  <div class="bg-muted/50 border border-border rounded-xl p-5">
    <div class="flex flex-wrap gap-4 mb-4">
      <!-- Search -->
      <div class="flex flex-col gap-1.5 flex-1 min-w-60">
        <Label for="search">검색어</Label>
        <div class="relative flex items-center">
          <svg class="absolute left-3 w-4 h-4 text-muted-foreground pointer-events-none" viewBox="0 0 20 20" fill="currentColor">
            <path fill-rule="evenodd" d="M8 4a4 4 0 100 8 4 4 0 000-8zM2 8a6 6 0 1110.89 3.476l4.817 4.817a1 1 0 01-1.414 1.414l-4.816-4.816A6 6 0 012 8z" clip-rule="evenodd"/>
          </svg>
          <Input
            id="search"
            type="text"
            placeholder="영업점코드, 영업점명, 대표자..."
            value={searchQuery}
            oninput={(e) => searchQuery = e.currentTarget.value}
            onkeydown={(e) => e.key === 'Enter' && handleSearch()}
            class="pl-9"
          />
        </div>
      </div>

      <!-- Type Filter -->
      <div class="flex flex-col gap-1.5">
        <Label for="type">영업점유형</Label>
        <select
          id="type"
          bind:value={typeFilter}
          class="h-10 px-3 pr-8 rounded-md border border-input bg-background text-sm focus:outline-none focus:ring-2 focus:ring-ring min-w-[140px]"
        >
          <option value="">전체</option>
          {#each Object.values(OrgType) as type}
            <option value={type}>{BRANCH_TYPE_LABELS[type]}</option>
          {/each}
        </select>
      </div>

      <!-- Status Filter -->
      <div class="flex flex-col gap-1.5">
        <Label for="status">상태</Label>
        <select
          id="status"
          bind:value={statusFilter}
          class="h-10 px-3 pr-8 rounded-md border border-input bg-background text-sm focus:outline-none focus:ring-2 focus:ring-ring min-w-[140px]"
        >
          <option value="">전체</option>
          {#each Object.values(OrgStatus) as status}
            <option value={status}>{STATUS_LABELS[status]}</option>
          {/each}
        </select>
      </div>

      <!-- Date Range -->
      <div class="flex flex-row items-end gap-3 flex-wrap">
        <div class="flex flex-col gap-1.5">
          <Label for="startDate">기간</Label>
          <div class="flex items-center gap-2">
            <input id="startDate" type="date" bind:value={startDate} class="h-10 w-[140px] px-3 rounded-md border border-input bg-background text-sm focus:outline-none focus:ring-2 focus:ring-ring" />
            <span class="text-muted-foreground font-medium">~</span>
            <input id="endDate" type="date" bind:value={endDate} aria-label="종료일" class="h-10 w-[140px] px-3 rounded-md border border-input bg-background text-sm focus:outline-none focus:ring-2 focus:ring-ring" />
          </div>
        </div>
        <div class="flex gap-1">
          <Button variant="outline" size="sm" onclick={() => setDateRange(7)}>7일</Button>
          <Button variant="outline" size="sm" onclick={() => setDateRange(30)}>30일</Button>
          <Button variant="outline" size="sm" onclick={() => setDateRange(90)}>90일</Button>
        </div>
      </div>
    </div>

    <div class="flex justify-end gap-2 pt-3 border-t border-border">
      <Button variant="outline" onclick={handleReset}>초기화</Button>
      <Button onclick={handleSearch}>조회</Button>
    </div>
  </div>

  <!-- Table Container -->
  <div class="bg-background border border-border rounded-xl overflow-hidden shadow-sm">
    <Table>
      <TableHeader>
        <TableRow class="bg-gradient-to-b from-muted/50 to-muted">
          <TableHead class="w-[140px] font-bold text-xs uppercase tracking-wide">영업점코드</TableHead>
          <TableHead class="min-w-[180px] font-bold text-xs uppercase tracking-wide">영업점명</TableHead>
          <TableHead class="w-[100px] text-center font-bold text-xs uppercase tracking-wide">유형</TableHead>
          <TableHead class="w-[100px] font-bold text-xs uppercase tracking-wide">대표자</TableHead>
          <TableHead class="w-[90px] text-center font-bold text-xs uppercase tracking-wide">상태</TableHead>
          <TableHead class="w-[110px] font-bold text-xs uppercase tracking-wide">등록일</TableHead>
        </TableRow>
      </TableHeader>
      <TableBody>
        {#if initialLoading}
          <!-- Loading Skeleton -->
          {#each Array(8) as _}
            <TableRow>
              <TableCell><div class="h-4 w-24 bg-muted animate-pulse rounded"></div></TableCell>
              <TableCell><div class="h-4 w-40 bg-muted animate-pulse rounded"></div></TableCell>
              <TableCell class="text-center"><div class="h-4 w-16 bg-muted animate-pulse rounded mx-auto"></div></TableCell>
              <TableCell><div class="h-4 w-14 bg-muted animate-pulse rounded"></div></TableCell>
              <TableCell class="text-center"><div class="h-4 w-12 bg-muted animate-pulse rounded mx-auto"></div></TableCell>
              <TableCell><div class="h-4 w-20 bg-muted animate-pulse rounded"></div></TableCell>
            </TableRow>
          {/each}
        {:else if branches.length === 0}
          <!-- Empty State -->
          <TableRow>
            <TableCell colspan={6} class="py-16 text-center">
              <div class="flex flex-col items-center gap-3">
                <svg class="w-16 h-16 text-muted-foreground/50" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                  <path d="M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16m14 0h2m-2 0h-5m-9 0H3m2 0h5M9 7h1m-1 4h1m4-4h1m-1 4h1m-5 10v-5a1 1 0 011-1h2a1 1 0 011 1v5m-4 0h4"/>
                </svg>
                <p class="text-base font-semibold text-muted-foreground">검색 결과가 없습니다</p>
                <p class="text-sm text-muted-foreground/70">다른 검색 조건을 시도해 보세요</p>
              </div>
            </TableCell>
          </TableRow>
        {:else}
          <!-- Data Rows -->
          {#each branches as branch (branch.id)}
            <TableRow 
              class="cursor-pointer hover:bg-primary/5 transition-colors even:bg-muted/30"
              onclick={() => handleRowClick(branch)}
              onkeydown={(e) => e.key === 'Enter' && handleRowClick(branch)}
              role="button"
              tabindex={0}
            >
              <TableCell>
                <span class="font-mono text-sm text-primary font-medium">{branch.orgCode}</span>
              </TableCell>
              <TableCell>
                <span class="font-medium text-foreground">{branch.name}</span>
              </TableCell>
              <TableCell class="text-center">
                <Badge variant="secondary" class="text-xs">
                  {BRANCH_TYPE_LABELS[branch.orgType as OrgType] || branch.orgType}
                </Badge>
              </TableCell>
              <TableCell>
                {branch.businessInfo?.representative || '-'}
              </TableCell>
              <TableCell class="text-center">
                <Badge variant={getStatusVariant(branch.status)}>
                  {STATUS_LABELS[branch.status as OrgStatus] || branch.status}
                </Badge>
              </TableCell>
              <TableCell>
                {formatDate(branch.createdAt)}
              </TableCell>
            </TableRow>
          {/each}
        {/if}
      </TableBody>
    </Table>

    <!-- Loading More Indicator -->
    {#if loading && !initialLoading}
      <div class="flex items-center justify-center gap-3 py-6 text-muted-foreground text-sm">
        <div class="w-5 h-5 border-2 border-muted border-t-primary rounded-full animate-spin"></div>
        <span>더 불러오는 중...</span>
      </div>
    {/if}

    <!-- Infinite Scroll Sentinel -->
    <div bind:this={sentinelEl} class="h-px invisible"></div>

    <!-- End of List -->
    {#if !hasMore && branches.length > 0 && !loading}
      <div class="py-5 text-center text-sm text-muted-foreground border-t border-border">
        모든 데이터를 불러왔습니다
      </div>
    {/if}
  </div>
</div>
