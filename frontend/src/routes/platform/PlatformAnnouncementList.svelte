<script lang="ts">
  import { platformApi } from '@/api/platform';
  import type { PlatformAnnouncementDto } from '@/types/platform';
  import { Card, CardContent } from '$lib/components/ui/card';
  import { Button } from '$lib/components/ui/button';
  import { Badge } from '$lib/components/ui/badge';
  import { Skeleton } from '$lib/components/ui/skeleton';
  import * as Table from '$lib/components/ui/table';
  import { tabStore, type Tab } from '@/stores/tab';

  let announcements = $state<PlatformAnnouncementDto[]>([]);
  let loading = $state(true);
  let error = $state<string | null>(null);

  async function loadAnnouncements() {
    loading = true;
    error = null;
    try {
      const response = await platformApi.getAnnouncements(0, 50);
      if (response.success && response.data) {
        announcements = response.data.content || response.data;
      }
    } catch (err) {
      error = err instanceof Error ? err.message : '데이터 로딩 실패';
    } finally {
      loading = false;
    }
  }

  $effect(() => { loadAnnouncements(); });

  function severityVariant(severity: string): 'default' | 'secondary' | 'destructive' | 'outline' {
    switch (severity) {
      case 'CRITICAL': return 'destructive';
      case 'WARNING': return 'secondary';
      default: return 'outline';
    }
  }

  function statusLabel(status: string): string {
    switch (status) {
      case 'DRAFT': return '임시저장';
      case 'PUBLISHED': return '게시됨';
      case 'ARCHIVED': return '보관';
      default: return status;
    }
  }
</script>

<div class="space-y-4">
  <div class="flex items-center justify-between">
    <h1 class="text-2xl font-bold">공지사항</h1>
  </div>

  <Card>
    <CardContent class="p-0">
      {#if loading}
        <div class="p-6 space-y-3">
          {#each Array(5) as _}
            <Skeleton class="h-12 w-full" />
          {/each}
        </div>
      {:else if announcements.length === 0}
        <div class="p-6 text-center text-muted-foreground">등록된 공지사항이 없습니다.</div>
      {:else}
        <Table.Root>
          <Table.Header>
            <Table.Row>
              <Table.Head>제목</Table.Head>
              <Table.Head>유형</Table.Head>
              <Table.Head>중요도</Table.Head>
              <Table.Head>상태</Table.Head>
              <Table.Head>대상</Table.Head>
              <Table.Head>등록일</Table.Head>
            </Table.Row>
          </Table.Header>
          <Table.Body>
            {#each announcements as ann}
              <Table.Row>
                <Table.Cell class="font-medium">{ann.title}</Table.Cell>
                <Table.Cell><Badge variant="outline">{ann.announcementType}</Badge></Table.Cell>
                <Table.Cell><Badge variant={severityVariant(ann.severity)}>{ann.severity}</Badge></Table.Cell>
                <Table.Cell class="text-sm">{statusLabel(ann.status)}</Table.Cell>
                <Table.Cell class="text-sm">{ann.targetType === 'ALL' ? '전체' : '특정'}</Table.Cell>
                <Table.Cell class="text-sm">{new Date(ann.createdAt).toLocaleDateString('ko-KR')}</Table.Cell>
              </Table.Row>
            {/each}
          </Table.Body>
        </Table.Root>
      {/if}
    </CardContent>
  </Card>
</div>
