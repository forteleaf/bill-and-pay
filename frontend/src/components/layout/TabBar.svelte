<script lang="ts">
  import { cn } from '$lib/utils';
  import { tabStore } from '@/stores/tab';
  import * as ContextMenu from '$lib/components/ui/context-menu';
  import { Button } from '$lib/components/ui/button';
  
  // Get reactive state from tabStore
  let tabs = $state(tabStore.getTabs());
  let activeTabId = $state(tabStore.getActiveTabId());
  
  // Drag state
  let draggedIndex = $state<number | null>(null);
  let dragOverIndex = $state<number | null>(null);
  
  // Update when tabStore changes
  $effect(() => {
    const interval = setInterval(() => {
      tabs = tabStore.getTabs();
      activeTabId = tabStore.getActiveTabId();
    }, 100);
    return () => clearInterval(interval);
  });
  
  function handleTabClick(tabId: string) {
    tabStore.focusTab(tabId);
    tabs = tabStore.getTabs();
    activeTabId = tabStore.getActiveTabId();
  }
  
  function handleCloseTab(e: MouseEvent, tabId: string) {
    e.stopPropagation();
    tabStore.closeTab(tabId);
    tabs = tabStore.getTabs();
    activeTabId = tabStore.getActiveTabId();
  }
  
  function handleCloseOtherTabs(tabId: string) {
    tabStore.closeOtherTabs(tabId);
    tabs = tabStore.getTabs();
    activeTabId = tabStore.getActiveTabId();
  }
  
  function handleCloseAllTabs() {
    tabStore.closeAllTabs();
    tabs = tabStore.getTabs();
    activeTabId = tabStore.getActiveTabId();
  }
  
  function handleDragStart(e: DragEvent, index: number) {
    draggedIndex = index;
    if (e.dataTransfer) {
      e.dataTransfer.effectAllowed = 'move';
      e.dataTransfer.setData('text/plain', index.toString());
    }
  }
  
  function handleDragOver(e: DragEvent, index: number) {
    e.preventDefault();
    if (e.dataTransfer) {
      e.dataTransfer.dropEffect = 'move';
    }
    dragOverIndex = index;
  }
  
  function handleDragLeave() {
    dragOverIndex = null;
  }
  
  function handleDrop(e: DragEvent, toIndex: number) {
    e.preventDefault();
    if (draggedIndex !== null && draggedIndex !== toIndex) {
      tabStore.reorderTabs(draggedIndex, toIndex);
      tabs = tabStore.getTabs();
    }
    draggedIndex = null;
    dragOverIndex = null;
  }
  
  function handleDragEnd() {
    draggedIndex = null;
    dragOverIndex = null;
  }
  
  function hasCloseableTabs(): boolean {
    return tabs.some(t => t.closeable);
  }
</script>

<div class="bg-slate-50 border-b border-slate-200 pt-2 px-4 flex items-end">
  <div class="flex gap-1 overflow-hidden pb-0 flex-1" role="tablist">
    {#each tabs as tab, index (tab.id)}
      <ContextMenu.Root>
        <ContextMenu.Trigger>
          <div 
            class={cn(
              "flex items-center gap-1.5 py-1.5 px-3 border border-b-0 rounded-t-lg cursor-pointer text-xs transition-all duration-150 whitespace-nowrap select-none",
              tab.id === activeTabId 
                ? "bg-white text-slate-800 font-medium border-slate-200 relative after:absolute after:bottom-[-1px] after:left-0 after:right-0 after:h-0.5 after:bg-white" 
                : "bg-slate-200 border-gray-300 text-slate-500 hover:bg-slate-100 hover:text-slate-700",
              draggedIndex === index && "opacity-50",
              dragOverIndex === index && draggedIndex !== index && "ring-2 ring-primary ring-offset-1"
            )}
            onclick={() => handleTabClick(tab.id)}
            onkeydown={(e) => e.key === 'Enter' && handleTabClick(tab.id)}
            role="tab"
            tabindex="0"
            aria-selected={tab.id === activeTabId}
            draggable="true"
            ondragstart={(e) => handleDragStart(e, index)}
            ondragover={(e) => handleDragOver(e, index)}
            ondragleave={handleDragLeave}
            ondrop={(e) => handleDrop(e, index)}
            ondragend={handleDragEnd}
          >
            <span class="text-sm">{tab.icon}</span>
            <span class="max-w-[100px] overflow-hidden text-ellipsis">{tab.title}</span>
            {#if tab.closeable}
              <button 
                class="flex items-center justify-center w-4 h-4 ml-0.5 -mr-0.5 bg-transparent border-none rounded cursor-pointer text-sm text-slate-400 leading-none hover:bg-red-600 hover:text-white"
                onclick={(e) => handleCloseTab(e, tab.id)}
                type="button"
                aria-label="닫기"
              >
                ×
              </button>
            {/if}
          </div>
        </ContextMenu.Trigger>
        <ContextMenu.Content class="w-48">
          {#if tab.closeable}
            <ContextMenu.Item onclick={() => tabStore.closeTab(tab.id)}>
              <span class="mr-2">✕</span>
              탭 닫기
            </ContextMenu.Item>
          {/if}
          <ContextMenu.Item onclick={() => handleCloseOtherTabs(tab.id)}>
            <span class="mr-2">⊗</span>
            다른 탭 닫기
          </ContextMenu.Item>
          <ContextMenu.Separator />
          <ContextMenu.Item onclick={handleCloseAllTabs}>
            <span class="mr-2">✖</span>
            전체 탭 닫기
          </ContextMenu.Item>
        </ContextMenu.Content>
      </ContextMenu.Root>
    {/each}
  </div>
  
  {#if hasCloseableTabs()}
    <Button 
      variant="ghost" 
      size="sm" 
      class="mb-0.5 ml-2 h-7 px-2 text-xs text-slate-500 hover:text-red-600 hover:bg-red-50"
      onclick={handleCloseAllTabs}
    >
      <span class="mr-1">✖</span>
      전체 닫기
    </Button>
  {/if}
</div>
