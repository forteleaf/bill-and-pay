import { apiClient } from './api';
import type { 
  ApiResponse, 
  PagedResponse, 
  Settlement, 
  SettlementSummary, 
  SettlementBatch,
  OrganizationSettlementSummary,
  OrganizationSettlementDetail
} from '../types/api';

function toApiDateFormat(date: string | undefined): string | undefined {
  if (!date) return undefined;
  return date.replace(/\//g, '-');
}

export interface SettlementListParams {
  page?: number;
  size?: number;
  entityType?: string;
  status?: string;
  startDate?: string;
  endDate?: string;
  sortBy?: string;
  direction?: 'ASC' | 'DESC';
}

export interface SettlementSummaryParams {
  entityType?: string;
  startDate?: string;
  endDate?: string;
}

export interface SettlementBatchListParams {
  page?: number;
  size?: number;
  startDate?: string;
  endDate?: string;
  status?: string;
}

export interface OrganizationSettlementParams {
  orgType?: string;
  search?: string;
  startDate?: string;
  endDate?: string;
}

class SettlementApi {
  async getSettlements(params: SettlementListParams = {}): Promise<ApiResponse<PagedResponse<Settlement>>> {
    const queryParams = new URLSearchParams();
    
    if (params.page !== undefined) queryParams.set('page', params.page.toString());
    if (params.size !== undefined) queryParams.set('size', params.size.toString());
    if (params.entityType) queryParams.set('entityType', params.entityType);
    if (params.status) queryParams.set('status', params.status);
    if (params.sortBy) queryParams.set('sortBy', params.sortBy);
    if (params.direction) queryParams.set('direction', params.direction);
    
    const startDateApi = toApiDateFormat(params.startDate);
    const endDateApi = toApiDateFormat(params.endDate);
    
    if (startDateApi) {
      queryParams.set('startDate', `${startDateApi}T00:00:00+09:00`);
    }
    if (endDateApi) {
      queryParams.set('endDate', `${endDateApi}T23:59:59+09:00`);
    }
    
    const queryString = queryParams.toString();
    const endpoint = `/settlements${queryString ? `?${queryString}` : ''}`;
    
    return apiClient.get<PagedResponse<Settlement>>(endpoint);
  }

  async getSummary(params: SettlementSummaryParams = {}): Promise<ApiResponse<SettlementSummary>> {
    const queryParams = new URLSearchParams();
    
    if (params.entityType) queryParams.set('entityType', params.entityType);
    
    const startDateApi = toApiDateFormat(params.startDate);
    const endDateApi = toApiDateFormat(params.endDate);
    
    if (startDateApi) {
      queryParams.set('startDate', `${startDateApi}T00:00:00+09:00`);
    }
    if (endDateApi) {
      queryParams.set('endDate', `${endDateApi}T23:59:59+09:00`);
    }
    
    const queryString = queryParams.toString();
    const endpoint = `/settlements/summary${queryString ? `?${queryString}` : ''}`;
    
    return apiClient.get<SettlementSummary>(endpoint);
  }

  async getDailyBatchReport(date: string): Promise<ApiResponse<Settlement[]>> {
    const dateApi = toApiDateFormat(date) || date;
    return apiClient.get<Settlement[]>(`/settlements/batch/${dateApi}`);
  }

  async getBatches(params: SettlementBatchListParams = {}): Promise<ApiResponse<PagedResponse<SettlementBatch>>> {
    const queryParams = new URLSearchParams();
    
    if (params.page !== undefined) queryParams.set('page', params.page.toString());
    if (params.size !== undefined) queryParams.set('size', params.size.toString());
    if (params.status) queryParams.set('status', params.status);
    
    const startDateApi = toApiDateFormat(params.startDate);
    const endDateApi = toApiDateFormat(params.endDate);
    
    if (startDateApi) queryParams.set('startDate', startDateApi);
    if (endDateApi) queryParams.set('endDate', endDateApi);
    
    const queryString = queryParams.toString();
    const endpoint = `/settlements/batches${queryString ? `?${queryString}` : ''}`;
    
    return apiClient.get<PagedResponse<SettlementBatch>>(endpoint);
  }

  async getSettlementById(id: string): Promise<ApiResponse<Settlement>> {
    return apiClient.get<Settlement>(`/settlements/${id}`);
  }

  async getSettlementsByOrganization(params: OrganizationSettlementParams = {}): Promise<ApiResponse<OrganizationSettlementSummary[]>> {
    const queryParams = new URLSearchParams();
    
    if (params.orgType) queryParams.set('orgType', params.orgType);
    if (params.search) queryParams.set('search', params.search);
    
    const startDateApi = toApiDateFormat(params.startDate);
    const endDateApi = toApiDateFormat(params.endDate);
    
    if (startDateApi) {
      queryParams.set('startDate', `${startDateApi}T00:00:00+09:00`);
    }
    if (endDateApi) {
      queryParams.set('endDate', `${endDateApi}T23:59:59+09:00`);
    }
    
    const queryString = queryParams.toString();
    const endpoint = `/settlements/by-organization${queryString ? `?${queryString}` : ''}`;
    
    return apiClient.get<OrganizationSettlementSummary[]>(endpoint);
  }

  async getOrganizationSettlementDetail(
    organizationId: string, 
    startDate?: string, 
    endDate?: string
  ): Promise<ApiResponse<OrganizationSettlementDetail>> {
    const queryParams = new URLSearchParams();
    
    const startDateApi = toApiDateFormat(startDate);
    const endDateApi = toApiDateFormat(endDate);
    
    if (startDateApi) {
      queryParams.set('startDate', `${startDateApi}T00:00:00+09:00`);
    }
    if (endDateApi) {
      queryParams.set('endDate', `${endDateApi}T23:59:59+09:00`);
    }
    
    const queryString = queryParams.toString();
    const endpoint = `/settlements/organization/${organizationId}/details${queryString ? `?${queryString}` : ''}`;
    
    return apiClient.get<OrganizationSettlementDetail>(endpoint);
  }
}

export const settlementApi = new SettlementApi();
