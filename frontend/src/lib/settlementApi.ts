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
import type {
  SettlementListParams,
  SettlementSummaryParams,
  SettlementBatchListParams,
  OrganizationSettlementParams
} from '../types/settlement';
import { toApiDateTime } from './utils';

// Re-export types for convenience
export type {
  SettlementListParams,
  SettlementSummaryParams,
  SettlementBatchListParams,
  OrganizationSettlementParams
};

class SettlementApi {
  async getSettlements(params: SettlementListParams = {}): Promise<ApiResponse<PagedResponse<Settlement>>> {
    const queryParams = new URLSearchParams();

    if (params.page !== undefined) queryParams.set('page', params.page.toString());
    if (params.size !== undefined) queryParams.set('size', params.size.toString());
    if (params.entityType) queryParams.set('entityType', params.entityType);
    if (params.status) queryParams.set('status', params.status);
    if (params.sortBy) queryParams.set('sortBy', params.sortBy);
    if (params.direction) queryParams.set('direction', params.direction);

    const startDateTime = toApiDateTime(params.startDate, '00:00:00');
    const endDateTime = toApiDateTime(params.endDate, '23:59:59');

    if (startDateTime) queryParams.set('startDate', startDateTime);
    if (endDateTime) queryParams.set('endDate', endDateTime);

    const queryString = queryParams.toString();
    const endpoint = `/settlements${queryString ? `?${queryString}` : ''}`;

    return apiClient.get<PagedResponse<Settlement>>(endpoint);
  }

  async getSummary(params: SettlementSummaryParams = {}): Promise<ApiResponse<SettlementSummary>> {
    const queryParams = new URLSearchParams();

    if (params.entityType) queryParams.set('entityType', params.entityType);

    const startDateTime = toApiDateTime(params.startDate, '00:00:00');
    const endDateTime = toApiDateTime(params.endDate, '23:59:59');

    if (startDateTime) queryParams.set('startDate', startDateTime);
    if (endDateTime) queryParams.set('endDate', endDateTime);

    const queryString = queryParams.toString();
    const endpoint = `/settlements/summary${queryString ? `?${queryString}` : ''}`;

    return apiClient.get<SettlementSummary>(endpoint);
  }

  async getDailyBatchReport(date: string): Promise<ApiResponse<Settlement[]>> {
    const dateApi = date.replace(/\//g, '-');
    return apiClient.get<Settlement[]>(`/settlements/batch/${dateApi}`);
  }

  async getBatches(params: SettlementBatchListParams = {}): Promise<ApiResponse<PagedResponse<SettlementBatch>>> {
    const queryParams = new URLSearchParams();

    if (params.page !== undefined) queryParams.set('page', params.page.toString());
    if (params.size !== undefined) queryParams.set('size', params.size.toString());
    if (params.status) queryParams.set('status', params.status);

    const startDateApi = params.startDate?.replace(/\//g, '-');
    const endDateApi = params.endDate?.replace(/\//g, '-');

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

    const startDateTime = toApiDateTime(params.startDate, '00:00:00');
    const endDateTime = toApiDateTime(params.endDate, '23:59:59');

    if (startDateTime) queryParams.set('startDate', startDateTime);
    if (endDateTime) queryParams.set('endDate', endDateTime);

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

    const startDateTime = toApiDateTime(startDate, '00:00:00');
    const endDateTime = toApiDateTime(endDate, '23:59:59');

    if (startDateTime) queryParams.set('startDate', startDateTime);
    if (endDateTime) queryParams.set('endDate', endDateTime);

    const queryString = queryParams.toString();
    const endpoint = `/settlements/organization/${organizationId}/details${queryString ? `?${queryString}` : ''}`;

    return apiClient.get<OrganizationSettlementDetail>(endpoint);
  }
}

export const settlementApi = new SettlementApi();
