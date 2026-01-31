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

<footer class="status-bar">
  <div class="status-left">
    <span class="version">Bill&Pay v1.0.0</span>
    <span class="status-indicator">🟢 정상</span>
  </div>
  
  <div class="status-center">
    <span class="current-time">{formattedTime}</span>
  </div>
  
  <div class="status-right">
    {#if lastLogin}
      <span class="last-login">마지막 로그인: {lastLogin}</span>
    {/if}
  </div>
</footer>

<style>
  .status-bar {
    display: flex;
    align-items: center;
    justify-content: space-between;
    height: 30px;
    padding: 0 1rem;
    background: #f1f5f9;
    border-top: 1px solid #e2e8f0;
    font-size: 0.75rem;
    color: #64748b;
  }

  .status-left,
  .status-center,
  .status-right {
    display: flex;
    align-items: center;
    gap: 1rem;
  }

  .version {
    font-weight: 500;
    color: #475569;
  }

  .status-indicator {
    display: flex;
    align-items: center;
    gap: 0.25rem;
  }

  .current-time {
    color: #475569;
  }

  .last-login {
    color: #64748b;
  }
</style>
