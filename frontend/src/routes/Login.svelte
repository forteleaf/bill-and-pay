<script lang="ts">
  import { apiClient } from '../lib/api';
  import { authStore } from '../lib/authStore';
  import { tenantStore } from '../lib/stores';
  import type { AuthResponse } from '../types/api';
  import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '$lib/components/ui/card';
  import { Button } from '$lib/components/ui/button';
  import { Input } from '$lib/components/ui/input';
  import { Label } from '$lib/components/ui/label';

  let username = $state('');
  let password = $state('');
  let error = $state<string | null>(null);
  let loading = $state(false);

  async function handleLogin(e: Event) {
    e.preventDefault();
    
    if (!username || !password) {
      error = 'Please enter username and password.';
      return;
    }

    loading = true;
    error = null;

    try {
      const response = await apiClient.post<AuthResponse>('/auth/login', { 
        username, 
        password 
      });

      if (response.success && response.data) {
        authStore.login(
          response.data.accessToken,
          response.data.refreshToken,
          response.data.username,
          response.data.tenantId
        );

        tenantStore.setCurrent(response.data.tenantId);
        
        window.location.reload();
      } else {
        error = response.error?.message || 'Login failed.';
      }
    } catch (err) {
      console.error('Login error:', err);
      error = err instanceof Error ? err.message : 'An error occurred during login.';
    } finally {
      loading = false;
    }
  }
</script>

<div class="min-h-screen flex items-center justify-center bg-gradient-to-br from-primary to-purple-600 p-4">
  <Card class="w-full max-w-md shadow-2xl">
    <CardHeader class="text-center space-y-2">
      <CardTitle class="text-2xl font-bold">Bill&Pay</CardTitle>
      <CardDescription>Settlement Platform</CardDescription>
    </CardHeader>
    
    <CardContent>
      <form onsubmit={handleLogin} class="space-y-6">
        <div class="space-y-2">
          <Label for="username">Username</Label>
          <Input
            id="username"
            type="text"
            value={username}
            oninput={(e) => username = e.currentTarget.value}
            placeholder="Enter your username"
            disabled={loading}
            autocomplete="username"
          />
        </div>

        <div class="space-y-2">
          <Label for="password">Password</Label>
          <Input
            id="password"
            type="password"
            value={password}
            oninput={(e) => password = e.currentTarget.value}
            placeholder="Enter your password"
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
          {loading ? 'Logging in...' : 'Login'}
        </Button>
      </form>
    </CardContent>
  </Card>
</div>
