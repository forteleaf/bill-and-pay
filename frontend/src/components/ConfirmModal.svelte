<script lang="ts">
  import { fade, fly } from 'svelte/transition';
  import { Button } from '$lib/components/ui/button';
  import { Card, CardContent, CardHeader, CardTitle, CardFooter } from '$lib/components/ui/card';
  
  interface Props {
    show: boolean;
    title: string;
    message: string;
    confirmText?: string;
    cancelText?: string;
    type?: 'warning' | 'danger' | 'info';
    onConfirm: () => void;
    onCancel: () => void;
  }
  
  let {
    show = $bindable(false),
    title,
    message,
    confirmText = 'Confirm',
    cancelText = 'Cancel',
    type = 'warning',
    onConfirm,
    onCancel
  }: Props = $props();
  
  function handleBackdropClick(e: MouseEvent) {
    if (e.target === e.currentTarget) {
      onCancel();
    }
  }
  
  function handleEscape(e: KeyboardEvent) {
    if (e.key === 'Escape' && show) {
      onCancel();
    }
  }

  const confirmVariant = {
    warning: 'default' as const,
    danger: 'destructive' as const,
    info: 'secondary' as const
  };
</script>

<svelte:window onkeydown={handleEscape} />

{#if show}
  <!-- svelte-ignore a11y_no_static_element_interactions -->
  <!-- svelte-ignore a11y_click_events_have_key_events -->
  <div
    class="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4"
    transition:fade={{ duration: 200 }}
    onclick={handleBackdropClick}
  >
    <div transition:fly={{ y: -20, duration: 300 }}>
      <Card class="w-full max-w-md shadow-xl">
        <CardHeader>
          <CardTitle id="modal-title" class="text-lg">
            {title}
          </CardTitle>
        </CardHeader>
        <CardContent>
          <p class="text-muted-foreground">
            {message}
          </p>
        </CardContent>
        <CardFooter class="flex justify-end gap-3">
          <Button variant="outline" onclick={onCancel}>
            {cancelText}
          </Button>
          <Button variant={confirmVariant[type]} onclick={onConfirm}>
            {confirmText}
          </Button>
        </CardFooter>
      </Card>
    </div>
  </div>
{/if}
