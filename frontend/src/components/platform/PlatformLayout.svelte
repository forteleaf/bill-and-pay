<script lang="ts">
  import PlatformSidebar from "./PlatformSidebar.svelte";
  import Header from "../layout/Header.svelte";
  import TabBar from "../layout/TabBar.svelte";
  import StatusBar from "../layout/StatusBar.svelte";
  import { Toaster } from "$lib/components/ui/sonner";
  import * as SidebarUI from "$lib/components/ui/sidebar";
  import { tabStore } from "@/stores/tab";

  import type { Component } from "svelte";

  type LazyModule = () => Promise<{ default: Component<any> }>;

  const componentMap: Record<string, LazyModule | null> = {
    PlatformDashboard: () => import("@/routes/platform/PlatformDashboard.svelte"),
    PlatformTenantList: () => import("@/routes/platform/TenantList.svelte"),
    PlatformTenantDetail: () => import("@/routes/platform/TenantDetail.svelte"),
    PlatformTenantCreate: () => import("@/routes/platform/TenantCreate.svelte"),
    PlatformAdminList: () => import("@/routes/platform/PlatformAdminList.svelte"),
    PlatformAuditLog: () => import("@/routes/platform/PlatformAuditLog.svelte"),
    PlatformAnnouncementList: () => import("@/routes/platform/PlatformAnnouncementList.svelte"),
    PlatformMonitoring: () => import("@/routes/platform/PlatformMonitoring.svelte"),
  };

  let activeTab = $state(tabStore.getActiveTab());

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
  <PlatformSidebar />
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
