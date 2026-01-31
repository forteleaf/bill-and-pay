<script lang="ts">
  import { DropdownMenu } from 'bits-ui';
  import { cn } from '$lib/utils';
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

<header class="bg-gradient-to-br from-white to-slate-50 border-b border-slate-200 shadow-[0_1px_3px_rgba(0,0,0,0.04)] sticky top-0 z-[100]">
  <div class="flex items-center justify-between px-6 h-[60px] max-w-full">
    <button 
      class="flex items-center gap-2 bg-transparent border-none cursor-pointer py-2 px-3 -ml-3 rounded-lg transition-all duration-200 hover:bg-blue-500/[0.08] active:scale-[0.98]" 
      onclick={handleLogoClick} 
      type="button"
    >
      <span class="text-2xl leading-none">💳</span>
      <span class="font-sans text-xl font-bold -tracking-[0.02em] bg-gradient-to-br from-blue-800 to-blue-500 bg-clip-text text-transparent">BILL&PAY</span>
    </button>
    
    <div class="flex items-center gap-2 bg-slate-100 border border-transparent rounded-[10px] py-2 px-4 w-[280px] transition-all duration-200 focus-within:bg-white focus-within:border-blue-500 focus-within:shadow-[0_0_0_3px_rgba(59,130,246,0.1)] max-md:hidden">
      <span class="text-[0.9rem] opacity-50">🔍</span>
      <input 
        type="text" 
        class="flex-1 bg-transparent border-none outline-none text-sm text-slate-700 placeholder:text-slate-400 disabled:cursor-not-allowed disabled:opacity-70" 
        placeholder="검색..." 
        disabled
      />
    </div>
    
    <div class="flex items-center gap-2">
      <button class="flex items-center justify-center relative w-10 h-10 bg-transparent border-none rounded-[10px] cursor-pointer transition-all duration-200 hover:bg-slate-100 active:scale-95" type="button">
        <span class="text-xl">🔔</span>
        <span class="absolute top-1 right-1 min-w-[18px] h-[18px] px-[5px] text-[0.7rem] font-semibold text-white bg-gradient-to-br from-red-500 to-red-600 rounded-[9px] flex items-center justify-center shadow-[0_1px_3px_rgba(239,68,68,0.4)]">3</span>
      </button>
      
      <button class="flex items-center justify-center relative w-10 h-10 bg-transparent border-none rounded-[10px] cursor-pointer transition-all duration-200 hover:bg-slate-100 active:scale-95" onclick={handleSettingsClick} type="button">
        <span class="text-xl">⚙️</span>
      </button>
      
      <DropdownMenu.Root>
        <DropdownMenu.Trigger class="flex items-center gap-2 py-2 px-3.5 bg-slate-50 border border-slate-200 rounded-[10px] cursor-pointer text-sm font-medium text-slate-700 transition-all duration-200 hover:bg-slate-100 hover:border-slate-300 focus:outline-none focus:border-blue-500 focus:shadow-[0_0_0_3px_rgba(59,130,246,0.1)] data-[state=open]:bg-slate-100 data-[state=open]:border-blue-500">
          <span class="max-w-[100px] overflow-hidden text-ellipsis whitespace-nowrap max-md:hidden">{currentTenantName}</span>
          <span class="text-[0.625rem] text-slate-500 transition-transform duration-200 [[data-state=open]_&]:rotate-180 max-md:hidden">▼</span>
        </DropdownMenu.Trigger>
        <DropdownMenu.Content class="min-w-[180px] bg-white border border-slate-200 rounded-xl p-1.5 shadow-[0_10px_38px_-10px_rgba(22,23,24,0.35),0_10px_20px_-15px_rgba(22,23,24,0.2)] animate-[slideDown_0.15s_ease-out] z-[1000]" sideOffset={8}>
          {#each tenants as tenant (tenant.id)}
            <DropdownMenu.Item 
              class={cn(
                "flex items-center gap-2.5 py-2.5 px-3 rounded-lg cursor-pointer text-sm text-slate-700 transition-all duration-150 outline-none hover:bg-slate-100 hover:text-slate-800 data-[highlighted]:bg-slate-100 data-[highlighted]:text-slate-800 active:bg-slate-200",
                tenant.id === currentTenantId && "bg-blue-500/[0.08] text-blue-700 font-medium hover:bg-blue-500/[0.12] data-[highlighted]:bg-blue-500/[0.12]"
              )}
            >
              <button 
                class="flex items-center gap-2.5 w-full p-0 m-0 bg-transparent border-none cursor-pointer text-inherit text-left"
                onclick={() => handleTenantChange(tenant.id)}
                type="button"
              >
                {#if tenant.id === currentTenantId}
                  <span class="text-xs text-blue-500 font-bold">✓</span>
                {/if}
                <span class="flex-1">{tenant.name}</span>
              </button>
            </DropdownMenu.Item>
          {/each}
        </DropdownMenu.Content>
      </DropdownMenu.Root>
      
      <DropdownMenu.Root>
        <DropdownMenu.Trigger class="flex items-center gap-2 py-2 pl-2.5 pr-3.5 bg-slate-50 border border-slate-200 rounded-[10px] cursor-pointer text-sm font-medium text-slate-700 transition-all duration-200 hover:bg-slate-100 hover:border-slate-300 focus:outline-none focus:border-blue-500 focus:shadow-[0_0_0_3px_rgba(59,130,246,0.1)] data-[state=open]:bg-slate-100 data-[state=open]:border-blue-500 max-md:py-2 max-md:px-2">
          <span class="text-xl">👤</span>
          <span class="max-w-[100px] overflow-hidden text-ellipsis whitespace-nowrap max-md:hidden">{username}</span>
          <span class="text-[0.625rem] text-slate-500 transition-transform duration-200 [[data-state=open]_&]:rotate-180 max-md:hidden">▼</span>
        </DropdownMenu.Trigger>
        <DropdownMenu.Content class="min-w-[180px] bg-white border border-slate-200 rounded-xl p-1.5 shadow-[0_10px_38px_-10px_rgba(22,23,24,0.35),0_10px_20px_-15px_rgba(22,23,24,0.2)] animate-[slideDown_0.15s_ease-out] z-[1000]" sideOffset={8} align="end">
          <DropdownMenu.Item class="flex items-center gap-2.5 py-2.5 px-3 rounded-lg cursor-pointer text-sm text-slate-700 transition-all duration-150 outline-none hover:bg-slate-100 hover:text-slate-800 data-[highlighted]:bg-slate-100 data-[highlighted]:text-slate-800 active:bg-slate-200">
            <button class="flex items-center gap-2.5 w-full p-0 m-0 bg-transparent border-none cursor-pointer text-inherit text-left" onclick={handleProfileClick} type="button">
              <span class="text-base">👤</span>
              <span class="flex-1">내 정보</span>
            </button>
          </DropdownMenu.Item>
          <DropdownMenu.Separator class="h-px bg-slate-200 my-1.5 mx-2" />
          <DropdownMenu.Item class="flex items-center gap-2.5 py-2.5 px-3 rounded-lg cursor-pointer text-sm text-red-600 transition-all duration-150 outline-none hover:bg-red-600/[0.08] hover:text-red-700 data-[highlighted]:bg-red-600/[0.08] data-[highlighted]:text-red-700">
            <button class="flex items-center gap-2.5 w-full p-0 m-0 bg-transparent border-none cursor-pointer text-inherit text-left" onclick={handleLogout} type="button">
              <span class="text-base">🚪</span>
              <span class="flex-1">로그아웃</span>
            </button>
          </DropdownMenu.Item>
        </DropdownMenu.Content>
      </DropdownMenu.Root>
    </div>
  </div>
</header>
