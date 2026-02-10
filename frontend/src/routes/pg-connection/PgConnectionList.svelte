<script lang="ts">
  import { onMount } from 'svelte';
  import { pgConnectionApi } from '@/api/pgConnection';
  import { tabStore } from '@/stores/tab';
  import {
    type PgConnectionDto,
    PgConnectionStatus,
    PG_CONNECTION_STATUS_LABELS
  } from '@/types/pgConnection';
  import { format } from 'date-fns';
  import { Button } from '$lib/components/ui/button';
  import { Badge } from '$lib/components/ui/badge';
  import {
    Table,
    TableBody,
    TableCell,
    TableHead,
    TableHeader,
    TableRow
  } from '$lib/components/ui/table';

  let connections = $state<PgConnectionDto[]>([]);
  let loading = $state(true);
  let error = $state<string | null>(null);
  let totalCount = $state(0);

  onMount(() => {
    loadConnections();
  });

  async function loadConnections() {
    loading = true;
    error = null;

    try {
      const response = await pgConnectionApi.getAll(0, 100);
      if (response.success && response.data) {
        connections = response.data.content || [];
        totalCount = response.data.totalElements || 0;
      } else {
        error = response.error?.message || '데이터를 불러올 수 없습니다.';
      }
    } catch (err) {
      error = '데이터를 불러올 수 없습니다.';
    } finally {
      loading = false;
    }
  }

  function handleRowClick(connection: PgConnectionDto) {
    tabStore.openTab({
      id: `pg-connection-${connection.id}`,
      title: connection.pgName,
      icon: '🔗',
      component: 'PgConnectionDetail',
      closeable: true,
      props: { connectionId: connection.id }
    });
  }

  function handleAddNew() {
    tabStore.openTab({
      id: 'pg-connection-new',
      title: 'PG 연결 등록',
      icon: '➕',
      component: 'PgConnectionDetail',
      closeable: true,
      props: { connectionId: null }
    });
  }

  async function handleToggleStatus(connection: PgConnectionDto, event: Event) {
    event.stopPropagation();
    const newStatus = connection.status === PgConnectionStatus.ACTIVE 
      ? PgConnectionStatus.INACTIVE 
      : PgConnectionStatus.ACTIVE;

    try {
      const response = await pgConnectionApi.updateStatus(connection.id, newStatus);
      if (response.success) {
        await loadConnections();
      } else {
        error = response.error?.message || '상태 변경에 실패했습니다.';
      }
    } catch (err) {
      error = '상태 변경에 실패했습니다.';
    }
  }

  function getStatusVariant(status: PgConnectionStatus): 'default' | 'secondary' | 'outline' {
    switch (status) {
      case PgConnectionStatus.ACTIVE:
        return 'default';
      case PgConnectionStatus.INACTIVE:
        return 'secondary';
      case PgConnectionStatus.MAINTENANCE:
        return 'outline';
      default:
        return 'secondary';
    }
  }

  function formatDate(dateStr?: string): string {
    if (!dateStr) return '-';
    try {
      return format(new Date(dateStr), 'yyyy-MM-dd HH:mm');
    } catch {
      return '-';
    }
  }
</script>

