<script lang="ts">
  import NewHeader from './NewHeader.svelte';
  import NewSidebar from './NewSidebar.svelte';
  import TabBar from './TabBar.svelte';
  import StatusBar from './StatusBar.svelte';
  import { Toaster } from '$lib/components/ui/sonner';
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
  import BranchOrganization from '../routes/branch/BranchOrganization.svelte';
  import PreferentialBusinessInquiry from '../routes/PreferentialBusinessInquiry.svelte';
  import MerchantRegistration from '../routes/merchant/MerchantRegistration.svelte';
  import MerchantList from '../routes/merchant/MerchantList.svelte';
  import MerchantDetail from '../routes/merchant/MerchantDetail.svelte';
  
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
    'BranchOrganization': BranchOrganization,
    'PreferentialBusinessInquiry': PreferentialBusinessInquiry,
    'MerchantRegistration': MerchantRegistration,
    'MerchantList': MerchantList,
    'MerchantDetail': MerchantDetail,
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

<Toaster position="top-right" richColors />

<div class="flex min-h-screen max-h-screen overflow-hidden">
  <NewSidebar />
  
  <div class="flex-1 flex flex-col min-w-0 overflow-hidden">
    <NewHeader />
    
    <TabBar />
    
    <main class="flex-1 p-6 bg-neutral-100 overflow-y-auto">
      {#key activeTab?.id}
        {#if activeComponent}
          {@const DynamicComponent = activeComponent}
          <DynamicComponent {...activeTab?.props || {}} />
        {:else if activeTab}
          <div class="flex flex-col items-center justify-center h-full min-h-[400px] text-center text-slate-500">
            <div class="text-7xl mb-6 opacity-80">🚧</div>
            <h2 class="text-2xl font-semibold text-slate-700 mb-2">준비 중입니다</h2>
            <p class="text-[15px] text-slate-400">{activeTab.title} 기능은 곧 제공될 예정입니다.</p>
          </div>
        {:else}
          <div class="flex flex-col items-center justify-center h-full min-h-[400px] text-center text-slate-500">
            <div class="text-7xl mb-6 opacity-80">📋</div>
            <h2 class="text-2xl font-semibold text-slate-700 mb-2">탭을 선택해주세요</h2>
            <p class="text-[15px] text-slate-400">좌측 메뉴에서 원하는 기능을 선택하세요.</p>
          </div>
        {/if}
      {/key}
    </main>
    
    <StatusBar />
  </div>
</div>
