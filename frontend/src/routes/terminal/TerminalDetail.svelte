<script lang="ts">
  import { onMount } from 'svelte';
  import { toast } from 'svelte-sonner';
  import { terminalApi } from '../../lib/terminalApi';
  import { tabStore } from '../../lib/tabStore';
  import {
    type TerminalDto,
    type TerminalUpdateRequest,
    TerminalStatus,
    TERMINAL_STATUS_LABELS,
    TERMINAL_TYPE_LABELS
  } from '../../types/terminal';
  import { Button } from '$lib/components/ui/button';
  import { Badge } from '$lib/components/ui/badge';
  import { Card, CardContent, CardHeader, CardTitle } from '$lib/components/ui/card';

  interface Props {
    terminalId: string;
  }

  let { terminalId }: Props = $props();

  let terminal = $state<TerminalDto | null>(null);
  let loading = $state(true);
  let editMode = $state(false);
  let saving = $state(false);
  let error = $state<string | null>(null);

  let editCatId = $state('');
  let editSerialNumber = $state('');
  let editModel = $state('');
  let editManufacturer = $state('');
  let editInstallAddress = $state('');

  onMount(() => {
    if (terminalId) {
      loadTerminal();
    }
  });

  async function loadTerminal() {
    loading = true;
    error = null;

    try {
      const response = await terminalApi.getTerminalById(terminalId);
      if (response.success && response.data) {
        terminal = response.data;
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
    if (!terminal) return;
    editCatId = terminal.catId || '';
    editSerialNumber = terminal.serialNumber || '';
    editModel = terminal.model || '';
    editManufacturer = terminal.manufacturer || '';
    editInstallAddress = terminal.installAddress || '';
  }

  function toggleEditMode() {
    editMode = !editMode;
    if (!editMode && terminal) {
      initEditFields();
    }
  }

  async function handleSave() {
    if (!terminal) return;
    saving = true;
    error = null;

    try {
      const updateData: TerminalUpdateRequest = {
        catId: editCatId || undefined,
        serialNumber: editSerialNumber || undefined,
        model: editModel || undefined,
        manufacturer: editManufacturer || undefined,
        installAddress: editInstallAddress || undefined
      };

      const response = await terminalApi.update(terminal.id, updateData);
      if (response.success && response.data) {
        terminal = response.data;
        editMode = false;
        toast.success('저장되었습니다.');
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
      case TerminalStatus.ACTIVE:
        return 'default';
      case TerminalStatus.INACTIVE:
        return 'secondary';
      case TerminalStatus.SUSPENDED:
        return 'outline';
      case TerminalStatus.TERMINATED:
        return 'destructive';
      default:
        return 'outline';
    }
  }

  const TYPE_COLORS: Record<string, { bg: string; border: string; text: string }> = {
    CAT: { bg: 'bg-indigo-500/10', border: 'border-indigo-500/40', text: 'text-indigo-600' },
    POS: { bg: 'bg-violet-500/10', border: 'border-violet-500/40', text: 'text-violet-600' },
    MOBILE: { bg: 'bg-blue-500/10', border: 'border-blue-500/40', text: 'text-blue-600' },
    KIOSK: { bg: 'bg-cyan-500/10', border: 'border-cyan-500/40', text: 'text-cyan-600' },
    ONLINE: { bg: 'bg-emerald-500/10', border: 'border-emerald-500/40', text: 'text-emerald-600' }
  };

  function getTypeClasses(terminalType: string): string {
    const colors = TYPE_COLORS[terminalType];
    if (!colors) return '';
    return `${colors.bg} ${colors.border} ${colors.text} border`;
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

  function openMerchantDetail() {
    if (!terminal?.merchantId) return;
    tabStore.openTab({
      id: `merchant-${terminal.merchantId}`,
      title: terminal.merchantName || '가맹점',
      icon: '🏪',
      component: 'MerchantDetail',
      closeable: true,
      props: { merchantId: terminal.merchantId }
    });
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
      <Button variant="outline" onclick={loadTerminal}>다시 시도</Button>
    </div>
  {:else if terminal}
    <div class="flex justify-between items-start p-6 bg-background border-b border-border">
      <div class="flex flex-col gap-2">
        <h1 class="text-2xl font-bold text-foreground font-mono">{terminal.tid}</h1>
        <div class="flex items-center gap-3">
          {#if terminal.catId}
            <span class="font-mono text-sm text-muted-foreground">CAT: {terminal.catId}</span>
          {/if}
          <Badge variant="outline" class="text-xs font-medium {getTypeClasses(terminal.terminalType)}">
            {TERMINAL_TYPE_LABELS[terminal.terminalType] || terminal.terminalType}
          </Badge>
          <Badge variant={getStatusVariant(terminal.status)}>
            {TERMINAL_STATUS_LABELS[terminal.status] || terminal.status}
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

    <div class="flex-1 overflow-y-auto p-6 flex flex-col gap-6">
      <Card>
        <CardHeader>
          <CardTitle class="text-base">기본정보</CardTitle>
        </CardHeader>
        <CardContent>
          <div class="grid grid-cols-2 gap-5">
            <div class="flex flex-col gap-1.5">
              <span class="text-xs font-medium text-muted-foreground">TID</span>
              <span class="text-sm font-mono font-medium">{terminal.tid}</span>
            </div>
            <div class="flex flex-col gap-1.5">
              <span class="text-xs font-medium text-muted-foreground">CAT ID</span>
              {#if editMode}
                <input type="text" bind:value={editCatId} class="h-9 px-3 rounded-md border border-input bg-background text-sm focus:outline-none focus:ring-2 focus:ring-ring font-mono" />
              {:else}
                <span class="text-sm font-mono">{terminal.catId || '-'}</span>
              {/if}
            </div>
            <div class="flex flex-col gap-1.5">
              <span class="text-xs font-medium text-muted-foreground">단말기유형</span>
              <span class="text-sm">{TERMINAL_TYPE_LABELS[terminal.terminalType] || terminal.terminalType}</span>
            </div>
            <div class="flex flex-col gap-1.5">
              <span class="text-xs font-medium text-muted-foreground">가맹점</span>
              {#if terminal.merchantId}
                <button
                  type="button"
                  class="text-sm text-primary hover:underline text-left cursor-pointer"
                  onclick={openMerchantDetail}
                >
                  {terminal.merchantName || '-'}
                  {#if terminal.merchantCode}
                    <span class="text-muted-foreground font-mono">({terminal.merchantCode})</span>
                  {/if}
                </button>
              {:else}
                <span class="text-sm">-</span>
              {/if}
            </div>
            <div class="flex flex-col gap-1.5">
              <span class="text-xs font-medium text-muted-foreground">영업점</span>
              <span class="text-sm">{terminal.organizationName || '-'}</span>
            </div>
            <div class="flex flex-col gap-1.5">
              <span class="text-xs font-medium text-muted-foreground">최근 거래일시</span>
              <span class="text-sm text-muted-foreground">{formatDate(terminal.lastTransactionAt)}</span>
            </div>
          </div>
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle class="text-base">단말기정보</CardTitle>
        </CardHeader>
        <CardContent>
          <div class="grid grid-cols-2 gap-5">
            <div class="flex flex-col gap-1.5">
              <span class="text-xs font-medium text-muted-foreground">시리얼번호</span>
              {#if editMode}
                <input type="text" bind:value={editSerialNumber} class="h-9 px-3 rounded-md border border-input bg-background text-sm focus:outline-none focus:ring-2 focus:ring-ring font-mono" />
              {:else}
                <span class="text-sm font-mono">{terminal.serialNumber || '-'}</span>
              {/if}
            </div>
            <div class="flex flex-col gap-1.5">
              <span class="text-xs font-medium text-muted-foreground">모델명</span>
              {#if editMode}
                <input type="text" bind:value={editModel} class="h-9 px-3 rounded-md border border-input bg-background text-sm focus:outline-none focus:ring-2 focus:ring-ring" />
              {:else}
                <span class="text-sm">{terminal.model || '-'}</span>
              {/if}
            </div>
            <div class="flex flex-col gap-1.5">
              <span class="text-xs font-medium text-muted-foreground">제조사</span>
              {#if editMode}
                <input type="text" bind:value={editManufacturer} class="h-9 px-3 rounded-md border border-input bg-background text-sm focus:outline-none focus:ring-2 focus:ring-ring" />
              {:else}
                <span class="text-sm">{terminal.manufacturer || '-'}</span>
              {/if}
            </div>
            <div class="flex flex-col gap-1.5">
              <span class="text-xs font-medium text-muted-foreground">설치일</span>
              <span class="text-sm">{terminal.installDate || '-'}</span>
            </div>
            <div class="flex flex-col gap-1.5 col-span-2">
              <span class="text-xs font-medium text-muted-foreground">설치주소</span>
              {#if editMode}
                <input type="text" bind:value={editInstallAddress} class="h-9 px-3 rounded-md border border-input bg-background text-sm focus:outline-none focus:ring-2 focus:ring-ring" />
              {:else}
                <span class="text-sm">{terminal.installAddress || '-'}</span>
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
              <span class="text-xs font-medium text-muted-foreground">등록일시</span>
              <span class="text-sm text-muted-foreground">{formatDate(terminal.createdAt)}</span>
            </div>
            <div class="flex flex-col gap-1.5">
              <span class="text-xs font-medium text-muted-foreground">수정일시</span>
              <span class="text-sm text-muted-foreground">{formatDate(terminal.updatedAt)}</span>
            </div>
          </div>
        </CardContent>
      </Card>
    </div>
  {/if}
</div>
