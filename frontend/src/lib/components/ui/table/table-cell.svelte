<script lang="ts">
  import { cn } from "$lib/utils";
  import type { HTMLTdAttributes } from "svelte/elements";

  interface Props extends HTMLTdAttributes {
    class?: string;
  }

  let { class: className, children, ...restProps }: Props = $props();
</script>

<td class={cn("p-4 align-middle [&:has([role=checkbox])]:pr-0", className)} {...restProps}>
  {@render children?.()}
</td>
