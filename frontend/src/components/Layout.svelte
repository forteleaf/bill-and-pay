<script lang="ts">
  import Sidebar from './Sidebar.svelte';
  import Header from './Header.svelte';
  
  interface Props {
    navigate: (route: string) => void;
    children?: import('svelte').Snippet;
  }
  
  let { navigate, children }: Props = $props();
</script>

<div class="layout">
  <Sidebar {navigate} />
  <div class="main-content">
    <Header />
    <main class="content">
      {@render children?.()}
    </main>
  </div>
</div>

<style>
  .layout {
    display: flex;
    min-height: 100vh;
  }
  
  .main-content {
    flex: 1;
    display: flex;
    flex-direction: column;
  }
  
  .content {
    flex: 1;
    padding: 2rem;
    background-color: #f5f5f5;
  }
</style>
