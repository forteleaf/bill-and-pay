<script lang="ts">
  import { authStore } from './lib/authStore';
  import { tenantStore } from './lib/stores';
  import { tabStore } from './lib/tabStore';
  import NewLayout from './components/NewLayout.svelte';
  import Login from './routes/Login.svelte';
  
  let isAuthenticated = $state(false);
  
  $effect(() => {
    isAuthenticated = authStore.isAuthenticated();
    
    if (isAuthenticated && !tenantStore.current) {
      const tenantId = authStore.getTenantId();
      if (tenantId) {
        tenantStore.setCurrent(tenantId);
      }
    }
    
    // Initialize Dashboard tab on authentication
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
</script>

{#if !isAuthenticated}
  <Login />
{:else}
  <NewLayout />
{/if}
