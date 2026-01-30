import { apiClient } from './api';

class TenantStore {
  current: string = '';

  setCurrent(tenantId: string) {
    this.current = tenantId;
    apiClient.setTenantId(tenantId);
  }
}

export const tenantStore = new TenantStore();
