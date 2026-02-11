import type { ApiResponse } from '@/types/api';
import { authStore } from '@/stores/auth';
import type {
  PlatformAuthResponse,
  TenantDto,
  PlatformDashboard,
  PlatformAdminDto,
  PlatformAnnouncementDto,
  PlatformAuditLogDto,
  TenantCreateRequest,
  PgConnectionDto,
} from '@/types/platform';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8100/api/v1';

class PlatformApiClient {
  private refreshing: Promise<boolean> | null = null;

  private async refreshAccessToken(): Promise<boolean> {
    if (this.refreshing) return this.refreshing;

    this.refreshing = (async () => {
      try {
        const refreshToken = authStore.getRefreshToken();
        if (!refreshToken) return false;

        const response = await fetch(`${API_BASE_URL}/platform/auth/refresh`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ refreshToken })
        });

        if (!response.ok) return false;

        const result = await response.json();
        if (result.success && result.data) {
          authStore.updateTokens(result.data.accessToken, result.data.refreshToken);
          return true;
        }
        return false;
      } catch (error) {
        console.error('Platform token refresh failed:', error);
        return false;
      } finally {
        this.refreshing = null;
      }
    })();

    return this.refreshing;
  }

  private async request<T>(
    endpoint: string,
    options: RequestInit = {},
    isRetry = false
  ): Promise<ApiResponse<T>> {
    const headers: Record<string, string> = {
      'Content-Type': 'application/json',
    };

    const accessToken = authStore.getAccessToken();
    if (accessToken) {
      headers['Authorization'] = `Bearer ${accessToken}`;
    }

    const response = await fetch(`${API_BASE_URL}${endpoint}`, {
      ...options,
      headers
    });

    if (response.status === 401 && !isRetry) {
      const refreshSuccess = await this.refreshAccessToken();
      if (refreshSuccess) {
        return this.request<T>(endpoint, options, true);
      }
      authStore.logout();
      throw new Error('인증이 만료되었습니다. 다시 로그인하세요.');
    }

    if (response.status === 403) {
      throw new Error('접근 권한이 없습니다.');
    }

    return response.json();
  }

  // Auth
  async login(username: string, password: string): Promise<ApiResponse<PlatformAuthResponse>> {
    const response = await fetch(`${API_BASE_URL}/platform/auth/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username, password })
    });
    return response.json();
  }

  // Dashboard
  async getDashboard(): Promise<ApiResponse<PlatformDashboard>> {
    return this.request('/platform/dashboard/overview');
  }

  // Tenants
  async getTenants(page = 0, size = 20, status?: string): Promise<ApiResponse<any>> {
    const params = new URLSearchParams({ page: String(page), size: String(size) });
    if (status) params.append('status', status);
    return this.request(`/platform/tenants?${params}`);
  }

  async getTenant(id: string): Promise<ApiResponse<TenantDto>> {
    return this.request(`/platform/tenants/${id}`);
  }

  async createTenant(data: TenantCreateRequest): Promise<ApiResponse<TenantDto>> {
    return this.request('/platform/tenants', {
      method: 'POST',
      body: JSON.stringify(data)
    });
  }

  async updateTenant(id: string, data: Partial<TenantDto>): Promise<ApiResponse<TenantDto>> {
    return this.request(`/platform/tenants/${id}`, {
      method: 'PUT',
      body: JSON.stringify(data)
    });
  }

  async suspendTenant(id: string, reason: string): Promise<ApiResponse<TenantDto>> {
    return this.request(`/platform/tenants/${id}/suspend`, {
      method: 'PATCH',
      body: JSON.stringify({ reason })
    });
  }

  async activateTenant(id: string): Promise<ApiResponse<TenantDto>> {
    return this.request(`/platform/tenants/${id}/activate`, {
      method: 'PATCH'
    });
  }

  // Admins
  async getAdmins(page = 0, size = 20): Promise<ApiResponse<any>> {
    return this.request(`/platform/admins?page=${page}&size=${size}`);
  }

  async createAdmin(data: any): Promise<ApiResponse<PlatformAdminDto>> {
    return this.request('/platform/admins', {
      method: 'POST',
      body: JSON.stringify(data)
    });
  }

  async updateAdmin(id: string, data: any): Promise<ApiResponse<PlatformAdminDto>> {
    return this.request(`/platform/admins/${id}`, {
      method: 'PUT',
      body: JSON.stringify(data)
    });
  }

  async resetAdminPassword(id: string, password: string): Promise<ApiResponse<void>> {
    return this.request(`/platform/admins/${id}/reset-password`, {
      method: 'PATCH',
      body: JSON.stringify({ password })
    });
  }

  // Announcements
  async getAnnouncements(page = 0, size = 20, status?: string): Promise<ApiResponse<any>> {
    const params = new URLSearchParams({ page: String(page), size: String(size) });
    if (status) params.append('status', status);
    return this.request(`/platform/announcements?${params}`);
  }

  async getAnnouncement(id: string): Promise<ApiResponse<PlatformAnnouncementDto>> {
    return this.request(`/platform/announcements/${id}`);
  }

  async createAnnouncement(data: Partial<PlatformAnnouncementDto>): Promise<ApiResponse<PlatformAnnouncementDto>> {
    return this.request('/platform/announcements', {
      method: 'POST',
      body: JSON.stringify(data)
    });
  }

  async updateAnnouncement(id: string, data: Partial<PlatformAnnouncementDto>): Promise<ApiResponse<PlatformAnnouncementDto>> {
    return this.request(`/platform/announcements/${id}`, {
      method: 'PUT',
      body: JSON.stringify(data)
    });
  }

  async deleteAnnouncement(id: string): Promise<ApiResponse<void>> {
    return this.request(`/platform/announcements/${id}`, { method: 'DELETE' });
  }

  // Audit Logs
  async getAuditLogs(page = 0, size = 20, tenantId?: string): Promise<ApiResponse<any>> {
    const params = new URLSearchParams({ page: String(page), size: String(size) });
    if (tenantId) params.append('tenantId', tenantId);
    return this.request(`/platform/audit-logs?${params}`);
  }

  // Monitoring
  async getPgStatus(): Promise<ApiResponse<PgConnectionDto[]>> {
    return this.request('/platform/monitoring/pg-status');
  }
}

export const platformApi = new PlatformApiClient();
