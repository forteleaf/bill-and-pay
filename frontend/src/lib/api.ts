import type { ApiResponse, PagedResponse } from '../types/api';
import { authStore } from './authStore';

const API_BASE_URL = 'http://localhost:8080/api/v1';

class ApiClient {
  private tenantId: string | null = null;
  private refreshing: Promise<boolean> | null = null;

  setTenantId(tenantId: string) {
    this.tenantId = tenantId;
  }

  private async refreshAccessToken(): Promise<boolean> {
    if (this.refreshing) {
      return this.refreshing;
    }

    this.refreshing = (async () => {
      try {
        const refreshToken = authStore.getRefreshToken();
        if (!refreshToken) {
          return false;
        }

        const response = await fetch(`${API_BASE_URL}/auth/refresh`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ refreshToken })
        });

        if (!response.ok) {
          return false;
        }

        const result = await response.json();
        if (result.success && result.data) {
          authStore.updateTokens(result.data.accessToken, result.data.refreshToken);
          return true;
        }

        return false;
      } catch (error) {
        console.error('Token refresh failed:', error);
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
    const headers: HeadersInit = {
      'Content-Type': 'application/json',
      ...options.headers
    };

    const accessToken = authStore.getAccessToken();
    if (accessToken) {
      headers['Authorization'] = `Bearer ${accessToken}`;
    }

    const tenantId = this.tenantId || authStore.getTenantId();
    if (tenantId) {
      headers['X-Tenant-ID'] = tenantId;
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
      if (typeof window !== 'undefined') {
        window.location.href = '/login';
      }
      throw new Error('Authentication failed. Please login again.');
    }

    if (!response.ok) {
      throw new Error(`API Error: ${response.status}`);
    }

    return response.json();
  }

  async get<T>(endpoint: string): Promise<ApiResponse<T>> {
    return this.request<T>(endpoint, { method: 'GET' });
  }

  async post<T>(endpoint: string, data: any): Promise<ApiResponse<T>> {
    return this.request<T>(endpoint, {
      method: 'POST',
      body: JSON.stringify(data)
    });
  }

  async put<T>(endpoint: string, data: any): Promise<ApiResponse<T>> {
    return this.request<T>(endpoint, {
      method: 'PUT',
      body: JSON.stringify(data)
    });
  }

  async delete<T>(endpoint: string): Promise<ApiResponse<T>> {
    return this.request<T>(endpoint, { method: 'DELETE' });
  }
}

export const apiClient = new ApiClient();