<div class="flex flex-col gap-6 p-6 max-w-[1400px] mx-auto">
  <header class="flex justify-between items-center">
    <div class="flex items-baseline gap-4">
      <h1 class="text-2xl font-bold text-foreground tracking-tight">PG 연결 관리</h1>
      <span class="text-sm text-muted-foreground font-medium">총 {totalCount}개</span>
    </div>
    <Button onclick={handleAddNew}>
      <svg class="w-4 h-4 mr-1.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
        <path d="M12 5v14m-7-7h14" />
      </svg>
      PG 연결 추가
    </Button>
  </header>

  <div class="bg-background border border-border rounded-xl overflow-hidden shadow-sm">
    <Table>
      <TableHeader>
        <TableRow class="bg-gradient-to-b from-muted/50 to-muted">
          <TableHead class="w-[100px] font-bold text-xs uppercase tracking-wide">코드</TableHead>
          <TableHead class="w-[150px] font-bold text-xs uppercase tracking-wide">이름</TableHead>
          <TableHead class="w-[120px] font-bold text-xs uppercase tracking-wide">가맹점 ID</TableHead>
          <TableHead class="w-[200px] font-bold text-xs uppercase tracking-wide">API Base URL</TableHead>
          <TableHead class="w-[90px] text-center font-bold text-xs uppercase tracking-wide">상태</TableHead>
          <TableHead class="w-[140px] font-bold text-xs uppercase tracking-wide">등록일</TableHead>
          <TableHead class="w-[100px] text-center font-bold text-xs uppercase tracking-wide">액션</TableHead>
        </TableRow>
      </TableHeader>
      <TableBody>
        {#if loading}
          {#each Array(5) as _}
            <TableRow>
              <TableCell><div class="h-4 w-16 bg-muted animate-pulse rounded"></div></TableCell>
              <TableCell><div class="h-4 w-24 bg-muted animate-pulse rounded"></div></TableCell>
              <TableCell><div class="h-4 w-20 bg-muted animate-pulse rounded"></div></TableCell>
              <TableCell><div class="h-4 w-32 bg-muted animate-pulse rounded"></div></TableCell>
              <TableCell class="text-center"><div class="h-4 w-12 bg-muted animate-pulse rounded mx-auto"></div></TableCell>
              <TableCell><div class="h-4 w-28 bg-muted animate-pulse rounded"></div></TableCell>
              <TableCell class="text-center"><div class="h-4 w-16 bg-muted animate-pulse rounded mx-auto"></div></TableCell>
            </TableRow>
          {/each}
        {:else if error}
          <TableRow>
            <TableCell colspan={7} class="py-16 text-center">
              <div class="flex flex-col items-center gap-3 text-destructive">
                <svg class="w-12 h-12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                  <circle cx="12" cy="12" r="10" />
                  <path d="M12 8v4m0 4h.01" />
                </svg>
                <p class="text-base font-semibold">{error}</p>
                <Button variant="outline" onclick={loadConnections}>다시 시도</Button>
              </div>
            </TableCell>
          </TableRow>
        {:else if connections.length === 0}
          <TableRow>
            <TableCell colspan={7} class="py-16 text-center">
              <div class="flex flex-col items-center gap-3">
                <svg class="w-16 h-16 text-muted-foreground/50" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                  <path d="M13 10V3L4 14h7v7l9-11h-7z" />
                </svg>
                <p class="text-base font-semibold text-muted-foreground">등록된 PG 연결이 없습니다</p>
                <p class="text-sm text-muted-foreground/70">새로운 PG 연결을 추가하세요</p>
                <Button onclick={handleAddNew}>PG 연결 추가</Button>
              </div>
            </TableCell>
          </TableRow>
        {:else}
          {#each connections as connection (connection.id)}
            <TableRow 
              class="cursor-pointer hover:bg-primary/5 transition-colors even:bg-muted/30"
              onclick={() => handleRowClick(connection)}
              onkeydown={(e) => e.key === 'Enter' && handleRowClick(connection)}
              role="button"
              tabindex={0}
            >
              <TableCell>
                <span class="font-mono text-sm font-semibold text-primary">{connection.pgCode}</span>
              </TableCell>
              <TableCell>
                <span class="font-medium">{connection.pgName}</span>
              </TableCell>
              <TableCell>
                <span class="font-mono text-sm text-muted-foreground">{connection.merchantId}</span>
              </TableCell>
              <TableCell>
                <span class="font-mono text-xs text-muted-foreground truncate block max-w-[180px]" title={connection.apiBaseUrl}>
                  {connection.apiBaseUrl}
                </span>
              </TableCell>
              <TableCell class="text-center">
                <Badge variant={getStatusVariant(connection.status)}>
                  {PG_CONNECTION_STATUS_LABELS[connection.status]}
                </Badge>
              </TableCell>
              <TableCell>
                <span class="text-sm text-muted-foreground">{formatDate(connection.createdAt)}</span>
              </TableCell>
              <TableCell class="text-center">
                <Button
                  variant="ghost"
                  size="sm"
                  class="h-8 px-2 text-xs"
                  onclick={(e) => handleToggleStatus(connection, e)}
                >
                  {connection.status === PgConnectionStatus.ACTIVE ? '비활성화' : '활성화'}
                </Button>
              </TableCell>
            </TableRow>
          {/each}
        {/if}
      </TableBody>
    </Table>
  </div>
</div>
