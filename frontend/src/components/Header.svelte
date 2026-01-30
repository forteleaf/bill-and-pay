<script lang="ts">
  import { authStore } from '../lib/authStore';
  
  let username = $state('');
  let tenantId = $state('');
  
  $effect(() => {
    username = authStore.getUsername() || '';
    tenantId = authStore.getTenantId() || '';
  });
  
  function handleLogout() {
    if (confirm('로그아웃 하시겠습니까?')) {
      authStore.logout();
      window.location.href = '/';
    }
  }
</script>

<header>
  <div class="header-content">
    <h2>정산 관리 시스템</h2>
    <div class="user-info">
      <div class="user-details">
        <span class="username">{username}</span>
        <span class="tenant-id">{tenantId}</span>
      </div>
      <button class="logout-button" onclick={handleLogout}>
        로그아웃
      </button>
    </div>
  </div>
</header>

<style>
  header {
    background-color: white;
    border-bottom: 1px solid #e0e0e0;
    padding: 1rem 2rem;
  }
  
  .header-content {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }
  
  h2 {
    font-size: 1.25rem;
    font-weight: 600;
  }
  
  .user-info {
    display: flex;
    align-items: center;
    gap: 1.5rem;
  }
  
  .user-details {
    display: flex;
    flex-direction: column;
    align-items: flex-end;
    gap: 0.25rem;
  }
  
  .username {
    font-weight: 600;
    color: #1a202c;
    font-size: 0.95rem;
  }
  
  .tenant-id {
    font-size: 0.8rem;
    color: #718096;
  }
  
  .logout-button {
    padding: 0.5rem 1.25rem;
    font-size: 0.875rem;
    font-weight: 500;
    color: #fff;
    background-color: #e53e3e;
    border: none;
    border-radius: 0.375rem;
    cursor: pointer;
    transition: background-color 0.2s;
  }
  
  .logout-button:hover {
    background-color: #c53030;
  }
  
  .logout-button:active {
    background-color: #9b2c2c;
  }
</style>
