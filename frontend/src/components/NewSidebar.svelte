<script lang="ts">
  import { Collapsible } from 'bits-ui';
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
        { id: 'branch-list', title: '영업점 목록', icon: '📑', component: 'BranchList' }
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

<aside class="sidebar">
  <div class="logo">
    <span class="logo-icon">💎</span>
    <h1>Bill&Pay</h1>
  </div>

  <nav class="nav">
    {#each menuItems as item}
      {#if isGroup(item)}
        <Collapsible.Root
          open={expandedGroups.has(item.id)}
          onOpenChange={() => toggleGroup(item.id)}
        >
          <Collapsible.Trigger class="menu-group-trigger">
            <span class="menu-icon">{item.icon}</span>
            <span class="menu-title">{item.title}</span>
            <span class="expand-arrow" class:expanded={expandedGroups.has(item.id)}>▶</span>
          </Collapsible.Trigger>
          <Collapsible.Content class="menu-group-content">
            {#each item.children as child}
              <button class="menu-item child-item" onclick={() => openTab(child)}>
                <span class="menu-title">{child.title}</span>
              </button>
            {/each}
          </Collapsible.Content>
        </Collapsible.Root>
      {:else}
        <button class="menu-item top-item" onclick={() => openTab(item)}>
          <span class="menu-icon">{item.icon}</span>
          <span class="menu-title">{item.title}</span>
        </button>
      {/if}
    {/each}
  </nav>

  <div class="sidebar-footer">
    <span class="version">v1.0.0</span>
  </div>
</aside>

<style>
  .sidebar {
    width: 250px;
    min-width: 250px;
    height: 100vh;
    background: linear-gradient(180deg, #1a1a1a 0%, #0d0d0d 100%);
    color: #e5e5e5;
    display: flex;
    flex-direction: column;
    border-right: 1px solid #2a2a2a;
    overflow-y: auto;
    overflow-x: hidden;
  }

  .logo {
    display: flex;
    align-items: center;
    gap: 0.75rem;
    padding: 1.5rem 1.25rem;
    border-bottom: 1px solid #2a2a2a;
    background: linear-gradient(135deg, rgba(99, 102, 241, 0.1) 0%, transparent 100%);
  }

  .logo-icon {
    font-size: 1.5rem;
  }

  .logo h1 {
    font-size: 1.25rem;
    font-weight: 700;
    background: linear-gradient(135deg, #a5b4fc 0%, #6366f1 100%);
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
    background-clip: text;
    letter-spacing: -0.02em;
  }

  .nav {
    flex: 1;
    padding: 0.75rem 0;
    display: flex;
    flex-direction: column;
    gap: 2px;
  }

  /* Shared menu item styles */
  .menu-item,
  :global(.menu-group-trigger) {
    display: flex;
    align-items: center;
    gap: 0.75rem;
    width: 100%;
    padding: 0.75rem 1.25rem;
    text-align: left;
    background: transparent;
    border: none;
    color: #a3a3a3;
    cursor: pointer;
    font-size: 0.875rem;
    transition: all 0.15s ease;
    position: relative;
  }

  .menu-item:hover,
  :global(.menu-group-trigger:hover) {
    background: rgba(255, 255, 255, 0.05);
    color: #ffffff;
  }

  .menu-item:active,
  :global(.menu-group-trigger:active) {
    background: rgba(255, 255, 255, 0.08);
  }

  /* Active state indicator */
  .menu-item::before,
  :global(.menu-group-trigger::before) {
    content: '';
    position: absolute;
    left: 0;
    top: 50%;
    transform: translateY(-50%);
    width: 3px;
    height: 0;
    background: linear-gradient(180deg, #6366f1 0%, #a5b4fc 100%);
    border-radius: 0 2px 2px 0;
    transition: height 0.2s ease;
  }

  .menu-item:hover::before,
  :global(.menu-group-trigger:hover::before) {
    height: 60%;
  }

  .top-item {
    padding: 0.875rem 1.25rem;
  }

  .menu-icon {
    font-size: 1rem;
    min-width: 1.25rem;
    text-align: center;
  }

  .menu-title {
    flex: 1;
    font-weight: 500;
  }

  .expand-arrow {
    font-size: 0.625rem;
    color: #666;
    transition: transform 0.25s cubic-bezier(0.4, 0, 0.2, 1);
  }

  .expand-arrow.expanded {
    transform: rotate(90deg);
  }

  /* Collapsible content */
  :global(.menu-group-content) {
    overflow: hidden;
    animation: slideDown 0.25s cubic-bezier(0.4, 0, 0.2, 1);
  }

  @keyframes slideDown {
    from {
      opacity: 0;
      transform: translateY(-8px);
    }
    to {
      opacity: 1;
      transform: translateY(0);
    }
  }

  .child-item {
    padding: 0.625rem 1.25rem 0.625rem 3rem;
    font-size: 0.8125rem;
    color: #8a8a8a;
  }

  .child-item:hover {
    color: #d4d4d4;
  }

  .child-item::before {
    left: 1.5rem;
  }

  .sidebar-footer {
    padding: 1rem 1.25rem;
    border-top: 1px solid #2a2a2a;
    display: flex;
    justify-content: center;
  }

  .version {
    font-size: 0.6875rem;
    color: #525252;
    font-family: 'Monaco', 'Menlo', monospace;
    letter-spacing: 0.05em;
  }

  /* Scrollbar styling */
  .sidebar::-webkit-scrollbar {
    width: 6px;
  }

  .sidebar::-webkit-scrollbar-track {
    background: transparent;
  }

  .sidebar::-webkit-scrollbar-thumb {
    background: #333;
    border-radius: 3px;
  }

  .sidebar::-webkit-scrollbar-thumb:hover {
    background: #444;
  }
</style>
