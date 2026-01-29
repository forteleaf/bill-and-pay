<script lang="ts">
  import Layout from './components/Layout.svelte';
  import Dashboard from './routes/Dashboard.svelte';
  import Transactions from './routes/Transactions.svelte';
  import Settlements from './routes/Settlements.svelte';
  
  let currentRoute = $state('dashboard');
  
  function navigate(route: string) {
    currentRoute = route;
  }
</script>

<Layout {navigate}>
  {#if currentRoute === 'dashboard'}
    <Dashboard />
  {:else if currentRoute === 'transactions'}
    <Transactions />
  {:else if currentRoute === 'settlements'}
    <Settlements />
  {/if}
</Layout>
