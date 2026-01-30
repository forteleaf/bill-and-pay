<script lang="ts">
  import { authStore } from './lib/authStore';
  import Layout from './components/Layout.svelte';
  import Dashboard from './routes/Dashboard.svelte';
  import Transactions from './routes/Transactions.svelte';
  import Settlements from './routes/Settlements.svelte';
  import Login from './routes/Login.svelte';
  
  let currentRoute = $state('dashboard');
  let isAuthenticated = $state(false);
  
  $effect(() => {
    isAuthenticated = authStore.isAuthenticated();
    
    if (!isAuthenticated && currentRoute !== 'login') {
      currentRoute = 'login';
    }
  });
  
  function navigate(route: string) {
    currentRoute = route;
  }
</script>

{#if currentRoute === 'login' || !isAuthenticated}
  <Login />
{:else}
  <Layout {navigate}>
    {#if currentRoute === 'dashboard'}
      <Dashboard />
    {:else if currentRoute === 'transactions'}
      <Transactions />
    {:else if currentRoute === 'settlements'}
      <Settlements />
    {/if}
  </Layout>
{/if}
