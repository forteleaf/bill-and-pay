<script lang="ts">
  import { onMount } from 'svelte';
  import { userApi } from '@/api/user';
  import {
    type UserDto,
    type UserUpdateRequest,
    type PasswordChangeRequest,
    UserStatus,
    UserRole,
    USER_STATUS_LABELS,
    USER_ROLE_LABELS
  } from '@/types/user';
  import { Button } from '$lib/components/ui/button';
  import { Badge } from '$lib/components/ui/badge';
  import { Card, CardContent, CardHeader, CardTitle } from '$lib/components/ui/card';
  import { Label } from '$lib/components/ui/label';
  import * as Select from '$lib/components/ui/select';
  import { getStatusBadgeVariant } from '@/utils/statusVariants';

  interface Props {
    userId: string;
  }

  let { userId }: Props = $props();

  let user = $state<UserDto | null>(null);
  let loading = $state(true);
  let editMode = $state(false);
  let saving = $state(false);
  let deleting = $state(false);
  let error = $state<string | null>(null);
  let successMessage = $state<string | null>(null);

  let editFullName = $state('');
  let editEmail = $state('');
  let editPhone = $state('');
  let editRole = $state<UserRole>(UserRole.VIEWER);
  let editStatus = $state<UserStatus>(UserStatus.ACTIVE);

  let showPasswordForm = $state(false);
  let currentPassword = $state('');
  let newPassword = $state('');
  let confirmPassword = $state('');
  let passwordError = $state<string | null>(null);
  let passwordSaving = $state(false);

  onMount(() => {
    if (userId) {
      loadUser();
    }
  });

  async function loadUser() {
    loading = true;
    error = null;

    try {
      const response = await userApi.getUserById(userId);
      if (response.success && response.data) {
        user = response.data;
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
    if (!user) return;
    editFullName = user.fullName || '';
    editEmail = user.email || '';
    editPhone = user.phone || '';
    editRole = user.role;
    editStatus = user.status;
  }

  function toggleEditMode() {
    editMode = !editMode;
    successMessage = null;
    if (!editMode && user) {
      initEditFields();
    }
  }

  async function handleSave() {
    if (!user) return;
    saving = true;
    error = null;
    successMessage = null;

    try {
      const updateData: UserUpdateRequest = {
        fullName: editFullName,
        email: editEmail,
        phone: editPhone || undefined,
        role: editRole,
        status: editStatus,
      };

      const response = await userApi.updateUser(user.id, updateData);
      if (response.success && response.data) {
        user = response.data;
        editMode = false;
        successMessage = '사용자 정보가 저장되었습니다.';
        setTimeout(() => successMessage = null, 3000);
      } else {
        error = response.error?.message || '저장에 실패했습니다.';
      }
    } catch (err) {
      error = '저장에 실패했습니다.';
    } finally {
      saving = false;
    }
  }

  async function handleDelete() {
    if (!user) return;
    if (!confirm('정말로 이 사용자를 삭제하시겠습니까?\n삭제된 사용자는 복구할 수 없습니다.')) return;

    deleting = true;
    error = null;

    try {
      const response = await userApi.deleteUser(user.id);
      if (response.success) {
        user = { ...user, status: UserStatus.DELETED };
        editStatus = UserStatus.DELETED;
        successMessage = '사용자가 삭제되었습니다.';
      } else {
        error = response.error?.message || '삭제에 실패했습니다.';
      }
    } catch (err) {
      error = '삭제에 실패했습니다.';
    } finally {
      deleting = false;
    }
  }

  async function handleChangePassword() {
    if (!user) return;
    passwordError = null;

    if (!currentPassword || !newPassword || !confirmPassword) {
      passwordError = '모든 필드를 입력해주세요.';
      return;
    }

    if (newPassword.length < 8) {
      passwordError = '새 비밀번호는 최소 8자 이상이어야 합니다.';
      return;
    }

    if (newPassword !== confirmPassword) {
      passwordError = '새 비밀번호가 일치하지 않습니다.';
      return;
    }

    passwordSaving = true;

    try {
      const request: PasswordChangeRequest = {
        currentPassword,
        newPassword
      };

      const response = await userApi.changePassword(user.id, request);
      if (response.success) {
        showPasswordForm = false;
        currentPassword = '';
        newPassword = '';
        confirmPassword = '';
        successMessage = '비밀번호가 변경되었습니다.';
        setTimeout(() => successMessage = null, 3000);
      } else {
        passwordError = response.error?.message || '비밀번호 변경에 실패했습니다.';
      }
    } catch (err) {
      passwordError = '비밀번호 변경에 실패했습니다.';
    } finally {
      passwordSaving = false;
    }
  }

  function cancelPasswordChange() {
    showPasswordForm = false;
    currentPassword = '';
    newPassword = '';
    confirmPassword = '';
    passwordError = null;
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
  {:else if error && !user}
    <div class="flex-1 flex flex-col items-center justify-center gap-4 text-destructive p-12">
      <span class="flex items-center justify-center w-12 h-12 rounded-full bg-destructive/10 text-destructive text-2xl font-bold">!</span>
      <span>{error}</span>
      <Button variant="outline" onclick={loadUser}>다시 시도</Button>
    </div>
  {:else if user}
    <div class="flex justify-between items-start p-6 bg-background border-b border-border">
      <div class="flex flex-col gap-2">
        <h1 class="text-2xl font-bold text-foreground">{user.fullName}</h1>
        <div class="flex items-center gap-3">
          <span class="font-mono text-sm text-muted-foreground">@{user.username}</span>
          <Badge variant="secondary" class="text-xs">
            {USER_ROLE_LABELS[user.role]}
          </Badge>
          <Badge variant={getStatusBadgeVariant(user.status)}>
            {USER_STATUS_LABELS[user.status]}
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
          {#if user.status !== UserStatus.DELETED}
            <Button variant="destructive" onclick={handleDelete} disabled={deleting}>
              {deleting ? '삭제 중...' : '삭제'}
            </Button>
          {/if}
          <Button onclick={toggleEditMode}>수정</Button>
        {/if}
      </div>
    </div>

    {#if error}
      <div class="mx-6 mt-4 p-4 rounded-lg bg-destructive/10 border border-destructive/20 text-destructive text-sm">
        {error}
      </div>
    {/if}

    {#if successMessage}
      <div class="mx-6 mt-4 p-4 rounded-lg bg-emerald-500/10 border border-emerald-500/20 text-emerald-600 text-sm">
        {successMessage}
      </div>
    {/if}

    <div class="flex-1 overflow-y-auto p-6 flex flex-col gap-6">
      <Card>
        <CardHeader>
          <CardTitle class="text-base">기본 정보</CardTitle>
        </CardHeader>
        <CardContent>
          <div class="grid grid-cols-2 gap-5">
            <div class="flex flex-col gap-1.5">
              <Label class="text-xs font-medium text-muted-foreground">아이디</Label>
              <span class="text-sm font-mono">{user.username}</span>
            </div>
            <div class="flex flex-col gap-1.5">
              <Label class="text-xs font-medium text-muted-foreground">이름</Label>
              {#if editMode}
                <input type="text" bind:value={editFullName} class="h-9 px-3 rounded-md border border-input bg-background text-sm focus:outline-none focus:ring-2 focus:ring-ring" />
              {:else}
                <span class="text-sm">{user.fullName}</span>
              {/if}
            </div>
            <div class="flex flex-col gap-1.5">
              <Label class="text-xs font-medium text-muted-foreground">이메일</Label>
              {#if editMode}
                <input type="email" bind:value={editEmail} class="h-9 px-3 rounded-md border border-input bg-background text-sm focus:outline-none focus:ring-2 focus:ring-ring" />
              {:else}
                <span class="text-sm">{user.email}</span>
              {/if}
            </div>
            <div class="flex flex-col gap-1.5">
              <Label class="text-xs font-medium text-muted-foreground">연락처</Label>
              {#if editMode}
                <input type="tel" bind:value={editPhone} placeholder="010-0000-0000" class="h-9 px-3 rounded-md border border-input bg-background text-sm focus:outline-none focus:ring-2 focus:ring-ring" />
              {:else}
                <span class="text-sm">{user.phone || '-'}</span>
              {/if}
            </div>
            <div class="flex flex-col gap-1.5">
              <Label class="text-xs font-medium text-muted-foreground">역할</Label>
              {#if editMode}
                <Select.Root type="single" bind:value={editRole}>
                  <Select.Trigger class="w-full">
                    {#if editRole}
                      {USER_ROLE_LABELS[editRole as UserRole] || editRole}
                    {:else}
                      <span class="text-muted-foreground">역할 선택</span>
                    {/if}
                  </Select.Trigger>
                  <Select.Content>
                    {#each Object.values(UserRole) as role}
                      <Select.Item value={role}>{USER_ROLE_LABELS[role]}</Select.Item>
                    {/each}
                  </Select.Content>
                </Select.Root>
              {:else}
                <Badge variant="secondary" class="w-fit">{USER_ROLE_LABELS[user.role]}</Badge>
              {/if}
            </div>
            <div class="flex flex-col gap-1.5">
              <Label class="text-xs font-medium text-muted-foreground">상태</Label>
              {#if editMode}
                <Select.Root type="single" bind:value={editStatus}>
                  <Select.Trigger class="w-full">
                    {#if editStatus}
                      {USER_STATUS_LABELS[editStatus as UserStatus] || editStatus}
                    {:else}
                      <span class="text-muted-foreground">상태 선택</span>
                    {/if}
                  </Select.Trigger>
                  <Select.Content>
                    {#each Object.values(UserStatus) as status}
                      <Select.Item value={status}>{USER_STATUS_LABELS[status]}</Select.Item>
                    {/each}
                  </Select.Content>
                </Select.Root>
              {:else}
                <Badge variant={getStatusBadgeVariant(user.status)} class="w-fit">{USER_STATUS_LABELS[user.status]}</Badge>
              {/if}
            </div>
          </div>
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle class="text-base">소속 정보</CardTitle>
        </CardHeader>
        <CardContent>
          <div class="grid grid-cols-2 gap-5">
            <div class="flex flex-col gap-1.5">
              <Label class="text-xs font-medium text-muted-foreground">소속 영업점</Label>
              <span class="text-sm">{user.orgPath || '-'}</span>
            </div>
            <div class="flex flex-col gap-1.5">
              <Label class="text-xs font-medium text-muted-foreground">조직 ID</Label>
              <span class="text-sm font-mono text-muted-foreground">{user.orgId || '-'}</span>
            </div>
          </div>
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle class="text-base">보안 정보</CardTitle>
        </CardHeader>
        <CardContent>
          <div class="grid grid-cols-2 gap-5 mb-5">
            <div class="flex flex-col gap-1.5">
              <Label class="text-xs font-medium text-muted-foreground">2단계 인증</Label>
              <Badge variant={user.twoFactorEnabled ? 'default' : 'secondary'} class="w-fit">
                {user.twoFactorEnabled ? '활성화' : '비활성화'}
              </Badge>
            </div>
            <div class="flex flex-col gap-1.5">
              <Label class="text-xs font-medium text-muted-foreground">마지막 로그인</Label>
              <span class="text-sm text-muted-foreground">{formatDate(user.lastLoginAt)}</span>
            </div>
            <div class="flex flex-col gap-1.5">
              <Label class="text-xs font-medium text-muted-foreground">비밀번호 변경일</Label>
              <span class="text-sm text-muted-foreground">{formatDate(user.passwordChangedAt)}</span>
            </div>
          </div>

          {#if !showPasswordForm}
            <Button variant="outline" onclick={() => showPasswordForm = true}>
              <svg class="w-4 h-4 mr-2" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <rect x="3" y="11" width="18" height="11" rx="2" ry="2"/>
                <path d="M7 11V7a5 5 0 0 1 10 0v4"/>
              </svg>
              비밀번호 변경
            </Button>
          {:else}
            <div class="border-t border-border pt-5 mt-3">
              <h4 class="text-sm font-semibold text-foreground mb-4">비밀번호 변경</h4>
              {#if passwordError}
                <div class="mb-4 p-3 rounded-lg bg-destructive/10 border border-destructive/20 text-destructive text-sm">
                  {passwordError}
                </div>
              {/if}
              <div class="grid gap-4 max-w-md">
                <div class="flex flex-col gap-1.5">
                  <Label for="currentPassword">현재 비밀번호</Label>
                  <input id="currentPassword" type="password" bind:value={currentPassword} class="h-10 w-full px-3 rounded-md border border-input bg-background text-sm focus:outline-none focus:ring-2 focus:ring-ring" />
                </div>
                <div class="flex flex-col gap-1.5">
                  <Label for="newPassword">새 비밀번호</Label>
                  <input id="newPassword" type="password" bind:value={newPassword} placeholder="최소 8자 이상" class="h-10 w-full px-3 rounded-md border border-input bg-background text-sm focus:outline-none focus:ring-2 focus:ring-ring" />
                </div>
                <div class="flex flex-col gap-1.5">
                  <Label for="confirmPassword">새 비밀번호 확인</Label>
                  <input id="confirmPassword" type="password" bind:value={confirmPassword} class="h-10 w-full px-3 rounded-md border border-input bg-background text-sm focus:outline-none focus:ring-2 focus:ring-ring" />
                </div>
                <div class="flex gap-2 pt-2">
                  <Button variant="outline" onclick={cancelPasswordChange} disabled={passwordSaving}>취소</Button>
                  <Button onclick={handleChangePassword} disabled={passwordSaving}>
                    {passwordSaving ? '변경 중...' : '비밀번호 변경'}
                  </Button>
                </div>
              </div>
            </div>
          {/if}
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle class="text-base">등록 정보</CardTitle>
        </CardHeader>
        <CardContent>
          <div class="grid grid-cols-2 gap-5">
            <div class="flex flex-col gap-1.5">
              <Label class="text-xs font-medium text-muted-foreground">등록일시</Label>
              <span class="text-sm text-muted-foreground">{formatDate(user.createdAt)}</span>
            </div>
            <div class="flex flex-col gap-1.5">
              <Label class="text-xs font-medium text-muted-foreground">수정일시</Label>
              <span class="text-sm text-muted-foreground">{formatDate(user.updatedAt)}</span>
            </div>
          </div>
        </CardContent>
      </Card>
    </div>
  {/if}
</div>
