import { apiClient } from './api';
import type { ApiResponse, PagedResponse } from '../types/api';
import type {
  PgConnectionDto,
  PgConnectionCreateRequest,
  PgConnectionUpdateRequest
} from '../types/pgConnection';
import { PgConnectionStatus } from '../types/pgConnection';

class PgConnectionApi {
  async getAll(page = 0, size = 50): Promise<ApiResponse<PagedResponse<PgConnectionDto>>> {
    return apiClient.get<PagedResponse<PgConnectionDto>>(
      `/pg-connections?page=${page}&size=${size}`
    );
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
