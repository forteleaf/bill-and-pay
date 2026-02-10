import { apiClient } from './client';
import type { ApiResponse, PagedResponse } from '@/types/api';
import type {
  MerchantPgMappingDto,
  MerchantPgMappingCreateRequest,
  MerchantPgMappingUpdateRequest
} from '@/types/merchantPgMapping';
import { MerchantPgMappingStatus } from '@/types/merchantPgMapping';

export interface MerchantPgMappingListParams {
  page?: number;
  size?: number;
}

class MerchantPgMappingApi {
  async getAll(params: MerchantPgMappingListParams = {}): Promise<ApiResponse<PagedResponse<MerchantPgMappingDto>>> {
    const queryParams = new URLSearchParams();
    if (params.page !== undefined) queryParams.set('page', params.page.toString());
    if (params.size !== undefined) queryParams.set('size', params.size.toString());

    const queryString = queryParams.toString();
    const endpoint = `/merchant-pg-mappings${queryString ? `?${queryString}` : ''}`;

    return apiClient.get<PagedResponse<MerchantPgMappingDto>>(endpoint);
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
