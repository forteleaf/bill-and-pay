<script lang="ts">
  import { Collapsible } from 'bits-ui';
  import { cn } from '$lib/utils';
  import { tabStore, type Tab } from '../lib/tabStore';

  // Menu item types
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

  // Full menu structure from PRD-01
  const menuItems: (MenuItem | MenuGroup)[] = [
    { id: 'dashboard', title: '대시보드', icon: '📊', component: 'Dashboard' },
    {
      id: 'preferred-business',
      title: '우대사업자',
      icon: '📋',
      children: [
        { id: 'business-lookup', title: '사업자 조회', icon: '🔍', component: 'ComingSoon' }
      ]
    },
    {
      id: 'branch-mgmt',
      title: '영업점 관리',
      icon: '🏢',
      children: [
        { id: 'branch-register', title: '영업점 등록', icon: '➕', component: 'BranchRegistration' },
        { id: 'branch-list', title: '영업점 목록', icon: '📑', component: 'BranchList' },
        { id: 'branch-org', title: '조직 구성', icon: '🌳', component: 'BranchOrganization' }
      ]
    },
    {
      id: 'merchant-mgmt',
      title: '가맹점 관리',
      icon: '🏪',
      children: [
        { id: 'merchant-register', title: '가맹점 등록', icon: '➕', component: 'ComingSoon' },
        { id: 'merchant-list', title: '가맹점 목록', icon: '📑', component: 'ComingSoon' },
        { id: 'terminal-mgmt', title: '단말기 관리', icon: '💻', component: 'ComingSoon' }
      ]
    },
    {
      id: 'settlement-mgmt',
      title: '정산 관리',
      icon: '💰',
      children: [
        { id: 'branch-settlement', title: '영업점 정산내역', icon: '📈', component: 'ComingSoon' },
        { id: 'merchant-settlement', title: '가맹점 정산내역', icon: '📉', component: 'Settlements' }
      ]
    },
    {
      id: 'transfer-mgmt',
      title: '지급 이체',
      icon: '💸',
      children: [
        { id: 'transfer-register', title: '지급이체 등록', icon: '➕', component: 'ComingSoon' },
        { id: 'transfer-list', title: '지급이체 조회', icon: '🔍', component: 'ComingSoon' }
      ]
    },
    {
      id: 'payment-mgmt',
      title: '결제 관리',
      icon: '💳',
      children: [
        { id: 'payment-history', title: '결제내역', icon: '📜', component: 'Transactions' },
        { id: 'payment-failures', title: '실패내역', icon: '⚠️', component: 'ComingSoon' }
      ]
    },
    {
      id: 'admin-mgmt',
      title: '운영 관리',
      icon: '⚙️',
      children: [
        { id: 'notices', title: '공지사항', icon: '📢', component: 'ComingSoon' },
        { id: 'account-mgmt', title: '계정관리', icon: '👤', component: 'ComingSoon' },
        { id: 'settings', title: '환경설정', icon: '🔧', component: 'ComingSoon' }
      ]
    }
  ];

  // Expanded state with localStorage persistence
  let expandedGroups = $state<Set<string>>(new Set());
  const STORAGE_KEY = 'billpay-sidebar-expanded';

  // Load expanded state from localStorage on mount
  $effect(() => {
    if (typeof window !== 'undefined') {
      const stored = localStorage.getItem(STORAGE_KEY);
      if (stored) {
        try {
          expandedGroups = new Set(JSON.parse(stored));
        } catch (e) {
          console.error('Failed to parse sidebar state:', e);
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
    expandedGroups = new Set(expandedGroups); // trigger reactivity
    saveExpandedState();
  }

  function saveExpandedState() {
    if (typeof window !== 'undefined') {
      localStorage.setItem(STORAGE_KEY, JSON.stringify([...expandedGroups]));
    }
  }

  function openTab(item: MenuItem) {
    const tab: Tab = {
      id: item.id,
      title: item.title,
      icon: item.icon || '📄',
      component: item.component,
      closeable: item.id !== 'dashboard'
    };
    tabStore.openTab(tab);
  }

  function isGroup(item: MenuItem | MenuGroup): item is MenuGroup {
    return 'children' in item;
  }
</script>

<aside class="w-[250px] min-w-[250px] h-screen bg-gradient-to-b from-[#1a1a1a] to-[#0d0d0d] text-neutral-200 flex flex-col border-r border-[#2a2a2a] overflow-y-auto overflow-x-hidden scrollbar-thin scrollbar-track-transparent scrollbar-thumb-[#333] hover:scrollbar-thumb-[#444]">
  <div class="flex items-center gap-3 py-6 px-5 border-b border-[#2a2a2a] bg-gradient-to-br from-indigo-500/10 to-transparent">
    <span class="text-2xl">💎</span>
    <h1 class="text-xl font-bold bg-gradient-to-br from-indigo-300 to-indigo-500 bg-clip-text text-transparent -tracking-[0.02em]">Bill&Pay</h1>
  </div>

  <nav class="flex-1 py-3 flex flex-col gap-0.5">
    {#each menuItems as item}
      {#if isGroup(item)}
        <Collapsible.Root
          open={expandedGroups.has(item.id)}
          onOpenChange={() => toggleGroup(item.id)}
        >
          <Collapsible.Trigger class="flex items-center gap-3 w-full py-3 px-5 text-left bg-transparent border-none text-neutral-400 cursor-pointer text-sm transition-all duration-150 relative before:content-[''] before:absolute before:left-0 before:top-1/2 before:-translate-y-1/2 before:w-[3px] before:h-0 before:bg-gradient-to-b before:from-indigo-500 before:to-indigo-300 before:rounded-r-sm before:transition-all before:duration-200 hover:bg-white/5 hover:text-white hover:before:h-[60%] active:bg-white/[0.08]">
            <span class="text-base min-w-[1.25rem] text-center">{item.icon}</span>
            <span class="flex-1 font-medium">{item.title}</span>
            <span class={cn(
              "text-[0.625rem] text-neutral-500 transition-transform duration-[250ms] ease-[cubic-bezier(0.4,0,0.2,1)]",
              expandedGroups.has(item.id) && "rotate-90"
            )}>▶</span>
          </Collapsible.Trigger>
          <Collapsible.Content class="overflow-hidden animate-[slideDown_0.25s_cubic-bezier(0.4,0,0.2,1)]">
            {#each item.children as child}
              <button 
                class="flex items-center gap-3 w-full py-2.5 pl-12 pr-5 text-left bg-transparent border-none text-[#8a8a8a] cursor-pointer text-[0.8125rem] transition-all duration-150 relative before:content-[''] before:absolute before:left-6 before:top-1/2 before:-translate-y-1/2 before:w-[3px] before:h-0 before:bg-gradient-to-b before:from-indigo-500 before:to-indigo-300 before:rounded-r-sm before:transition-all before:duration-200 hover:bg-white/5 hover:text-neutral-300 hover:before:h-[60%]" 
                onclick={() => openTab(child)}
              >
                <span class="flex-1 font-medium">{child.title}</span>
              </button>
            {/each}
          </Collapsible.Content>
        </Collapsible.Root>
      {:else}
        <button 
          class="flex items-center gap-3 w-full py-3.5 px-5 text-left bg-transparent border-none text-neutral-400 cursor-pointer text-sm transition-all duration-150 relative before:content-[''] before:absolute before:left-0 before:top-1/2 before:-translate-y-1/2 before:w-[3px] before:h-0 before:bg-gradient-to-b before:from-indigo-500 before:to-indigo-300 before:rounded-r-sm before:transition-all before:duration-200 hover:bg-white/5 hover:text-white hover:before:h-[60%] active:bg-white/[0.08]" 
          onclick={() => openTab(item)}
        >
          <span class="text-base min-w-[1.25rem] text-center">{item.icon}</span>
          <span class="flex-1 font-medium">{item.title}</span>
        </button>
      {/if}
    {/each}
  </nav>

  <div class="py-4 px-5 border-t border-[#2a2a2a] flex justify-center">
    <span class="text-[0.6875rem] text-neutral-600 font-mono tracking-[0.05em]">v1.0.0</span>
  </div>
</aside>
