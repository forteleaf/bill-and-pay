<script lang="ts">
  import { platformApi } from '@/api/platform';
  import type { TenantDto } from '@/types/platform';
  import { Card, CardContent, CardHeader, CardTitle } from '$lib/components/ui/card';
  import { Button } from '$lib/components/ui/button';
  import { Badge } from '$lib/components/ui/badge';
  import { Input } from '$lib/components/ui/input';
  import { Label } from '$lib/components/ui/label';
  import { Skeleton } from '$lib/components/ui/skeleton';

  interface Props {
    tenantId: string;
  }
  let { tenantId }: Props = $props();

  let tenant = $state<TenantDto | null>(null);
  let loading = $state(true);
  let error = $state<string | null>(null);
  let editing = $state(false);
  let editName = $state('');
  let editEmail = $state('');
  let editPhone = $state('');
  let suspendReason = $state('');
  let showSuspendConfirm = $state(false);

  async function loadTenant() {
    loading = true;
    error = null;
    try {
      const response = await platformApi.getTenant(tenantId);
      if (response.success && response.data) {
        tenant = response.data;
        editName = tenant.name;
        editEmail = tenant.contactEmail || '';
        editPhone = tenant.contactPhone || '';
      } else {
        error = response.error?.message || '테넌트 정보를 불러올 수 없습니다.';
      }
    } catch (err) {
      error = err instanceof Error ? err.message : '데이터 로딩 실패';
    } finally {
      loading = false;
    }
  }

  async function handleUpdate() {
    if (!tenant) return;
    try {
      const response = await platformApi.updateTenant(tenant.id, {
        name: editName,
        contactEmail: editEmail,
        contactPhone: editPhone,
      });
      if (response.success && response.data) {
        tenant = response.data;
        editing = false;
      }
    } catch (err) {
      error = err instanceof Error ? err.message : '수정 실패';
    }
  }

  async function handleSuspend() {
    if (!tenant) return;
    try {
      const response = await platformApi.suspendTenant(tenant.id, suspendReason);
      if (response.success && response.data) {
        tenant = response.data;
        showSuspendConfirm = false;
        suspendReason = '';
      }
    } catch (err) {
      error = err instanceof Error ? err.message : '정지 실패';
    }
  }

  async function handleActivate() {
    if (!tenant) return;
    try {
      const response = await platformApi.activateTenant(tenant.id);
      if (response.success && response.data) {
        tenant = response.data;
      }
    } catch (err) {
      error = err instanceof Error ? err.message : '활성화 실패';
    }
  }

  let prevTenantId = $state<string | null>(null);
  $effect(() => {
    if (tenantId && tenantId !== prevTenantId) {
      prevTenantId = tenantId;
      loadTenant();
    }
  });
</script>

<div class="space-y-6">
  {#if loading}
    <Skeleton class="h-8 w-64" />
    <Skeleton class="h-[300px]" />
  {:else if error}
    <div class="p-4 rounded-md bg-destructive/10 text-destructive">{error}</div>
  {:else if tenant}
    <div class="flex items-center justify-between">
      <div class="flex items-center gap-3">
        <h1 class="text-2xl font-bold">{tenant.name}</h1>
        <Badge variant={tenant.status === 'ACTIVE' ? 'default' : 'destructive'}>
          {tenant.status === 'ACTIVE' ? '활성' : tenant.status === 'SUSPENDED' ? '정지' : tenant.status}
        </Badge>
      </div>
      <div class="flex gap-2">
        {#if tenant.status === 'ACTIVE'}
          <Button variant="destructive" size="sm" onclick={() => showSuspendConfirm = true}>정지</Button>
        {:else if tenant.status === 'SUSPENDED'}
          <Button variant="default" size="sm" onclick={handleActivate}>활성화</Button>
        {/if}
        <Button variant="outline" size="sm" onclick={() => editing = !editing}>
          {editing ? '취소' : '수정'}
        </Button>
      </div>
    </div>

    <div class="grid grid-cols-1 lg:grid-cols-2 gap-6">
      <Card>
        <CardHeader>
          <CardTitle class="text-lg">기본 정보</CardTitle>
        </CardHeader>
        <CardContent class="space-y-4">
          {#if editing}
            <div class="space-y-2">
              <Label>이름</Label>
              <Input value={editName} oninput={(e) => editName = e.currentTarget.value} />
            </div>
            <div class="space-y-2">
              <Label>이메일</Label>
              <Input value={editEmail} oninput={(e) => editEmail = e.currentTarget.value} />
            </div>
            <div class="space-y-2">
              <Label>전화번호</Label>
              <Input value={editPhone} oninput={(e) => editPhone = e.currentTarget.value} />
            </div>
            <Button onclick={handleUpdate}>저장</Button>
          {:else}
            <div class="grid grid-cols-2 gap-4 text-sm">
              <div><span class="text-muted-foreground">ID:</span></div>
              <div class="font-mono">{tenant.id}</div>
              <div><span class="text-muted-foreground">이름:</span></div>
              <div>{tenant.name}</div>
              <div><span class="text-muted-foreground">스키마:</span></div>
              <div class="font-mono">{tenant.schemaName}</div>
              <div><span class="text-muted-foreground">이메일:</span></div>
              <div>{tenant.contactEmail || '-'}</div>
              <div><span class="text-muted-foreground">전화번호:</span></div>
              <div>{tenant.contactPhone || '-'}</div>
              <div><span class="text-muted-foreground">생성일:</span></div>
              <div>{new Date(tenant.createdAt).toLocaleString('ko-KR')}</div>
              <div><span class="text-muted-foreground">수정일:</span></div>
              <div>{new Date(tenant.updatedAt).toLocaleString('ko-KR')}</div>
            </div>
          {/if}
        </CardContent>
      </Card>
    </div>

    {#if showSuspendConfirm}
      <Card class="border-destructive">
        <CardHeader>
          <CardTitle class="text-lg text-destructive">테넌트 정지 확인</CardTitle>
        </CardHeader>
        <CardContent class="space-y-4">
          <p class="text-sm">"{tenant.name}" ({tenant.id})을 정지하시겠습니까?</p>
          <div class="space-y-2">
            <Label>정지 사유</Label>
            <Input value={suspendReason} oninput={(e) => suspendReason = e.currentTarget.value} placeholder="정지 사유를 입력하세요" />
          </div>
          <div class="flex gap-2 justify-end">
            <Button variant="outline" onclick={() => showSuspendConfirm = false}>취소</Button>
            <Button variant="destructive" onclick={handleSuspend}>정지 실행</Button>
          </div>
        </CardContent>
      </Card>
    {/if}
  {/if}
</div>
