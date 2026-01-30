<script lang="ts">
  interface Props {
    navigate: (route: string) => void;
  }
  
  let { navigate }: Props = $props();
</script>

<aside class="sidebar">
  <div class="logo">
    <h1>Bill&Pay</h1>
  </div>
  
  <nav>
    <button onclick={() => navigate('dashboard')}>🏦 대시보드</button>
    <button onclick={() => navigate('transactions')}>🧮 거래 내역</button>
    <button onclick={() => navigate('settlements')}>💸 정산 내역</button>
    <button onclick={() => navigate('settlement-batches')}>📦 정산 배치</button>
    <button onclick={() => navigate('settlement-summary')}>📊 정산 요약</button>
    <button onclick={() => navigate('merchants')}>🏪 가맹점 관리</button>
    <button onclick={() => navigate('organizations')}>🏢 조직 관리</button>
  </nav>
</aside>

<style>
  .sidebar {
    width: 250px;
    background-color: #1a1a1a;
    color: white;
    padding: 1rem;
  }
  
  .logo {
    padding: 1rem;
    margin-bottom: 2rem;
  }
  
  .logo h1 {
    font-size: 1.5rem;
    font-weight: bold;
  }
  
  nav {
    display: flex;
    flex-direction: column;
    gap: 0.5rem;
  }
  
  nav button {
    padding: 0.75rem 1rem;
    text-align: left;
    background: none;
    border: none;
    color: #ccc;
    cursor: pointer;
    border-radius: 0.375rem;
    transition: background-color 0.2s;
  }
  
  nav button:hover {
    background-color: #333;
    color: white;
  }
</style>
