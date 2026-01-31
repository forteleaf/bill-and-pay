<script lang="ts">
  import { DropdownMenu } from 'bits-ui';
  import { authStore } from '../lib/authStore';
  import { tenantStore } from '../lib/stores';
  import { tabStore } from '../lib/tabStore';
  
  let username = $state('');
  let currentTenantId = $state('');
  
  const tenants = [
    { id: 'tenant_001', name: '총판 A' },
    { id: 'tenant_002', name: '총판 B' },
    { id: 'tenant_003', name: '총판 C' }
  ];
  
  const currentTenantName = $derived(
    tenants.find(t => t.id === currentTenantId)?.name || '테넌트 선택'
  );
  
  $effect(() => {
    username = authStore.getUsername() || '사용자';
    currentTenantId = authStore.getTenantId() || tenants[0].id;
  });
  
  function handleLogoClick() {
    tabStore.focusTab('dashboard');
  }
  
  function handleTenantChange(tenantId: string) {
    currentTenantId = tenantId;
    tenantStore.setCurrent(tenantId);
  }
  
  function handleLogout() {
    if (confirm('로그아웃 하시겠습니까?')) {
      authStore.logout();
      window.location.href = '/';
    }
  }
  
  function handleSettingsClick() {
    alert('설정 기능은 준비 중입니다.');
  }
  
  function handleProfileClick() {
    alert('내 정보 기능은 준비 중입니다.');
  }
</script>

