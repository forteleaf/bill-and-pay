import { apiClient } from './client';
import type { ApiResponse, PagedResponse } from '@/types/api';
import type {
  PgConnectionDto,
  PgConnectionCreateRequest,
  PgConnectionUpdateRequest
} from '@/types/pgConnection';
import { PgConnectionStatus } from '@/types/pgConnection';

export interface PgConnectionListParams {
  page?: number;
  size?: number;
}

class PgConnectionApi {
  async getAll(params: PgConnectionListParams = {}): Promise<ApiResponse<PagedResponse<PgConnectionDto>>> {
    const queryParams = new URLSearchParams();
    if (params.page !== undefined) queryParams.set('page', params.page.toString());
    if (params.size !== undefined) queryParams.set('size', params.size.toString());

    const queryString = queryParams.toString();
    const endpoint = `/pg-connections${queryString ? `?${queryString}` : ''}`;

    return apiClient.get<PagedResponse<PgConnectionDto>>(endpoint);
  }

  async getById(id: number): Promise<ApiResponse<PgConnectionDto>> {
    return apiClient.get<PgConnectionDto>(`/pg-connections/${id}`);
  }

  async getByPgCode(pgCode: string): Promise<ApiResponse<PgConnectionDto>> {
    return apiClient.get<PgConnectionDto>(`/pg-connections/code/${pgCode}`);
  }

  async create(data: PgConnectionCreateRequest): Promise<ApiResponse<PgConnectionDto>> {
    return apiClient.post<PgConnectionDto>('/pg-connections', data);
  }

  async update(id: number, data: PgConnectionUpdateRequest): Promise<ApiResponse<PgConnectionDto>> {
    return apiClient.put<PgConnectionDto>(`/pg-connections/${id}`, data);
  }

  async updateStatus(id: number, status: PgConnectionStatus): Promise<ApiResponse<PgConnectionDto>> {
    return apiClient.patch<PgConnectionDto>(`/pg-connections/${id}/status?status=${status}`);
  }

  async delete(id: number): Promise<ApiResponse<void>> {
    return apiClient.delete<void>(`/pg-connections/${id}`);
  }
}

export const pgConnectionApi = new PgConnectionApi();
