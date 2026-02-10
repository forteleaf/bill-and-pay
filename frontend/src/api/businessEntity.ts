import { apiClient } from './client';
import type { ApiResponse, PagedResponse } from '@/types/api';
import type {
  BusinessEntity,
  BusinessEntityCreateRequest
} from '@/types/branch';

class BusinessEntityApi {
  async getAll(params: { page?: number; size?: number } = {}): Promise<ApiResponse<PagedResponse<BusinessEntity>>> {
    const queryParams = new URLSearchParams();
    if (params.page !== undefined) queryParams.set('page', params.page.toString());
    if (params.size !== undefined) queryParams.set('size', params.size.toString());

    const queryString = queryParams.toString();
    const endpoint = `/business-entities${queryString ? `?${queryString}` : ''}`;

    return apiClient.get<PagedResponse<BusinessEntity>>(endpoint);
  }

  async getById(id: string): Promise<ApiResponse<BusinessEntity>> {
    return apiClient.get<BusinessEntity>(`/business-entities/${id}`);
  }

  async searchByBusinessNumber(businessNumber: string): Promise<ApiResponse<BusinessEntity | null>> {
    const cleanNumber = businessNumber.replace(/-/g, '');
    const formattedNumber = `${cleanNumber.slice(0, 3)}-${cleanNumber.slice(3, 5)}-${cleanNumber.slice(5)}`;
    return apiClient.get<BusinessEntity | null>(`/business-entities/search?businessNumber=${encodeURIComponent(formattedNumber)}`);
  }

  async searchByName(name: string): Promise<ApiResponse<BusinessEntity[]>> {
    return apiClient.get<BusinessEntity[]>(`/business-entities/search/name?name=${encodeURIComponent(name)}`);
  }

  async create(data: BusinessEntityCreateRequest): Promise<ApiResponse<BusinessEntity>> {
    return apiClient.post<BusinessEntity>('/business-entities', data);
  }

  async update(id: string, data: BusinessEntityCreateRequest): Promise<ApiResponse<BusinessEntity>> {
    return apiClient.put<BusinessEntity>(`/business-entities/${id}`, data);
  }
}

export const businessEntityApi = new BusinessEntityApi();
