import { apiClient } from './client';
import { buildEndpoint } from './queryUtils';
import type { ApiResponse, PagedResponse } from '@/types/api';
import type {
  BusinessEntity,
  BusinessEntityCreateRequest
} from '@/types/branch';

class BusinessEntityApi {
  async getAll(params: { page?: number; size?: number } = {}): Promise<ApiResponse<PagedResponse<BusinessEntity>>> {
    return apiClient.get<PagedResponse<BusinessEntity>>(buildEndpoint('/business-entities', {
      page: params.page,
      size: params.size,
    }));
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
