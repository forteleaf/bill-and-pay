<script lang="ts">
  import { platformApi } from '@/api/platform';
  import type { PgConnectionDto } from '@/types/platform';
  import { Card, CardContent, CardHeader, CardTitle } from '$lib/components/ui/card';
  import { Badge } from '$lib/components/ui/badge';
  import { Skeleton } from '$lib/components/ui/skeleton';

  let connections = $state<PgConnectionDto[]>([]);
  let loading = $state(true);
  let error = $state<string | null>(null);

  async function loadPgStatus() {
    loading = true;
    error = null;
    try {
      const response = await platformApi.getPgStatus();
      if (response.success && response.data) {
        connections = response.data;
      }
    } catch (err) {
      error = err instanceof Error ? err.message : '데이터 로딩 실패';
    } finally {
      loading = false;
    }
  }

  $effect(() => { loadPgStatus(); });
</script>

<div class="space-y-6">
  <h1 class="text-2xl font-bold">PG 모니터링</h1>

  {#if loading}
    <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
      {#each Array(4) as _}
        <Skeleton class="h-[150px]" />
      {/each}
    </div>
  {:else if error}
    <div class="p-4 rounded-md bg-destructive/10 text-destructive">{error}</div>
  {:else if connections.length === 0}
    <div class="text-center text-muted-foreground">등록된 PG 연결이 없습니다.</div>
  {:else}
    <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
      {#each connections as pg}
        <Card>
          <CardHeader class="pb-2">
            <div class="flex items-center justify-between">
              <CardTitle class="text-lg">{pg.pgName}</CardTitle>
              <Badge variant={pg.status === 'ACTIVE' ? 'default' : pg.status === 'MAINTENANCE' ? 'secondary' : 'destructive'}>
                {pg.status === 'ACTIVE' ? '정상' : pg.status === 'MAINTENANCE' ? '점검중' : '비활성'}
              </Badge>
            </div>
          </CardHeader>
          <CardContent class="space-y-2 text-sm">
            <div class="flex justify-between">
              <span class="text-muted-foreground">코드</span>
              <span class="font-mono">{pg.pgCode}</span>
            </div>
            <div class="flex justify-between">
              <span class="text-muted-foreground">API URL</span>
              <span class="font-mono text-xs truncate max-w-[200px]">{pg.apiBaseUrl}</span>
            </div>
            {#if pg.tenantId}
              <div class="flex justify-between">
                <span class="text-muted-foreground">테넌트</span>
                <span class="font-mono">{pg.tenantId}</span>
              </div>
            {/if}
          </CardContent>
        </Card>
      {/each}
    </div>
  {/if}
</div>
