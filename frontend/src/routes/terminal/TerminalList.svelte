<script lang="ts">
  import { onMount } from 'svelte';
  import { terminalApi } from '@/api/terminal';
  import { tabStore } from '@/stores/tab';
  import {
    type TerminalDto,
    type TerminalListParams,
    TerminalStatus,
    TerminalType,
    TERMINAL_STATUS_LABELS,
    TERMINAL_TYPE_LABELS
  } from '@/types/terminal';
  import { format } from 'date-fns';
  import { Button } from '$lib/components/ui/button';
  import { Input } from '$lib/components/ui/input';
  import { Label } from '$lib/components/ui/label';
  import { Badge } from '$lib/components/ui/badge';
  import * as Select from '$lib/components/ui/select';
  import {
    Table,
    TableBody,
    TableCell,
    TableHead,
    TableHeader,
    TableRow
  } from '$lib/components/ui/table';

  let terminals = $state<TerminalDto[]>([]);
  let loading = $state(false);
  let initialLoading = $state(true);
  let hasMore = $state(true);
  let page = $state(0);
  const pageSize = 20;
  let totalCount = $state(0);

  let searchQuery = $state('');
  let typeFilter = $state<TerminalType | ''>('');
  let statusFilter = $state<TerminalStatus | ''>('');

  let sentinelEl: HTMLDivElement;

  function getStatusVariant(status: string): 'default' | 'secondary' | 'destructive' | 'outline' {
    switch (status) {
      case TerminalStatus.ACTIVE:
        return 'default';
      case TerminalStatus.INACTIVE:
        return 'secondary';
      case TerminalStatus.SUSPENDED:
        return 'outline';
      case TerminalStatus.TERMINATED:
        return 'destructive';
      default:
        return 'outline';
    }
  }

  const TYPE_COLORS: Record<string, { bg: string; border: string; text: string }> = {
    CAT: {
      bg: 'bg-indigo-500/10',
      border: 'border-indigo-500/40',
      text: 'text-indigo-600'
    },
    POS: {
      bg: 'bg-violet-500/10',
      border: 'border-violet-500/40',
      text: 'text-violet-600'
    },
    MOBILE: {
      bg: 'bg-blue-500/10',
      border: 'border-blue-500/40',
      text: 'text-blue-600'
    },
    KIOSK: {
      bg: 'bg-cyan-500/10',
      border: 'border-cyan-500/40',
      text: 'text-cyan-600'
    },
    ONLINE: {
      bg: 'bg-emerald-500/10',
      border: 'border-emerald-500/40',
      text: 'text-emerald-600'
    }
  };

  function getTypeClasses(terminalType: string): string {
    const colors = TYPE_COLORS[terminalType];
    if (!colors) return '';
    return `${colors.bg} ${colors.border} ${colors.text} border`;
  }

  function formatDate(dateStr?: string): string {
    if (!dateStr) return '-';
    try {
      return format(new Date(dateStr), 'yyyy-MM-dd');
    } catch {
      return '-';
    }
  }

  function formatDateTime(dateStr?: string): string {
    if (!dateStr) return '-';
    try {
      return format(new Date(dateStr), 'yyyy-MM-dd HH:mm');
    } catch {
      return '-';
    }
  }

  async function loadTerminals(reset: boolean = false) {
    if (loading) return;
    if (!reset && !hasMore) return;

    loading = true;

    if (reset) {
      page = 0;
      terminals = [];
      hasMore = true;
    }

    try {
      const params: TerminalListParams = {
        page,
        size: pageSize,
        terminalType: typeFilter || undefined,
        status: statusFilter || undefined,
        search: searchQuery.trim() || undefined
      };

      const response = await terminalApi.getTerminals(params);

      if (response.success && response.data) {
        const newData = response.data.content || [];
        if (reset) {
          terminals = newData;
        } else {
          terminals = [...terminals, ...newData];
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
      console.error('Failed to load terminals:', err);
      hasMore = false;
    } finally {
      loading = false;
      initialLoading = false;
    }
  }

  function handleSearch() {
    loadTerminals(true);
  }

  function handleReset() {
    searchQuery = '';
    typeFilter = '';
    statusFilter = '';
    loadTerminals(true);
  }

  function handleRowClick(terminal: TerminalDto) {
    tabStore.openTab({
      id: `terminal-${terminal.id}`,
      title: terminal.tid,
      icon: '💻',
      component: 'TerminalDetail',
      closeable: true,
      props: { terminalId: terminal.id }
    });
  }

  onMount(() => {
    loadTerminals(true);
  });

  $effect(() => {
    if (!sentinelEl) return;
    if (initialLoading) return;

    const observer = new IntersectionObserver(
      (entries) => {
        const entry = entries[0];
        if (entry.isIntersecting && !loading && hasMore && terminals.length > 0) {
          loadTerminals(false);
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
      <h1 class="text-2xl font-bold text-foreground tracking-tight">단말기 목록</h1>
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
            placeholder="CAT ID, 가맹점명..."
            value={searchQuery}
            oninput={(e) => searchQuery = e.currentTarget.value}
            onkeydown={(e) => e.key === 'Enter' && handleSearch()}
            class="pl-9"
          />
        </div>
      </div>

      <!-- Type Filter -->
      <div class="flex flex-col gap-1.5">
        <Label>단말기유형</Label>
        <Select.Root type="single" bind:value={typeFilter}>
          <Select.Trigger class="min-w-[140px]">
            {#if typeFilter}
              {TERMINAL_TYPE_LABELS[typeFilter as TerminalType] || typeFilter}
            {:else}
              <span class="text-muted-foreground">전체</span>
            {/if}
          </Select.Trigger>
          <Select.Content>
            <Select.Item value="">전체</Select.Item>
            {#each Object.values(TerminalType) as type}
              <Select.Item value={type}>{TERMINAL_TYPE_LABELS[type]}</Select.Item>
            {/each}
          </Select.Content>
        </Select.Root>
      </div>

      <!-- Status Filter -->
      <div class="flex flex-col gap-1.5">
        <Label>상태</Label>
        <Select.Root type="single" bind:value={statusFilter}>
          <Select.Trigger class="min-w-[140px]">
            {#if statusFilter}
              {TERMINAL_STATUS_LABELS[statusFilter as TerminalStatus] || statusFilter}
            {:else}
              <span class="text-muted-foreground">전체</span>
            {/if}
          </Select.Trigger>
          <Select.Content>
            <Select.Item value="">전체</Select.Item>
            {#each Object.values(TerminalStatus) as status}
              <Select.Item value={status}>{TERMINAL_STATUS_LABELS[status]}</Select.Item>
            {/each}
          </Select.Content>
        </Select.Root>
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
          <TableHead class="w-[120px] font-bold text-xs uppercase tracking-wide">CAT ID</TableHead>
          <TableHead class="w-[100px] text-center font-bold text-xs uppercase tracking-wide">유형</TableHead>
          <TableHead class="min-w-[180px] font-bold text-xs uppercase tracking-wide">가맹점</TableHead>
          <TableHead class="w-[90px] text-center font-bold text-xs uppercase tracking-wide">상태</TableHead>
          <TableHead class="w-[140px] font-bold text-xs uppercase tracking-wide">최근 거래</TableHead>
          <TableHead class="w-[110px] font-bold text-xs uppercase tracking-wide">등록일</TableHead>
        </TableRow>
      </TableHeader>
      <TableBody>
        {#if initialLoading}
          <!-- Loading Skeleton -->
          {#each Array(8) as _}
            <TableRow>
              <TableCell><div class="h-4 w-20 bg-muted animate-pulse rounded"></div></TableCell>
              <TableCell class="text-center"><div class="h-4 w-16 bg-muted animate-pulse rounded mx-auto"></div></TableCell>
              <TableCell><div class="h-4 w-40 bg-muted animate-pulse rounded"></div></TableCell>
              <TableCell class="text-center"><div class="h-4 w-12 bg-muted animate-pulse rounded mx-auto"></div></TableCell>
              <TableCell><div class="h-4 w-28 bg-muted animate-pulse rounded"></div></TableCell>
              <TableCell><div class="h-4 w-20 bg-muted animate-pulse rounded"></div></TableCell>
            </TableRow>
          {/each}
        {:else if terminals.length === 0}
          <!-- Empty State -->
          <TableRow>
            <TableCell colspan={6} class="py-16 text-center">
              <div class="flex flex-col items-center gap-3">
                <svg class="w-16 h-16 text-muted-foreground/50" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                  <rect x="2" y="3" width="20" height="14" rx="2" ry="2"/>
                  <line x1="8" y1="21" x2="16" y2="21"/>
                  <line x1="12" y1="17" x2="12" y2="21"/>
                </svg>
                <p class="text-base font-semibold text-muted-foreground">검색 결과가 없습니다</p>
                <p class="text-sm text-muted-foreground/70">다른 검색 조건을 시도해 보세요</p>
              </div>
            </TableCell>
          </TableRow>
        {:else}
          <!-- Data Rows -->
          {#each terminals as terminal (terminal.id)}
            <TableRow 
              class="cursor-pointer hover:bg-primary/5 transition-colors even:bg-muted/30"
              onclick={() => handleRowClick(terminal)}
              onkeydown={(e) => e.key === 'Enter' && handleRowClick(terminal)}
              role="button"
              tabindex={0}
            >
              <TableCell>
                <span class="font-mono text-sm text-muted-foreground">{terminal.catId || '-'}</span>
              </TableCell>
              <TableCell class="text-center">
                <Badge variant="outline" class="text-xs font-medium {getTypeClasses(terminal.terminalType)}">
                  {TERMINAL_TYPE_LABELS[terminal.terminalType] || terminal.terminalType}
                </Badge>
              </TableCell>
              <TableCell>
                <div class="flex flex-col">
                  <span class="font-medium text-foreground">{terminal.merchantName || '-'}</span>
                  {#if terminal.merchantCode}
                    <span class="text-xs text-muted-foreground font-mono">{terminal.merchantCode}</span>
                  {/if}
                </div>
              </TableCell>
              <TableCell class="text-center">
                <Badge variant={getStatusVariant(terminal.status)}>
                  {TERMINAL_STATUS_LABELS[terminal.status] || terminal.status}
                </Badge>
              </TableCell>
              <TableCell>
                <span class="text-sm text-muted-foreground">{formatDateTime(terminal.lastTransactionAt)}</span>
              </TableCell>
              <TableCell>
                {formatDate(terminal.createdAt)}
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
    {#if !hasMore && terminals.length > 0 && !loading}
      <div class="py-5 text-center text-sm text-muted-foreground border-t border-border">
        모든 데이터를 불러왔습니다
      </div>
    {/if}
  </div>
</div>
