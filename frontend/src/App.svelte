<script lang="ts">
  import { authStore } from '@/stores/auth';
  import { tenantStore } from '@/stores/tenant';
  import { tabStore } from '@/stores/tab';
  import Layout from '@/components/layout/Layout.svelte';
  import PlatformLayout from '@/components/platform/PlatformLayout.svelte';
  import Login from '@/routes/auth/Login.svelte';
  import PlatformLogin from '@/routes/platform/PlatformLogin.svelte';
  import Landing from '@/routes/auth/Landing.svelte';
  import DemoRequest from '@/routes/misc/DemoRequest.svelte';

  type Page = 'landing' | 'login' | 'platform-login' | 'demo';

  let isAuthenticated = $state(false);
  let isPlatformAdmin = $state(false);
  let currentPage = $state<Page>('landing');

  $effect(() => {
    isAuthenticated = authStore.isAuthenticated();
    isPlatformAdmin = authStore.isPlatformAdmin();

    if (isAuthenticated && !isPlatformAdmin && !tenantStore.current) {
      const tenantId = authStore.getTenantId();
      if (tenantId) {
        tenantStore.setCurrent(tenantId);
      }
    }

    if (isAuthenticated && !isPlatformAdmin) {
      const tabs = tabStore.getTabs();
      if (tabs.length === 0) {
        tabStore.openTab({
          id: 'dashboard',
          title: '대시보드',
          icon: '📊',
          component: 'Dashboard',
          closeable: false
        });
      }
    }

    if (isAuthenticated && isPlatformAdmin) {
      const tabs = tabStore.getTabs();
      if (tabs.length === 0) {
        tabStore.openTab({
          id: 'platform-dashboard',
          title: '대시보드',
          icon: '📊',
          component: 'PlatformDashboard',
          closeable: false
        });
      }
    }
  });

  function navigate(page: Page) {
    currentPage = page;
  }
</script>

{#if isAuthenticated && isPlatformAdmin}
  <PlatformLayout />
{:else if isAuthenticated}
  <Layout />
{:else if currentPage === 'login'}
  <Login />
{:else if currentPage === 'platform-login'}
  <PlatformLogin onBack={() => navigate('landing')} />
{:else if currentPage === 'demo'}
  <DemoRequest onBack={() => navigate('landing')} />
{:else}
  <Landing onLogin={() => navigate('login')} onDemo={() => navigate('demo')} onPlatformLogin={() => navigate('platform-login')} />
{/if}
