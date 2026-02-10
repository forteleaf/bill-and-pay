<script lang="ts">
  import { onMount } from 'svelte';
  import { toast } from 'svelte-sonner';
  import { branchApi } from '@/api/branch';
  import {
    BRANCH_TYPE_LABELS,
    OrgType,
    type Branch,
    type BranchUpdateRequest
  } from '@/types/branch';
  import { Button } from '$lib/components/ui/button';
  import { Badge } from '$lib/components/ui/badge';
  import { Card, CardContent, CardHeader, CardTitle } from '$lib/components/ui/card';
  import { SettlementAccountEntityType } from '@/types/settlementAccount';
  import SettlementAccountManager from '@/components/settlement/SettlementAccountManager.svelte';

  interface Props {
    branchId: string;
  }

  let { branchId }: Props = $props();

  let branch = $state<Branch | null>(null);
  let loading = $state(true);
  let editMode = $state(false);
  let saving = $state(false);
  let error = $state<string | null>(null);

  let editName = $state('');
  let editPhone = $state('');
  let editEmail = $state('');
  let editManagerName = $state('');
  let editManagerPhone = $state('');
  let editAddress = $state('');

  let activeSection = $state<'basic' | 'fee' | 'settlement'>('basic');
  let activeFeeTab = $state<'terminal' | 'oldAuth' | 'nonAuth' | 'authPay' | 'recurring'>('terminal');

  const feeTabs: Array<{ key: 'terminal' | 'oldAuth' | 'nonAuth' | 'authPay' | 'recurring'; label: string }> = [
    { key: 'terminal', label: '단말기' },
    { key: 'oldAuth', label: '구인증' },
    { key: 'nonAuth', label: '비인증' },
    { key: 'authPay', label: '인증결제' },
    { key: 'recurring', label: '정기과금' }
  ];

  // Initial load on mount - prevents infinite loop from $effect
  onMount(() => {
    if (branchId) {
      loadBranch();
    }
  });

  async function loadBranch() {
    loading = true;
    error = null;

    try {
      const response = await branchApi.getBranchById(branchId);
      if (response.success && response.data) {
        branch = response.data;
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
    if (!branch) return;
    editName = branch.name || '';
    editPhone = branch.businessEntity?.mainPhone || branch.phone || '';
    editEmail = branch.businessEntity?.email || branch.email || '';
    editManagerName = branch.businessEntity?.managerName || '';
    editManagerPhone = branch.businessEntity?.managerPhone || '';
    editAddress = branch.businessEntity?.businessAddress || branch.address || '';
  }

  function toggleEditMode() {
    editMode = !editMode;
    if (!editMode && branch) {
      initEditFields();
    }
  }

  async function handleSave() {
    if (!branch) return;

    saving = true;
    error = null;

    try {
      const data: BranchUpdateRequest = {
        name: editName,
        phone: editPhone,
        email: editEmail,
        address: editAddress
      };

      const response = await branchApi.updateBranch(branch.id, data);

      if (response.success && response.data) {
        branch = response.data;
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

  function formatStatus(status: string): string {
    const statusMap: Record<string, string> = {
      'ACTIVE': '활성',
      'SUSPENDED': '정지',
      'TERMINATED': '해지'
    };
    return statusMap[status] || status;
  }

  function getStatusVariant(status: string): 'default' | 'secondary' | 'destructive' | 'outline' {
    switch (status?.toLowerCase()) {
      case 'active':
        return 'default';
      case 'suspended':
        return 'secondary';
      case 'terminated':
        return 'destructive';
      default:
        return 'outline';
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
      <Button variant="outline" onclick={loadBranch}>다시 시도</Button>
    </div>
  {:else if branch}
    <div class="flex justify-between items-start p-6 bg-background border-b border-border">
      <div class="flex flex-col gap-2">
        <h1 class="text-2xl font-bold text-foreground">{branch.name}</h1>
        <div class="flex items-center gap-3">
          <span class="font-mono text-sm text-muted-foreground">{branch.orgCode}</span>
          <span class="text-sm text-primary font-medium">{BRANCH_TYPE_LABELS[branch.orgType as OrgType] || branch.orgType}</span>
          <Badge variant={getStatusVariant(branch.status)}>{formatStatus(branch.status)}</Badge>
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
        data-active={activeSection === 'fee'}
        onclick={() => activeSection = 'fee'}
      >
        수수료설정
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
                <span class="text-xs font-medium text-muted-foreground">사업자번호</span>
                <span class="text-sm font-mono text-muted-foreground">{branch.businessEntity?.businessNumber || '-'}</span>
              </div>
              <div class="flex flex-col gap-1.5">
                <span class="text-xs font-medium text-muted-foreground">상호</span>
                {#if editMode}
                  <input type="text" bind:value={editName} class="h-9 px-3 rounded-md border border-input bg-background text-sm focus:outline-none focus:ring-2 focus:ring-ring" />
                {:else}
                  <span class="text-sm">{branch.name}</span>
                {/if}
              </div>
              <div class="flex flex-col gap-1.5">
                <span class="text-xs font-medium text-muted-foreground">대표자</span>
                <span class="text-sm">{branch.businessEntity?.representativeName || '-'}</span>
              </div>
              <div class="flex flex-col gap-1.5">
                <span class="text-xs font-medium text-muted-foreground">연락처</span>
                {#if editMode}
                  <input type="text" bind:value={editPhone} placeholder="010-0000-0000" class="h-9 px-3 rounded-md border border-input bg-background text-sm focus:outline-none focus:ring-2 focus:ring-ring" />
                {:else}
                  <span class="text-sm">{branch.businessEntity?.mainPhone || branch.phone || '-'}</span>
                {/if}
              </div>
              <div class="flex flex-col gap-1.5 col-span-2">
                <span class="text-xs font-medium text-muted-foreground">주소</span>
                {#if editMode}
                  <input type="text" bind:value={editAddress} class="h-9 px-3 rounded-md border border-input bg-background text-sm focus:outline-none focus:ring-2 focus:ring-ring" />
                {:else}
                  <span class="text-sm">{branch.businessEntity?.businessAddress || branch.address || '-'}</span>
                {/if}
              </div>
              <div class="flex flex-col gap-1.5">
                <span class="text-xs font-medium text-muted-foreground">담당자</span>
                {#if editMode}
                  <input type="text" bind:value={editManagerName} class="h-9 px-3 rounded-md border border-input bg-background text-sm focus:outline-none focus:ring-2 focus:ring-ring" />
                {:else}
                  <span class="text-sm">{branch.businessEntity?.managerName || '-'}</span>
                {/if}
              </div>
              <div class="flex flex-col gap-1.5">
                <span class="text-xs font-medium text-muted-foreground">담당자 연락처</span>
                {#if editMode}
                  <input type="text" bind:value={editManagerPhone} placeholder="010-0000-0000" class="h-9 px-3 rounded-md border border-input bg-background text-sm focus:outline-none focus:ring-2 focus:ring-ring" />
                {:else}
                  <span class="text-sm">{branch.businessEntity?.managerPhone || '-'}</span>
                {/if}
              </div>
              <div class="flex flex-col gap-1.5 col-span-2">
                <span class="text-xs font-medium text-muted-foreground">이메일</span>
                {#if editMode}
                  <input type="email" bind:value={editEmail} placeholder="example@email.com" class="h-9 px-3 rounded-md border border-input bg-background text-sm focus:outline-none focus:ring-2 focus:ring-ring" />
                {:else}
                  <span class="text-sm">{branch.businessEntity?.email || branch.email || '-'}</span>
                {/if}
              </div>
            </div>
          </CardContent>
        </Card>

         <SettlementAccountManager 
           entityType={SettlementAccountEntityType.BUSINESS_ENTITY} 
           entityId={branch.businessEntity?.id || ''} 
         />

        <Card>
          <CardHeader>
            <CardTitle class="text-base">한도설정</CardTitle>
          </CardHeader>
          <CardContent>
            <div class="grid grid-cols-2 gap-5">
              <div class="flex flex-col gap-1.5">
                <span class="text-xs font-medium text-muted-foreground">1회 한도</span>
                <span class="text-sm font-semibold text-primary">{(branch.limitConfig?.perTransaction || 0).toLocaleString()}백만원</span>
              </div>
              <div class="flex flex-col gap-1.5">
                <span class="text-xs font-medium text-muted-foreground">1일 한도</span>
                <span class="text-sm font-semibold text-primary">{(branch.limitConfig?.perDay || 0).toLocaleString()}백만원</span>
              </div>
            </div>
          </CardContent>
        </Card>

      {:else if activeSection === 'fee'}
        <Card>
          <CardContent class="pt-6">
            <div class="flex gap-1 p-1 bg-muted rounded-lg mb-5">
              {#each feeTabs as tab}
                <button
                  class="flex-1 py-2.5 text-sm font-medium text-muted-foreground rounded-md transition-all hover:text-foreground data-[active=true]:bg-background data-[active=true]:text-foreground data-[active=true]:shadow-sm"
                  data-active={activeFeeTab === tab.key}
                  onclick={() => activeFeeTab = tab.key}
                >
                  {tab.label}
                </button>
              {/each}
            </div>
            
            <div class="bg-muted/50 rounded-lg p-5">
              {#if branch.feeConfig?.[activeFeeTab]}
                <div class="grid grid-cols-3 gap-4">
                  <div class="flex flex-col items-center gap-1.5 p-4 bg-background rounded-lg border border-border">
                    <span class="text-xs font-medium text-muted-foreground">일반</span>
                    <span class="text-xl font-semibold text-primary">{branch.feeConfig[activeFeeTab]!.general}%</span>
                  </div>
                  <div class="flex flex-col items-center gap-1.5 p-4 bg-background rounded-lg border border-border">
                    <span class="text-xs font-medium text-muted-foreground">영세</span>
                    <span class="text-xl font-semibold text-primary">{branch.feeConfig[activeFeeTab]!.small}%</span>
                  </div>
                  <div class="flex flex-col items-center gap-1.5 p-4 bg-background rounded-lg border border-border">
                    <span class="text-xs font-medium text-muted-foreground">중소1</span>
                    <span class="text-xl font-semibold text-primary">{branch.feeConfig[activeFeeTab]!.medium1}%</span>
                  </div>
                  <div class="flex flex-col items-center gap-1.5 p-4 bg-background rounded-lg border border-border">
                    <span class="text-xs font-medium text-muted-foreground">중소2</span>
                    <span class="text-xl font-semibold text-primary">{branch.feeConfig[activeFeeTab]!.medium2}%</span>
                  </div>
                  <div class="flex flex-col items-center gap-1.5 p-4 bg-background rounded-lg border border-border">
                    <span class="text-xs font-medium text-muted-foreground">중소3</span>
                    <span class="text-xl font-semibold text-primary">{branch.feeConfig[activeFeeTab]!.medium3}%</span>
                  </div>
                  <div class="flex flex-col items-center gap-1.5 p-4 bg-background rounded-lg border border-border">
                    <span class="text-xs font-medium text-muted-foreground">해외카드</span>
                    <span class="text-xl font-semibold text-primary">{branch.feeConfig[activeFeeTab]!.foreign}%</span>
                  </div>
                </div>
              {:else}
                <div class="text-center text-muted-foreground text-sm py-8">설정된 수수료가 없습니다.</div>
              {/if}
            </div>
          </CardContent>
        </Card>

      {:else if activeSection === 'settlement'}
        <Card>
          <CardContent class="pt-6">
            <div class="flex flex-col items-center justify-center py-12 text-center text-muted-foreground">
              <span class="text-5xl mb-4">📊</span>
              <h3 class="text-lg font-semibold text-foreground mb-2">정산내역</h3>
              <p class="text-sm">정산내역 조회 기능이 곧 제공될 예정입니다.</p>
            </div>
          </CardContent>
        </Card>
      {/if}
    </div>
  {/if}
</div>