<header class="header">
  <div class="header-content">
    <button class="logo-section" onclick={handleLogoClick} type="button">
      <span class="logo-icon">💳</span>
      <span class="logo-text">BILL&PAY</span>
    </button>
    
    <div class="search-section">
      <span class="search-icon">🔍</span>
      <input 
        type="text" 
        class="search-input" 
        placeholder="검색..." 
        disabled
      />
    </div>
    
    <div class="actions-section">
      <button class="action-button notification-button" type="button">
        <span class="action-icon">🔔</span>
        <span class="notification-badge">3</span>
      </button>
      
      <button class="action-button" onclick={handleSettingsClick} type="button">
        <span class="action-icon">⚙️</span>
      </button>
      
      <DropdownMenu.Root>
        <DropdownMenu.Trigger class="dropdown-trigger tenant-trigger">
          <span class="trigger-text">{currentTenantName}</span>
          <span class="trigger-arrow">▼</span>
        </DropdownMenu.Trigger>
        <DropdownMenu.Content class="dropdown-content" sideOffset={8}>
          {#each tenants as tenant (tenant.id)}
            <DropdownMenu.Item 
              class="dropdown-item {tenant.id === currentTenantId ? 'active' : ''}"
            >
              <button 
                class="dropdown-item-btn"
                onclick={() => handleTenantChange(tenant.id)}
                type="button"
              >
                {#if tenant.id === currentTenantId}
                  <span class="check-icon">✓</span>
                {/if}
                <span class="item-text">{tenant.name}</span>
              </button>
            </DropdownMenu.Item>
          {/each}
        </DropdownMenu.Content>
      </DropdownMenu.Root>
      
      <DropdownMenu.Root>
        <DropdownMenu.Trigger class="dropdown-trigger user-trigger">
          <span class="user-avatar">👤</span>
          <span class="trigger-text">{username}</span>
          <span class="trigger-arrow">▼</span>
        </DropdownMenu.Trigger>
        <DropdownMenu.Content class="dropdown-content" sideOffset={8} align="end">
          <DropdownMenu.Item class="dropdown-item">
            <button class="dropdown-item-btn" onclick={handleProfileClick} type="button">
              <span class="item-icon">👤</span>
              <span class="item-text">내 정보</span>
            </button>
          </DropdownMenu.Item>
          <DropdownMenu.Separator class="dropdown-separator" />
          <DropdownMenu.Item class="dropdown-item logout-item">
            <button class="dropdown-item-btn" onclick={handleLogout} type="button">
              <span class="item-icon">🚪</span>
              <span class="item-text">로그아웃</span>
            </button>
          </DropdownMenu.Item>
        </DropdownMenu.Content>
      </DropdownMenu.Root>
    </div>
  </div>
</header>

<style>
  .header {
    background: linear-gradient(135deg, #ffffff 0%, #f8fafc 100%);
    border-bottom: 1px solid #e2e8f0;
    box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
    position: sticky;
    top: 0;
    z-index: 100;
  }
  
  .header-content {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 0 1.5rem;
    height: 60px;
    max-width: 100%;
  }
  
  .logo-section {
    display: flex;
    align-items: center;
    gap: 0.5rem;
    background: none;
    border: none;
    cursor: pointer;
    padding: 0.5rem 0.75rem;
    margin-left: -0.75rem;
    border-radius: 8px;
    transition: all 0.2s ease;
  }
  
  .logo-section:hover {
    background: rgba(59, 130, 246, 0.08);
  }
  
  .logo-section:active {
    transform: scale(0.98);
  }
  
  .logo-icon {
    font-size: 1.5rem;
    line-height: 1;
  }
  
  .logo-text {
    font-family: 'SF Pro Display', -apple-system, BlinkMacSystemFont, sans-serif;
    font-size: 1.25rem;
    font-weight: 700;
    letter-spacing: -0.02em;
    background: linear-gradient(135deg, #1e40af 0%, #3b82f6 100%);
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
    background-clip: text;
  }
  
  .search-section {
    display: flex;
    align-items: center;
    gap: 0.5rem;
    background: #f1f5f9;
    border: 1px solid transparent;
    border-radius: 10px;
    padding: 0.5rem 1rem;
    width: 280px;
    transition: all 0.2s ease;
  }
  
  .search-section:focus-within {
    background: #ffffff;
    border-color: #3b82f6;
    box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
  }
  
  .search-icon {
    font-size: 0.9rem;
    opacity: 0.5;
  }
  
  .search-input {
    flex: 1;
    background: none;
    border: none;
    outline: none;
    font-size: 0.875rem;
    color: #334155;
  }
  
  .search-input::placeholder {
    color: #94a3b8;
  }
  
  .search-input:disabled {
    cursor: not-allowed;
    opacity: 0.7;
  }
  
  .actions-section {
    display: flex;
    align-items: center;
    gap: 0.5rem;
  }
  
  .action-button {
    display: flex;
    align-items: center;
    justify-content: center;
    position: relative;
    width: 40px;
    height: 40px;
    background: none;
    border: none;
    border-radius: 10px;
    cursor: pointer;
    transition: all 0.2s ease;
  }
  
  .action-button:hover {
    background: #f1f5f9;
  }
  
  .action-button:active {
    transform: scale(0.95);
  }
  
  .action-icon {
    font-size: 1.25rem;
  }
  
  .notification-badge {
    position: absolute;
    top: 4px;
    right: 4px;
    min-width: 18px;
    height: 18px;
    padding: 0 5px;
    font-size: 0.7rem;
    font-weight: 600;
    color: white;
    background: linear-gradient(135deg, #ef4444 0%, #dc2626 100%);
    border-radius: 9px;
    display: flex;
    align-items: center;
    justify-content: center;
    box-shadow: 0 1px 3px rgba(239, 68, 68, 0.4);
  }
  
  :global(.dropdown-trigger) {
    display: flex;
    align-items: center;
    gap: 0.5rem;
    padding: 0.5rem 0.875rem;
    background: #f8fafc;
    border: 1px solid #e2e8f0;
    border-radius: 10px;
    cursor: pointer;
    font-size: 0.875rem;
    font-weight: 500;
    color: #334155;
    transition: all 0.2s ease;
  }
  
  :global(.dropdown-trigger:hover) {
    background: #f1f5f9;
    border-color: #cbd5e1;
  }
  
  :global(.dropdown-trigger:focus) {
    outline: none;
    border-color: #3b82f6;
    box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
  }
  
  :global(.dropdown-trigger[data-state="open"]) {
    background: #f1f5f9;
    border-color: #3b82f6;
  }
  
  .trigger-text {
    max-width: 100px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
  
  .trigger-arrow {
    font-size: 0.625rem;
    color: #64748b;
    transition: transform 0.2s ease;
  }
  
  :global(.dropdown-trigger[data-state="open"]) .trigger-arrow {
    transform: rotate(180deg);
  }
  
  .user-avatar {
    font-size: 1.25rem;
  }
  
  :global(.user-trigger) {
    padding-left: 0.625rem;
  }
  
  :global(.dropdown-content) {
    min-width: 180px;
    background: white;
    border: 1px solid #e2e8f0;
    border-radius: 12px;
    padding: 0.375rem;
    box-shadow: 
      0 10px 38px -10px rgba(22, 23, 24, 0.35),
      0 10px 20px -15px rgba(22, 23, 24, 0.2);
    animation: slideDown 0.15s ease-out;
    z-index: 1000;
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
  
  :global(.dropdown-item) {
    display: flex;
    align-items: center;
    gap: 0.625rem;
    padding: 0.625rem 0.75rem;
    border-radius: 8px;
    cursor: pointer;
    font-size: 0.875rem;
    color: #334155;
    transition: all 0.15s ease;
    outline: none;
  }
  
  :global(.dropdown-item:hover),
  :global(.dropdown-item[data-highlighted]) {
    background: #f1f5f9;
    color: #1e293b;
  }
  
  :global(.dropdown-item:active) {
    background: #e2e8f0;
  }
  
  :global(.dropdown-item.active) {
    background: rgba(59, 130, 246, 0.08);
    color: #1d4ed8;
    font-weight: 500;
  }
  
  :global(.dropdown-item.active:hover),
  :global(.dropdown-item.active[data-highlighted]) {
    background: rgba(59, 130, 246, 0.12);
  }
  
  .check-icon {
    font-size: 0.75rem;
    color: #3b82f6;
    font-weight: 700;
  }
  
  .item-icon {
    font-size: 1rem;
  }
  
  .item-text {
    flex: 1;
  }
  
  .dropdown-item-btn {
    display: flex;
    align-items: center;
    gap: 0.625rem;
    width: 100%;
    padding: 0;
    margin: 0;
    background: none;
    border: none;
    cursor: pointer;
    font-size: inherit;
    font-family: inherit;
    color: inherit;
    text-align: left;
  }
  
  :global(.dropdown-separator) {
    height: 1px;
    background: #e2e8f0;
    margin: 0.375rem 0.5rem;
  }
  
  :global(.logout-item) {
    color: #dc2626;
  }
  
  :global(.logout-item:hover),
  :global(.logout-item[data-highlighted]) {
    background: rgba(220, 38, 38, 0.08);
    color: #b91c1c;
  }
  
  @media (max-width: 768px) {
    .search-section {
      display: none;
    }
    
    .trigger-text {
      display: none;
    }
    
    :global(.tenant-trigger) {
      padding: 0.5rem;
    }
    
    :global(.user-trigger) {
      padding: 0.5rem;
    }
    
    .trigger-arrow {
      display: none;
    }
  }
</style>
