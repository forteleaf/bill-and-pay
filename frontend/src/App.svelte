<script lang="ts">
  import { authStore } from '@/stores/auth';
  import { tenantStore } from '@/stores/tenant';
  import { tabStore } from '@/stores/tab';
  import Layout from '@/components/layout/Layout.svelte';
  import Login from '@/routes/auth/Login.svelte';
  import Landing from '@/routes/auth/Landing.svelte';
  import DemoRequest from '@/routes/misc/DemoRequest.svelte';
  
  type Page = 'landing' | 'login' | 'demo';
  
  let isAuthenticated = $state(false);
  let currentPage = $state<Page>('landing');
  
  $effect(() => {
    isAuthenticated = authStore.isAuthenticated();
    
    if (isAuthenticated && !tenantStore.current) {
      const tenantId = authStore.getTenantId();
      if (tenantId) {
        tenantStore.setCurrent(tenantId);
      }
    }
    
    if (isAuthenticated) {
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
  });
  
  function navigate(page: Page) {
    currentPage = page;
  }
</script>

{#if isAuthenticated}
  <Layout />
{:else if currentPage === 'login'}
  <Login />
{:else if currentPage === 'demo'}
  <DemoRequest onBack={() => navigate('landing')} />
{:else}
  <Landing onLogin={() => navigate('login')} onDemo={() => navigate('demo')} />
{/if}
