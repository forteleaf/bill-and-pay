<script lang="ts">
  import { cn } from "$lib/utils";
  import type { HTMLAttributes } from "svelte/elements";

  interface Props extends HTMLAttributes<HTMLTableSectionElement> {
    class?: string;
  }

  let { class: className, children, ...restProps }: Props = $props();
</script>

<tfoot class={cn("border-t bg-muted/50 font-medium [&>tr]:last:border-b-0", className)} {...restProps}>
  {@render children?.()}
</tfoot>
