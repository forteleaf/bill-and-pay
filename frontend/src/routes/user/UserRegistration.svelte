<script lang="ts">
  import { onMount } from 'svelte';
  import { userApi } from '@/api/user';
  import { branchApi } from '@/api/branch';
  import {
    type UserCreateRequest,
    UserRole,
    USER_ROLE_LABELS
  } from '@/types/user';
  import type { Branch } from '@/types/branch';
  import { Button } from '$lib/components/ui/button';
  import { Card, CardContent } from '$lib/components/ui/card';
  import { Label } from '$lib/components/ui/label';

  interface Props {
    oncomplete?: () => void;
    oncancel?: () => void;
  }

  let { oncomplete, oncancel }: Props = $props();

  let username = $state('');
  let password = $state('');
  let confirmPassword = $state('');
  let fullName = $state('');
  let email = $state('');
  let phone = $state('');
  let role = $state<UserRole>(UserRole.VIEWER);
  let orgId = $state('');

  let organizations = $state<Branch[]>([]);
  let loadingOrgs = $state(true);
  let loading = $state(false);
  let errors = $state<Record<string, string>>({});
  let submitResult = $state<{ success: boolean; message: string } | null>(null);

  onMount(() => {
    loadOrganizations();
  });

  async function loadOrganizations() {
    loadingOrgs = true;
    try {
      const response = await branchApi.getBranches({ page: 0, size: 1000 });
      if (response.success && response.data) {
        organizations = response.data.content || [];
      }
    } catch (err) {
      console.error('Failed to load organizations:', err);
    } finally {
      loadingOrgs = false;
    }
  }

  function validate(): boolean {
    const newErrors: Record<string, string> = {};

    if (!username.trim()) {
      newErrors.username = '아이디를 입력해주세요.';
    } else if (username.length < 4) {
      newErrors.username = '아이디는 최소 4자 이상이어야 합니다.';
    } else if (!/^[a-zA-Z0-9_]+$/.test(username)) {
      newErrors.username = '아이디는 영문, 숫자, 밑줄(_)만 사용할 수 있습니다.';
    }

    if (!password) {
      newErrors.password = '비밀번호를 입력해주세요.';
    } else if (password.length < 8) {
      newErrors.password = '비밀번호는 최소 8자 이상이어야 합니다.';
    }

    if (!confirmPassword) {
      newErrors.confirmPassword = '비밀번호 확인을 입력해주세요.';
    } else if (password !== confirmPassword) {
      newErrors.confirmPassword = '비밀번호가 일치하지 않습니다.';
    }

    if (!fullName.trim()) {
      newErrors.fullName = '이름을 입력해주세요.';
    }

    if (!email.trim()) {
      newErrors.email = '이메일을 입력해주세요.';
    } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
      newErrors.email = '올바른 이메일 형식이 아닙니다.';
    }

    if (!orgId) {
      newErrors.orgId = '소속 영업점을 선택해주세요.';
    }

    errors = newErrors;
    return Object.keys(newErrors).length === 0;
  }

  async function handleSubmit() {
    if (!validate()) return;

    loading = true;
    submitResult = null;

    try {
      const requestData: UserCreateRequest = {
        username: username.trim(),
        password,
        email: email.trim(),
        fullName: fullName.trim(),
        phone: phone.trim() || undefined,
        role,
        orgId
      };

      const response = await userApi.createUser(requestData);
      if (response.success && response.data) {
        submitResult = {
          success: true,
          message: `사용자 "${response.data.fullName}"이(가) 성공적으로 등록되었습니다.`
        };
      } else {
        submitResult = {
          success: false,
          message: response.error?.message || '등록에 실패했습니다. 다시 시도해주세요.'
        };
      }
    } catch (err) {
      submitResult = {
        success: false,
        message: err instanceof Error ? err.message : '네트워크 오류가 발생했습니다.'
      };
    } finally {
      loading = false;
    }
  }

  function handleCancel() {
    oncancel?.();
  }

  function handleComplete() {
    oncomplete?.();
  }

  function resetForm() {
    username = '';
    password = '';
    confirmPassword = '';
    fullName = '';
    email = '';
    phone = '';
    role = UserRole.VIEWER;
    orgId = '';
    errors = {};
    submitResult = null;
  }
</script>

