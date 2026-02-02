<script lang="ts">
  import { onMount } from 'svelte';
  import { toast } from 'svelte-sonner';
  import { pgConnectionApi } from '../../lib/pgConnectionApi';
  import { tabStore } from '../../lib/tabStore';
  import {
    type PgConnectionDto,
    type PgConnectionCreateRequest,
    type PgConnectionUpdateRequest,
    PgConnectionStatus,
    PG_CONNECTION_STATUS_LABELS
  } from '../../types/pgConnection';
  import { Button } from '$lib/components/ui/button';
  import { Badge } from '$lib/components/ui/badge';
  import { Card, CardContent, CardHeader, CardTitle } from '$lib/components/ui/card';
  import { Input } from '$lib/components/ui/input';
  import { Label } from '$lib/components/ui/label';

  interface Props {
    connectionId: string | null;
  }

  let { connectionId }: Props = $props();

  let connection = $state<PgConnectionDto | null>(null);
  let loading = $state(true);
  let editMode = $state(false);
  let saving = $state(false);
  let error = $state<string | null>(null);

  let formPgCode = $state('');
  let formPgName = $state('');
  let formApiBaseUrl = $state('');
  let formWebhookBaseUrl = $state('');
  let formMerchantId = $state('');
  let formApiKey = $state('');
  let formSecretKey = $state('');
  let formWebhookSecret = $state('');
  let formStatus = $state<PgConnectionStatus>(PgConnectionStatus.ACTIVE);

  const isNewMode = $derived(connectionId === null);

  onMount(() => {
    if (connectionId) {
      loadConnection();
    } else {
      loading = false;
      editMode = true;
    }
  });

  async function loadConnection() {
    if (!connectionId) return;
    loading = true;
    error = null;

    try {
      const response = await pgConnectionApi.getById(connectionId);
      if (response.success && response.data) {
        connection = response.data;
        initEditFields();
      } else {
        error = response.error?.message || '정보를 불러올 수 없습니다.';
      }
    } catch (err) {
      error = '정보를 불러올 수 없습니다.';
    } finally {
      loading = false;
    }
  }

  function initEditFields() {
    if (!connection) return;
    formPgCode = connection.pgCode || '';
    formPgName = connection.pgName || '';
    formApiBaseUrl = connection.apiBaseUrl || '';
    formWebhookBaseUrl = connection.webhookBaseUrl || '';
    formMerchantId = connection.merchantId || '';
    formApiKey = '';
    formSecretKey = '';
    formWebhookSecret = '';
    formStatus = connection.status;
  }

  function resetForm() {
    formPgCode = '';
    formPgName = '';
    formApiBaseUrl = '';
    formWebhookBaseUrl = '';
    formMerchantId = '';
    formApiKey = '';
    formSecretKey = '';
    formWebhookSecret = '';
    formStatus = PgConnectionStatus.ACTIVE;
  }

  function toggleEditMode() {
    editMode = !editMode;
    if (!editMode && connection) {
      initEditFields();
    }
  }

  async function handleSave() {
    if (!formPgCode || !formPgName || !formApiBaseUrl || !formMerchantId) {
      toast.error('필수 항목을 입력해주세요.');
      return;
    }

    if (isNewMode && (!formApiKey || !formSecretKey)) {
      toast.error('API Key와 Secret Key는 필수입니다.');
      return;
    }

    saving = true;
    error = null;

    try {
      if (isNewMode) {
        const createData: PgConnectionCreateRequest = {
          pgCode: formPgCode,
          pgName: formPgName,
          apiBaseUrl: formApiBaseUrl,
          webhookBaseUrl: formWebhookBaseUrl || undefined,
          merchantId: formMerchantId,
          apiKey: formApiKey,
          secretKey: formSecretKey,
          webhookSecret: formWebhookSecret || undefined
        };

        const response = await pgConnectionApi.create(createData);
        if (response.success && response.data) {
          toast.success('PG 연결이 등록되었습니다.');
          tabStore.closeTab('pg-connection-new');
          tabStore.openTab({
            id: `pg-connection-${response.data.id}`,
            title: response.data.pgName,
            icon: '🔗',
            component: 'PgConnectionDetail',
            closeable: true,
            props: { connectionId: response.data.id }
          });
        } else {
          error = response.error?.message || '등록에 실패했습니다.';
        }
      } else if (connection) {
        const updateData: PgConnectionUpdateRequest = {
          pgName: formPgName,
          apiBaseUrl: formApiBaseUrl,
          webhookBaseUrl: formWebhookBaseUrl || undefined,
          merchantId: formMerchantId,
          apiKey: formApiKey || undefined,
          secretKey: formSecretKey || undefined,
          webhookSecret: formWebhookSecret || undefined,
          status: formStatus
        };

        const response = await pgConnectionApi.update(connection.id, updateData);
        if (response.success && response.data) {
          connection = response.data;
          editMode = false;
          toast.success('저장되었습니다.');
        } else {
          error = response.error?.message || '저장에 실패했습니다.';
        }
      }
    } catch (err) {
      error = isNewMode ? '등록에 실패했습니다.' : '저장에 실패했습니다.';
    } finally {
      saving = false;
    }
  }

  async function handleToggleStatus() {
    if (!connection) return;
    
    const newStatus = connection.status === PgConnectionStatus.ACTIVE 
      ? PgConnectionStatus.INACTIVE 
      : PgConnectionStatus.ACTIVE;

    try {
      const response = await pgConnectionApi.updateStatus(connection.id, newStatus);
      if (response.success && response.data) {
        connection = response.data;
        toast.success(`상태가 ${PG_CONNECTION_STATUS_LABELS[newStatus]}(으)로 변경되었습니다.`);
      } else {
        toast.error(response.error?.message || '상태 변경에 실패했습니다.');
      }
    } catch (err) {
      toast.error('상태 변경에 실패했습니다.');
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
      const date = new Date(dateStr);
      return date.toLocaleDateString('ko-KR', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit'
      });
    } catch {
      return '-';
    }
  }

  function handleCancel() {
    if (isNewMode) {
      tabStore.closeTab('pg-connection-new');
    } else {
      toggleEditMode();
    }
  }
