<script lang="ts">
  import { platformApi } from '@/api/platform';
  import type { PlatformAuditLogDto } from '@/types/platform';
  import { Card, CardContent } from '$lib/components/ui/card';
  import { Badge } from '$lib/components/ui/badge';
  import { Skeleton } from '$lib/components/ui/skeleton';
  import * as Table from '$lib/components/ui/table';

  let logs = $state<PlatformAuditLogDto[]>([]);
  let loading = $state(true);
  let error = $state<string | null>(null);

  async function loadLogs() {
    loading = true;
    error = null;
    try {
      const response = await platformApi.getAuditLogs(0, 50);
      if (response.success && response.data) {
        logs = response.data.content || response.data;
      }
    } catch (err) {
      error = err instanceof Error ? err.message : '데이터 로딩 실패';
    } finally {
      loading = false;
    }
  }

  $effect(() => { loadLogs(); });
</script>

<div class="space-y-4">
  <h1 class="text-2xl font-bold">감사 로그</h1>
  <Card>
    <CardContent class="p-0">
      {#if loading}
        <div class="p-6 space-y-3">
          {#each Array(5) as _}
            <Skeleton class="h-12 w-full" />
          {/each}
        </div>
      {:else if error}
        <div class="p-6 text-destructive">{error}</div>
      {:else if logs.length === 0}
        <div class="p-6 text-center text-muted-foreground">감사 로그가 없습니다.</div>
      {:else}
        <Table.Root>
          <Table.Header>
            <Table.Row>
              <Table.Head>일시</Table.Head>
              <Table.Head>관리자</Table.Head>
              <Table.Head>액션</Table.Head>
              <Table.Head>리소스</Table.Head>
              <Table.Head>대상 테넌트</Table.Head>
              <Table.Head>IP</Table.Head>
            </Table.Row>
          </Table.Header>
          <Table.Body>
            {#each logs as log}
              <Table.Row>
                <Table.Cell class="text-sm">{new Date(log.createdAt).toLocaleString('ko-KR')}</Table.Cell>
                <Table.Cell class="font-mono text-sm">{log.adminUsername}</Table.Cell>
                <Table.Cell><Badge variant="outline">{log.action}</Badge></Table.Cell>
                <Table.Cell class="text-sm">{log.resourceType}{log.resourceId ? `: ${log.resourceId}` : ''}</Table.Cell>
                <Table.Cell class="text-sm font-mono">{log.targetTenantId || '-'}</Table.Cell>
                <Table.Cell class="text-sm text-muted-foreground">{log.ipAddress || '-'}</Table.Cell>
              </Table.Row>
            {/each}
          </Table.Body>
        </Table.Root>
      {/if}
    </CardContent>
  </Card>
</div>
