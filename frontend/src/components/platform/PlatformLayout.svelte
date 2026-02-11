<script lang="ts">
  import PlatformSidebar from "./PlatformSidebar.svelte";
  import Header from "../layout/Header.svelte";
  import TabBar from "../layout/TabBar.svelte";
  import StatusBar from "../layout/StatusBar.svelte";
  import { Toaster } from "$lib/components/ui/sonner";
  import * as SidebarUI from "$lib/components/ui/sidebar";
  import { tabStore } from "@/stores/tab";

  import PlatformDashboard from "@/routes/platform/PlatformDashboard.svelte";
  import TenantList from "@/routes/platform/TenantList.svelte";
  import TenantDetail from "@/routes/platform/TenantDetail.svelte";
  import TenantCreate from "@/routes/platform/TenantCreate.svelte";
  import PlatformAdminList from "@/routes/platform/PlatformAdminList.svelte";
  import PlatformAuditLog from "@/routes/platform/PlatformAuditLog.svelte";
  import PlatformAnnouncementList from "@/routes/platform/PlatformAnnouncementList.svelte";
  import PlatformMonitoring from "@/routes/platform/PlatformMonitoring.svelte";

  import type { Component } from "svelte";

  const componentMap: Record<string, Component<any> | null> = {
    PlatformDashboard: PlatformDashboard,
    PlatformTenantList: TenantList,
    PlatformTenantDetail: TenantDetail,
    PlatformTenantCreate: TenantCreate,
    PlatformAdminList: PlatformAdminList,
    PlatformAuditLog: PlatformAuditLog,
    PlatformAnnouncementList: PlatformAnnouncementList,
    PlatformMonitoring: PlatformMonitoring,
  };

  let activeTab = $state(tabStore.getActiveTab());

  $effect(() => {
    const interval = setInterval(() => {
      activeTab = tabStore.getActiveTab();
    }, 100);
    return () => clearInterval(interval);
  });

  const activeComponent = $derived(
    activeTab ? componentMap[activeTab.component] : null,
  );
</script>

<Toaster position="top-right" richColors />

<SidebarUI.Provider>
  <PlatformSidebar />
  <SidebarUI.Inset>
    <div class="flex flex-1 flex-col min-w-0 overflow-hidden h-screen">
      <Header />

      <TabBar />

      <main class="flex-1 min-h-0 p-6 pb-12 bg-neutral-100 overflow-y-auto">
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
              <div class="text-7xl mb-6 opacity-80">🛡️</div>
              <h2 class="text-2xl font-semibold text-slate-700 mb-2">플랫폼 관리</h2>
              <p class="text-[15px] text-slate-400">좌측 메뉴에서 관리 기능을 선택하세요.</p>
            </div>
          {/if}
        {/key}
      </main>
    </div>
  </SidebarUI.Inset>
</SidebarUI.Provider>
<StatusBar />
