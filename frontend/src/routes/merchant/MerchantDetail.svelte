<script lang="ts">
  import { onMount } from 'svelte';
  import { merchantApi } from '../../lib/merchantApi';
  import {
    type MerchantDto,
    type MerchantUpdateRequest,
    MerchantStatus,
    MERCHANT_BUSINESS_TYPE_LABELS
  } from '../../types/merchant';
  import { BusinessType } from '../../types/branch';
  import { Button } from '$lib/components/ui/button';
  import { Badge } from '$lib/components/ui/badge';
  import { Card, CardContent, CardHeader, CardTitle } from '$lib/components/ui/card';
  import MerchantTransactions from './MerchantTransactions.svelte';
  import MerchantSettlements from './MerchantSettlements.svelte';

  interface Props {
    merchantId: string;
  }

  let { merchantId }: Props = $props();

  let merchant = $state<MerchantDto | null>(null);
  let loading = $state(true);
  let editMode = $state(false);
  let saving = $state(false);
  let error = $state<string | null>(null);

  let editName = $state('');
  let editContactName = $state('');
  let editContactPhone = $state('');
  let editContactEmail = $state('');
  let editAddress = $state('');

  let activeSection = $state<'basic' | 'transaction' | 'settlement'>('basic');

  const STATUS_LABELS: Record<MerchantStatus, string> = {
    [MerchantStatus.ACTIVE]: '정상',
    [MerchantStatus.SUSPENDED]: '정지',
    [MerchantStatus.TERMINATED]: '해지'
  };

  onMount(() => {
    if (merchantId) {
      loadMerchant();
    }
  });

  async function loadMerchant() {
    loading = true;
    error = null;

    try {
      const response = await merchantApi.getMerchantById(merchantId);
      if (response.success && response.data) {
        merchant = response.data;
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
    if (!merchant) return;
    editName = merchant.name || '';
    editContactName = merchant.primaryContact?.name || '';
    editContactPhone = merchant.primaryContact?.phone || '';
    editContactEmail = merchant.primaryContact?.email || '';
    editAddress = merchant.address || '';
  }

  function toggleEditMode() {
    editMode = !editMode;
    if (!editMode && merchant) {
      initEditFields();
    }
  }

  async function handleSave() {
    if (!merchant) return;
    saving = true;
    error = null;

    try {
      const updateData: MerchantUpdateRequest = {
        name: editName,
        contactName: editContactName,
        contactPhone: editContactPhone,
        contactEmail: editContactEmail,
        address: editAddress,
      };

      const response = await merchantApi.update(merchant.id, updateData);
      if (response.success && response.data) {
        merchant = response.data;
        editMode = false;
      } else {
        error = response.error?.message || '저장에 실패했습니다.';
      }
    } catch (err) {
      error = '저장에 실패했습니다.';
    } finally {
      saving = false;
    }
  }

  function getStatusVariant(status: string): 'default' | 'secondary' | 'destructive' | 'outline' {
    switch (status) {
      case MerchantStatus.ACTIVE:
        return 'default';
      case MerchantStatus.SUSPENDED:
        return 'secondary';
      case MerchantStatus.TERMINATED:
        return 'destructive';
      default:
        return 'outline';
    }
  }

  function formatBusinessNumber(num?: string): string {
    if (!num) return '-';
    const digits = num.replace(/\D/g, '');
    if (digits.length !== 10) return num;
    return `${digits.slice(0, 3)}-${digits.slice(3, 5)}-${digits.slice(5)}`;
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
</script>

<div class="h-full flex flex-col bg-muted/30">
  {#if loading}
    <div class="flex-1 flex flex-col items-center justify-center gap-4 text-muted-foreground p-12">
      <div class="w-9 h-9 border-3 border-muted border-t-primary rounded-full animate-spin"></div>
      <span>불러오는 중...</span>
    </div>
  {:else if error}
    <div class="flex-1 flex flex-col items-center justify-center gap-4 text-destructive p-12">
      <span class="flex items-center justify-center w-12 h-12 rounded-full bg-destructive/10 text-destructive text-2xl font-bold">!</span>
      <span>{error}</span>
      <Button variant="outline" onclick={loadMerchant}>다시 시도</Button>
    </div>
  {:else if merchant}
    <div class="flex justify-between items-start p-6 bg-background border-b border-border">
      <div class="flex flex-col gap-2">
        <h1 class="text-2xl font-bold text-foreground">{merchant.name}</h1>
        <div class="flex items-center gap-3">
          <span class="font-mono text-sm text-muted-foreground">{merchant.merchantCode}</span>
          {#if merchant.businessType}
            <span class="text-sm text-primary font-medium">
              {MERCHANT_BUSINESS_TYPE_LABELS[merchant.businessType as BusinessType] || merchant.businessType}
            </span>
          {/if}
          <Badge variant={getStatusVariant(merchant.status)}>
            {STATUS_LABELS[merchant.status] || merchant.status}
          </Badge>
        </div>
      </div>
      <div class="flex gap-2">
        {#if editMode}
          <Button variant="outline" onclick={toggleEditMode} disabled={saving}>취소</Button>
          <Button onclick={handleSave} disabled={saving}>
            {saving ? '저장 중...' : '저장'}
          </Button>
        {:else}
          <Button onclick={toggleEditMode}>수정</Button>
        {/if}
      </div>
    </div>

    <div class="flex gap-0 bg-background border-b border-border px-6">
      <button
        class="px-6 py-4 text-sm font-medium text-muted-foreground border-b-2 border-transparent transition-colors hover:text-foreground data-[active=true]:text-primary data-[active=true]:border-primary"
        data-active={activeSection === 'basic'}
        onclick={() => activeSection = 'basic'}
      >
        기본정보
      </button>
      <button
        class="px-6 py-4 text-sm font-medium text-muted-foreground border-b-2 border-transparent transition-colors hover:text-foreground data-[active=true]:text-primary data-[active=true]:border-primary"
        data-active={activeSection === 'transaction'}
        onclick={() => activeSection = 'transaction'}
      >
        거래내역
      </button>
      <button
        class="px-6 py-4 text-sm font-medium text-muted-foreground border-b-2 border-transparent transition-colors hover:text-foreground data-[active=true]:text-primary data-[active=true]:border-primary"
        data-active={activeSection === 'settlement'}
        onclick={() => activeSection = 'settlement'}
      >
        정산내역
      </button>
    </div>

    <div class="flex-1 overflow-y-auto p-6 flex flex-col gap-6">
      {#if activeSection === 'basic'}
        <Card>
          <CardHeader>
            <CardTitle class="text-base">사업자정보</CardTitle>
          </CardHeader>
          <CardContent>
            <div class="grid grid-cols-2 gap-5">
              <div class="flex flex-col gap-1.5">
                <label class="text-xs font-medium text-muted-foreground">사업자번호</label>
                <span class="text-sm font-mono">{formatBusinessNumber(merchant.businessNumber)}</span>
              </div>
              <div class="flex flex-col gap-1.5">
                <label class="text-xs font-medium text-muted-foreground">상호</label>
                {#if editMode}
                  <input type="text" bind:value={editName} class="h-9 px-3 rounded-md border border-input bg-background text-sm focus:outline-none focus:ring-2 focus:ring-ring" />
                {:else}
                  <span class="text-sm">{merchant.name}</span>
                {/if}
              </div>
              <div class="flex flex-col gap-1.5">
                <label class="text-xs font-medium text-muted-foreground">사업자유형</label>
                <span class="text-sm">
                  {merchant.businessType ? MERCHANT_BUSINESS_TYPE_LABELS[merchant.businessType as BusinessType] || merchant.businessType : '-'}
                </span>
              </div>
              <div class="flex flex-col gap-1.5">
                <label class="text-xs font-medium text-muted-foreground">소속 영업점</label>
                <span class="text-sm">{merchant.organizationName || '-'}</span>
              </div>
              <div class="flex flex-col gap-1.5 col-span-2">
                <label class="text-xs font-medium text-muted-foreground">주소</label>
                {#if editMode}
                  <input type="text" bind:value={editAddress} class="h-9 px-3 rounded-md border border-input bg-background text-sm focus:outline-none focus:ring-2 focus:ring-ring" />
                {:else}
                  <span class="text-sm">{merchant.address || '-'}</span>
                {/if}
              </div>
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle class="text-base">담당자정보</CardTitle>
          </CardHeader>
          <CardContent>
            <div class="grid grid-cols-2 gap-5">
              <div class="flex flex-col gap-1.5">
                <label class="text-xs font-medium text-muted-foreground">담당자명</label>
                {#if editMode}
                  <input type="text" bind:value={editContactName} class="h-9 px-3 rounded-md border border-input bg-background text-sm focus:outline-none focus:ring-2 focus:ring-ring" />
                {:else}
                  <span class="text-sm">{merchant.primaryContact?.name || '-'}</span>
                {/if}
              </div>
              <div class="flex flex-col gap-1.5">
                <label class="text-xs font-medium text-muted-foreground">연락처</label>
                {#if editMode}
                  <input type="text" bind:value={editContactPhone} placeholder="010-0000-0000" class="h-9 px-3 rounded-md border border-input bg-background text-sm focus:outline-none focus:ring-2 focus:ring-ring" />
                {:else}
                  <span class="text-sm">{merchant.primaryContact?.phone || '-'}</span>
                {/if}
              </div>
              <div class="flex flex-col gap-1.5 col-span-2">
                <label class="text-xs font-medium text-muted-foreground">이메일</label>
                {#if editMode}
                  <input type="email" bind:value={editContactEmail} placeholder="example@email.com" class="h-9 px-3 rounded-md border border-input bg-background text-sm focus:outline-none focus:ring-2 focus:ring-ring" />
                {:else}
                  <span class="text-sm">{merchant.primaryContact?.email || '-'}</span>
                {/if}
              </div>
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle class="text-base">등록정보</CardTitle>
          </CardHeader>
          <CardContent>
            <div class="grid grid-cols-2 gap-5">
              <div class="flex flex-col gap-1.5">
                <label class="text-xs font-medium text-muted-foreground">등록일시</label>
                <span class="text-sm text-muted-foreground">{formatDate(merchant.createdAt)}</span>
              </div>
              <div class="flex flex-col gap-1.5">
                <label class="text-xs font-medium text-muted-foreground">수정일시</label>
                <span class="text-sm text-muted-foreground">{formatDate(merchant.updatedAt)}</span>
              </div>
            </div>
          </CardContent>
        </Card>

      {:else if activeSection === 'transaction'}
        <Card>
          <CardHeader>
            <CardTitle class="text-base">거래내역</CardTitle>
          </CardHeader>
          <CardContent>
            <MerchantTransactions merchantId={merchantId} />
          </CardContent>
        </Card>

      {:else if activeSection === 'settlement'}
        <Card>
          <CardHeader>
            <CardTitle class="text-base">정산내역</CardTitle>
          </CardHeader>
          <CardContent>
            <MerchantSettlements merchantId={merchantId} />
          </CardContent>
        </Card>
      {/if}
    </div>
  {/if}
</div>
