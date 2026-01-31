<script lang="ts">
  import NewHeader from './NewHeader.svelte';
  import NewSidebar from './NewSidebar.svelte';
  import TabBar from './TabBar.svelte';
  import StatusBar from './StatusBar.svelte';
  import { tabStore } from '../lib/tabStore';
  
  import Dashboard from '../routes/Dashboard.svelte';
  import Transactions from '../routes/Transactions.svelte';
  import Settlements from '../routes/Settlements.svelte';
  import SettlementBatches from '../routes/SettlementBatches.svelte';
  import SettlementSummary from '../routes/SettlementSummary.svelte';
  import MerchantManagement from '../routes/MerchantManagement.svelte';
  import Organizations from '../routes/Organizations.svelte';
  import BranchRegistration from '../routes/branch/BranchRegistration.svelte';
  import BranchList from '../routes/branch/BranchList.svelte';
  import BranchDetail from '../routes/branch/BranchDetail.svelte';
  
  import type { Component } from 'svelte';
  
  const componentMap: Record<string, Component<any> | null> = {
    'Dashboard': Dashboard,
    'Transactions': Transactions,
    'Settlements': Settlements,
    'SettlementBatches': SettlementBatches,
    'SettlementSummary': SettlementSummary,
    'MerchantManagement': MerchantManagement,
    'Organizations': Organizations,
    'BranchRegistration': BranchRegistration,
    'BranchList': BranchList,
    'BranchDetail': BranchDetail,
    'ComingSoon': null,
  };
  
  // Track active tab
  let activeTab = $state(tabStore.getActiveTab());
  
  // Poll for updates from tabStore
  $effect(() => {
    const interval = setInterval(() => {
      activeTab = tabStore.getActiveTab();
    }, 100);
    return () => clearInterval(interval);
  });
  
  // Get active component
  const activeComponent = $derived(
    activeTab ? componentMap[activeTab.component] : null
  );
</script>

<div class="layout">
  <NewSidebar />
  
  <div class="main-area">
    <NewHeader />
    
    <TabBar />
    
    <main class="content-area">
      {#if activeComponent}
        {@const DynamicComponent = activeComponent}
        <DynamicComponent {...activeTab?.props || {}} />
      {:else if activeTab}
        <div class="coming-soon">
          <div class="coming-soon-icon">🚧</div>
          <h2>준비 중입니다</h2>
          <p>{activeTab.title} 기능은 곧 제공될 예정입니다.</p>
        </div>
      {:else}
        <div class="no-content">
          <div class="no-content-icon">📋</div>
          <h2>탭을 선택해주세요</h2>
          <p>좌측 메뉴에서 원하는 기능을 선택하세요.</p>
        </div>
      {/if}
    </main>
    
    <StatusBar />
  </div>
</div>

<style>
  .layout {
    display: flex;
    min-height: 100vh;
    max-height: 100vh;
    overflow: hidden;
  }
  
  .main-area {
    flex: 1;
    display: flex;
    flex-direction: column;
    min-width: 0;
    overflow: hidden;
  }
  
  .content-area {
    flex: 1;
    padding: 1.5rem;
    background-color: #f5f5f5;
    overflow-y: auto;
  }
  
  .coming-soon,
  .no-content {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    height: 100%;
    min-height: 400px;
    text-align: center;
    color: #64748b;
  }
  
  .coming-soon-icon,
  .no-content-icon {
    font-size: 4rem;
    margin-bottom: 1.5rem;
    opacity: 0.8;
  }
  
  .coming-soon h2,
  .no-content h2 {
    font-size: 1.5rem;
    font-weight: 600;
    color: #334155;
    margin-bottom: 0.5rem;
  }
  
  .coming-soon p,
  .no-content p {
    font-size: 0.95rem;
    color: #94a3b8;
  }
</style>
