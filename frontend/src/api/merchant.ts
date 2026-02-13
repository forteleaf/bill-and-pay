import { apiClient } from './client';
import { buildEndpoint } from './queryUtils';
import type { ApiResponse, PagedResponse } from '@/types/api';
import type {
  MerchantDto,
  MerchantCreateRequest,
  MerchantUpdateRequest,
  MerchantListParams,
  MerchantStatisticsDto,
  AccessibleOrganization,
  BlacklistCheckResponse
} from '@/types/merchant';

class MerchantApi {
  async getAccessibleOrganizations(): Promise<ApiResponse<AccessibleOrganization[]>> {
    return apiClient.get<AccessibleOrganization[]>('/merchants/accessible-organizations');
  }

  async checkBlacklist(businessNumber: string): Promise<ApiResponse<BlacklistCheckResponse>> {
    const cleanNumber = businessNumber.replace(/-/g, '');
    return apiClient.get<BlacklistCheckResponse>(`/merchants/blacklist-check?businessNumber=${encodeURIComponent(cleanNumber)}`);
  }

  async create(data: MerchantCreateRequest): Promise<ApiResponse<MerchantDto>> {
    return apiClient.post<MerchantDto>('/merchants', data);
  }

  async getMerchants(params: MerchantListParams = {}): Promise<ApiResponse<PagedResponse<MerchantDto>>> {
    return apiClient.get<PagedResponse<MerchantDto>>(buildEndpoint('/merchants', {
      page: params.page,
      size: params.size,
      status: params.status,
      businessType: params.businessType,
      organizationId: params.organizationId,
      search: params.search,
    }));
  }

  async getMerchantById(id: string): Promise<ApiResponse<MerchantDto>> {
    return apiClient.get<MerchantDto>(`/merchants/${id}`);
  }

  async update(id: string, data: MerchantUpdateRequest): Promise<ApiResponse<MerchantDto>> {
    return apiClient.put<MerchantDto>(`/merchants/${id}`, data);
  }

  async delete(id: string): Promise<ApiResponse<void>> {
    return apiClient.delete<void>(`/merchants/${id}`);
  }

  async getStatistics(id: string): Promise<ApiResponse<MerchantStatisticsDto>> {
    return apiClient.get<MerchantStatisticsDto>(`/merchants/${id}/statistics`);
  }
}

export const merchantApi = new MerchantApi();
