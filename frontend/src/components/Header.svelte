<script lang="ts">
  import { tenantStore } from '../lib/stores';
  
  let currentTenant = $derived(tenantStore.current);
</script>

<header>
  <div class="header-content">
    <h2>정산 관리 시스템</h2>
    <div class="tenant-selector">
      <label for="tenant">테넌트:</label>
      <select id="tenant" value={currentTenant} onchange={(e) => tenantStore.setCurrent(e.currentTarget.value)}>
        <option value="">선택하세요</option>
        <option value="tenant_001">Tenant 001</option>
        <option value="tenant_002">Tenant 002</option>
      </select>
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
  
  .tenant-selector {
    display: flex;
    align-items: center;
    gap: 0.5rem;
  }
  
  label {
    font-weight: 500;
  }
  
  select {
    padding: 0.5rem 1rem;
    border: 1px solid #ccc;
    border-radius: 0.375rem;
    background-color: white;
  }
</style>
