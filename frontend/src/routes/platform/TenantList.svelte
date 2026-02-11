<script lang="ts">
  import { platformApi } from '@/api/platform';
  import type { TenantDto } from '@/types/platform';
  import { Card, CardContent, CardHeader, CardTitle } from '$lib/components/ui/card';
  import { Button } from '$lib/components/ui/button';
  import { Badge } from '$lib/components/ui/badge';
  import { Input } from '$lib/components/ui/input';
  import { Skeleton } from '$lib/components/ui/skeleton';
  import * as Table from '$lib/components/ui/table';
  import { tabStore, type Tab } from '@/stores/tab';

  let tenants = $state<TenantDto[]>([]);
  let loading = $state(true);
  let error = $state<string | null>(null);
  let statusFilter = $state('');

  async function loadTenants() {
    loading = true;
    error = null;
    try {
      const response = await platformApi.getTenants(0, 50, statusFilter || undefined);
      if (response.success && response.data) {
        tenants = response.data.content || response.data;
      } else {
        error = response.error?.message || '테넌트 목록을 불러올 수 없습니다.';
      }
    } catch (err) {
      error = err instanceof Error ? err.message : '데이터 로딩 실패';
    } finally {
      loading = false;
    }
  }

  $effect(() => {
    loadTenants();
  });

  function openDetail(tenant: TenantDto) {
    const tab: Tab = {
      id: `tenant-${tenant.id}`,
      title: `테넌트: ${tenant.name}`,
      icon: '🏢',
      component: 'PlatformTenantDetail',
      closeable: true,
      props: { tenantId: tenant.id }
    };
    tabStore.openTab(tab);
  }

  function openCreate() {
    const tab: Tab = {
      id: 'tenant-create',
      title: '테넌트 등록',
      icon: '➕',
      component: 'PlatformTenantCreate',
      closeable: true,
    };
    tabStore.openTab(tab);
  }

  function statusVariant(status: string): 'default' | 'secondary' | 'destructive' | 'outline' {
    switch (status) {
      case 'ACTIVE': return 'default';
      case 'SUSPENDED': return 'destructive';
      case 'PROVISIONING': return 'secondary';
      default: return 'outline';
    }
  }

  function statusLabel(status: string): string {
    switch (status) {
      case 'ACTIVE': return '활성';
      case 'SUSPENDED': return '정지';
      case 'PROVISIONING': return '생성중';
      case 'DELETED': return '삭제';
      default: return status;
    }
  }
</script>

<div class="space-y-4">
  <div class="flex items-center justify-between">
    <h1 class="text-2xl font-bold">테넌트 관리</h1>
    <Button onclick={openCreate}>테넌트 등록</Button>
  </div>

  <div class="flex gap-2">
    <Button variant={statusFilter === '' ? 'default' : 'outline'} size="sm" onclick={() => { statusFilter = ''; loadTenants(); }}>전체</Button>
    <Button variant={statusFilter === 'ACTIVE' ? 'default' : 'outline'} size="sm" onclick={() => { statusFilter = 'ACTIVE'; loadTenants(); }}>활성</Button>
    <Button variant={statusFilter === 'SUSPENDED' ? 'default' : 'outline'} size="sm" onclick={() => { statusFilter = 'SUSPENDED'; loadTenants(); }}>정지</Button>
  </div>

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
      {:else if tenants.length === 0}
        <div class="p-6 text-center text-muted-foreground">등록된 테넌트가 없습니다.</div>
      {:else}
        <Table.Root>
          <Table.Header>
            <Table.Row>
              <Table.Head>ID</Table.Head>
              <Table.Head>이름</Table.Head>
              <Table.Head>스키마</Table.Head>
              <Table.Head>상태</Table.Head>
              <Table.Head>연락처</Table.Head>
              <Table.Head>생성일</Table.Head>
            </Table.Row>
          </Table.Header>
          <Table.Body>
            {#each tenants as tenant}
              <Table.Row class="cursor-pointer hover:bg-muted/50" onclick={() => openDetail(tenant)}>
                <Table.Cell class="font-mono text-sm">{tenant.id}</Table.Cell>
                <Table.Cell class="font-medium">{tenant.name}</Table.Cell>
                <Table.Cell class="font-mono text-sm text-muted-foreground">{tenant.schemaName}</Table.Cell>
                <Table.Cell>
                  <Badge variant={statusVariant(tenant.status)}>{statusLabel(tenant.status)}</Badge>
                </Table.Cell>
                <Table.Cell class="text-sm">{tenant.contactEmail || '-'}</Table.Cell>
                <Table.Cell class="text-sm">{new Date(tenant.createdAt).toLocaleDateString('ko-KR')}</Table.Cell>
              </Table.Row>
            {/each}
          </Table.Body>
        </Table.Root>
      {/if}
    </CardContent>
  </Card>
</div>
