<script lang="ts">
  import { Popover as PopoverPrimitive } from "bits-ui";
  import { cn } from "$lib/utils";
  import type { Snippet } from "svelte";

  interface Props {
    class?: string;
    children: Snippet;
  }

  let { class: className, children, ...restProps }: Props = $props();
</script>

<PopoverPrimitive.Trigger class={cn(className)} {...restProps}>
  {@render children()}
</PopoverPrimitive.Trigger>
