import { apiClient } from './api';

class TenantStore {
  current = $state<string>('');

  setCurrent(tenantId: string) {
    this.current = tenantId;
    apiClient.setTenantId(tenantId);
  }
}

export const tenantStore = new TenantStore();
