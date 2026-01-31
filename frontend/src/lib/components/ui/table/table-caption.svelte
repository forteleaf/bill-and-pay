<script lang="ts">
  import { cn } from "$lib/utils";

  interface Props {
    class?: string;
    children?: any;
  }

  let { class: className, children }: Props = $props();
</script>

<caption class={cn("mt-4 text-sm text-muted-foreground", className)}>
  {@render children?.()}
</caption>
