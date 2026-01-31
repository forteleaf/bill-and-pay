<script lang="ts">
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

<div class="tab-bar">
  <div class="tab-list" role="tablist">
    {#each tabs as tab (tab.id)}
      <div 
        class="tab {tab.id === activeTabId ? 'active' : ''}"
        onclick={() => handleTabClick(tab.id)}
        onkeydown={(e) => e.key === 'Enter' && handleTabClick(tab.id)}
        role="tab"
        tabindex="0"
        aria-selected={tab.id === activeTabId}
      >
        <span class="tab-icon">{tab.icon}</span>
        <span class="tab-title">{tab.title}</span>
        {#if tab.closeable}
          <button 
            class="tab-close"
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

<style>
  .tab-bar {
    background: #f8fafc;
    border-bottom: 1px solid #e2e8f0;
    padding: 0.5rem 1rem 0;
    display: flex;
    align-items: flex-end;
  }

  .tab-list {
    display: flex;
    gap: 0.25rem;
    overflow-x: auto;
    padding-bottom: 0;
  }

  .tab {
    display: flex;
    align-items: center;
    gap: 0.5rem;
    padding: 0.625rem 1rem;
    background: #e2e8f0;
    border: 1px solid #d1d5db;
    border-bottom: none;
    border-radius: 8px 8px 0 0;
    cursor: pointer;
    font-size: 0.875rem;
    color: #64748b;
    transition: all 0.15s ease;
    white-space: nowrap;
  }

  .tab:hover {
    background: #f1f5f9;
    color: #334155;
  }

  .tab.active {
    background: white;
    color: #1e293b;
    font-weight: 500;
    border-color: #e2e8f0;
    position: relative;
  }

  /* Active tab overlaps border */
  .tab.active::after {
    content: '';
    position: absolute;
    bottom: -1px;
    left: 0;
    right: 0;
    height: 2px;
    background: white;
  }

  .tab-icon {
    font-size: 1rem;
  }

  .tab-title {
    max-width: 120px;
    overflow: hidden;
    text-overflow: ellipsis;
  }

  .tab-close {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 18px;
    height: 18px;
    margin-left: 0.25rem;
    margin-right: -0.25rem;
    background: none;
    border: none;
    border-radius: 4px;
    cursor: pointer;
    font-size: 1rem;
    color: #94a3b8;
    line-height: 1;
  }

  .tab-close:hover {
    background: #dc2626;
    color: white;
  }

  /* Scrollbar styling */
  .tab-list::-webkit-scrollbar {
    height: 4px;
  }

  .tab-list::-webkit-scrollbar-track {
    background: transparent;
  }

  .tab-list::-webkit-scrollbar-thumb {
    background: #cbd5e1;
    border-radius: 2px;
  }
</style>
