<script lang="ts">
  import { cn } from '$lib/utils';
  import { tabStore } from '../lib/tabStore';
  
  // Get reactive state from tabStore
  let tabs = $state(tabStore.getTabs());
  let activeTabId = $state(tabStore.getActiveTabId());
  
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
</script>

<div class="bg-slate-50 border-b border-slate-200 pt-2 px-4 flex items-end">
  <div class="flex gap-1 overflow-hidden pb-0" role="tablist">
    {#each tabs as tab (tab.id)}
      <div 
        class={cn(
          "flex items-center gap-2 py-2.5 px-4 border border-b-0 rounded-t-lg cursor-pointer text-sm transition-all duration-150 whitespace-nowrap",
          tab.id === activeTabId 
            ? "bg-white text-slate-800 font-medium border-slate-200 relative after:absolute after:bottom-[-1px] after:left-0 after:right-0 after:h-0.5 after:bg-white" 
            : "bg-slate-200 border-gray-300 text-slate-500 hover:bg-slate-100 hover:text-slate-700"
        )}
        onclick={() => handleTabClick(tab.id)}
        onkeydown={(e) => e.key === 'Enter' && handleTabClick(tab.id)}
        role="tab"
        tabindex="0"
        aria-selected={tab.id === activeTabId}
      >
        <span class="text-base">{tab.icon}</span>
        <span class="max-w-[120px] overflow-hidden text-ellipsis">{tab.title}</span>
        {#if tab.closeable}
          <button 
            class="flex items-center justify-center w-[18px] h-[18px] ml-1 -mr-1 bg-transparent border-none rounded cursor-pointer text-base text-slate-400 leading-none hover:bg-red-600 hover:text-white"
            onclick={(e) => handleCloseTab(e, tab.id)}
            type="button"
            aria-label="닫기"
          >
            ×
          </button>
        {/if}
      </div>
    {/each}
  </div>
</div>
