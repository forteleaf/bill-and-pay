<script lang="ts">
  import { platformApi } from '@/api/platform';
  import type { PlatformAdminDto } from '@/types/platform';
  import { Card, CardContent } from '$lib/components/ui/card';
  import { Badge } from '$lib/components/ui/badge';
  import { Skeleton } from '$lib/components/ui/skeleton';
  import * as Table from '$lib/components/ui/table';

  let admins = $state<PlatformAdminDto[]>([]);
  let loading = $state(true);
  let error = $state<string | null>(null);

  async function loadAdmins() {
    loading = true;
    error = null;
    try {
      const response = await platformApi.getAdmins(0, 50);
      if (response.success && response.data) {
        admins = response.data.content || response.data;
      }
    } catch (err) {
      error = err instanceof Error ? err.message : '데이터 로딩 실패';
    } finally {
      loading = false;
    }
  }

  $effect(() => { loadAdmins(); });

  function roleLabel(role: string): string {
    switch (role) {
      case 'SUPER_ADMIN': return '최고 관리자';
      case 'PLATFORM_OPERATOR': return '운영자';
      case 'PLATFORM_VIEWER': return '조회자';
      default: return role;
    }
  }
</script>

<div class="space-y-4">
  <h1 class="text-2xl font-bold">관리자 계정</h1>
  <Card>
    <CardContent class="p-0">
      {#if loading}
        <div class="p-6 space-y-3">
          {#each Array(3) as _}
            <Skeleton class="h-12 w-full" />
          {/each}
        </div>
      {:else if error}
        <div class="p-6 text-destructive">{error}</div>
      {:else}
        <Table.Root>
          <Table.Header>
            <Table.Row>
              <Table.Head>사용자명</Table.Head>
              <Table.Head>이름</Table.Head>
              <Table.Head>이메일</Table.Head>
              <Table.Head>역할</Table.Head>
              <Table.Head>상태</Table.Head>
              <Table.Head>마지막 로그인</Table.Head>
            </Table.Row>
          </Table.Header>
          <Table.Body>
            {#each admins as admin}
              <Table.Row>
                <Table.Cell class="font-mono">{admin.username}</Table.Cell>
                <Table.Cell>{admin.fullName}</Table.Cell>
                <Table.Cell class="text-sm">{admin.email}</Table.Cell>
                <Table.Cell><Badge variant="secondary">{roleLabel(admin.role)}</Badge></Table.Cell>
                <Table.Cell>
                  <Badge variant={admin.status === 'ACTIVE' ? 'default' : 'destructive'}>
                    {admin.status === 'ACTIVE' ? '활성' : '정지'}
                  </Badge>
                </Table.Cell>
                <Table.Cell class="text-sm text-muted-foreground">
                  {admin.lastLoginAt ? new Date(admin.lastLoginAt).toLocaleString('ko-KR') : '-'}
                </Table.Cell>
              </Table.Row>
            {/each}
          </Table.Body>
        </Table.Root>
      {/if}
    </CardContent>
  </Card>
</div>
