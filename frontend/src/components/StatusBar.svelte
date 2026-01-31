<script lang="ts">
  import { format } from 'date-fns';
  import { ko } from 'date-fns/locale';
  import { authStore } from '../lib/authStore';
  
  let currentTime = $state(new Date());
  let lastLogin = $state('');
  
  $effect(() => {
    // Update time every minute
    const interval = setInterval(() => {
      currentTime = new Date();
    }, 60000);
    
    // Get last login (mock for now - could store in authStore)
    const username = authStore.getUsername();
    if (username) {
      lastLogin = format(new Date(), 'yyyy-MM-dd HH:mm', { locale: ko });
    }
    
    return () => clearInterval(interval);
  });
  
  const formattedTime = $derived(
    format(currentTime, 'yyyy년 M월 d일 (E) HH:mm', { locale: ko })
  );
</script>

<footer class="flex items-center justify-between h-[30px] px-4 bg-slate-100 border-t border-slate-200 text-xs text-slate-500">
  <div class="flex items-center gap-4">
    <span class="font-medium text-slate-600">Bill&Pay v1.0.0</span>
    <span class="flex items-center gap-1">🟢 정상</span>
  </div>
  
  <div class="flex items-center gap-4">
    <span class="text-slate-600">{formattedTime}</span>
  </div>
  
  <div class="flex items-center gap-4">
    {#if lastLogin}
      <span class="text-slate-500">마지막 로그인: {lastLogin}</span>
    {/if}
  </div>
</footer>
