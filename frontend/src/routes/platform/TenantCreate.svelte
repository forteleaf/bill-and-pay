<script lang="ts">
  import { platformApi } from '@/api/platform';
  import type { TenantCreateRequest } from '@/types/platform';
  import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '$lib/components/ui/card';
  import { Button } from '$lib/components/ui/button';
  import { Input } from '$lib/components/ui/input';
  import { Label } from '$lib/components/ui/label';
  import { tabStore, type Tab } from '@/stores/tab';

  let step = $state(1);
  let loading = $state(false);
  let error = $state<string | null>(null);

  // Step 1: 기본 정보
  let tenantId = $state('');
  let tenantName = $state('');
  let contactEmail = $state('');
  let contactPhone = $state('');

  // Step 2: 관리자 계정
  let adminUsername = $state('');
  let adminPassword = $state('');
  let adminEmail = $state('');

  function nextStep() {
    if (step === 1) {
      if (!tenantId || !tenantName) {
        error = '테넌트 ID와 이름은 필수입니다.';
        return;
      }
      if (!/^[a-z0-9_]{3,30}$/.test(tenantId)) {
        error = '테넌트 ID는 영소문자, 숫자, 언더스코어만 가능합니다 (3~30자).';
        return;
      }
    }
    if (step === 2) {
      if (!adminUsername || !adminPassword) {
        error = '관리자 사용자명과 비밀번호는 필수입니다.';
        return;
      }
    }
    error = null;
    step = Math.min(step + 1, 3);
  }

  function prevStep() {
    error = null;
    step = Math.max(step - 1, 1);
  }

  async function handleCreate() {
    loading = true;
    error = null;

    const request: TenantCreateRequest = {
      tenantId,
      name: tenantName,
      contactEmail: contactEmail || undefined,
      contactPhone: contactPhone || undefined,
      adminUsername,
      adminPassword,
      adminEmail: adminEmail || undefined,
    };

    try {
      const response = await platformApi.createTenant(request);
      if (response.success && response.data) {
        const tab: Tab = {
          id: `tenant-${response.data.id}`,
          title: `테넌트: ${response.data.name}`,
          icon: '🏢',
          component: 'PlatformTenantDetail',
          closeable: true,
          props: { tenantId: response.data.id }
        };
        tabStore.openTab(tab);
      } else {
        error = response.error?.message || '테넌트 생성에 실패했습니다.';
      }
    } catch (err) {
      error = err instanceof Error ? err.message : '테넌트 생성 실패';
    } finally {
      loading = false;
    }
  }
</script>

<div class="max-w-2xl mx-auto space-y-6">
  <h1 class="text-2xl font-bold">테넌트 등록</h1>

  <!-- Steps indicator -->
  <div class="flex items-center gap-2">
    {#each [1, 2, 3] as s}
      <div class="flex items-center gap-2">
        <div class="w-8 h-8 rounded-full flex items-center justify-center text-sm font-medium {step >= s ? 'bg-primary text-primary-foreground' : 'bg-muted text-muted-foreground'}">
          {s}
        </div>
        <span class="text-sm {step >= s ? 'font-medium' : 'text-muted-foreground'}">
          {s === 1 ? '기본 정보' : s === 2 ? '관리자 계정' : '확인'}
        </span>
        {#if s < 3}
          <div class="w-8 h-px {step > s ? 'bg-primary' : 'bg-muted'}"></div>
        {/if}
      </div>
    {/each}
  </div>

  {#if error}
    <div class="p-3 rounded-md bg-destructive/10 border border-destructive/20 text-destructive text-sm">{error}</div>
  {/if}

  {#if step === 1}
    <Card>
      <CardHeader>
        <CardTitle>기본 정보</CardTitle>
        <CardDescription>테넌트의 기본 정보를 입력하세요.</CardDescription>
      </CardHeader>
      <CardContent class="space-y-4">
        <div class="space-y-2">
          <Label>테넌트 ID *</Label>
          <Input value={tenantId} oninput={(e) => tenantId = e.currentTarget.value} placeholder="영소문자, 숫자, 언더스코어 (예: beta_inc)" />
          <p class="text-xs text-muted-foreground">스키마명: tenant_{tenantId || '???'}</p>
        </div>
        <div class="space-y-2">
          <Label>테넌트 이름 *</Label>
          <Input value={tenantName} oninput={(e) => tenantName = e.currentTarget.value} placeholder="테넌트 이름" />
        </div>
        <div class="space-y-2">
          <Label>연락처 이메일</Label>
          <Input type="email" value={contactEmail} oninput={(e) => contactEmail = e.currentTarget.value} placeholder="admin@example.com" />
        </div>
        <div class="space-y-2">
          <Label>연락처 전화번호</Label>
          <Input value={contactPhone} oninput={(e) => contactPhone = e.currentTarget.value} placeholder="01012345678" />
        </div>
      </CardContent>
    </Card>
  {:else if step === 2}
    <Card>
      <CardHeader>
        <CardTitle>관리자 계정</CardTitle>
        <CardDescription>해당 테넌트의 초기 관리자 계정을 설정합니다.</CardDescription>
      </CardHeader>
      <CardContent class="space-y-4">
        <div class="space-y-2">
          <Label>사용자명 *</Label>
          <Input value={adminUsername} oninput={(e) => adminUsername = e.currentTarget.value} placeholder="admin" />
        </div>
        <div class="space-y-2">
          <Label>초기 비밀번호 *</Label>
          <Input type="password" value={adminPassword} oninput={(e) => adminPassword = e.currentTarget.value} placeholder="초기 비밀번호" />
        </div>
        <div class="space-y-2">
          <Label>이메일</Label>
          <Input type="email" value={adminEmail} oninput={(e) => adminEmail = e.currentTarget.value} placeholder="admin@example.com" />
        </div>
      </CardContent>
    </Card>
  {:else}
    <Card>
      <CardHeader>
        <CardTitle>확인</CardTitle>
        <CardDescription>입력한 정보를 확인하고 테넌트를 생성합니다.</CardDescription>
      </CardHeader>
      <CardContent class="space-y-4">
        <div class="grid grid-cols-2 gap-3 text-sm">
          <div class="text-muted-foreground">테넌트 ID:</div>
          <div class="font-mono font-medium">{tenantId}</div>
          <div class="text-muted-foreground">테넌트 이름:</div>
          <div class="font-medium">{tenantName}</div>
          <div class="text-muted-foreground">스키마명:</div>
          <div class="font-mono">tenant_{tenantId}</div>
          <div class="text-muted-foreground">연락처:</div>
          <div>{contactEmail || '-'} / {contactPhone || '-'}</div>
          <div class="text-muted-foreground">관리자:</div>
          <div>{adminUsername}</div>
        </div>
        <div class="p-3 rounded-md bg-muted text-sm">
          <p class="font-medium mb-1">자동 실행 항목:</p>
          <ul class="list-disc list-inside space-y-1 text-muted-foreground">
            <li>DB 스키마 생성 (tenant_{tenantId})</li>
            <li>인증 사용자 생성 (public.users)</li>
            <li>테넌트 상태 ACTIVE 전환</li>
          </ul>
        </div>
      </CardContent>
    </Card>
  {/if}

  <div class="flex justify-between">
    <Button variant="outline" disabled={step === 1} onclick={prevStep}>이전</Button>
    {#if step < 3}
      <Button onclick={nextStep}>다음</Button>
    {:else}
      <Button onclick={handleCreate} disabled={loading}>
        {loading ? '생성 중...' : '테넌트 생성'}
      </Button>
    {/if}
  </div>
</div>
