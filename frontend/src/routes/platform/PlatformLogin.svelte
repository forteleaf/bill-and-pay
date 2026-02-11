<script lang="ts">
  import { authStore } from '@/stores/auth';
  import { platformApi } from '@/api/platform';
  import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '$lib/components/ui/card';
  import { Button } from '$lib/components/ui/button';
  import { Input } from '$lib/components/ui/input';
  import { Label } from '$lib/components/ui/label';

  let username = $state('');
  let password = $state('');
  let error = $state<string | null>(null);
  let loading = $state(false);

  interface Props {
    onBack?: () => void;
  }
  let { onBack }: Props = $props();

  async function handleLogin(e: Event) {
    e.preventDefault();

    if (!username || !password) {
      error = '사용자명과 비밀번호를 입력하세요.';
      return;
    }

    loading = true;
    error = null;

    try {
      const response = await platformApi.login(username, password);

      if (response.success && response.data) {
        authStore.platformLogin(
          response.data.accessToken,
          response.data.refreshToken,
          response.data.username,
          response.data.role,
          response.data.fullName
        );
        window.location.reload();
      } else {
        error = response.error?.message || '로그인에 실패했습니다.';
      }
    } catch (err) {
      error = err instanceof Error ? err.message : '로그인 중 오류가 발생했습니다.';
    } finally {
      loading = false;
    }
  }
</script>

<div class="min-h-screen flex items-center justify-center bg-gradient-to-br from-slate-900 to-slate-700 p-4">
  <Card class="w-full max-w-md shadow-2xl">
    <CardHeader class="text-center space-y-2">
      <CardTitle class="text-2xl font-bold">Bill&Pay Platform</CardTitle>
      <CardDescription>플랫폼 관리자 로그인</CardDescription>
    </CardHeader>

    <CardContent>
      <form onsubmit={handleLogin} class="space-y-6">
        <div class="space-y-2">
          <Label for="username">사용자명</Label>
          <Input
            id="username"
            type="text"
            value={username}
            oninput={(e) => username = e.currentTarget.value}
            placeholder="관리자 사용자명"
            disabled={loading}
            autocomplete="username"
          />
        </div>

        <div class="space-y-2">
          <Label for="password">비밀번호</Label>
          <Input
            id="password"
            type="password"
            value={password}
            oninput={(e) => password = e.currentTarget.value}
            placeholder="비밀번호"
            disabled={loading}
            autocomplete="current-password"
          />
        </div>

        {#if error}
          <div class="p-3 rounded-md bg-destructive/10 border border-destructive/20 text-destructive text-sm text-center">
            {error}
          </div>
        {/if}

        <Button type="submit" disabled={loading} class="w-full">
          {loading ? '로그인 중...' : '관리자 로그인'}
        </Button>

        {#if onBack}
          <Button variant="ghost" class="w-full" onclick={onBack}>
            일반 로그인으로 돌아가기
          </Button>
        {/if}
      </form>
    </CardContent>
  </Card>
</div>
