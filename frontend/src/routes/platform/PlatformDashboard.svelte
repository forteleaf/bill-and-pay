<script lang="ts">
  import { platformApi } from '@/api/platform';
  import type { PlatformDashboard } from '@/types/platform';
  import { Card, CardContent, CardHeader, CardTitle } from '$lib/components/ui/card';
  import { Skeleton } from '$lib/components/ui/skeleton';

  let dashboard = $state<PlatformDashboard | null>(null);
  let loading = $state(true);
  let error = $state<string | null>(null);

  async function loadDashboard() {
    loading = true;
    error = null;
    try {
      const response = await platformApi.getDashboard();
      if (response.success && response.data) {
        dashboard = response.data;
      } else {
        error = response.error?.message || '대시보드를 불러올 수 없습니다.';
      }
    } catch (err) {
      error = err instanceof Error ? err.message : '대시보드 로딩 실패';
    } finally {
      loading = false;
    }
  }

  $effect(() => {
    loadDashboard();
  });

  const stats = $derived(dashboard ? [
    { label: '전체 테넌트', value: dashboard.totalTenants, color: 'text-blue-600', bg: 'bg-blue-50' },
    { label: '활성 테넌트', value: dashboard.activeTenants, color: 'text-green-600', bg: 'bg-green-50' },
    { label: '정지 테넌트', value: dashboard.suspendedTenants, color: 'text-red-600', bg: 'bg-red-50' },
    { label: '프로비저닝', value: dashboard.provisioningTenants, color: 'text-yellow-600', bg: 'bg-yellow-50' },
    { label: '전체 사용자', value: dashboard.totalAuthUsers, color: 'text-purple-600', bg: 'bg-purple-50' },
    { label: 'PG 연결', value: dashboard.totalPgConnections, color: 'text-indigo-600', bg: 'bg-indigo-50' },
  ] : []);
</script>

<div class="space-y-6">
  <h1 class="text-2xl font-bold">플랫폼 대시보드</h1>

  {#if loading}
    <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
      {#each Array(6) as _}
        <Skeleton class="h-[120px] rounded-lg" />
      {/each}
    </div>
  {:else if error}
    <div class="p-4 rounded-md bg-destructive/10 text-destructive">{error}</div>
  {:else}
    <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
      {#each stats as stat}
        <Card>
          <CardHeader class="pb-2">
            <CardTitle class="text-sm font-medium text-muted-foreground">{stat.label}</CardTitle>
          </CardHeader>
          <CardContent>
            <div class="text-3xl font-bold {stat.color}">{stat.value.toLocaleString()}</div>
          </CardContent>
        </Card>
      {/each}
    </div>
  {/if}
</div>
