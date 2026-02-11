<script lang="ts">
  import { Collapsible } from "bits-ui";
  import * as Sidebar from "$lib/components/ui/sidebar";
  import { tabStore, type Tab } from "@/stores/tab";
  import { authStore } from "@/stores/auth";
  import ChevronRightIcon from "@lucide/svelte/icons/chevron-right";

  interface MenuItem {
    id: string;
    title: string;
    icon: string;
    component: string;
  }

  interface MenuGroup {
    id: string;
    title: string;
    icon: string;
    children: MenuItem[];
    requiredRole?: string;
  }

  const role = authStore.getRole();

  const menuItems: (MenuItem | MenuGroup)[] = [
    { id: "platform-dashboard", title: "대시보드", icon: "📊", component: "PlatformDashboard" },
    {
      id: "tenant-mgmt",
      title: "테넌트 관리",
      icon: "🏢",
      children: [
        { id: "tenant-list", title: "테넌트 목록", icon: "📋", component: "PlatformTenantList" },
        { id: "tenant-create", title: "테넌트 등록", icon: "➕", component: "PlatformTenantCreate" },
      ],
    },
    {
      id: "monitoring",
      title: "모니터링",
      icon: "📡",
      children: [
        { id: "pg-monitoring", title: "PG 연결 현황", icon: "🔗", component: "PlatformMonitoring" },
      ],
    },
    {
      id: "announcement-mgmt",
      title: "공지사항",
      icon: "📢",
      children: [
        { id: "announcement-list", title: "공지 목록", icon: "📋", component: "PlatformAnnouncementList" },
      ],
    },
    ...(role === 'SUPER_ADMIN' ? [{
      id: "admin-mgmt",
      title: "관리",
      icon: "⚙️",
      children: [
        { id: "admin-list", title: "관리자 계정", icon: "👤", component: "PlatformAdminList" },
        { id: "audit-log", title: "감사 로그", icon: "📜", component: "PlatformAuditLog" },
      ],
    }] : []),
  ];

  let expandedGroups = $state<Set<string>>(new Set());

  function toggleGroup(groupId: string) {
    if (expandedGroups.has(groupId)) {
      expandedGroups.delete(groupId);
    } else {
      expandedGroups.add(groupId);
    }
    expandedGroups = new Set(expandedGroups);
  }

  function openTab(item: MenuItem) {
    const tab: Tab = {
      id: item.id,
      title: item.title,
      icon: item.icon || "📄",
      component: item.component,
      closeable: item.id !== "platform-dashboard",
    };
    tabStore.openTab(tab);
  }

  function isGroup(item: MenuItem | MenuGroup): item is MenuGroup {
    return "children" in item;
  }

  function handleLogout() {
    authStore.logout();
    window.location.reload();
  }
</script>

<Sidebar.Root class="border-r border-sidebar-border">
  <Sidebar.Header class="border-b border-sidebar-border">
    <Sidebar.Menu>
      <Sidebar.MenuItem>
        <Sidebar.MenuButton size="lg" class="hover:bg-sidebar-accent">
          <div class="flex items-center gap-3">
            <span class="text-2xl">🛡️</span>
            <div class="flex flex-col">
              <span class="text-lg font-bold">Bill&Pay</span>
              <span class="text-xs text-sidebar-foreground/60">Platform Admin</span>
            </div>
          </div>
        </Sidebar.MenuButton>
      </Sidebar.MenuItem>
    </Sidebar.Menu>
  </Sidebar.Header>

  <Sidebar.Content>
    {#each menuItems as item}
      {#if isGroup(item)}
        <Collapsible.Root
          open={expandedGroups.has(item.id)}
          onOpenChange={() => toggleGroup(item.id)}
          class="group/collapsible"
        >
          <Sidebar.Group>
            <Sidebar.GroupLabel class="pr-0">
              {#snippet child({ props })}
                <Collapsible.Trigger
                  {...props}
                  class="flex w-full items-center gap-2 rounded-md px-2 py-1.5 text-sm font-medium text-sidebar-foreground/70 hover:bg-sidebar-accent hover:text-sidebar-foreground"
                >
                  <span class="text-base">{item.icon}</span>
                  <span class="flex-1 text-left">{item.title}</span>
                  <ChevronRightIcon
                    class="size-4 transition-transform duration-200 group-data-[state=open]/collapsible:rotate-90"
                  />
                </Collapsible.Trigger>
              {/snippet}
            </Sidebar.GroupLabel>
            <Collapsible.Content>
              <Sidebar.GroupContent>
                <Sidebar.Menu>
                  {#each item.children as child}
                    <Sidebar.MenuItem>
                      <Sidebar.MenuButton
                        class="pl-8 text-sidebar-foreground/60 hover:text-sidebar-foreground"
                        onclick={() => openTab(child)}
                      >
                        <span>{child.title}</span>
                      </Sidebar.MenuButton>
                    </Sidebar.MenuItem>
                  {/each}
                </Sidebar.Menu>
              </Sidebar.GroupContent>
            </Collapsible.Content>
          </Sidebar.Group>
        </Collapsible.Root>
      {:else}
        <Sidebar.Group>
          <Sidebar.GroupContent>
            <Sidebar.Menu>
              <Sidebar.MenuItem>
                <Sidebar.MenuButton onclick={() => openTab(item)}>
                  <span class="text-base">{item.icon}</span>
                  <span>{item.title}</span>
                </Sidebar.MenuButton>
              </Sidebar.MenuItem>
            </Sidebar.Menu>
          </Sidebar.GroupContent>
        </Sidebar.Group>
      {/if}
    {/each}
  </Sidebar.Content>

  <Sidebar.Footer class="border-t border-sidebar-border">
    <Sidebar.Menu>
      <Sidebar.MenuItem>
        <div class="flex flex-col gap-2 px-2 py-2">
          <div class="text-xs text-sidebar-foreground/70">
            {authStore.getFullName()} ({authStore.getRole()})
          </div>
          <button
            class="text-xs text-sidebar-foreground/50 hover:text-destructive text-left"
            onclick={handleLogout}
          >
            로그아웃
          </button>
        </div>
      </Sidebar.MenuItem>
    </Sidebar.Menu>
  </Sidebar.Footer>

  <Sidebar.Rail />
</Sidebar.Root>
