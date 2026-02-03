import type { ApiResponse } from '../types/api';
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
    const headers: Record<string, string> = {
      'Content-Type': 'application/json',
    };

    if (options.headers) {
      Object.assign(headers, options.headers as Record<string, string>);
    }

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
      throw new Error('Authentication failed. Please login again.');
    }

    if (response.status === 403) {
      authStore.logout();
      throw new Error('Access denied. Please login again.');
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

  async patch<T>(endpoint: string, data?: any): Promise<ApiResponse<T>> {
    return this.request<T>(endpoint, {
      method: 'PATCH',
      body: data ? JSON.stringify(data) : undefined
    });
  }
}

export const apiClient = new ApiClient();
