import { apiClient } from './api';
import type { ApiResponse, PagedResponse } from '../types/api';
import type {
  MerchantPgMappingDto,
  MerchantPgMappingCreateRequest,
  MerchantPgMappingUpdateRequest
} from '../types/merchantPgMapping';
import { MerchantPgMappingStatus } from '../types/merchantPgMapping';

class MerchantPgMappingApi {
  async getAll(page = 0, size = 20): Promise<ApiResponse<PagedResponse<MerchantPgMappingDto>>> {
    return apiClient.get<PagedResponse<MerchantPgMappingDto>>(
      `/merchant-pg-mappings?page=${page}&size=${size}`
    );
  }

  async getById(id: string): Promise<ApiResponse<MerchantPgMappingDto>> {
    return apiClient.get<MerchantPgMappingDto>(`/merchant-pg-mappings/${id}`);
  }

  async getByMerchantId(merchantId: string): Promise<ApiResponse<MerchantPgMappingDto[]>> {
    return apiClient.get<MerchantPgMappingDto[]>(`/merchant-pg-mappings/merchant/${merchantId}`);
  }

  async create(data: MerchantPgMappingCreateRequest): Promise<ApiResponse<MerchantPgMappingDto>> {
    return apiClient.post<MerchantPgMappingDto>('/merchant-pg-mappings', data);
  }

  async update(id: string, data: MerchantPgMappingUpdateRequest): Promise<ApiResponse<MerchantPgMappingDto>> {
    return apiClient.put<MerchantPgMappingDto>(`/merchant-pg-mappings/${id}`, data);
  }

  async updateStatus(id: string, status: MerchantPgMappingStatus): Promise<ApiResponse<MerchantPgMappingDto>> {
    return apiClient.patch<MerchantPgMappingDto>(`/merchant-pg-mappings/${id}/status?status=${status}`);
  }

  async delete(id: string): Promise<ApiResponse<void>> {
    return apiClient.delete<void>(`/merchant-pg-mappings/${id}`);
  }
}

export const merchantPgMappingApi = new MerchantPgMappingApi();
