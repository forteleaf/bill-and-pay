<script lang="ts">
  import type { HTMLAttributes } from "svelte/elements";
  import { cn, type WithElementRef } from "$lib/utils";

  type Variant = "default" | "destructive";

  let {
    ref = $bindable(null),
    class: className,
    variant = "default",
    children,
    ...restProps
  }: WithElementRef<HTMLAttributes<HTMLDivElement>> & { variant?: Variant } = $props();

  const variantClasses: Record<Variant, string> = {
    default: "bg-background text-foreground [&>svg]:text-foreground",
    destructive: "border-destructive/50 text-destructive dark:border-destructive [&>svg]:text-destructive"
  };
</script>

<div
  bind:this={ref}
  data-slot="alert"
  role="alert"
  class={cn(
    "relative grid w-full grid-cols-[0_1fr] items-start gap-y-0.5 rounded-lg border p-4 text-sm has-[>svg]:grid-cols-[calc(var(--spacing)*4)_1fr] has-[>svg]:gap-x-3 [&>svg]:size-4 [&>svg]:translate-y-0.5",
    variantClasses[variant],
    className
  )}
  {...restProps}
>
  {@render children?.()}
</div>
