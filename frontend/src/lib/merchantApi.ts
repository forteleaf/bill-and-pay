import { apiClient } from './api';
import type { ApiResponse, PagedResponse } from '../types/api';
import type {
  MerchantDto,
  MerchantCreateRequest,
  MerchantUpdateRequest,
  MerchantListParams,
  AccessibleOrganization,
  BlacklistCheckResponse
} from '../types/merchant';

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
    const queryParams = new URLSearchParams();
    if (params.page !== undefined) queryParams.set('page', params.page.toString());
    if (params.size !== undefined) queryParams.set('size', params.size.toString());
    if (params.status) queryParams.set('status', params.status);
    if (params.businessType) queryParams.set('businessType', params.businessType);
    if (params.organizationId) queryParams.set('organizationId', params.organizationId);
    if (params.search) queryParams.set('search', params.search);

    const queryString = queryParams.toString();
    const endpoint = `/merchants${queryString ? `?${queryString}` : ''}`;

    return apiClient.get<PagedResponse<MerchantDto>>(endpoint);
  }

  async getMerchantById(id: string): Promise<ApiResponse<MerchantDto>> {
    return apiClient.get<MerchantDto>(`/merchants/${id}`);
  }

  async update(id: string, data: MerchantUpdateRequest): Promise<ApiResponse<MerchantDto>> {
    return apiClient.put<MerchantDto>(`/merchants/${id}`, data);
  }
}

export const merchantApi = new MerchantApi();