<div class="max-w-[720px] mx-auto px-6 pb-12">
  <div class="mb-8 pb-6 border-b-2 border-foreground">
    <h1 class="text-3xl font-extrabold text-foreground tracking-tight">사용자 등록</h1>
    <p class="text-muted-foreground text-sm mt-2">새로운 사용자를 등록합니다</p>
  </div>

  <Card class="shadow-lg">
    {#if submitResult}
      <CardContent class="py-16 px-8">
        <div class="flex flex-col items-center justify-center text-center">
          <div class="mb-6 {submitResult.success ? 'text-emerald-500' : 'text-destructive'}">
            {#if submitResult.success}
              <svg width="64" height="64" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <circle cx="12" cy="12" r="10"/>
                <polyline points="16 10 10 16 8 14"/>
              </svg>
            {:else}
              <svg width="64" height="64" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <circle cx="12" cy="12" r="10"/>
                <line x1="15" y1="9" x2="9" y2="15"/>
                <line x1="9" y1="9" x2="15" y2="15"/>
              </svg>
            {/if}
          </div>
          <h2 class="text-2xl font-bold text-foreground mb-3">
            {submitResult.success ? '등록 완료' : '등록 실패'}
          </h2>
          <p class="text-muted-foreground mb-8 max-w-[400px]">
            {submitResult.message}
          </p>
          <div class="flex gap-4">
            {#if submitResult.success}
              <Button variant="outline" onclick={resetForm}>새 사용자 등록</Button>
              <Button onclick={handleComplete}>목록으로</Button>
            {:else}
              <Button onclick={() => submitResult = null}>다시 시도</Button>
            {/if}
          </div>
        </div>
      </CardContent>
    {:else}
      <CardContent class="p-8">
        <div class="space-y-8">
          <div class="pb-8 border-b border-border">
            <h3 class="flex items-center gap-3 text-lg font-bold text-foreground mb-5">
              <span class="flex items-center justify-center w-9 h-9 bg-gradient-to-br from-primary to-violet-500 rounded-lg text-white">
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/>
                  <circle cx="12" cy="7" r="4"/>
                </svg>
              </span>
              계정 정보
            </h3>
            <div class="grid grid-cols-2 gap-5">
              <div class="flex flex-col gap-2">
                <Label for="username">아이디 <span class="text-destructive">*</span></Label>
                <input
                  id="username"
                  type="text"
                  bind:value={username}
                  placeholder="영문, 숫자, 밑줄 4자 이상"
                  class="h-11 w-full px-4 rounded-md border-[1.5px] border-input bg-background text-sm focus:outline-none focus:ring-2 focus:ring-ring focus:border-primary transition-all {errors.username ? 'border-destructive focus:ring-destructive/20' : ''}"
                />
                {#if errors.username}
                  <span class="text-xs text-destructive">{errors.username}</span>
                {/if}
              </div>
              <div class="flex flex-col gap-2">
                <Label for="fullName">이름 <span class="text-destructive">*</span></Label>
                <input
                  id="fullName"
                  type="text"
                  bind:value={fullName}
                  placeholder="사용자 이름"
                  class="h-11 w-full px-4 rounded-md border-[1.5px] border-input bg-background text-sm focus:outline-none focus:ring-2 focus:ring-ring focus:border-primary transition-all {errors.fullName ? 'border-destructive focus:ring-destructive/20' : ''}"
                />
                {#if errors.fullName}
                  <span class="text-xs text-destructive">{errors.fullName}</span>
                {/if}
              </div>
              <div class="flex flex-col gap-2">
                <Label for="password">비밀번호 <span class="text-destructive">*</span></Label>
                <input
                  id="password"
                  type="password"
                  bind:value={password}
                  placeholder="최소 8자 이상"
                  class="h-11 w-full px-4 rounded-md border-[1.5px] border-input bg-background text-sm focus:outline-none focus:ring-2 focus:ring-ring focus:border-primary transition-all {errors.password ? 'border-destructive focus:ring-destructive/20' : ''}"
                />
                {#if errors.password}
                  <span class="text-xs text-destructive">{errors.password}</span>
                {/if}
              </div>
              <div class="flex flex-col gap-2">
                <Label for="confirmPassword">비밀번호 확인 <span class="text-destructive">*</span></Label>
                <input
                  id="confirmPassword"
                  type="password"
                  bind:value={confirmPassword}
                  placeholder="비밀번호 재입력"
                  class="h-11 w-full px-4 rounded-md border-[1.5px] border-input bg-background text-sm focus:outline-none focus:ring-2 focus:ring-ring focus:border-primary transition-all {errors.confirmPassword ? 'border-destructive focus:ring-destructive/20' : ''}"
                />
                {#if errors.confirmPassword}
                  <span class="text-xs text-destructive">{errors.confirmPassword}</span>
                {/if}
              </div>
            </div>
          </div>

          <div class="pb-8 border-b border-border">
            <h3 class="flex items-center gap-3 text-lg font-bold text-foreground mb-5">
              <span class="flex items-center justify-center w-9 h-9 bg-gradient-to-br from-primary to-violet-500 rounded-lg text-white">
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M4 4h16c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2V6c0-1.1.9-2 2-2z"/>
                  <polyline points="22,6 12,13 2,6"/>
                </svg>
              </span>
              연락처 정보
            </h3>
            <div class="grid grid-cols-2 gap-5">
              <div class="flex flex-col gap-2">
                <Label for="email">이메일 <span class="text-destructive">*</span></Label>
                <input
                  id="email"
                  type="email"
                  bind:value={email}
                  placeholder="example@email.com"
                  class="h-11 w-full px-4 rounded-md border-[1.5px] border-input bg-background text-sm focus:outline-none focus:ring-2 focus:ring-ring focus:border-primary transition-all {errors.email ? 'border-destructive focus:ring-destructive/20' : ''}"
                />
                {#if errors.email}
                  <span class="text-xs text-destructive">{errors.email}</span>
                {/if}
              </div>
              <div class="flex flex-col gap-2">
                <Label for="phone">연락처</Label>
                <input
                  id="phone"
                  type="tel"
                  bind:value={phone}
                  placeholder="010-0000-0000"
                  class="h-11 w-full px-4 rounded-md border-[1.5px] border-input bg-background text-sm focus:outline-none focus:ring-2 focus:ring-ring focus:border-primary transition-all"
                />
              </div>
            </div>
          </div>

          <div>
            <h3 class="flex items-center gap-3 text-lg font-bold text-foreground mb-5">
              <span class="flex items-center justify-center w-9 h-9 bg-gradient-to-br from-primary to-violet-500 rounded-lg text-white">
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <rect x="2" y="7" width="20" height="14" rx="2" ry="2"/>
                  <path d="M16 21V5a2 2 0 0 0-2-2h-4a2 2 0 0 0-2 2v16"/>
                </svg>
              </span>
              소속 및 권한
            </h3>
            <div class="grid grid-cols-2 gap-5">
              <div class="flex flex-col gap-2">
                <Label for="orgId">소속 영업점 <span class="text-destructive">*</span></Label>
                <select
                  id="orgId"
                  bind:value={orgId}
                  disabled={loadingOrgs}
                  class="h-11 px-4 pr-8 rounded-md border-[1.5px] border-input bg-background text-sm focus:outline-none focus:ring-2 focus:ring-ring focus:border-primary transition-all {errors.orgId ? 'border-destructive focus:ring-destructive/20' : ''} {loadingOrgs ? 'opacity-50' : ''}"
                >
                  <option value="">
                    {loadingOrgs ? '불러오는 중...' : '영업점을 선택하세요'}
                  </option>
                  {#each organizations as org}
                    <option value={org.id}>{org.name}</option>
                  {/each}
                </select>
                {#if errors.orgId}
                  <span class="text-xs text-destructive">{errors.orgId}</span>
                {/if}
              </div>
              <div class="flex flex-col gap-2">
                <Label for="role">역할 <span class="text-destructive">*</span></Label>
                <select
                  id="role"
                  bind:value={role}
                  class="h-11 px-4 pr-8 rounded-md border-[1.5px] border-input bg-background text-sm focus:outline-none focus:ring-2 focus:ring-ring focus:border-primary transition-all"
                >
                  {#each Object.values(UserRole) as r}
                    <option value={r}>{USER_ROLE_LABELS[r]}</option>
                  {/each}
                </select>
              </div>
            </div>
          </div>
        </div>

        <div class="flex justify-end gap-3 pt-8 mt-8 border-t border-border">
          <Button variant="outline" onclick={handleCancel} disabled={loading}>취소</Button>
          <Button onclick={handleSubmit} disabled={loading}>
            {#if loading}
              <div class="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin mr-2"></div>
              등록 중...
            {:else}
              사용자 등록
            {/if}
          </Button>
        </div>
      </CardContent>
    {/if}
  </Card>
</div>
