<script lang="ts">
  import Header from "./Header.svelte";
  import Sidebar from "./Sidebar.svelte";
  import TabBar from "./TabBar.svelte";
  import StatusBar from "./StatusBar.svelte";
  import { Toaster } from "$lib/components/ui/sonner";
  import * as SidebarUI from "$lib/components/ui/sidebar";
  import { tabStore } from "@/stores/tab";

  import type { Component } from "svelte";

  type LazyModule = () => Promise<{ default: Component<any> }>;

  const componentMap: Record<string, LazyModule | null> = {
    Dashboard: () => import("@/routes/dashboard/Dashboard.svelte"),
    Transactions: () => import("@/routes/transaction/Transactions.svelte"),
    Settlements: () => import("@/routes/settlement/Settlements.svelte"),
    SettlementBatches: () => import("@/routes/settlement/SettlementBatches.svelte"),
    SettlementSummary: () => import("@/routes/settlement/SettlementSummary.svelte"),
    MerchantManagement: () => import("@/routes/merchant/MerchantManagement.svelte"),
    Organizations: () => import("@/routes/organization/Organizations.svelte"),
    BranchRegistration: () => import("@/routes/branch/BranchRegistration.svelte"),
    BranchList: () => import("@/routes/branch/BranchList.svelte"),
    BranchDetail: () => import("@/routes/branch/BranchDetail.svelte"),
    BranchOrganization: () => import("@/routes/branch/BranchOrganization.svelte"),
    PreferentialBusinessInquiry: () => import("@/routes/misc/PreferentialBusinessInquiry.svelte"),
    MerchantRegistration: () => import("@/routes/merchant/MerchantRegistration.svelte"),
    MerchantList: () => import("@/routes/merchant/MerchantList.svelte"),
    MerchantDetail: () => import("@/routes/merchant/MerchantDetail.svelte"),
    TerminalList: () => import("@/routes/terminal/TerminalList.svelte"),
    TerminalDetail: () => import("@/routes/terminal/TerminalDetail.svelte"),
    PgConnectionList: () => import("@/routes/pg-connection/PgConnectionList.svelte"),
    PgConnectionDetail: () => import("@/routes/pg-connection/PgConnectionDetail.svelte"),
    BranchSettlement: () => import("@/routes/settlement/BranchSettlement.svelte"),
    MerchantDailySettlement: () => import("@/routes/settlement/MerchantDailySettlement.svelte"),
    MerchantStatement: () => import("@/routes/settlement/MerchantStatement.svelte"),
    OrgDailySettlement: () => import("@/routes/settlement/OrgDailySettlement.svelte"),
    OrgStatement: () => import("@/routes/settlement/OrgStatement.svelte"),
    ComingSoon: null,
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

  const activeLoader = $derived(
    activeTab ? componentMap[activeTab.component] : null,
  );
</script>

<Toaster position="top-right" richColors />

<SidebarUI.Provider>
  <Sidebar />
  <SidebarUI.Inset>
    <div class="flex flex-1 flex-col min-w-0 overflow-hidden h-screen">
      <Header />

      <TabBar />

      <main class="flex-1 min-h-0 p-6 pb-12 bg-neutral-100 overflow-y-auto">
        {#key activeTab?.id}
          {#if activeLoader}
            {#await activeLoader()}
              <div class="flex items-center justify-center h-full min-h-[400px]">
                <div class="flex flex-col items-center gap-3">
                  <div class="h-8 w-8 animate-spin rounded-full border-4 border-primary border-t-transparent"></div>
                  <p class="text-sm text-muted-foreground">로딩 중...</p>
                </div>
              </div>
            {:then module}
              {@const Comp = module.default}
              <Comp {...activeTab?.props || {}} />
            {:catch}
              <div class="flex flex-col items-center justify-center h-full min-h-[400px] text-center text-slate-500">
                <div class="text-5xl mb-4">!</div>
                <h2 class="text-xl font-semibold text-slate-700 mb-2">페이지를 불러올 수 없습니다</h2>
                <p class="text-sm text-slate-400">잠시 후 다시 시도해주세요.</p>
              </div>
            {/await}
          {:else if activeTab}
            <div
              class="flex flex-col items-center justify-center h-full min-h-[400px] text-center text-slate-500"
            >
              <div class="text-7xl mb-6 opacity-80">🚧</div>
              <h2 class="text-2xl font-semibold text-slate-700 mb-2">
                준비 중입니다
              </h2>
              <p class="text-[15px] text-slate-400">
                {activeTab.title} 기능은 곧 제공될 예정입니다.
              </p>
            </div>
          {:else}
            <div
              class="flex flex-col items-center justify-center h-full min-h-[400px] text-center text-slate-500"
            >
              <div class="text-7xl mb-6 opacity-80">📋</div>
              <h2 class="text-2xl font-semibold text-slate-700 mb-2">
                탭을 선택해주세요
              </h2>
              <p class="text-[15px] text-slate-400">
                좌측 메뉴에서 원하는 기능을 선택하세요.
              </p>
            </div>
          {/if}
        {/key}
      </main>
    </div>
  </SidebarUI.Inset>
</SidebarUI.Provider>
<StatusBar />