</script>

<div class="h-full flex flex-col bg-muted/30">
  {#if loading}
    <div class="flex-1 flex flex-col items-center justify-center gap-4 text-muted-foreground p-12">
      <div class="w-9 h-9 border-3 border-muted border-t-primary rounded-full animate-spin"></div>
      <span>불러오는 중...</span>
    </div>
  {:else if error && !isNewMode}
    <div class="flex-1 flex flex-col items-center justify-center gap-4 text-destructive p-12">
      <span class="flex items-center justify-center w-12 h-12 rounded-full bg-destructive/10 text-destructive text-2xl font-bold">!</span>
      <span>{error}</span>
      <Button variant="outline" onclick={loadConnection}>다시 시도</Button>
    </div>
  {:else}
    <div class="flex justify-between items-start p-6 bg-background border-b border-border">
      <div class="flex flex-col gap-2">
        {#if isNewMode}
          <h1 class="text-2xl font-bold text-foreground">PG 연결 등록</h1>
          <p class="text-sm text-muted-foreground">새로운 PG 연결 정보를 입력하세요</p>
        {:else if connection}
          <h1 class="text-2xl font-bold text-foreground">{connection.pgName}</h1>
          <div class="flex items-center gap-3">
            <span class="font-mono text-sm text-muted-foreground">{connection.pgCode}</span>
            <Badge variant={getStatusVariant(connection.status)}>
              {PG_CONNECTION_STATUS_LABELS[connection.status]}
            </Badge>
          </div>
        {/if}
      </div>
      <div class="flex gap-2">
        {#if editMode}
          <Button variant="outline" onclick={handleCancel} disabled={saving}>취소</Button>
          <Button onclick={handleSave} disabled={saving || !formPgCode || !formPgName || !formApiBaseUrl || !formMerchantId}>
            {saving ? '저장 중...' : (isNewMode ? '등록' : '저장')}
          </Button>
        {:else}
          <Button variant="outline" onclick={handleToggleStatus}>
            {connection?.status === PgConnectionStatus.ACTIVE ? '비활성화' : '활성화'}
          </Button>
          <Button onclick={toggleEditMode}>수정</Button>
        {/if}
      </div>
    </div>

    <div class="flex-1 overflow-y-auto p-6 flex flex-col gap-6">
      {#if error}
        <div class="p-4 rounded-lg bg-destructive/10 border border-destructive/20 text-destructive text-sm">
          {error}
        </div>
      {/if}

      <Card>
        <CardHeader>
          <CardTitle class="text-base">기본 정보</CardTitle>
        </CardHeader>
        <CardContent>
          <div class="grid grid-cols-2 gap-5">
            <div class="flex flex-col gap-1.5">
              <Label for="pg-code" class="text-xs font-medium text-muted-foreground">
                PG 코드 <span class="text-destructive">*</span>
              </Label>
              {#if editMode}
                <Input
                  id="pg-code"
                  type="text"
                  placeholder="예: KORPAY"
                  value={formPgCode}
                  oninput={(e) => formPgCode = e.currentTarget.value.toUpperCase()}
                  disabled={!isNewMode}
                  class="font-mono uppercase"
                />
                {#if !isNewMode}
                  <p class="text-xs text-muted-foreground">코드는 수정할 수 없습니다</p>
                {/if}
              {:else}
                <span class="text-sm font-mono">{connection?.pgCode || '-'}</span>
              {/if}
            </div>

            <div class="flex flex-col gap-1.5">
              <Label for="pg-name" class="text-xs font-medium text-muted-foreground">
                PG 이름 <span class="text-destructive">*</span>
              </Label>
              {#if editMode}
                <Input
                  id="pg-name"
                  type="text"
                  placeholder="예: 코르페이"
                  value={formPgName}
                  oninput={(e) => formPgName = e.currentTarget.value}
                />
              {:else}
                <span class="text-sm">{connection?.pgName || '-'}</span>
              {/if}
            </div>

            <div class="flex flex-col gap-1.5 col-span-2">
              <Label for="api-base-url" class="text-xs font-medium text-muted-foreground">
                API Base URL <span class="text-destructive">*</span>
              </Label>
              {#if editMode}
                <Input
                  id="api-base-url"
                  type="text"
                  placeholder="https://api.pg.example.com"
                  value={formApiBaseUrl}
                  oninput={(e) => formApiBaseUrl = e.currentTarget.value}
                  class="font-mono"
                />
              {:else}
                <span class="text-sm font-mono">{connection?.apiBaseUrl || '-'}</span>
              {/if}
            </div>

            <div class="flex flex-col gap-1.5 col-span-2">
              <Label for="webhook-base-url" class="text-xs font-medium text-muted-foreground">Webhook Base URL</Label>
              {#if editMode}
                <Input
                  id="webhook-base-url"
                  type="text"
                  placeholder="https://your-domain.com/webhook"
                  value={formWebhookBaseUrl}
                  oninput={(e) => formWebhookBaseUrl = e.currentTarget.value}
                  class="font-mono"
                />
                <p class="text-xs text-muted-foreground">PG사에서 결제 알림을 수신할 URL입니다</p>
              {:else}
                <span class="text-sm font-mono">{connection?.webhookBaseUrl || '-'}</span>
              {/if}
            </div>

            <div class="flex flex-col gap-1.5">
              <Label for="status" class="text-xs font-medium text-muted-foreground">상태</Label>
              {#if editMode}
                <select
                  id="status"
                  bind:value={formStatus}
                  class="h-10 px-3 rounded-md border border-input bg-background text-sm focus:outline-none focus:ring-2 focus:ring-ring"
                >
                  {#each Object.values(PgConnectionStatus) as status}
                    <option value={status}>{PG_CONNECTION_STATUS_LABELS[status]}</option>
                  {/each}
                </select>
              {:else}
                <Badge variant={getStatusVariant(connection?.status || PgConnectionStatus.INACTIVE)}>
                  {connection ? PG_CONNECTION_STATUS_LABELS[connection.status] : '-'}
                </Badge>
              {/if}
            </div>
          </div>
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle class="text-base">인증 정보</CardTitle>
        </CardHeader>
        <CardContent>
          <div class="grid grid-cols-2 gap-5">
            <div class="flex flex-col gap-1.5">
              <Label for="merchant-id" class="text-xs font-medium text-muted-foreground">
                가맹점 ID <span class="text-destructive">*</span>
              </Label>
              {#if editMode}
                <Input
                  id="merchant-id"
                  type="text"
                  placeholder="PG사에서 발급받은 가맹점 ID"
                  value={formMerchantId}
                  oninput={(e) => formMerchantId = e.currentTarget.value}
                  class="font-mono"
                />
              {:else}
                <span class="text-sm font-mono">{connection?.merchantId || '-'}</span>
              {/if}
            </div>

            <div class="flex flex-col gap-1.5">
              <Label for="webhook-secret" class="text-xs font-medium text-muted-foreground">Webhook Secret</Label>
              {#if editMode}
                <Input
                  id="webhook-secret"
                  type="password"
                  placeholder={isNewMode ? 'Webhook 검증 키' : '변경하려면 입력하세요'}
                  value={formWebhookSecret}
                  oninput={(e) => formWebhookSecret = e.currentTarget.value}
                  class="font-mono"
                />
              {:else}
                <span class="text-sm font-mono">********</span>
              {/if}
            </div>

            <div class="flex flex-col gap-1.5">
              <Label for="api-key" class="text-xs font-medium text-muted-foreground">
                API Key {#if isNewMode}<span class="text-destructive">*</span>{/if}
              </Label>
              {#if editMode}
                <Input
                  id="api-key"
                  type="text"
                  placeholder={isNewMode ? 'API 인증 키' : '변경하려면 입력하세요'}
                  value={formApiKey}
                  oninput={(e) => formApiKey = e.currentTarget.value}
                  class="font-mono"
                />
                {#if !isNewMode}
                  <p class="text-xs text-muted-foreground">비워두면 기존 값이 유지됩니다</p>
                {/if}
              {:else}
                <span class="text-sm font-mono">********</span>
              {/if}
            </div>

            <div class="flex flex-col gap-1.5">
              <Label for="secret-key" class="text-xs font-medium text-muted-foreground">
                Secret Key {#if isNewMode}<span class="text-destructive">*</span>{/if}
              </Label>
              {#if editMode}
                <Input
                  id="secret-key"
                  type="password"
                  placeholder={isNewMode ? 'Secret 키' : '변경하려면 입력하세요'}
                  value={formSecretKey}
                  oninput={(e) => formSecretKey = e.currentTarget.value}
                  class="font-mono"
                />
                {#if !isNewMode}
                  <p class="text-xs text-muted-foreground">비워두면 기존 값이 유지됩니다</p>
                {/if}
              {:else}
                <span class="text-sm font-mono">********</span>
              {/if}
            </div>
          </div>
        </CardContent>
      </Card>

      {#if !isNewMode && connection}
        <Card>
          <CardHeader>
            <CardTitle class="text-base">등록 정보</CardTitle>
          </CardHeader>
          <CardContent>
            <div class="grid grid-cols-2 gap-5">
              <div class="flex flex-col gap-1.5">
                <span class="text-xs font-medium text-muted-foreground">등록일시</span>
                <span class="text-sm text-muted-foreground">{formatDate(connection.createdAt)}</span>
              </div>
              <div class="flex flex-col gap-1.5">
                <span class="text-xs font-medium text-muted-foreground">수정일시</span>
                <span class="text-sm text-muted-foreground">{formatDate(connection.updatedAt)}</span>
              </div>
            </div>
          </CardContent>
        </Card>
      {/if}
    </div>
  {/if}
</div>
