<script lang="ts">
  import Sidebar from './Sidebar.svelte';
  import Header from './Header.svelte';
  
  interface Props {
    navigate: (route: string) => void;
    children?: import('svelte').Snippet;
  }
  
  let { navigate, children }: Props = $props();
</script>

<div class="flex min-h-screen">
  <Sidebar {navigate} />
  <div class="flex-1 flex flex-col">
    <Header />
    <main class="flex-1 p-6 bg-muted/50">
      {@render children?.()}
    </main>
  </div>
</div>
