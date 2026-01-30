<script lang="ts">
  import { fade, fly } from 'svelte/transition';
  
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
  
  const typeColors = {
    warning: 'bg-yellow-500 hover:bg-yellow-600',
    danger: 'bg-red-500 hover:bg-red-600',
    info: 'bg-blue-500 hover:bg-blue-600'
  };
</script>

<svelte:window on:keydown={handleEscape} />

{#if show}
  <div
    class="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50"
    transition:fade={{ duration: 200 }}
    onclick={handleBackdropClick}
    role="dialog"
    aria-modal="true"
    aria-labelledby="modal-title"
  >
    <div
      class="bg-white rounded-lg shadow-xl max-w-md w-full mx-4"
      transition:fly={{ y: -20, duration: 300 }}
    >
      <div class="p-6">
        <h3 id="modal-title" class="text-lg font-semibold text-gray-900 mb-3">
          {title}
        </h3>
        <p class="text-gray-600 mb-6">
          {message}
        </p>
        <div class="flex gap-3 justify-end">
          <button
            type="button"
            class="px-4 py-2 text-gray-700 bg-gray-100 hover:bg-gray-200 rounded-md transition-colors"
            onclick={onCancel}
          >
            {cancelText}
          </button>
          <button
            type="button"
            class="px-4 py-2 text-white rounded-md transition-colors {typeColors[type]}"
            onclick={onConfirm}
          >
            {confirmText}
          </button>
        </div>
      </div>
    </div>
  </div>
{/if}

<style>
  @media (max-width: 640px) {
    .max-w-md {
      max-width: calc(100% - 2rem);
    }
  }
</style>
