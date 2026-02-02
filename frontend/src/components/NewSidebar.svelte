<script lang="ts">
  import { Collapsible } from "bits-ui";
  import * as Sidebar from "$lib/components/ui/sidebar";
  import { tabStore, type Tab } from "../lib/tabStore";
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
  }

  const menuItems: (MenuItem | MenuGroup)[] = [
    { id: "dashboard", title: "대시보드", icon: "📊", component: "Dashboard" },
    {
      id: "preferred-business",
      title: "우대사업자",
      icon: "📋",
      children: [
        {
          id: "business-lookup",
          title: "사업자 조회",
          icon: "🔍",
          component: "PreferentialBusinessInquiry",
        },
      ],
    },
    {
      id: "branch-mgmt",
      title: "영업점 관리",
      icon: "🏢",
      children: [
        {
          id: "branch-register",
          title: "영업점 등록",
          icon: "➕",
          component: "BranchRegistration",
        },
        {
          id: "branch-list",
          title: "영업점 목록",
          icon: "📑",
          component: "BranchList",
        },
        {
          id: "branch-org",
          title: "조직 구성",
          icon: "🌳",
          component: "BranchOrganization",
        },
      ],
    },
    {
      id: "merchant-mgmt",
      title: "가맹점 관리",
      icon: "🏪",
      children: [
        {
          id: "merchant-register",
          title: "가맹점 등록",
          icon: "➕",
          component: "MerchantRegistration",
        },
        {
          id: "merchant-list",
          title: "가맹점 목록",
          icon: "📑",
          component: "MerchantList",
        },
        {
          id: "terminal-mgmt",
          title: "단말기 관리",
          icon: "💻",
          component: "TerminalList",
        },
      ],
    },
    {
      id: "settlement-mgmt",
      title: "정산 관리",
      icon: "💰",
      children: [
        {
          id: "branch-settlement",
          title: "영업점 정산내역",
          icon: "📈",
          component: "ComingSoon",
        },
        {
          id: "merchant-settlement",
          title: "가맹점 정산내역",
          icon: "📉",
          component: "Settlements",
        },
      ],
    },
    {
      id: "transfer-mgmt",
      title: "지급 이체",
      icon: "💸",
      children: [
        {
          id: "transfer-register",
          title: "지급이체 등록",
          icon: "➕",
          component: "ComingSoon",
        },
        {
          id: "transfer-list",
          title: "지급이체 조회",
          icon: "🔍",
          component: "ComingSoon",
        },
      ],
    },
    {
      id: "payment-mgmt",
      title: "결제 관리",
      icon: "💳",
      children: [
        {
          id: "payment-history",
          title: "결제내역",
          icon: "📜",
          component: "Transactions",
        },
        {
          id: "payment-failures",
          title: "실패내역",
          icon: "⚠️",
          component: "ComingSoon",
        },
      ],
    },
    {
      id: "admin-mgmt",
      title: "운영 관리",
      icon: "⚙️",
      children: [
        {
          id: "notices",
          title: "공지사항",
          icon: "📢",
          component: "ComingSoon",
        },
        {
          id: "account-mgmt",
          title: "계정관리",
          icon: "👤",
          component: "ComingSoon",
        },
        {
          id: "settings",
          title: "환경설정",
          icon: "🔧",
          component: "ComingSoon",
        },
      ],
    },
  ];

  let expandedGroups = $state<Set<string>>(new Set());
  const STORAGE_KEY = "billpay-sidebar-expanded";

  $effect(() => {
    if (typeof window !== "undefined") {
      const stored = localStorage.getItem(STORAGE_KEY);
      if (stored) {
        try {
          expandedGroups = new Set(JSON.parse(stored));
        } catch (e) {
          console.error("Failed to parse sidebar state:", e);
        }
      }
    }
  });

  function toggleGroup(groupId: string) {
    if (expandedGroups.has(groupId)) {
      expandedGroups.delete(groupId);
    } else {
      expandedGroups.add(groupId);
    }
    expandedGroups = new Set(expandedGroups);
    saveExpandedState();
  }

  function saveExpandedState() {
    if (typeof window !== "undefined") {
      localStorage.setItem(STORAGE_KEY, JSON.stringify([...expandedGroups]));
    }
  }

  function openTab(item: MenuItem) {
    const tab: Tab = {
      id: item.id,
      title: item.title,
      icon: item.icon || "📄",
      component: item.component,
      closeable: item.id !== "dashboard",
    };
    tabStore.openTab(tab);
  }

  function isGroup(item: MenuItem | MenuGroup): item is MenuGroup {
    return "children" in item;
  }
</script>

<Sidebar.Root class="border-r border-sidebar-border">
  <Sidebar.Header class="border-b border-sidebar-border">
    <Sidebar.Menu>
      <Sidebar.MenuItem>
        <Sidebar.MenuButton size="lg" class="hover:bg-sidebar-accent">
          <div class="flex items-center gap-3">
            <span class="text-2xl">💎</span>
            <div class="flex flex-col">
              <span class="text-lg font-bold">Bill&Pay</span>
              <span class="text-xs text-sidebar-foreground/60"
                >Settlement Platform</span
              >
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
        <div class="flex justify-center py-2">
          <span class="text-xs text-sidebar-foreground/50 font-mono"
            >v1.0.0</span
          >
        </div>
      </Sidebar.MenuItem>
    </Sidebar.Menu>
  </Sidebar.Footer>

  <Sidebar.Rail />
</Sidebar.Root>
