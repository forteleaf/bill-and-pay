import { apiClient } from './client';
import type { ApiResponse, PagedResponse } from '@/types/api';
import type {
  TerminalDto,
  TerminalCreateRequest,
  TerminalUpdateRequest,
  TerminalListParams
} from '@/types/terminal';

class TerminalApi {
  async create(data: TerminalCreateRequest): Promise<ApiResponse<TerminalDto>> {
    return apiClient.post<TerminalDto>('/terminals', data);
  }

  async getTerminals(params: TerminalListParams = {}): Promise<ApiResponse<PagedResponse<TerminalDto>>> {
    const queryParams = new URLSearchParams();
    if (params.page !== undefined) queryParams.set('page', params.page.toString());
    if (params.size !== undefined) queryParams.set('size', params.size.toString());
    if (params.status) queryParams.set('status', params.status);
    if (params.terminalType) queryParams.set('terminalType', params.terminalType);
    if (params.merchantId) queryParams.set('merchantId', params.merchantId);
    if (params.organizationId) queryParams.set('organizationId', params.organizationId);
    if (params.search) queryParams.set('search', params.search);

    const queryString = queryParams.toString();
    const endpoint = `/terminals${queryString ? `?${queryString}` : ''}`;

    return apiClient.get<PagedResponse<TerminalDto>>(endpoint);
  }

  async getTerminalById(id: string): Promise<ApiResponse<TerminalDto>> {
    return apiClient.get<TerminalDto>(`/terminals/${id}`);
  }

  async getTerminalByTid(tid: string): Promise<ApiResponse<TerminalDto>> {
    return apiClient.get<TerminalDto>(`/terminals/tid/${tid}`);
  }

  async getTerminalsByMerchant(merchantId: string): Promise<ApiResponse<TerminalDto[]>> {
    return apiClient.get<TerminalDto[]>(`/terminals/merchant/${merchantId}`);
  }

  async update(id: string, data: TerminalUpdateRequest): Promise<ApiResponse<TerminalDto>> {
    return apiClient.put<TerminalDto>(`/terminals/${id}`, data);
  }

  async delete(id: string): Promise<ApiResponse<void>> {
    return apiClient.delete<void>(`/terminals/${id}`);
  }
}

export const terminalApi = new TerminalApi();
