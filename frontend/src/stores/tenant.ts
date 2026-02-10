import { apiClient } from '@/api/client';

class TenantStore {
  current: string = '';

  setCurrent(tenantId: string) {
    this.current = tenantId;
    apiClient.setTenantId(tenantId);
  }
}

export const tenantStore = new TenantStore();
